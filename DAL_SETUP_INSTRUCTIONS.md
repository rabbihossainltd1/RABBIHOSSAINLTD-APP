# Digital Asset Links (DAL) Setup Guide
## For: RABBI HOSSAIN LTD TWA App

---

## What is Digital Asset Links?

For your Trusted Web Activity to run **without the browser toolbar** (fully native-looking),
Google Chrome must verify that your website and your Android app are owned by the same person.
This is done via a file called `assetlinks.json` hosted on your server.

Without it, the app still works perfectly — it just shows a Chrome toolbar.
Google Login works **either way** because it uses Chrome's browser engine, not WebView.

---

## Step 1 — Get your APK signing fingerprint

After building your release APK (or debug APK), run:

```bash
# For debug builds (uses Android debug keystore):
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android \
  | grep "SHA-256"

# For release builds (replace with your keystore path):
keytool -list -v \
  -keystore /path/to/your-release.jks \
  -alias YOUR_KEY_ALIAS \
  | grep "SHA-256"
```

This gives you a line like:
```
SHA256: AB:CD:EF:12:34:56:...
```

---

## Step 2 — Create the assetlinks.json file

Create this file with your actual SHA-256 fingerprint:

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.rabbihossainltd.app",
      "sha256_cert_fingerprints": [
        "AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78"
      ]
    }
  }
]
```

Replace the SHA-256 string with your actual fingerprint from Step 1.

---

## Step 3 — Host the file on your server

Upload `assetlinks.json` to:
```
https://rabbihossainltd.online/.well-known/assetlinks.json
https://www.rabbihossainltd.online/.well-known/assetlinks.json
```

**Important requirements:**
- Must be accessible via HTTPS (not HTTP)
- Must return `Content-Type: application/json`
- Must NOT redirect (must be at the exact path)
- File must be publicly accessible (no login required)

### nginx example:
```nginx
location /.well-known/assetlinks.json {
    add_header Content-Type application/json;
    alias /var/www/html/.well-known/assetlinks.json;
}
```

### Apache example:
```apache
<Files "assetlinks.json">
    Header set Content-Type "application/json"
</Files>
```

---

## Step 4 — Verify it works

Use Google's statement list tool:
```
https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://rabbihossainltd.online&relation=delegate_permission/common.handle_all_urls
```

Or use the Android Asset Links Tool app on your phone.

---

## Step 5 — Clear Chrome data and re-test

After uploading assetlinks.json:
1. Go to Android Settings → Apps → Chrome → Storage → Clear Cache
2. Launch your TWA app
3. Chrome fetches and caches the assetlinks.json
4. The toolbar should disappear (may take 1–2 opens)

---

## Quick Summary

| What                        | Value                                 |
|-----------------------------|---------------------------------------|
| Package name                | com.rabbihossainltd.app               |
| DAL file location           | /.well-known/assetlinks.json          |
| Primary domain              | https://rabbihossainltd.online        |
| www domain                  | https://www.rabbihossainltd.online    |
| Google Login works?         | YES — always (DAL only removes toolbar)|

---

## Troubleshooting

**Google login fails with "disallowed_useragent"?**
→ This means Google detected a WebView. Your old app used WebView — this new TWA app does NOT.
  Rebuild and reinstall the new TWA APK.

**Toolbar still showing after assetlinks.json is set up?**
→ Wait 5 minutes and try again. Chrome caches DAL verification.
→ Make sure the SHA-256 fingerprint matches the APK you're installing.
→ Check that the JSON file is valid at the URL above.

**App crashes on launch?**
→ Chrome must be installed and up to date on the device.
→ If Chrome is missing, the app falls back to Custom Tabs via any installed browser.
