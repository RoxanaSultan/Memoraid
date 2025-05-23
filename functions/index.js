const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendMedicationReminder = onDocumentCreated(
  {document: "medicine/{medicationId}"},
  async (event) => {
    const snap = event.data;
    const medication = snap.data();
    const userId = medication.userId;

    const userDoc = await admin.
      firestore().
      collection("users").
      doc(userId).get();
    const fcmToken = userDoc.get("fcmToken");

    if (!fcmToken) {
      console.log("⚠️ No FCM token for user:", userId);
      return null;
    }

    const message = {
      token: fcmToken,
      notification: {
        title: "New medicine added!",
        body: `Your caretaker added ${medication.name} to your calendar.`,
      },
      data: {
        type: "medicationReminder",
        date: medication.date,
        time: medication.time,
        note: medication.note || "",
        dose: medication.dose || "",
      },
      android: {
        priority: "high",
      },
    };

    try {
      const response = await admin.messaging().send(message);
      console.log("✅ Notification sent:", response);
    } catch (error) {
      console.error("❌ Error sending notification:", error);
    }

    return null;
  },
);
