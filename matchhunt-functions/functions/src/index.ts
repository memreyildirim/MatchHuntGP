import * as admin from "firebase-admin";
import {onDocumentCreated} from "firebase-functions/v2/firestore";

admin.initializeApp();

export const onEventCreated = onDocumentCreated(
  {
    region: "us-central1",
    document: "events/{eventId}",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const eventData = snap.data() as any;
    const title = eventData?.title || "Yeni etkinlik!";
    const desc = eventData?.description || "";
    const sportType = (eventData?.sportType || "general") as string;

    const topic = `events_${sportType.toLowerCase()}`;

    const message = {
      topic,
      notification: {
        title: `Yeni ${sportType} etkinliği: ${title}`,
        body: desc || "MatchHunt'ta yeni bir etkinlik açıldı.",
      },
      data: {
        type: "event",
        eventId: event.params.eventId,
        sportType,
      },
    };

    await admin.messaging().send(message);
    console.log("Notification sent:", event.params.eventId, "topic:", topic);
  }
);