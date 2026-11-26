/**
 * MB SEEKHO — PRIVATE BACKEND-ONLY TEACHER ACCESS CODE SEEDING SCRIPT
 *
 * SECURITY WARNING:
 * - Run this script ONLY from a trusted owner machine using Firebase Admin SDK credentials.
 * - NEVER bundle this script in the Android APK or public client code.
 * - Student Access Codes are NOT hardcoded here — use `import-student-codes.js` to bulk-import
 *   any 1,000 / 10,000 / 50,000 / 100,000+ Student Access Code CSV/TXT batch into Firestore.
 * - Only SHA-256 hashes are written to Firestore (`accessCodes/{codeHash}`).
 *
 * Usage:
 *   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccountKey.json"
 *   node scripts/seed_access_codes.js
 */

const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
});

const db = admin.firestore();

const INITIAL_TEACHER_CODES = [
  ["TCH001", "MBTC-9I11-DDF1"], ["TCH002", "MBTC-RPW1-REDA"], ["TCH003", "MBTC-LWE9-IMCL"],
  ["TCH004", "MBTC-GE8F-FZUV"], ["TCH005", "MBTC-5KZC-3M8N"], ["TCH006", "MBTC-VNM9-0C9O"],
  ["TCH007", "MBTC-M5AU-1IPH"], ["TCH008", "MBTC-689C-RAGJ"], ["TCH009", "MBTC-EWEX-MDX7"],
  ["TCH010", "MBTC-HV8A-E2TC"],
];

function sha256Hex(raw) {
  return crypto.createHash("sha256").update(raw.trim().toUpperCase(), "utf8").digest("hex");
}

async function seed() {
  const batch = db.batch();
  let count = 0;

  for (const [codeId, raw] of INITIAL_TEACHER_CODES) {
    const codeHash = sha256Hex(raw);
    const ref = db.collection("accessCodes").doc(codeHash);
    const snap = await ref.get();
    if (!snap.exists) {
      batch.set(ref, {
        codeId,
        codeHash,
        role: "teacher",
        status: "unused",
        batchId: "INITIAL_TEACHER_SEED",
        assignedToName: null,
        redeemedByUid: null,
        redeemedAt: null,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      count++;
    }
  }

  await batch.commit();
  console.log(
    `Seeded ${count} hashed Teacher Access Codes into Firestore. ` +
      `For Student Access Codes (1,000 / 10,000 / 100,000+), run: node scripts/import-student-codes.js <file.txt|csv>`
  );
}

seed().catch((err) => {
  console.error("Seeding failed:", err);
  process.exit(1);
});
