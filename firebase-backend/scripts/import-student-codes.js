#!/usr/bin/env node
/**
 * MB Seekho — Unlimited Student & Teacher Access Code Bulk Importer (Firebase Admin SDK)
 *
 * Streams any CSV or TXT code file (1,000, 10,000, 50,000, 100,000+ codes), hashes every
 * code with SHA-256, and inserts into Firestore `accessCodes/{codeHash}` via BulkWriter.create()
 * so duplicates across all existing and future batches are atomically prevented.
 * Raw codes are NEVER stored in Firestore or logged to stdout.
 *
 * Usage:
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json \
 *     node scripts/import-student-codes.js <path-to-codes.txt|csv> [--role student|teacher] [--label "Batch Name"]
 */

const fs = require("fs");
const path = require("path");
const readline = require("readline");
const crypto = require("crypto");
const admin = require("firebase-admin");

const CODE_TOKEN_REGEX = /^[A-Z0-9]{2,8}(?:-[A-Z0-9]{3,8}){1,5}$/;
const CODE_ID_REGEX = /^(STU|TCH)[0-9A-Z_-]{1,24}$/;
const NUMBERED_PREFIX_REGEX = /^#?([0-9]{1,10})[.):]?$/;
const ALLOWED_EXTENSIONS = new Set([".txt", ".csv"]);

function sha256Hex(raw) {
  return crypto
    .createHash("sha256")
    .update(String(raw || "").trim().toUpperCase(), "utf8")
    .digest("hex");
}

function parseLine(rawLine) {
  const trimmed = String(rawLine || "").trim().replace(/^\uFEFF/, "");
  if (!trimmed || trimmed.startsWith("#") || trimmed.startsWith("//")) return null;

  const upper = trimmed.toUpperCase();
  if (
    upper.startsWith("MB SEEKHO") ||
    upper.startsWith("CODE_ID") ||
    upper.startsWith("CODEID") ||
    upper.startsWith("SERIAL") ||
    upper.startsWith("PREVIOUS ") ||
    upper.startsWith("TOTAL ")
  ) {
    return null;
  }

  const tokens = trimmed
    .split(/[\s,;\t|]+/)
    .map((t) => t.replace(/^["']|["']$/g, "").trim().toUpperCase())
    .filter(Boolean);

  if (tokens.length === 0) return null;

  let codeToken = null;
  for (const token of tokens) {
    if (CODE_TOKEN_REGEX.test(token) && token.length >= 6 && token.length <= 64) {
      codeToken = token;
      break;
    }
  }
  if (!codeToken && tokens.length === 1 && tokens[0].length >= 8 && /^[A-Z0-9_-]+$/.test(tokens[0])) {
    codeToken = tokens[0];
  }
  if (!codeToken) return null;

  let codeId = null;
  for (const token of tokens) {
    if (token !== codeToken && CODE_ID_REGEX.test(token)) {
      codeId = token;
      break;
    }
  }
  if (!codeId) {
    for (const token of tokens) {
      const m = token !== codeToken ? token.match(NUMBERED_PREFIX_REGEX) : null;
      if (m) {
        if (codeToken.startsWith("MBTC-")) {
          codeId = `TCH${m[1].padStart(3, "0")}`;
        } else if (codeToken.startsWith("MBST-")) {
          codeId = `STU${m[1].padStart(5, "0")}`;
        }
        break;
      }
    }
  }

  return { codeId, rawCode: codeToken };
}

function parseArgs(argv) {
  const args = argv.slice(2);
  let filePath = null;
  let role = "student";
  let label = null;

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg === "--role" && args[i + 1]) {
      role = String(args[++i]).trim().toLowerCase();
    } else if (arg === "--label" && args[i + 1]) {
      label = String(args[++i]).trim().slice(0, 80);
    } else if (!arg.startsWith("--") && !filePath) {
      filePath = arg;
    }
  }

  if (!filePath || filePath.includes("\0")) {
    throw new Error("Usage: node scripts/import-student-codes.js <codes.txt|csv> [--role student|teacher] [--label \"Batch Name\"]");
  }
  if (role !== "student" && role !== "teacher") {
    throw new Error("--role must be 'student' or 'teacher'.");
  }

  const resolved = path.resolve(filePath);
  const ext = path.extname(resolved).toLowerCase();
  if (!ALLOWED_EXTENSIONS.has(ext)) {
    throw new Error(`Unsupported file extension '${ext}'. Only .txt and .csv files are accepted.`);
  }
  const stat = fs.statSync(resolved);
  if (!stat.isFile()) {
    throw new Error("Input path must be a regular file.");
  }

  return {
    filePath: resolved,
    role,
    label: label || `CLI Import (${path.basename(resolved)})`,
  };
}

async function main() {
  const { filePath, role, label } = parseArgs(process.argv);

  if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.applicationDefault(),
    });
  }
  const db = admin.firestore();

  const batchId = `BATCH_${Date.now().toString(36).toUpperCase()}_${crypto
    .randomBytes(3)
    .toString("hex")
    .toUpperCase()}`;
  const prefix = role === "teacher" ? "TCH" : "STU";

  const existingCountSnap = await db.collection("accessCodes").where("role", "==", role).count().get();
  let seq = existingCountSnap.data().count;

  const seenHashes = new Set();
  let totalParsed = 0;
  let importedCount = 0;
  let duplicateCount = 0;
  let skippedLines = 0;

  const writer = db.bulkWriter();
  writer.onWriteError((error) => {
    if (error.code === 6 || String(error.message || "").includes("ALREADY_EXISTS")) {
      return false; // Duplicate handled by catch block
    }
    return error.failedAttempts < 3;
  });

  const pending = [];
  const rl = readline.createInterface({
    input: fs.createReadStream(filePath, { encoding: "utf8" }),
    crlfDelay: Infinity,
  });

  for await (const line of rl) {
    const parsed = parseLine(line);
    if (!parsed) {
      skippedLines += 1;
      continue;
    }

    totalParsed += 1;
    const codeHash = sha256Hex(parsed.rawCode);
    if (seenHashes.has(codeHash)) {
      duplicateCount += 1;
      continue;
    }
    seenHashes.add(codeHash);

    seq += 1;
    const codeId = parsed.codeId || `${prefix}${String(seq).padStart(5, "0")}`;
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
        importedCount += 1;
      })
      .catch((err) => {
        if (err && (err.code === 6 || String(err.message || "").includes("ALREADY_EXISTS"))) {
          duplicateCount += 1;
        } else {
          throw err;
        }
      });

    pending.push(p);

    // Flush periodically to keep memory bounded for 100,000+ line imports
    if (pending.length >= 2000) {
      await Promise.all(pending.splice(0, pending.length));
    }
  }

  await writer.close();
  await Promise.all(pending);

  await db.collection("codeBatches").doc(batchId).set({
    batchId,
    role,
    label,
    source: "cli_import",
    addedCount: importedCount,
    duplicateCount,
    invalidCount: 0,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  console.log(
    JSON.stringify(
      {
        status: "completed",
        batchId,
        role,
        label,
        totalParsed,
        importedCount,
        duplicateCount,
        skippedHeaderOrBlankLines: skippedLines,
      },
      null,
      2
    )
  );
}

main().catch((err) => {
  console.error("Import failed:", err.message || err);
  process.exit(1);
});
