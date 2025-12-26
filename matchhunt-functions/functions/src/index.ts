import * as admin from "firebase-admin";
import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {onDocumentUpdated} from "firebase-functions/v2/firestore";

admin.initializeApp();

export const onEventCreated = onDocumentCreated(
  {
    region: "europe-west3", // frankfurt
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

// Mesaj oluşturulduğunda bildirim gönder
export const onMessageCreated = onDocumentCreated(
  {
    region: "europe-west3",
    document: "messages/{messageId}",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const messageData = snap.data() as any;

    const receiverId = messageData.receiverId as string;
    const senderId = messageData.senderId as string;
    const text = (messageData.text as string) || "";
    const chatId = (messageData.chatId as string) || "";

    if (!receiverId || !senderId) {
      console.log("Missing receiverId or senderId, skipping notification");
      return;
    }

    // Alıcı kullanıcının FCM token'ını getir
    const userSnap = await admin.firestore().doc(`users/${receiverId}`).get();
    const fcmToken = userSnap.get("fcmToken") as string | undefined;

    if (!fcmToken) {
      console.log("No fcmToken for user", receiverId);
      return;
    }

    // Gönderenin adını başlıkta kullan
    let senderName = "Yeni mesaj";
    const senderSnap = await admin.firestore().doc(`users/${senderId}`).get();
    if (senderSnap.exists) {
      senderName = (senderSnap.get("username") as string) || senderName;
    }

    const payload: admin.messaging.Message = {
      token: fcmToken,
      notification: {
        title: senderName,
        body: text || "Yeni mesajınız var",
      },
      data: {
        type: "message",
        chatId,
        senderId,
      },
    };

    await admin.messaging().send(payload);
    console.log("Message notification sent to", receiverId, "for chat", chatId);
  }
);


// Etkinlik güncellendiğinde bildirim gönder

export const onEventUpdated = onDocumentUpdated(
  {
    region: "europe-west3",
    document: "events/{eventId}",
  },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    const ignoredFields = ["pendingRequests"];

    // Değişen alanları tespit et
    const changedFields = Object.keys(after).filter(
      (k) => !ignoredFields.includes(k) && JSON.stringify(after[k]) !== JSON.stringify(before[k])
    );
    if (changedFields.length === 0) return; // anlamlı değişiklik yoksa gönderme

    // Eğer sadece participants değiştiyse ve pendingRequests de değiştiyse,
    // bu muhtemelen bir join request onay/red işlemi, bu durumda onEventJoinDecisionUpdated
    // bildirim gönderecek, burada göndermeyelim
    const onlyParticipantsChanged = changedFields.length === 1 && changedFields[0] === "participants";
    const pendingRequestsChanged = JSON.stringify(before.pendingRequests || []) !== JSON.stringify(after.pendingRequests || []);

    if (onlyParticipantsChanged && pendingRequestsChanged) {
      console.log("Only participants and pendingRequests changed, skipping onEventUpdated notification (onEventJoinDecisionUpdated will handle it)");
      return;
    }

    const eventId = event.params.eventId;
    const creatorId = after.createdBy || after.creatorId;
    const participants: string[] = after.participants || [];

    // Bildirimi alacak kullanıcılar: katılımcılar - oluşturucu
    const targetUserIds = participants.filter((uid) => uid && uid !== creatorId);
    if (targetUserIds.length === 0) {
      console.log("No participants to notify for", eventId);
      return;
    }

    // Kullanıcı tokenlarını topla
    const userRefs = targetUserIds.map((uid) =>
      admin.firestore().doc(`users/${uid}`)
    );
    const userDocs = await admin.firestore().getAll(...userRefs);
    const tokens = userDocs
      .map((d) => d.get("fcmToken") as string | undefined)
      .filter((t): t is string => !!t);

    if (tokens.length === 0) {
      console.log("No tokens for participants of", eventId);
      return;
    }

    // Alan adlarını okunur hale getir
    const fieldLabels: Record<string, string> = {
      location: "konum",
      place: "konum",
      time: "saat",
      date: "tarih",
      description: "açıklama",
      title: "başlık",
      sportType: "spor türü",
      participants: "katılımcılar",
    };

    const readableChanges = changedFields
      .map((f) => fieldLabels[f] || f)
      .slice(0, 3); // çok uzamasın

    const title = "Event Updated";
    const body =
      readableChanges.length > 0
        ? `Etkinlik güncellendi: ${readableChanges.join(", ")} değişti`
        : "Etkinlik güncellendi";

    const payload: admin.messaging.MulticastMessage = {
      tokens,
      notification: {
        title,
        body,
      },
      data: {
        type: "event_update",
        eventId,
        changedFields: JSON.stringify(changedFields),
      },
    };

    await admin.messaging().sendEachForMulticast(payload);
    console.log(
      "Event update notification sent",
      eventId,
      "to",
      tokens.length,
      "devices"
    );
  }
);

// Etkinlik için yeni katılma isteği geldiğinde bildirim gönder
export const onEventJoinRequestUpdated = onDocumentUpdated(
  {
    region: "europe-west3",
    document: "events/{eventId}",
  },
  async (event) => {
    const before = event.data?.before.data() as any;
    const after = event.data?.after.data() as any;
    if (!before || !after) return;

    const eventId = event.params.eventId;
    console.log("onEventJoinRequestUpdated TRIGGERED for", eventId);

    const title = after.title || "Etkinlik";

    // DİKKAT: alan adı pendingRequests (çoğul)
    const beforeReq: string[] = before.pendingRequests || [];
    const afterReq: string[] = after.pendingRequests || [];

    console.log("beforeReq:", beforeReq);
    console.log("afterReq:", afterReq);

    if (!Array.isArray(beforeReq) || !Array.isArray(afterReq)) {
      console.log("pendingRequests is not array for", eventId);
      return;
    }

    // Sadece yeni eklenen istekleri bul
    const newRequests = afterReq.filter((uid) => !beforeReq.includes(uid));

    console.log("newRequests:", newRequests);

    if (newRequests.length === 0) {
      // Yeni istek yok, sadece silinmiş / aynı kalmış
      return;
    }

    const creatorId = after.createdBy || after.creatorId;
    if (!creatorId) {
      console.log("No creatorId for event", eventId);
      return;
    }

    // Etkinlik sahibinin token'ını al
    const creatorSnap = await admin.firestore().doc(`users/${creatorId}`).get();
    const fcmToken = creatorSnap.get("fcmToken") as string | undefined;

    if (!fcmToken) {
      console.log("No fcmToken for creator of", eventId);
      return;
    }

    // İlk istekte bulunan kullanıcının ismini al (opsiyonel)
    let requesterName = "bir kullanıcı";
    const firstRequesterId = newRequests[0];
    if (firstRequesterId) {
      const requesterSnap = await admin.firestore().doc(`users/${firstRequesterId}`).get();
      if (requesterSnap.exists) {
        requesterName =
          (requesterSnap.get("username") as string) ||
          (requesterSnap.get("name") as string) ||
          requesterName;
      }
    }

    const count = newRequests.length;
    const notifTitle = "Yeni Katılma İsteği";
    const notifBody =
      count === 1
        ? `${requesterName}, "${title}" etkinliğine katılmak istiyor.`
        : `${count} yeni katılma isteği var: "${title}"`;

    const payload: admin.messaging.Message = {
      token: fcmToken,
      notification: {
        title: notifTitle,
        body: notifBody,
      },
      data: {
        type: "join_request",
        eventId,
      },
    };

    await admin.messaging().send(payload);
    console.log(
      "Join request notification sent to creator of",
      eventId,
      "for",
      count,
      "new requests"
    );
  }
);

// etkinlik isteği onay ve red durumu için metot
export const onEventJoinDecisionUpdated = onDocumentUpdated(
  {
    region: "europe-west3",
    document: "events/{eventId}",
  },
  async (event) => {
    const before = event.data?.before.data() as any;
    const after = event.data?.after.data() as any;
    if (!before || !after) return;

    const eventId = event.params.eventId;
    const title = after.title || "Etkinlik";

    const beforeParticipants: string[] = before.participants || [];
    const afterParticipants: string[] = after.participants || [];
    const beforePending: string[] = before.pendingRequests || [];
    const afterPending: string[] = after.pendingRequests || [];

    // 1) ONAYLANANLAR: yeni katılımcı olup, önceden pending’de olanlar
    const newParticipants = afterParticipants.filter(
      (uid) => !beforeParticipants.includes(uid)
    );
    const approvedUsers = newParticipants.filter((uid) =>
      beforePending.includes(uid)
    );

    for (const userId of approvedUsers) {
      const userSnap = await admin.firestore().doc(`users/${userId}`).get();
      const fcmToken = userSnap.get("fcmToken") as string | undefined;
      if (!fcmToken) continue;

      const payload: admin.messaging.Message = {
        token: fcmToken,
        notification: {
          title: "Katılma isteğin onaylandı",
          body: `"${title}" etkinliğine katılım isteğin kabul edildi.`,
        },
        data: {
          type: "join_approved",
          eventId,
        },
      };

      await admin.messaging().send(payload);
      console.log("Join approved notification sent to", userId, "for", eventId);
    }

    // 2) REDDEDİLENLER: pending’den silinmiş ama participants’a eklenmemiş olanlar
    const removedFromPending = beforePending.filter(
      (uid) => !afterPending.includes(uid)
    );
    const rejectedUsers = removedFromPending.filter(
      (uid) => !afterParticipants.includes(uid)
    );

    for (const userId of rejectedUsers) {
      const userSnap = await admin.firestore().doc(`users/${userId}`).get();
      const fcmToken = userSnap.get("fcmToken") as string | undefined;
      if (!fcmToken) continue;

      const payload: admin.messaging.Message = {
        token: fcmToken,
        notification: {
          title: "Katılma isteğin reddedildi",
          body: `"${title}" etkinliğine katılım isteğin maalesef reddedildi.`,
        },
        data: {
          type: "join_rejected",
          eventId,
        },
      };

      await admin.messaging().send(payload);
      console.log("Join rejected notification sent to", userId, "for", eventId);
    }
  }
);

// Post beğenildiğinde bildirim gönder
export const onPostLiked = onDocumentUpdated(
  {
    region: "europe-west3",
    document: "posts/{postId}",
  },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    // Document ID'yi al (event.params.postId zaten document ID)
    const postId = event.params.postId;
    const beforeLikedBy: string[] = before.likedBy || [];
    const afterLikedBy: string[] = after.likedBy || [];
    const postOwnerId = after.userId as string;

    if (!postOwnerId) {
      console.log("No userId for post", postId);
      return;
    }

    // Yeni eklenen beğenileri bul (array'de olan ama önceden olmayan)
    const newLikes = afterLikedBy.filter((uid) => !beforeLikedBy.includes(uid));

    if (newLikes.length === 0) {
      // Yeni beğeni yok (sadece beğeni kaldırılmış olabilir)
      return;
    }

    // İlk beğenen kullanıcının ID'si
    const likerId = newLikes[0];

    // Kendi paylaşımına beğeni yapıyorsa bildirim gönderme
    if (likerId === postOwnerId) {
      console.log("User liked their own post, skipping notification");
      return;
    }

    // Post sahibinin FCM token'ını al
    const ownerSnap = await admin.firestore().doc(`users/${postOwnerId}`).get();
    const fcmToken = ownerSnap.get("fcmToken") as string | undefined;

    if (!fcmToken) {
      console.log("No fcmToken for post owner", postOwnerId);
      return;
    }

    // Beğenen kullanıcının adını al
    let likerName = "Birisi";
    const likerSnap = await admin.firestore().doc(`users/${likerId}`).get();
    if (likerSnap.exists) {
      likerName = (likerSnap.get("username") as string) || likerName;
    }

    const payload: admin.messaging.Message = {
      token: fcmToken,
      notification: {
        title: "Yeni Beğeni",
        body: `${likerName} paylaşımını beğendi`,
      },
      data: {
        type: "post_like",
        postId,
        likerId,
      },
    };

    await admin.messaging().send(payload);
    console.log("Post like notification sent to", postOwnerId, "for post", postId);
  }
);

// Yorum oluşturulduğunda bildirim gönder
export const onCommentCreated = onDocumentCreated(
  {
    region: "europe-west3",
    document: "comments/{commentId}",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const commentData = snap.data() as any;
    const postId = commentData.postId as string;
    const commenterId = commentData.userId as string;
    const commenterName = commentData.userName as string || "Birisi";

    if (!postId || !commenterId) {
      console.log("Missing postId or userId in comment");
      return;
    }

    // Post bilgilerini al
    const postSnap = await admin.firestore().doc(`posts/${postId}`).get();
    if (!postSnap.exists) {
      console.log("Post not found", postId);
      return;
    }

    const postData = postSnap.data() as any;
    const postOwnerId = postData.userId as string;

    if (!postOwnerId) {
      console.log("No userId for post", postId);
      return;
    }

    // Kendi paylaşımına yorum yapıyorsa bildirim gönderme
    if (commenterId === postOwnerId) {
      console.log("User commented on their own post, skipping notification");
      return;
    }

    // Post sahibinin FCM token'ını al
    const ownerSnap = await admin.firestore().doc(`users/${postOwnerId}`).get();
    const fcmToken = ownerSnap.get("fcmToken") as string | undefined;

    if (!fcmToken) {
      console.log("No fcmToken for post owner", postOwnerId);
      return;
    }

    const commentContent = (commentData.content as string) || "";
    const preview = commentContent.length > 50
      ? commentContent.substring(0, 50) + "..."
      : commentContent;

    const payload: admin.messaging.Message = {
      token: fcmToken,
      notification: {
        title: "Yeni Yorum",
        body: `${commenterName}: ${preview}`,
      },
      data: {
        type: "post_comment",
        postId,
        commentId: event.params.commentId,
        commenterId,
      },
    };

    await admin.messaging().send(payload);
    console.log("Comment notification sent to", postOwnerId, "for post", postId);
  }
);