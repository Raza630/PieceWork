const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Cloud Function: Triggered when a workOffer document is updated.
 * Sends a push notification to the boss when a worker accepts their job,
 * including the worker's name. Also creates an in-app notification record.
 */
exports.sendWorkAcceptedNotification = functions.firestore
    .document("workOffers/{workOfferId}")
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();

      // Only trigger when acceptedBy changes from null/empty to a value
      if (before.acceptedBy === after.acceptedBy || !after.acceptedBy) {
        return null;
      }

      const workOfferId = context.params.workOfferId;
      const workerId = after.acceptedBy;
      const bossId = after.bossId; // Your app stores "bossId" not "createdBy"
      const jobTitle = after.title || "Untitled Job";
      const workerName = after.acceptedByName || "A worker";
      const workerPhoto = after.acceptedByPhoto || "";

      if (!bossId) {
        console.error("No bossId found on work offer", workOfferId);
        return null;
      }

      try {
        // 1. Get boss's FCM token
        const bossDoc = await admin.firestore().collection("users").doc(bossId).get();
        if (!bossDoc.exists) {
          console.error("Boss document not found:", bossId);
          return null;
        }

        const bossData = bossDoc.data();
        const bossToken = bossData.fcmToken;

        // 2. Create in-app notification record
        const notificationData = {
          recipientId: bossId,
          type: "work_accepted",
          title: "Job Accepted! 🎉",
          body: `${workerName} has accepted your job "${jobTitle}"`,
          jobId: workOfferId,
          workerId: workerId,
          workerName: workerName,
          workerPhoto: workerPhoto,
          read: false,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        };

        await admin.firestore().collection("notifications").add(notificationData);
        console.log("In-app notification created for boss:", bossId);

        // 3. Send push notification if FCM token exists
        if (bossToken) {
          const message = {
            notification: {
              title: "Job Accepted! 🎉",
              body: `${workerName} has accepted your job "${jobTitle}"`,
            },
            data: {
              type: "work_accepted",
              jobId: workOfferId,
              workerId: workerId,
              workerName: workerName,
              title: "Job Accepted! 🎉",
              body: `${workerName} has accepted your job "${jobTitle}"`,
            },
            token: bossToken,
          };

          await admin.messaging().send(message);
          console.log("Push notification sent to boss:", bossId);
        } else {
          console.warn("No FCM token for boss:", bossId);
        }

        // 4. Create chat room between boss and worker
        const chatId = bossId < workerId ? `${bossId}_${workerId}` : `${workerId}_${bossId}`;
        const chatRef = admin.firestore().collection("chats").doc(chatId);
        const chatDoc = await chatRef.get();

        // Fetch worker doc once (reused for chat + booking)
        const workerDoc = await admin.firestore().collection("users").doc(workerId).get();
        const workerData = workerDoc.exists ? workerDoc.data() : {};
        const bossName = bossData.firstName ?
          `${bossData.firstName} ${bossData.lastName || ""}`.trim() : "Boss";

        if (!chatDoc.exists) {
          await chatRef.set({
            participants: [bossId, workerId],
            participantNames: {
              [bossId]: bossName,
              [workerId]: workerName || workerData.firstName || "Worker",
            },
            participantPhotos: {
              [bossId]: bossData.photoUrl || "",
              [workerId]: workerPhoto || workerData.photoUrl || "",
            },
            jobId: workOfferId,
            jobTitle: jobTitle,
            lastMessage: `${workerName} accepted the job`,
            lastMessageTime: Date.now(),
            lastActivity: admin.firestore.FieldValue.serverTimestamp(),
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          });
          console.log("Chat room created:", chatId);
        }

        // 5. Create/Update the linked Booking record (Work Offer → Bookings tab)
        // Booking ID == workOfferId for a clean 1:1 link.
        const bookingRef = admin.firestore().collection("bookings").doc(workOfferId);
        const bookingDoc = await bookingRef.get();

        const workerFields = {
          workerId: workerId,
          workerName: workerName || workerData.firstName || "Worker",
          workerPhotoUrl: workerPhoto || workerData.photoUrl || "",
          status: "ACTIVE", // Worker confirmed by accepting
        };

        if (bookingDoc.exists) {
          // PENDING booking created at job-post time → move it to ACTIVE
          await bookingRef.update(workerFields);
          console.log("Booking moved to ACTIVE:", workOfferId);
        } else {
          // Fallback: booking didn't exist (older job) → create it directly as ACTIVE
          await bookingRef.set({
            jobId: workOfferId,
            bossId: bossId,
            bossName: bossName,
            ...workerFields,
            serviceName: jobTitle,
            agreedRate: String(workerData.ratePerHour || after.budget || "Negotiable"),
            date: after.scheduledDate ?
              new Date(after.scheduledDate) :
              admin.firestore.FieldValue.serverTimestamp(),
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          });
          console.log("Booking created (ACTIVE) for job:", workOfferId);
        }

        return null;
      } catch (error) {
        console.error("Error in sendWorkAcceptedNotification:", error);
        return null; // Don't throw — let the function exit gracefully
      }
    });

/**
 * Cloud Function: Notify nearby workers when an URGENT job is posted.
 * Triggered on workOffer creation. Only fires for urgency == "URGENT".
 * Sends push notifications to workers within 25km whose category matches.
 */
exports.notifyNearbyWorkersForUrgentJob = functions.firestore
    .document("workOffers/{workOfferId}")
    .onCreate(async (snapshot, context) => {
      const jobData = snapshot.data();

      // Only notify for urgent jobs
      if (jobData.urgency !== "URGENT") {
        return null;
      }

      const jobLat = jobData.latitude || 0;
      const jobLng = jobData.longitude || 0;
      const jobCategory = jobData.category || "";
      const jobTitle = jobData.title || "Untitled";
      const workOfferId = context.params.workOfferId;
      const RADIUS_KM = 25;

      if (jobLat === 0 && jobLng === 0) {
        console.log("No location on urgent job, skipping notification");
        return null;
      }

      try {
        // Get all workers
        const workersSnapshot = await admin.firestore()
            .collection("users")
            .where("role", "==", "Worker")
            .get();

        const tokensToNotify = [];

        for (const doc of workersSnapshot.docs) {
          const worker = doc.data();
          const workerLat = worker.latitude || 0;
          const workerLng = worker.longitude || 0;
          const workerCategory = worker.category || "";
          const fcmToken = worker.fcmToken;

          if (!fcmToken || workerLat === 0) continue;

          // Check distance (Haversine approximation)
          const distance = haversineDistance(jobLat, jobLng, workerLat, workerLng);
          if (distance > RADIUS_KM) continue;

          // Category match (optional - notify all nearby if category is empty)
          if (jobCategory && workerCategory &&
              !workerCategory.toLowerCase().includes(jobCategory.toLowerCase())) {
            continue;
          }

          tokensToNotify.push(fcmToken);
        }

        if (tokensToNotify.length === 0) {
          console.log("No nearby workers found for urgent job");
          return null;
        }

        // Send multicast notification
        const message = {
          notification: {
            title: "🔴 Urgent Job Near You!",
            body: `"${jobTitle}" needs someone ASAP. Tap to view.`,
          },
          data: {
            type: "urgent_job",
            jobId: workOfferId,
            title: "🔴 Urgent Job Near You!",
            body: `"${jobTitle}" needs someone ASAP.`,
          },
          tokens: tokensToNotify,
        };

        const response = await admin.messaging().sendEachForMulticast(message);
        console.log(
            `Urgent job notification sent to ${response.successCount}/${tokensToNotify.length} workers`
        );

        return null;
      } catch (error) {
        console.error("Error in notifyNearbyWorkersForUrgentJob:", error);
        return null;
      }
    });

/**
 * Cloud Function: Update worker level when a job is marked COMPLETED.
 * Counts all completed jobs for the worker and updates their level field.
 * Also notifies the boss that the job is complete and ready for review.
 */
exports.updateWorkerLevelOnCompletion = functions.firestore
    .document("workOffers/{workOfferId}")
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();

      // Only trigger when status changes to COMPLETED
      if (before.status === after.status || after.status !== "COMPLETED") {
        return null;
      }

      const workerId = after.acceptedBy;
      const bossId = after.bossId;
      const workOfferId = context.params.workOfferId;
      const jobTitle = after.title || "Untitled Job";
      if (!workerId) return null;

      try {
        // Count completed jobs
        const completedSnapshot = await admin.firestore()
            .collection("workOffers")
            .where("acceptedBy", "==", workerId)
            .where("status", "==", "COMPLETED")
            .get();

        const completedCount = completedSnapshot.size;

        // Determine level
        let level;
        if (completedCount >= 50) level = "PLATINUM";
        else if (completedCount >= 21) level = "GOLD";
        else if (completedCount >= 6) level = "SILVER";
        else level = "BRONZE";

        // Update worker profile
        await admin.firestore().collection("users").doc(workerId).update({
          completedJobsCount: completedCount,
          workerLevel: level,
        });

        console.log(`Worker ${workerId}: ${completedCount} jobs, level=${level}`);

        // Sync the linked Booking record → COMPLETED
        const bookingRef = admin.firestore().collection("bookings").doc(workOfferId);
        const bookingSnap = await bookingRef.get();
        if (bookingSnap.exists) {
          await bookingRef.update({status: "COMPLETED"});
          console.log("Booking marked COMPLETED:", workOfferId);
        }

        // Notify boss that job is completed and ready for review
        if (bossId) {
          const bossDoc = await admin.firestore().collection("users").doc(bossId).get();
          const bossData = bossDoc.exists ? bossDoc.data() : {};

          // In-app notification
          await admin.firestore().collection("notifications").add({
            recipientId: bossId,
            type: "work_completed",
            title: "Job Completed! ✅",
            body: `The worker has marked "${jobTitle}" as complete. Tap to review.`,
            jobId: workOfferId,
            isRead: false,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
          });

          // Push notification
          if (bossData.fcmToken) {
            await admin.messaging().send({
              notification: {
                title: "Job Completed! ✅",
                body: `"${jobTitle}" is done. Rate the worker now.`,
              },
              data: {
                type: "work_completed",
                jobId: workOfferId,
                title: "Job Completed! ✅",
                body: `"${jobTitle}" is done. Rate the worker now.`,
              },
              token: bossData.fcmToken,
            });
          }
        }

        return null;
      } catch (error) {
        console.error("Error updating worker level:", error);
        return null;
      }
    });

/**
 * Cloud Function: Sync booking status when a work offer moves to IN_PROGRESS.
 */
exports.syncBookingInProgress = functions.firestore
    .document("workOffers/{workOfferId}")
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();
      const workOfferId = context.params.workOfferId;

      // Only when status changes to IN_PROGRESS
      if (before.status === after.status || after.status !== "IN_PROGRESS") {
        return null;
      }

      try {
        const bookingRef = admin.firestore().collection("bookings").doc(workOfferId);
        const bookingSnap = await bookingRef.get();
        if (bookingSnap.exists) {
          await bookingRef.update({status: "IN_PROGRESS"});
          console.log("Booking marked IN_PROGRESS:", workOfferId);
        }
        return null;
      } catch (error) {
        console.error("Error in syncBookingInProgress:", error);
        return null;
      }
    });

/**
 * Cloud Function: Send alert when a report is filed.
 * Creates an admin notification record for review.
 */
exports.onReportFiled = functions.firestore
    .document("reports/{reportId}")
    .onCreate(async (snapshot, context) => {
      const report = snapshot.data();
      const reportId = context.params.reportId;

      try {
        // Create admin alert
        await admin.firestore().collection("adminAlerts").add({
          type: "new_report",
          reportId: reportId,
          reporterId: report.reporterId,
          reportedEntityId: report.reportedEntityId,
          reportType: report.reportType,
          reason: report.reason,
          status: "PENDING",
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        console.log(`Admin alert created for report: ${reportId}`);

        // TODO: Send email notification to admin
        // You can integrate with SendGrid, Mailgun, or Firebase Extensions for email

        return null;
      } catch (error) {
        console.error("Error in onReportFiled:", error);
        return null;
      }
    });

/**
 * Cloud Function: Auto-expire stale ASSIGNED jobs.
 * Runs every hour. If a job has been ASSIGNED for more than 48 hours
 * without moving to IN_PROGRESS, it gets reset to OPEN.
 */
exports.autoExpireStaleJobs = functions.pubsub
    .schedule("every 1 hours")
    .onRun(async (context) => {
      const cutoffTime = new Date(Date.now() - 48 * 60 * 60 * 1000); // 48 hours ago

      try {
        const staleJobs = await admin.firestore()
            .collection("workOffers")
            .where("status", "==", "ASSIGNED")
            .where("createdAt", "<", cutoffTime)
            .get();

        const batch = admin.firestore().batch();
        let count = 0;

        for (const doc of staleJobs.docs) {
          batch.update(doc.ref, {
            status: "OPEN",
            acceptedBy: null,
            isAccepted: false,
            acceptedByName: null,
            acceptedByPhoto: null,
          });
          count++;
        }

        if (count > 0) {
          await batch.commit();
          console.log(`Auto-expired ${count} stale ASSIGNED jobs`);
        }

        return null;
      } catch (error) {
        console.error("Error in autoExpireStaleJobs:", error);
        return null;
      }
    });

// ── Helper: Haversine Distance ──────────────────────────────────────────────

function haversineDistance(lat1, lng1, lat2, lng2) {
  const R = 6371; // Earth radius in km
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
      Math.sin(dLng / 2) * Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function toRad(deg) {
  return deg * (Math.PI / 180);
}

