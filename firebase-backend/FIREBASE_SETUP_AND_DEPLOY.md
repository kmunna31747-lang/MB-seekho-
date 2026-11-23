# MB Seekho — Firebase Production Setup, Deployment & Release APK Guide

## 1. Connect Firebase Project
1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com).
2. Enable **Authentication**, **Cloud Firestore**, **Firebase Storage**, and **App Check (Play Integrity)**.
3. Register the Android app with package name `com.aistudio.mbseekho.vqkzmw`.
4. Download `google-services.json` and place it inside `app/google-services.json`.

## 2. Deploy Security Rules & Cloud Functions
```bash
cd firebase-backend
firebase login
firebase use <YOUR_FIREBASE_PROJECT_ID>

# Deploy Firestore & Storage security rules
firebase deploy --only firestore:rules,storage

# Install & deploy Cloud Functions
cd functions && npm install && cd ..
firebase deploy --only functions
```

## 3. Seed the 60 Initial Access Codes (SHA-256 Hashes Only)
Download a Firebase Service Account JSON key from **Project Settings -> Service Accounts** and keep it strictly off version control:
```bash
export GOOGLE_APPLICATION_CREDENTIALS="/secure/path/serviceAccountKey.json"
node firebase-backend/scripts/seed_access_codes.js
```
This writes SHA-256 hashes for `STU001`–`STU050` (50 student codes) and `TCH001`–`TCH010` (10 teacher codes) into the `accessCodes` collection.

## 4. Provision the Owner Admin Custom Claim
Create your owner account in Firebase Authentication, copy its UID, and run:
```bash
node firebase-backend/scripts/set_owner_admin_claim.js <OWNER_FIREBASE_UID>
```

## 5. Build Signed Release APK / AAB
Configure your upload keystore environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) and run:
```bash
gradle :app:assembleRelease
# Or for Google Play App Bundle:
gradle :app:bundleRelease
```
The release artifacts are generated in `app/build/outputs/apk/release/` and `app/build/outputs/bundle/release/`.
