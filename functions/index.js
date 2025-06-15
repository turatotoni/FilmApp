const functions = require("firebase-functions");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  databaseURL: "https://firestore.googleapis.com/v1/" +
    "projects/fittnm-app-92731/databases/(default)",
});

setGlobalOptions({
  region: "europe-west3",
  maxInstances: 10,
});

exports.storeNotification = onDocumentCreated(
    "pending_notifications/{notificationId}",
    async (event) => {
      const snapshot = event.data;
      const notification = snapshot.data();

      try {
        const receiverDoc = await admin.firestore()
            .collection("users")
            .doc(notification.receiverId)
            .get();

        if (!receiverDoc.exists) {
          console.log("Receiver user not found");
          await snapshot.ref.delete();
          return;
        }

        if (!notification.type || !notification.senderId) {
          console.log("Invalid notification data");
          await snapshot.ref.delete();
          return;
        }

        await admin.firestore()
            .collection("users")
            .doc(notification.receiverId)
            .collection("notifications")
            .add({
              ...notification,
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
              read: false,
              delivered: false,
            });

        await snapshot.ref.delete();
        console.log("Notification stored successfully");
        return;
      } catch (error) {
        console.error("Error storing notification:", error);
        return;
      }
    },
);

exports.deliverNotifications = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentication required",
    );
  }

  const userId = context.auth.uid;

  try {
    const notificationsSnapshot = await admin.firestore()
        .collection("users")
        .doc(userId)
        .collection("notifications")
        .where("delivered", "==", false)
        .orderBy("createdAt", "desc")
        .limit(10)
        .get();

    if (notificationsSnapshot.empty) {
      return {success: true, count: 0};
    }

    const userDoc = await admin.firestore()
        .collection("users")
        .doc(userId)
        .get();

    const userData = userDoc.data();
    const fcmToken = userData && userData.fcmToken ? userData.fcmToken : null;

    if (!fcmToken) {
      console.log("No FCM token for user");
      return {success: false, error: "No FCM token"};
    }

    let deliveredCount = 0;
    const batch = admin.firestore().batch();

    for (const doc of notificationsSnapshot.docs) {
      const notification = doc.data();
      const senderName = notification.username || "Someone";

      let title;
      let body;
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
        token: fcmToken,
        notification: {title, body},
        data: {
          type: notification.type,
          senderId: notification.senderId,
          notificationId: doc.id,
          click_action: "FLUTTER_NOTIFICATION_CLICK",
        },
        android: {priority: "high"},
        apns: {payload: {aps: {sound: "default"}}},
      };

      try {
        await admin.messaging().send(message);
        deliveredCount++;
        batch.update(doc.ref, {delivered: true});
      } catch (error) {
        console.error(`Failed to send notification ${doc.id}:`, error);
      }
    }

    await batch.commit();
    return {success: true, count: deliveredCount};
  } catch (error) {
    console.error("Error in deliverNotifications:", error);
    throw new functions.https.HttpsError(
        "internal",
        "Failed to deliver notifications",
    );
  }
});
