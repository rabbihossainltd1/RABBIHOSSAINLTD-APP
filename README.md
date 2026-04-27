# RABBI HOSSAIN LTD — TWA Android App

A **Trusted Web Activity (TWA)** Android app for [rabbihossainltd.online](https://rabbihossainltd.online).

## Why TWA instead of WebView?

Google permanently blocked OAuth authentication (including "Sign in with Google") inside
Android WebViews in 2021. This app uses TWA — which opens the website inside Chrome's real
browser engine — so Google login works exactly like it does in any browser.

| Feature                  | Old WebView App | This TWA App  |
|--------------------------|-----------------|---------------|
| Google Login             | ❌ Blocked       | ✅ Works       |
| Browser cookies/session  | ❌ Isolated      | ✅ Shared      |
| Chrome extensions/saved passwords | ❌ No  | ✅ Yes        |
| App looks native         | ✅ Yes           | ✅ Yes (with DAL) |
| Requires Chrome          | ❌ No            | ✅ Yes (fallback available) |

## Project Structure

```
app/
├── src/main/
│   ├── AndroidManifest.xml          # TWA intent filters + autoVerify
│   ├── java/com/rabbihossainltd/app/
│   │   ├── LauncherActivity.kt      # TWA entry point (NO WebView)
│   │   └── TwaHelper.kt             # Chrome session management
│   └── res/
│       ├── mipmap-*/ic_launcher*    # Original app icons (preserved)
│       ├── values/themes.xml        # App theme
│       ├── values/colors.xml
│       └── values/strings.xml
├── build.gradle
└── proguard-rules.pro
.github/workflows/build.yml          # GitHub Actions CI
DAL_SETUP_INSTRUCTIONS.md            # How to set up Digital Asset Links
assetlinks.json                      # Template — host at /.well-known/
```

## Package Name
`com.rabbihossainltd.app`

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

## Setup Digital Asset Links (Required for toolbar-free experience)

See **DAL_SETUP_INSTRUCTIONS.md** for full steps. Short version:

1. Get your APK SHA-256 fingerprint via `keytool`
2. Update `assetlinks.json` with the fingerprint
3. Upload to `https://rabbihossainltd.online/.well-known/assetlinks.json`
4. Also upload to `https://www.rabbihossainltd.online/.well-known/assetlinks.json`

> **Note:** Google Login works regardless of DAL setup. DAL only removes the Chrome toolbar.
