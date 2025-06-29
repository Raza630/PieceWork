const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.notifyBossWhenJobAccepted = functions.firestore
  .document("jobs/{jobId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    // Check if `isAccepted` changed from false to true
    if (!before.isAccepted && after.isAccepted) {
      const bossId = after.createdBy;

      // Fetch the boss's FCM token
      const bossDoc = await admin.firestore().collection("users").doc(bossId).get();
      const bossToken = bossDoc.data().fcmToken;

      if (!bossToken) {
        console.error("No FCM token for boss");
        return;
      }

      // Create the notification payload
      const payload = {
        notification: {
          title: "Job Accepted",
          body: `Your job "${after.title}" has been accepted.`,
        },
        token: bossToken,
      };

      // Send the notification
      try {
        await admin.messaging().send(payload);
        console.log("Notification sent successfully");
      } catch (error) {
        console.error("Error sending notification:", error);
      }
    }
  });
