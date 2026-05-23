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

        return null;
      } catch (error) {
        console.error("Error in sendWorkAcceptedNotification:", error);
        return null; // Don't throw — let the function exit gracefully
      }
    });
