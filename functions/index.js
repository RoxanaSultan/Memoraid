const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

exports.notifyNewMedication = onDocumentCreated("medicine/{medId}", async (event) => {
  const medication = event.data.data();

  if (!medication || !medication.userId || !medication.name) {
    console.error("Missing medication data");
    return;
  }

  try {
    const userDoc = await db.collection("users").doc(medication.userId).get();
    const userData = userDoc.data();
    const fcmToken = userData.fcmToken;

    if (!fcmToken) {
      console.error("Missing FCM token for user:", medication.userId);
      return;
    }

    const message = {
      token: fcmToken,
      notification: {
        title: "New medication added!",
        body: `Medication ${medication.name} has been added to your calendar.`,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "medication_channel",
        },
      },
      data: {
        navigate_to: "medication",
      },
    };

    await getMessaging().send(message);
    console.log("✅ Notification sent to patient!");
  } catch (error) {
    console.error("⛔ Error sending notification:", error);
  }
});
