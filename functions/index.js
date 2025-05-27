const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

exports.notifyNewMedication = onDocumentCreated("medicine/{medId}", async (event) => {
  const medication = event.data.data();

  if (!medication || !medication.userId || !medication.name) {
    console.error("Missing medication data");
    return;
  }

  const patientTopic = medication.userId;

  const message = {
    topic: patientTopic,
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

  try {
    await getMessaging().send(message);
    console.log("✅ Notification sent to patient!");
  } catch (error) {
    console.error("⛔ Error sending notification:", error);
  }
});
