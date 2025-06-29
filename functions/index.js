const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendWorkAcceptedNotification = functions.firestore
    .document("workOffers/{workOfferId}")
    .onUpdate((change, context) => {
      const before = change.before.data();
      const after = change.after.data();

      if (before.acceptedBy !== after.acceptedBy && after.acceptedBy) {
        const workOfferId = context.params.workOfferId;
        const workerId = after.acceptedBy;
        const bossId = after.createdBy;

        return admin.firestore().collection("users").doc(workerId).get()
            .then((workerDoc) => {
              const workerToken = workerDoc.data().fcmToken; // Assign worker's FCM token

              // If you need to do something with the workerToken, for example, send a notification
              const workerMessage = {
                notification: {
                  title: "Work Offer Accepted",
                  body: `Your work offer with ID ${workOfferId} has been accepted.`,
                },
                token: workerToken, // Send notification to the worker
              };

              return admin.messaging().send(workerMessage); // Send notification to worker
            })
            .then(() => {
              // Continue with your logic for the boss
              return admin.firestore().collection("users").doc(bossId).get();
            })
            .then((bossDoc) => {
              const bossToken = bossDoc.data().fcmToken; // Assuming boss' FCM token is stored here

              const bossMessage = {
                notification: {
                  title: "Work Accepted",
                  body: `Your work offer with ID ${workOfferId} has been accepted by ${workerId}.`,
                },
                token: bossToken, // Send notification to the boss
              };

              // Send notification to the boss
              return admin.messaging().send(bossMessage);
            })
            .then(() => {
              console.log("Notifications sent successfully.");
              return null;
            })
            .catch((error) => {
              console.error("Error sending notifications:", error);
              throw new Error("Failed to send notifications.");
            });
      }
      return null;
    });


// // Import necessary libraries
// const functions = require("firebase-functions");
// const admin = require("firebase-admin");
// admin.initializeApp(); // Initialize Firebase Admin SDK
//
// // Cloud Function to handle the worker accepting a work offer and notify the boss
// exports.sendWorkAcceptedNotification = functions.firestore
//    .document("workOffers/{workOfferId}") // Firestore path to the workOffers collection
//    .onUpdate((change, context) => {
//      const before = change.before.data();
//      const after = change.after.data();
//
//      // Check if the 'acceptedBy' field has changed (worker accepted the job)
//      if (before.acceptedBy !== after.acceptedBy && after.acceptedBy) {
//        const workOfferId = context.params.workOfferId; // Get the document ID (workOfferId)
//        const workerId = after.acceptedBy; // Get the worker's ID
//        const bossId = after.createdBy; // Get the boss' ID
//
//        // Get the worker's FCM token
//        return admin.firestore().collection("users").doc(workerId).get()
//            .then((workerDoc) => {
//              const workerToken = workerDoc.data().fcmToken; // Assuming worker's FCM token is stored here
//
//              // Get the boss' FCM token
//              return admin.firestore().collection("users").doc(bossId).get();
//            })
//            .then((bossDoc) => {
//              const bossToken = bossDoc.data().fcmToken; // Assuming boss' FCM token is stored here
//
//              // Define the message to send to the boss
//              const message = {
//                notification: {
//                  title: "Work Accepted",
//                  body: `Your work offer with ID ${workOfferId} has been accepted by ${workerId}.`,
//                },
//                token: bossToken, // Send notification to the boss
//              };
//
//              // Send the notification to the boss
//              return admin.messaging().send(message);
//            })
//            .then((response) => {
//              console.log("Notification sent successfully:", response);
//              return null;
//            })
//            .catch((error) => {
//              console.error("Error sending notification:", error);
//              throw new Error("Failed to send notification.");
//            });
//      }
//      return null; // If 'acceptedBy' hasn't changed, return null
//    });
