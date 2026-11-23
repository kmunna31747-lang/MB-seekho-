/**
 * MB SEEKHO — TRUSTED SERVER-SIDE OWNER ADMIN PROVISIONING SCRIPT
 *
 * Grants Firebase Custom Claims { admin: true, role: "admin" } to the application owner's UID.
 *
 * Usage:
 *   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccountKey.json"
 *   node set_owner_admin_claim.js <OWNER_FIREBASE_UID>
 */

const admin = require("firebase-admin");

const ownerUid = process.argv[2];
if (!ownerUid) {
  console.error("Usage: node set_owner_admin_claim.js <OWNER_FIREBASE_UID>");
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
});

async function grantOwnerClaim() {
  await admin.auth().setCustomUserClaims(ownerUid, {
    admin: true,
    role: "admin",
  });
  console.log(`Owner admin custom claim granted to UID: ${ownerUid}`);
}

grantOwnerClaim().catch((err) => {
  console.error("Failed to set admin custom claim:", err);
  process.exit(1);
});
