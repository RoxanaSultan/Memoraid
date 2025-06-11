const { onDocumentCreated, onDocumentUpdated, onDocumentDeleted } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

exports.notifyNewMedication = onDocumentCreated("medicine/{medId}", async (event) => {
  const medication = event.data.data();
  if (!medication || !medication.userId || !medication.name) return;

  await sendNotification(medication.userId, {
    title: "New Medication",
    body: `Medication ${medication.name} has been added!`,
    medId: event.params.medId,
    navigate_to: "medication",
    added: "true",
    updated: "false",
    deleted: "false"
  });
});

exports.notifyUpdatedMedication = onDocumentUpdated("medicine/{medId}", async (event) => {
  const medication = event.data.after.data();
  if (!medication || !medication.userId || !medication.name) return;

  await sendNotification(medication.userId, {
    title: "Medication Updated",
    body: `Medication ${medication.name} has been updated!`,
    medId: event.params.medId,
    navigate_to: "medication",
    added: "false",
    updated: "true",
    deleted: "false"
  });
});

exports.notifyDeletedMedication = onDocumentDeleted("medicine/{medId}", async (event) => {
  const medication = event.data.data();
  if (!medication || !medication.userId || !medication.name) return;

  await sendNotification(medication.userId, {
    title: "Medication Deleted",
    body: `Medication ${medication.name} has been deleted!`,
    medId: event.params.medId,
    navigate_to: "medication",
    added: "false",
    updated: "false",
    deleted: "true"
  });
});

async function sendNotification(userId, { title, body, medId, navigate_to, added, updated, deleted }) {
  try {
    const userDoc = await db.collection("users").doc(userId).get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (!fcmToken) return;

    const message = {
      token: fcmToken,
      android: {
        priority: "high",
      },
      data: {
        title: title,
        body: body,
        medId: medId || "",
        navigate_to: navigate_to || "medication",
        added: added,
        updated: updated,
        deleted: deleted
      },
    };

    await getMessaging().send(message);
    console.log("✅ Notification sent!");
  } catch (error) {
    console.error("⛔ Error sending notification:", error);
  }
}
