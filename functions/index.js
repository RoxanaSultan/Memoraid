const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendMedicationReminder = onDocumentCreated(
  {document: "medicine/{medicationId}"},
  async (event) => {
    const snap = event.data;
    const medication = snap.data();

    if (!medication) {
      console.log("⚠️ No medication data found");
      return null;
    }

    const userId = medication.userId;
    if (!userId) {
      console.log("⚠️ No userId in medication document");
      return null;
    }

    const userDoc = await admin
      .firestore()
      .collection("users")
      .doc(userId)
      .get();
    const fcmToken = userDoc.get("fcmToken");

    if (!fcmToken) {
      console.log(`⚠️ No FCM token for user: ${userId}`);
      return null;
    }

    let hour = "00";
    let minute = "00";
    if (typeof medication.time === "string" &&
        medication.time.includes(":")) {
      [hour, minute] = medication.time.split(":");
    }

    const dose = medication.dose || "medicamentul";
    const medicationName = medication.medicationName || medication.name || dose;

    // MODIFICARE: Trimite notificare de "medicament nou" + datele pentru alarm
    const message = {
      token: fcmToken,
      notification: {
        title: "Medicament nou adăugat",
        body: `${medicationName} - programat la ${medication.time || hour + ":" + minute}`,
      },
      data: {
        type: "newMedication", // SCHIMBAT din "medicationReminder"
        medicationName: medicationName,
        time: medication.time || hour + ":" + minute,
        hour: hour ? hour.toString() : "0",
        minute: minute ? minute.toString() : "0",
        dose: dose,
      },
      android: {
        priority: "high",
        notification: {
          sound: "default",
          channelId: "new_medication_channel", // Canal diferit
        },
      },
    };

    try {
      const response = await admin.messaging().send(message);
      console.log("✅ New medication notification sent:", response);
    } catch (error) {
      console.error("❌ Error sending notification:", error);
    }

    return null;
  },
);
