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
    id: event.params.medId,
    type: "medication",
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
    id: event.params.medId,
    type: "medication",
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
    id: event.params.medId,
    type: "medication",
    navigate_to: "medication",
    added: "false",
    updated: "false",
    deleted: "true"
  });
});

exports.notifyNewAppointment = onDocumentCreated("appointments/{appointmentId}", async (event) => {
  const appointment = event.data.data();
  if (!appointment || !appointment.userId || !appointment.name) return;

  await sendNotification(appointment.userId, {
    title: "New Appointment",
    body: `Appointment '${appointment.name}' has been added!`,
    id: event.params.appointmentId,
    type: "appointment",
    navigate_to: "appointments",
    added: "true",
    updated: "false",
    deleted: "false"
  });
});

exports.notifyUpdatedAppointment = onDocumentUpdated("appointments/{appointmentId}", async (event) => {
  const appointment = event.data.after.data();
  if (!appointment || !appointment.userId || !appointment.name) return;

  await sendNotification(appointment.userId, {
    title: "Appointment Updated",
    body: `Appointment '${appointment.name}' has been updated!`,
    id: event.params.appointmentId,
    type: "appointment",
    navigate_to: "appointments",
    added: "false",
    updated: "true",
    deleted: "false"
  });
});

exports.notifyDeletedAppointment = onDocumentDeleted("appointments/{appointmentId}", async (event) => {
  const appointment = event.data.data();
  if (!appointment || !appointment.userId || !appointment.name) return;

  await sendNotification(appointment.userId, {
    title: "Appointment Deleted",
    body: `Appointment '${appointment.name}' has been deleted!`,
    id: event.params.appointmentId,
    type: "appointment",
    navigate_to: "appointments",
    added: "false",
    updated: "false",
    deleted: "true"
  });
});

async function sendNotification(userId, { title, body, id, type, navigate_to, added, updated, deleted }) {
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
        id: id || "",
        type: type || "",
        navigate_to: navigate_to || "",
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
