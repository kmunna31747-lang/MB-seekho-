/**
 * MB SEEKHO — PRIVATE BACKEND-ONLY ACCESS CODE SEEDING SCRIPT
 *
 * SECURITY WARNING:
 * - Run this script ONLY from a trusted owner machine using Firebase Admin SDK credentials.
 * - NEVER bundle this script in the Android APK or public client code.
 * - Only SHA-256 hashes of the 50 student codes and 10 teacher codes are uploaded to Firestore.
 *
 * Usage:
 *   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccountKey.json"
 *   node seed_access_codes.js
 */

const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
});

const db = admin.firestore();

const INITIAL_STUDENT_CODES = [
  ["STU001", "MBST-EFZD-LZT9"], ["STU002", "MBST-Q8W5-LX5Y"], ["STU003", "MBST-PRVQ-8ZKD"],
  ["STU004", "MBST-ZL8Z-FYKC"], ["STU005", "MBST-9DBC-K4Q4"], ["STU006", "MBST-J0TH-QADJ"],
  ["STU007", "MBST-5ZMN-4YSK"], ["STU008", "MBST-LRS3-K7YC"], ["STU009", "MBST-E7P3-KTDA"],
  ["STU010", "MBST-9X5H-13BZ"], ["STU011", "MBST-ZKGE-PBYW"], ["STU012", "MBST-IB1S-NOO2"],
  ["STU013", "MBST-34IC-FY11"], ["STU014", "MBST-6I7X-83WJ"], ["STU015", "MBST-IPGS-P3QH"],
  ["STU016", "MBST-I1CF-EZZI"], ["STU017", "MBST-93K6-XV2E"], ["STU018", "MBST-MRGJ-6D7T"],
  ["STU019", "MBST-G6NJ-EWRL"], ["STU020", "MBST-WXW8-LDY5"], ["STU021", "MBST-IASS-Z7V7"],
  ["STU022", "MBST-62QP-1XBL"], ["STU023", "MBST-SBVN-4Y34"], ["STU024", "MBST-28I4-A7FE"],
  ["STU025", "MBST-M9L8-IROO"], ["STU026", "MBST-GKO0-G8GT"], ["STU027", "MBST-PRXV-QDS2"],
  ["STU028", "MBST-EE9U-DHYD"], ["STU029", "MBST-1Z6R-E8QH"], ["STU030", "MBST-A1D5-OCRQ"],
  ["STU031", "MBST-O8DZ-K79V"], ["STU032", "MBST-8YMV-GEOR"], ["STU033", "MBST-HW9V-DS70"],
  ["STU034", "MBST-4MLM-UC2D"], ["STU035", "MBST-8WMK-SA2V"], ["STU036", "MBST-KIOS-2RJL"],
  ["STU037", "MBST-F1M0-B3YO"], ["STU038", "MBST-7BQX-E9ED"], ["STU039", "MBST-PG46-JCEO"],
  ["STU040", "MBST-XPQL-7YNS"], ["STU041", "MBST-GU28-IAME"], ["STU042", "MBST-PTHN-1DQW"],
  ["STU043", "MBST-2ITL-V3OT"], ["STU044", "MBST-KSO4-WI13"], ["STU045", "MBST-FJ7X-EWFB"],
  ["STU046", "MBST-LBTX-ZHA2"], ["STU047", "MBST-ASL6-VTQC"], ["STU048", "MBST-BEWZ-5WYA"],
  ["STU049", "MBST-II4D-6SZX"], ["STU050", "MBST-A15R-4EEZ"],
];

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

  for (const [codeId, raw] of INITIAL_STUDENT_CODES) {
    const ref = db.collection("accessCodes").doc(codeId);
    const snap = await ref.get();
    if (!snap.exists) {
      batch.set(ref, {
        codeId,
        codeHash: sha256Hex(raw),
        role: "student",
        status: "available",
        assignedToName: null,
        redeemedByUid: null,
        redeemedAt: null,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      count++;
    }
  }

  for (const [codeId, raw] of INITIAL_TEACHER_CODES) {
    const ref = db.collection("accessCodes").doc(codeId);
    const snap = await ref.get();
    if (!snap.exists) {
      batch.set(ref, {
        codeId,
        codeHash: sha256Hex(raw),
        role: "teacher",
        status: "available",
        assignedToName: null,
        redeemedByUid: null,
        redeemedAt: null,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      count++;
    }
  }

  await batch.commit();
  console.log(`Seeded ${count} hashed access codes into Firestore (raw codes never stored).`);
}

seed().catch((err) => {
  console.error("Seeding failed:", err);
  process.exit(1);
});
