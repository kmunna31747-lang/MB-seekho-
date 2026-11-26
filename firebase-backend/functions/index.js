const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

const RATE_LIMIT_WINDOW_MS = 60 * 1000;
const MAX_FAILED_ATTEMPTS = 5;
const VIEW_COOLDOWN_MS = 24 * 60 * 60 * 1000; // 1 valid view per viewer per video per 24h
const STUDIO_WINDOW_DAYS = 28;
const STUDIO_WINDOW_MS = STUDIO_WINDOW_DAYS * 24 * 60 * 60 * 1000;
const MAX_CODES_PER_RPC_CALL = 2500; // Per-call chunk limit; unlimited cumulative batches (10k, 50k, 100k+)

const VALID_CATEGORIES = [
  "Facebook Course",
  "YouTube Course",
  "Social Media Course",
  "Snapchat Course",
  "Earning App Course",
  "Earning Website Course",
  "Other Course",
];

const UNUSED_STATUSES = new Set(["unused", "available", "assigned"]);
const DISABLED_STATUSES = new Set(["disabled", "revoked"]);

function normalizeCode(raw) {
  return String(raw || "").trim().toUpperCase();
}

function sha256Hex(raw) {
  return crypto
    .createHash("sha256")
    .update(normalizeCode(raw), "utf8")
    .digest("hex");
}

function normalizeRole(rawRole) {
  const r = String(rawRole || "").trim().toLowerCase();
  if (r !== "student" && r !== "teacher") {
    throw new HttpsError("invalid-argument", "Role must be 'student' or 'teacher'.");
  }
  return r;
}

function assertAdmin(request) {
  if (!request.auth || request.auth.token.admin !== true) {
    throw new HttpsError("permission-denied", "Owner Admin privileges required.");
  }
}

function assertSignedIn(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Authentication required.");
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

async function resolveAccessCodeDocRef(codeIdOrHash) {
  const key = String(codeIdOrHash || "").trim();
  if (!key) return null;

  const directRef = db.collection("accessCodes").doc(key);
  const directSnap = await directRef.get();
  if (directSnap.exists) return directRef;

  const querySnap = await db
    .collection("accessCodes")
    .where("codeId", "==", key)
    .limit(1)
    .get();
  if (!querySnap.empty) return querySnap.docs[0].ref;
  return null;
}

/**
 * Atomic, SHA-256 verified single-use Access Code redemption for BOTH Student and Teacher.
 * - O(1) lookup via accessCodes/{codeHash} (with legacy fallback query for TCH001-keyed docs).
 * - Scales to unlimited future batches (10,000, 50,000, 100,000+ codes) without app updates.
 * - Rejects invalid, already-used, disabled/revoked, or wrong-portal codes.
 */
exports.redeemAccessCode = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    const { code, expectedRole: rawRole, displayName } = request.data || {};
    const expectedRole = normalizeRole(rawRole || "student");
    const normalized = normalizeCode(code);
    const rateKey = `${expectedRole}_${request.rawRequest.ip || "unknown"}`;
    await checkRateLimit(rateKey);

    if (normalized.length < 6 || normalized.length > 64) {
      await recordFailedAttempt(rateKey);
      throw new HttpsError(
        "invalid-argument",
        `Invalid ${expectedRole === "teacher" ? "Teacher" : "Student"} Access Code.`
      );
    }

    const codeHash = sha256Hex(normalized);
    let codeDocRef = db.collection("accessCodes").doc(codeHash);
    let preSnap = await codeDocRef.get();

    // Backwards-compatibility fallback for legacy Teacher documents keyed by codeId
    if (!preSnap.exists) {
      const legacySnap = await db
        .collection("accessCodes")
        .where("codeHash", "==", codeHash)
        .limit(1)
        .get();
      if (legacySnap.empty) {
        await recordFailedAttempt(rateKey);
        throw new HttpsError(
          "not-found",
          `Invalid ${expectedRole === "teacher" ? "Teacher" : "Student"} Access Code.`
        );
      }
      codeDocRef = legacySnap.docs[0].ref;
      preSnap = legacySnap.docs[0];
    }

    const preData = preSnap.data();
    const resolvedCodeId = preData.codeId || preSnap.id.slice(0, 12);
    const safeName =
      String(displayName || "").trim().slice(0, 60) ||
      (preData.assignedToName ? String(preData.assignedToName).trim().slice(0, 60) : "") ||
      `${expectedRole === "teacher" ? "Teacher" : "Student"} ${resolvedCodeId}`;

    const authUser = await admin.auth().createUser({ displayName: safeName });

    try {
      await db.runTransaction(async (tx) => {
        const freshSnap = await tx.get(codeDocRef);
        if (!freshSnap.exists) {
          throw new HttpsError("not-found", "Invalid Access Code.");
        }
        const data = freshSnap.data();
        const docRole = String(data.role || "").toLowerCase();
        if (docRole !== expectedRole) {
          throw new HttpsError(
            "permission-denied",
            expectedRole === "teacher"
              ? "This code belongs to the Student portal. Please enter a Teacher Access Code."
              : "This code belongs to the Teacher portal. Please enter a Student Access Code."
          );
        }

        const status = String(data.status || "").toLowerCase();
        if (status === "used") {
          throw new HttpsError(
            "already-exists",
            "This access code has already been used."
          );
        }
        if (DISABLED_STATUSES.has(status)) {
          throw new HttpsError(
            "permission-denied",
            "This access code has been disabled by the administrator."
          );
        }
        if (!UNUSED_STATUSES.has(status)) {
          throw new HttpsError("failed-precondition", "Access code is not redeemable.");
        }

        tx.update(codeDocRef, {
          status: "used",
          redeemedByUid: authUser.uid,
          redeemedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        tx.set(db.collection("users").doc(authUser.uid), {
          uid: authUser.uid,
          name: safeName,
          photoUrl: null,
          role: expectedRole,
          codeId: data.codeId || resolvedCodeId,
          totalCourses: 0,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          active: true,
        });

        if (expectedRole === "teacher") {
          tx.set(db.collection("teacherCoins").doc(authUser.uid), {
            teacherId: authUser.uid,
            totalCoins: 0,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });
        }
      });

      await admin.auth().setCustomUserClaims(authUser.uid, {
        role: expectedRole,
        admin: false,
      });

      const customToken = await admin.auth().createCustomToken(authUser.uid, {
        role: expectedRole,
      });

      return {
        uid: authUser.uid,
        role: expectedRole,
        codeId: resolvedCodeId,
        customToken,
      };
    } catch (err) {
      await admin.auth().deleteUser(authUser.uid).catch(() => {});
      await recordFailedAttempt(rateKey);
      throw err;
    }
  }
);

/**
 * Owner-only: bulk import a batch of unique Student or Teacher Access Codes.
 * - Supports unlimited future batches (10,000, 50,000, 100,000+ codes) via chunked calls.
 * - Stores ONLY SHA-256 hashes keyed at accessCodes/{codeHash}.
 * - Uses BulkWriter.create() to guarantee global duplicate prevention across all batches.
 */
exports.importAccessCodesBatch = onCall(
  { enforceAppCheck: true, region: "asia-south1", timeoutSeconds: 120 },
  async (request) => {
    assertAdmin(request);
    const { role: rawRole, codes, batchLabel } = request.data || {};
    const role = normalizeRole(rawRole || "student");

    if (!Array.isArray(codes) || codes.length === 0) {
      throw new HttpsError("invalid-argument", "Provide a non-empty array of codes to import.");
    }
    if (codes.length > MAX_CODES_PER_RPC_CALL) {
      throw new HttpsError(
        "invalid-argument",
        `Send at most ${MAX_CODES_PER_RPC_CALL} codes per request; split larger imports into sequential chunks.`
      );
    }

    const batchId = `BATCH_${Date.now().toString(36).toUpperCase()}_${crypto
      .randomBytes(3)
      .toString("hex")
      .toUpperCase()}`;
    const safeLabel = String(batchLabel || `Imported ${role} batch`).trim().slice(0, 80);
    const prefix = role === "teacher" ? "TCH" : "STU";

    const existingCountSnap = await db
      .collection("accessCodes")
      .where("role", "==", role)
      .count()
      .get();
    let seq = existingCountSnap.data().count;

    const seenInRequest = new Set();
    let importedCount = 0;
    let duplicateCount = 0;
    let invalidCount = 0;

    const writer = db.bulkWriter();
    const pendingWrites = [];

    for (const item of codes) {
      const rawCode = normalizeCode(typeof item === "string" ? item : item && item.rawCode);
      if (rawCode.length < 6 || rawCode.length > 64 || !/^[A-Z0-9\-_]+$/.test(rawCode)) {
        invalidCount += 1;
        continue;
      }

      const codeHash = sha256Hex(rawCode);
      if (seenInRequest.has(codeHash)) {
        duplicateCount += 1;
        continue;
      }
      seenInRequest.add(codeHash);

      seq += 1;
      const providedId =
        item && typeof item === "object" && item.codeId
          ? String(item.codeId).trim().toUpperCase().replace(/[^A-Z0-9_\-]/g, "").slice(0, 32)
          : "";
      const codeId = providedId || `${prefix}${String(seq).padStart(5, "0")}`;

      const docRef = db.collection("accessCodes").doc(codeHash);
      const writePromise = writer
        .create(docRef, {
          codeId,
          codeHash,
          role,
          status: "unused",
          batchId,
          assignedToName: null,
          redeemedByUid: null,
          redeemedAt: null,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        })
        .then(() => {
          importedCount += 1;
        })
        .catch((err) => {
          // Firestore ALREADY_EXISTS (code 6) => global duplicate across existing/future batches
          if (err && (err.code === 6 || String(err.message || "").includes("ALREADY_EXISTS"))) {
            duplicateCount += 1;
          } else {
            throw err;
          }
        });
      pendingWrites.push(writePromise);
    }

    await writer.close();
    await Promise.all(pendingWrites);

    await db.collection("codeBatches").doc(batchId).set({
      batchId,
      role,
      label: safeLabel,
      source: "import",
      addedCount: importedCount,
      duplicateCount,
      invalidCount,
      createdByAdminUid: request.auth.uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return { batchId, role, importedCount, duplicateCount, invalidCount };
  }
);

/**
 * Owner-only: generate unlimited future batches of unique Student or Teacher Access Codes.
 * - Returns raw codes ONCE to Owner Admin; stores ONLY SHA-256 hashes in Firestore.
 * - Zero fixed 10,000 limit; prevents duplicates globally via accessCodes/{codeHash} create().
 */
exports.generateAccessCodes = onCall(
  { enforceAppCheck: true, region: "asia-south1", timeoutSeconds: 120 },
  async (request) => {
    assertAdmin(request);
    const { count, role: rawRole, batchLabel } = request.data || {};
    const role = normalizeRole(rawRole || "student");
    const qty = Math.min(Math.max(parseInt(count, 10) || 1, 1), MAX_CODES_PER_RPC_CALL);
    const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    const idPrefix = role === "teacher" ? "TCH" : "STU";
    const codePrefix = role === "teacher" ? "MBTC" : "MBST";

    const batchId = `BATCH_${Date.now().toString(36).toUpperCase()}_${crypto
      .randomBytes(3)
      .toString("hex")
      .toUpperCase()}`;
    const safeLabel = String(batchLabel || `Generated ${qty} ${role} codes`).trim().slice(0, 80);

    const existingSnap = await db.collection("accessCodes").where("role", "==", role).count().get();
    let seq = existingSnap.data().count;

    const generated = [];
    const seenHashes = new Set();
    const writer = db.bulkWriter();
    const pending = [];

    const makeChunk = () =>
      Array.from(crypto.randomBytes(4))
        .map((b) => alphabet[b % alphabet.length])
        .join("");

    let attempts = 0;
    const maxAttempts = qty * 3;
    while (generated.length < qty && attempts < maxAttempts) {
      attempts += 1;
      const rawCode = `${codePrefix}-${makeChunk()}-${makeChunk()}`;
      const codeHash = sha256Hex(rawCode);
      if (seenHashes.has(codeHash)) continue;
      seenHashes.add(codeHash);

      seq += 1;
      const padLen = role === "student" ? Math.max(5, String(seq).length) : Math.max(3, String(seq).length);
      const codeId = `${idPrefix}${String(seq).padStart(padLen, "0")}`;
      const docRef = db.collection("accessCodes").doc(codeHash);

      const p = writer
        .create(docRef, {
          codeId,
          codeHash,
          role,
          status: "unused",
          batchId,
          assignedToName: null,
          redeemedByUid: null,
          redeemedAt: null,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        })
        .then(() => {
          generated.push({ codeId, rawCode, role });
        })
        .catch((err) => {
          if (!(err && (err.code === 6 || String(err.message || "").includes("ALREADY_EXISTS")))) {
            throw err;
          }
        });
      pending.push(p);
    }

    await writer.close();
    await Promise.all(pending);

    await db.collection("codeBatches").doc(batchId).set({
      batchId,
      role,
      label: safeLabel,
      source: "generated",
      addedCount: generated.length,
      duplicateCount: 0,
      invalidCount: 0,
      createdByAdminUid: request.auth.uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return { batchId, role, count: generated.length, generated };
  }
);

/**
 * Owner-only: enable or disable a Student or Teacher Access Code.
 * Used codes can never be modified.
 */
exports.setAccessCodeStatus = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    assertAdmin(request);
    const { codeId, codeHash, status } = request.data || {};
    const targetStatus = String(status || "").trim().toLowerCase();
    if (targetStatus !== "disabled" && targetStatus !== "unused") {
      throw new HttpsError("invalid-argument", "Status must be 'disabled' or 'unused'.");
    }

    const ref = await resolveAccessCodeDocRef(codeHash || codeId);
    if (!ref) {
      throw new HttpsError("not-found", "Access code not found.");
    }

    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists) throw new HttpsError("not-found", "Access code not found.");
      const cur = String(snap.data().status || "").toLowerCase();
      if (cur === "used") {
        throw new HttpsError("failed-precondition", "Cannot modify an already used access code.");
      }
      tx.update(ref, { status: targetStatus });
    });

    return { ok: true, status: targetStatus };
  }
);

/**
 * Owner-only: scalable server-side aggregate counts for Student & Teacher Access Codes
 * (supports 10,000 / 50,000 / 100,000+ codes in constant client memory).
 */
exports.getAccessCodeStats = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    assertAdmin(request);
    const { role: rawRole } = request.data || {};
    const role = normalizeRole(rawRole || "student");
    const col = db.collection("accessCodes");

    const [totalSnap, unusedSnap, usedSnap, disabledSnap] = await Promise.all([
      col.where("role", "==", role).count().get(),
      col.where("role", "==", role).where("status", "in", ["unused", "available", "assigned"]).count().get(),
      col.where("role", "==", role).where("status", "==", "used").count().get(),
      col.where("role", "==", role).where("status", "in", ["disabled", "revoked"]).count().get(),
    ]);

    return {
      role,
      total: totalSnap.data().count,
      unused: unusedSnap.data().count,
      used: usedSnap.data().count,
      disabled: disabledSnap.data().count,
    };
  }
);

/**
 * Validated Video View & Automated Coin Reward Engine:
 * - Blocks teacher self-views
 * - Blocks repeated views from the same viewer within 24 hours
 * - Awards exactly 1 coin per 10 valid views (10 = 1, 20 = 2, 100 = 10)
 * - Uses deterministic transaction IDs (`COIN_<teacherId>_M<milestone>`) to prevent duplicate coin awards
 */
exports.recordValidVideoView = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    assertSignedIn(request);
    const viewerId = request.auth.uid;
    const { videoId } = request.data || {};
    if (!videoId || typeof videoId !== "string") {
      throw new HttpsError("invalid-argument", "Valid videoId is required.");
    }

    const courseRef = db.collection("courses").doc(videoId);
    const nowMs = Date.now();
    const cooldownSince = admin.firestore.Timestamp.fromMillis(nowMs - VIEW_COOLDOWN_MS);

    const courseSnap = await courseRef.get();
    if (!courseSnap.exists || courseSnap.data().published !== true) {
      return { status: "course_unavailable" };
    }
    const course = courseSnap.data();
    const teacherId = course.teacherId;

    // Block teacher self-views
    if (viewerId === teacherId) {
      return { status: "self_view_ignored" };
    }

    // Check 24-hour anti-duplicate cooldown per (videoId, viewerId)
    const recentSnap = await db
      .collection("videoViews")
      .where("videoId", "==", videoId)
      .where("viewerId", "==", viewerId)
      .where("validationStatus", "==", "valid")
      .where("viewedAt", ">=", cooldownSince)
      .limit(1)
      .get();

    if (!recentSnap.empty) {
      return { status: "duplicate_within_cooldown" };
    }

    const viewId = `VW_${crypto.randomBytes(8).toString("hex")}`;
    const viewRef = db.collection("videoViews").doc(viewId);
    const coinsRef = db.collection("teacherCoins").doc(teacherId);

    const existingViewsCountSnap = await db
      .collection("videoViews")
      .where("teacherId", "==", teacherId)
      .where("validationStatus", "==", "valid")
      .count()
      .get();

    const newTotalValidViews = existingViewsCountSnap.data().count + 1;
    const eligibleMilestones = Math.floor(newTotalValidViews / 10);

    const result = await db.runTransaction(async (tx) => {
      const freshCourse = await tx.get(courseRef);
      if (!freshCourse.exists || freshCourse.data().published !== true) {
        return { status: "course_unavailable" };
      }

      const coinsSnap = await tx.get(coinsRef);
      let totalCoins = coinsSnap.exists ? coinsSnap.data().totalCoins || 0 : 0;
      let coinsAwardedNow = 0;

      tx.set(viewRef, {
        viewId,
        videoId,
        teacherId,
        viewerId,
        viewedAt: admin.firestore.FieldValue.serverTimestamp(),
        validationStatus: "valid",
      });

      tx.update(courseRef, {
        totalViews: admin.firestore.FieldValue.increment(1),
      });

      if (eligibleMilestones > totalCoins) {
        for (let m = totalCoins + 1; m <= eligibleMilestones; m++) {
          const txId = `COIN_${teacherId}_M${m}`;
          const txRef = db.collection("coinTransactions").doc(txId);
          const existingTx = await tx.get(txRef);
          if (!existingTx.exists) {
            tx.set(txRef, {
              transactionId: txId,
              teacherId,
              videoId,
              viewsCount: m * 10,
              coinsEarned: 1,
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            coinsAwardedNow += 1;
          }
        }
        totalCoins += coinsAwardedNow;
      }

      tx.set(
        coinsRef,
        {
          teacherId,
          totalCoins,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );

      return {
        status: "recorded",
        validViewsTotalForTeacher: newTotalValidViews,
        coinsAwardedNow,
        newTeacherCoinBalance: totalCoins,
      };
    });

    return result;
  }
);

/**
 * Teacher Studio 28-Day Analytics (strictly restricted to owning Teacher or Owner Admin).
 */
exports.getTeacherStudioAnalytics = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    assertSignedIn(request);
    const isAdminUser = request.auth.token.admin === true;
    const isTeacherUser = request.auth.token.role === "teacher";

    if (!isAdminUser && !isTeacherUser) {
      throw new HttpsError("permission-denied", "Teacher Studio is available only for verified Teachers.");
    }

    const targetTeacherId =
      isAdminUser && request.data?.teacherId
        ? String(request.data.teacherId)
        : request.auth.uid;

    const nowMs = Date.now();
    const windowStart = admin.firestore.Timestamp.fromMillis(nowMs - STUDIO_WINDOW_MS);

    const [viewsSnap, coursesSnap, coinsSnap, txSnap] = await Promise.all([
      db.collection("videoViews")
        .where("teacherId", "==", targetTeacherId)
        .where("validationStatus", "==", "valid")
        .get(),
      db.collection("courses")
        .where("teacherId", "==", targetTeacherId)
        .get(),
      db.collection("teacherCoins").doc(targetTeacherId).get(),
      db.collection("coinTransactions")
        .where("teacherId", "==", targetTeacherId)
        .orderBy("createdAt", "desc")
        .get(),
    ]);

    const views28d = viewsSnap.docs.filter((d) => {
      const ts = d.data().viewedAt;
      return ts && ts.toMillis() >= windowStart.toMillis();
    });

    const videos28d = coursesSnap.docs.filter((d) => {
      const ts = d.data().createdAt;
      return ts && ts.toMillis() >= windowStart.toMillis();
    });

    const tx28d = txSnap.docs.filter((d) => {
      const ts = d.data().createdAt;
      return ts && ts.toMillis() >= windowStart.toMillis();
    });

    const coinsEarnedLast28Days = tx28d.reduce((sum, d) => sum + (d.data().coinsEarned || 0), 0);
    const totalCoinBalance = coinsSnap.exists ? coinsSnap.data().totalCoins || 0 : 0;
    const totalValidViewsAllTime = viewsSnap.size;

    return {
      teacherId: targetTeacherId,
      viewsLast28Days: views28d.length,
      videosUploadedLast28Days: videos28d.length,
      coinsEarnedLast28Days,
      totalCoinBalance,
      totalValidViewsAllTime,
      viewsTowardNextCoin: totalValidViewsAllTime % 10,
      windowStartMillis: windowStart.toMillis(),
      windowEndMillis: nowMs,
    };
  }
);

exports.assignAccessCode = onCall(
  { enforceAppCheck: true, region: "asia-south1" },
  async (request) => {
    assertAdmin(request);
    const { codeId, codeHash, customerName } = request.data || {};
    const ref = await resolveAccessCodeDocRef(codeHash || codeId);
    if (!ref) throw new HttpsError("not-found", "Code not found.");

    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists) throw new HttpsError("not-found", "Code not found.");
      const status = String(snap.data().status || "").toLowerCase();
      if (!UNUSED_STATUSES.has(status)) {
        throw new HttpsError("failed-precondition", "Code is already used or disabled.");
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
    const { codeId, codeHash } = request.data || {};
    const ref = await resolveAccessCodeDocRef(codeHash || codeId);
    if (!ref) throw new HttpsError("not-found", "Code not found.");

    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists) throw new HttpsError("not-found", "Code not found.");
      if (String(snap.data().status || "").toLowerCase() === "used") {
        throw new HttpsError("failed-precondition", "Cannot disable an already used code.");
      }
      tx.update(ref, { status: "disabled" });
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
    const codeRef = status === "verified" && assignedCodeId
      ? await resolveAccessCodeDocRef(assignedCodeId)
      : null;

    await db.runTransaction(async (tx) => {
      const paySnap = await tx.get(payRef);
      if (!paySnap.exists) throw new HttpsError("not-found", "Payment record not found.");
      if (status === "verified" && assignedCodeId) {
        if (!codeRef) {
          throw new HttpsError("failed-precondition", "Selected access code was not found.");
        }
        const codeSnap = await tx.get(codeRef);
        if (!codeSnap.exists || !UNUSED_STATUSES.has(String(codeSnap.data().status || "").toLowerCase())) {
          throw new HttpsError("failed-precondition", "Selected access code is unavailable.");
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

exports.VALID_CATEGORIES = VALID_CATEGORIES;
