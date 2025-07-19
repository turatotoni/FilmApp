const {onDocumentCreated, onDocumentUpdated} =
  require("firebase-functions/v2/firestore");
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

        if (fcmToken) {
          console.log("User is online - sending immediately");

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

          await admin.messaging().send(message);
          await snapshot.ref.update({
            sent: true,
            read: true,
          });
        } else {
          console.log("User is offline - marking for send on login");
          await snapshot.ref.update({
            sendOnLogin: true,
          });
        }
      } catch (error) {
        console.error("Error in notification function:", error);
      }
    },
);

exports.sendPendingNotifications = onDocumentUpdated(
    "users/{userId}",
    async (event) => {
      const before = event.data.before.data();
      const after = event.data.after.data();
      const userId = event.params.userId;

      console.log("=== FCM TOKEN DEBUG ===");
      console.log("User:", userId);
      console.log("Old token:", before.fcmToken || "null");
      console.log("New token:", after.fcmToken || "null");
      console.log("Token changed?:", before.fcmToken !== after.fcmToken);
      console.log("New token exists?:", !!after.fcmToken);
      console.log("======================");

      if (before.fcmToken === after.fcmToken || !after.fcmToken) {
        console.log("No relevant FCM token change - skipping");
        return;
      }

      try {
        const notificationsQuery = admin.firestore()
            .collection("notifications")
            .where("receiverId", "==", userId)
            .where("sent", "==", false)
            .where("sendOnLogin", "==", true);

        const snapshot = await notificationsQuery.get();
        console.log(`Found ${snapshot.size} pending notifications`);

        for (const doc of snapshot.docs) {
          const notification = doc.data();
          console.log(`Processing ${notification.type} notification`);

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
              continue;
          }

          const message = {
            token: after.fcmToken,
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

          await admin.messaging().send(message);
          await doc.ref.update({
            sent: true,
            read: true,
          });
          console.log("Notification sent successfully");
        }
      } catch (error) {
        console.error("Error in sendPendingNotifications:", error);
      }
    },
);
