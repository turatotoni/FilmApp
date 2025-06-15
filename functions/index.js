const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  databaseURL: "https://firestore.googleapis.com/v1/projects/fittnm-app-92731/databases/(default)",
});

setGlobalOptions({
  region: "europe-west3",
  maxInstances: 10,
});

// Glavna promjena: Ova funkcija samo sprema notifikacije, ne šalje ih odmah
exports.storeNotification = onDocumentCreated(
    "pending_notifications/{notificationId}",
    async (event) => {
      const snapshot = event.data;
      const notification = snapshot.data();

      try {
        // Provjeri postoji li korisnik
        const receiverDoc = await admin.firestore()
            .collection("users")
            .doc(notification.receiverId)
            .get();

        if (!receiverDoc.exists) {
          console.log("Receiver user not found");
          await snapshot.ref.delete(); // Obriši nepotreban dokument
          return;
        }

        // Dodatne provjere podataka
        if (!notification.type || !notification.senderId) {
          console.log("Invalid notification data");
          await snapshot.ref.delete();
          return;
        }

        // Spremi notifikaciju u korisnikovu kolekciju
        await admin.firestore()
            .collection("users")
            .doc(notification.receiverId)
            .collection("notifications")
            .add({
              ...notification,
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
              read: false,
              delivered: false // Nova oznaka da nije još isporučena
            });

        // Obriši privremeni dokument
        await snapshot.ref.delete();

        console.log("Notification stored successfully");
        return;
      } catch (error) {
        console.error("Error storing notification:", error);
        return;
      }
    }
);

// Nova funkcija koja se poziva kada korisnik zatraži notifikacije
exports.deliverNotifications = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
        'unauthenticated', 'Authentication required');
  }

  const userId = context.auth.uid;

  try {
    // Dohvati nedostavljene notifikacije
    const notificationsSnapshot = await admin.firestore()
        .collection("users")
        .doc(userId)
        .collection("notifications")
        .where("delivered", "==", false)
        .orderBy("createdAt", "desc")
        .limit(10) // Limitiraj broj notifikacija po pozivu
        .get();

    if (notificationsSnapshot.empty) {
      return {success: true, count: 0};
    }

    // Dohvati FCM token korisnika
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

    // Pošalji svaku notifikaciju
    for (const doc of notificationsSnapshot.docs) {
      const notification = doc.data();
      const senderName = notification.username || "Someone";

      let title, body;
      switch (notification.type) {
        case "like":
          title = "New Like ★";
          body = `${senderName} liked your review for "${notification.movieTitle}"`;
          break;
        case "dislike":
          title = "New Dislike";
          body = `${senderName} disliked your review for "${notification.movieTitle}"`;
          break;
        case "follow":
          title = "New Follower ✨";
          body = `${senderName} started following you!`;
          break;
        default:
          continue; // Preskoči nepoznate tipove
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

        // Označi notifikaciju kao dostavljenu (ali ne i pročitanu)
        batch.update(doc.ref, {delivered: true});
      } catch (error) {
        console.error(`Failed to send notification ${doc.id}:`, error);
      }
    }

    // Pohrani sve promjene odjednom
    await batch.commit();

    return {success: true, count: deliveredCount};
  } catch (error) {
    console.error("Error in deliverNotifications:", error);
    throw new functions.https.HttpsError(
        'internal', 'Failed to deliver notifications');
  }
});
