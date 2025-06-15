const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  databaseURL: "https://firestore.googleapis.com/v1/projects/fittnm-app-92731/databases/(default)",
});

// Set global options for all functions (optional)
setGlobalOptions({
  region: "europe-west3",
  maxInstances: 10,
});

exports.sendNotification = onDocumentCreated(
    "notifications/{notificationId}",
    async (event) => {
      const snapshot = event.data;
      const notification = snapshot.data();


      if (notification.read) {
        console.log("Notification already read, skipping");
        return;
      }

      try {

        const receiverDoc = await admin.firestore()
            .collection("users")
            .doc(notification.receiverId)
            .get();

        if (!receiverDoc.exists) {
          console.log("Receiver user not found");
          return;
        }

        const fcmToken = receiverDoc.data().fcmToken;
        if (!fcmToken) {
          console.log("No FCM token for receiver");
          return;
        }


        let title;
        let body;
        const senderName = notification.username || "Someone";

        switch (notification.type) {
          case "like":
            title = "New Like ★";
            body = `${senderName} liked your review for ` +
                `"${notification.movieTitle}"`;
            break;

          case "dislike":
            title = "New Dislike";
            body = `${senderName} disliked your review for ` +
                `"${notification.movieTitle}"`;
            break;

          case "follow":
            title = "New Follower ✨";
            body = `${senderName} started following you!`;
            break;

          default:
            console.log("Unknown notification type:", notification.type);
            return;
        }


        const message = {
          token: fcmToken,
          notification: {title, body},
          data: {
            type: notification.type,
            senderId: notification.senderId,
            click_action: "FLUTTER_NOTIFICATION_CLICK",
          },
          android: {
            priority: "high",
          },
          apns: {
            payload: {
              aps: {
                sound: "default",
              },
            },
          },
        };


        const response = await admin.messaging().send(message);
        console.log("Successfully sent notification:", response);


        await snapshot.ref.update({read: true});

        return;
      } catch (error) {
        console.error("Error in notification function:", error);
        return;
      }
    },
);
