const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

const RATE_LIMIT_WINDOW_MS = 60 * 1000;
const MAX_FAILED_ATTEMPTS = 5;

function sha256Hex(raw) {
  return crypto
    .createHash("sha256")
    .update(String(raw || "").trim().toUpperCase(), "utf8")
    .digest("hex");
}

function assertAdmin(request) {
  if (!request.auth || request.auth.token.admin !== true) {
    throw new HttpsError("permission-denied", "Owner Admin privileges required.");
  }
}

async function checkRateLimit(rateKey) {
  const ref = db.collection("rateLimits").doc(rateKey);
  const now = Date.now();
  const snap = await ref.get();
  if (!snap.exists) return;
  const data = snap.data();
  const attempts = (data.attempts || []).filter((ts) => now - ts < RATE_LIMIT_WINDOW_MS);
  if (attempts.length >= MAX_FAILED_ATTEMPTS) {
    throw new HttpsError(
      "resource-exhausted",
      "Too many failed login attempts. Please wait 60 seconds and try again."
    );
  }
}

async function recordFailedAttempt(rateKey) {
  const ref = db.collection("rateLimits").doc(rateKey);
  const now = Date.now();
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const prev = snap.exists ? snap.data().attempts || [] : [];
    const recent = prev.filter((ts) => now - ts < RATE_LIMIT_WINDOW_MS);
    recent.push(now);
    tx.set(ref, { attempts: recent, updatedAt: admin.firestore.FieldValue.serverTimestamp() });
  });
}

/**
 * Atomic, SHA-256 verified single-use access code redemption.
 * Issues a Firebase Auth user + role custom claim and creates the user document.
 */
exports.redeemAccessCode = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    const { code, expectedRole, displayName } = request.data || {};
    if (expectedRole !== "student" && expectedRole !== "teacher") {
      throw new HttpsError("invalid-argument", "Invalid portal role.");
    }
    const normalized = String(code || "").trim().toUpperCase();
    const rateKey = `${expectedRole}_${request.rawRequest.ip || "unknown"}`;
    await checkRateLimit(rateKey);

    if (normalized.length < 8) {
      await recordFailedAttempt(rateKey);
      throw new HttpsError("invalid-argument", "Invalid access code.");
    }

    const codeHash = sha256Hex(normalized);
    const querySnap = await db
      .collection("accessCodes")
      .where("codeHash", "==", codeHash)
      .limit(1)
      .get();

    if (querySnap.empty) {
      await recordFailedAttempt(rateKey);
      throw new HttpsError("not-found", "Invalid access code.");
    }

    const codeDocRef = querySnap.docs[0].ref;
    const codeId = querySnap.docs[0].id;

    // Create dedicated Firebase Auth account for this student or teacher
    const safeName =
      String(displayName || "").trim().slice(0, 60) ||
      `${expectedRole === "teacher" ? "Teacher" : "Student"} ${codeId}`;
    const authUser = await admin.auth().createUser({ displayName: safeName });

    try {
      await db.runTransaction(async (tx) => {
        const freshSnap = await tx.get(codeDocRef);
        if (!freshSnap.exists) {
          throw new HttpsError("not-found", "Invalid access code.");
        }
        const data = freshSnap.data();
        if (data.role !== expectedRole) {
          throw new HttpsError("permission-denied", "Access code is not valid for this portal.");
        }
        if (data.status === "used") {
          throw new HttpsError("already-exists", "This access code has already been used.");
        }
        if (data.status === "revoked") {
          throw new HttpsError("permission-denied", "This access code has been revoked.");
        }

        tx.update(codeDocRef, {
          status: "used",
          redeemedByUid: authUser.uid,
          redeemedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        const userProfile = {
          uid: authUser.uid,
          name: safeName,
          photoUrl: null,
          role: expectedRole,
          codeId,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          active: true,
        };
        if (expectedRole === "teacher") {
          userProfile.totalCourses = 0;
        }
        tx.set(db.collection("users").doc(authUser.uid), userProfile);
      });

      await admin.auth().setCustomUserClaims(authUser.uid, {
        role: expectedRole,
        admin: false,
      });

      const customToken = await admin.auth().createCustomToken(authUser.uid, {
        role: expectedRole,
      });

      return { uid: authUser.uid, role: expectedRole, customToken };
    } catch (err) {
      // Roll back created auth user if transaction fails (e.g., race condition / already used)
      await admin.auth().deleteUser(authUser.uid).catch(() => {});
      await recordFailedAttempt(rateKey);
      throw err;
    }
  }
);

/**
 * Owner-only: generate additional student/teacher access codes.
 * Returns raw codes ONCE to owner and stores only SHA-256 hashes in Firestore.
 */
exports.generateAccessCodes = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    assertAdmin(request);
    const { role, count } = request.data || {};
    if (role !== "student" && role !== "teacher") {
      throw new HttpsError("invalid-argument", "Role must be student or teacher.");
    }
    const qty = Math.min(Math.max(parseInt(count, 10) || 1, 1), 25);
    const idPrefix = role === "student" ? "STU" : "TCH";
    const rawPrefix = role === "student" ? "MBST" : "MBTC";
    const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    const existingSnap = await db.collection("accessCodes").where("role", "==", role).count().get();
    const startIndex = existingSnap.data().count;

    const batch = db.batch();
    const generated = [];

    for (let i = 1; i <= qty; i++) {
      const codeId = `${idPrefix}${String(startIndex + i).padStart(3, "0")}`;
      const chunk = () =>
        Array.from(crypto.randomBytes(4))
          .map((b) => alphabet[b % alphabet.length])
          .join("");
      const rawCode = `${rawPrefix}-${chunk()}-${chunk()}`;
      const codeHash = sha256Hex(rawCode);

      batch.set(db.collection("accessCodes").doc(codeId), {
        codeId,
        codeHash,
        role,
        status: "available",
        assignedToName: null,
        redeemedByUid: null,
        redeemedAt: null,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      generated.push({ codeId, rawCode, role });
    }

    await batch.commit();
    return { generated };
  }
);

exports.assignAccessCode = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    assertAdmin(request);
    const { codeId, customerName } = request.data || {};
    const ref = db.collection("accessCodes").doc(String(codeId || ""));
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists) throw new HttpsError("not-found", "Code not found.");
      const status = snap.data().status;
      if (status !== "available" && status !== "assigned") {
        throw new HttpsError("failed-precondition", "Code is already used or revoked.");
      }
      tx.update(ref, {
        status: "assigned",
        assignedToName: String(customerName || "").trim(),
      });
    });
    return { ok: true };
  }
);

exports.revokeAccessCode = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    assertAdmin(request);
    const { codeId } = request.data || {};
    const ref = db.collection("accessCodes").doc(String(codeId || ""));
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists) throw new HttpsError("not-found", "Code not found.");
      if (snap.data().status === "used") {
        throw new HttpsError("failed-precondition", "Cannot revoke an already used code.");
      }
      tx.update(ref, { status: "revoked" });
    });
    return { ok: true };
  }
);

exports.verifyPayment = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    assertAdmin(request);
    const { paymentId, status, assignedCodeId, customerName } = request.data || {};
    if (status !== "verified" && status !== "rejected") {
      throw new HttpsError("invalid-argument", "Status must be verified or rejected.");
    }
    const payRef = db.collection("payments").doc(String(paymentId || ""));
    await db.runTransaction(async (tx) => {
      const paySnap = await tx.get(payRef);
      if (!paySnap.exists) throw new HttpsError("not-found", "Payment record not found.");
      if (status === "verified" && assignedCodeId) {
        const codeRef = db.collection("accessCodes").doc(String(assignedCodeId));
        const codeSnap = await tx.get(codeRef);
        if (!codeSnap.exists || !["available", "assigned"].includes(codeSnap.data().status)) {
          throw new HttpsError("failed-precondition", "Selected code is unavailable.");
        }
        tx.update(codeRef, {
          status: "assigned",
          assignedToName: String(customerName || paySnap.data().customerName || ""),
        });
      }
      tx.update(payRef, {
        verificationStatus: status,
        assignedCodeId: status === "verified" ? assignedCodeId || null : null,
        verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });
    return { ok: true };
  }
);

exports.setUserActiveStatus = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    assertAdmin(request);
    const { targetUid, active } = request.data || {};
    await admin.auth().updateUser(String(targetUid), { disabled: !active });
    await db.collection("users").doc(String(targetUid)).update({ active: Boolean(active) });
    return { ok: true };
  }
);
