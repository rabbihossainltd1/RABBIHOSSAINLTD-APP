package com.rabbihossainltd.app

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.trusted.TrustedWebActivityIntentBuilder
import androidx.core.content.ContextCompat

/**
 * LauncherActivity — entry point for the Trusted Web Activity (TWA).
 *
 * WHY TWA instead of WebView:
 * Google OAuth explicitly blocks authentication inside Android WebViews (since 2021).
 * A TWA runs the website inside Chrome's real browser engine, sharing the same cookies,
 * session storage, and account credentials as Chrome itself. This means:
 *   - Google "Sign in with Google" works correctly.
 *   - No redirect_uri_mismatch errors.
 *   - Full browser cookie support.
 *   - App passes Google's "disallowed_useragent" check.
 *
 * The app will appear completely native (no browser toolbar) once Digital Asset Links
 * are verified. See the DAL_SETUP_INSTRUCTIONS.md file for setup steps.
 */
class LauncherActivity : AppCompatActivity() {

    companion object {
        private const val SITE_URL = "https://rabbihossainltd.online"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val siteUri = Uri.parse(SITE_URL)

        // Attempt to launch as a Trusted Web Activity first.
        // TWA removes the browser toolbar entirely (requires Digital Asset Links).
        // Falls back to Custom Tabs automatically if DAL is not verified.
        try {
            val twaBuilder = TrustedWebActivityIntentBuilder(siteUri)
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))

            val session = TwaHelper.getOrCreateSession(this)
            if (session != null) {
                val intent = twaBuilder.build(session)
                startActivity(intent.intent)
            } else {
                // Session not ready yet — fall back to Custom Tabs
                launchAsCustomTab(siteUri)
            }
        } catch (e: Exception) {
            // Fallback: Custom Tabs (still uses Chrome, so Google login still works)
            launchAsCustomTab(siteUri)
        }

        // Finish this transparent activity so the back stack is clean
        finish()
    }

    private fun launchAsCustomTab(uri: Uri) {
        val colorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorSchemeParams)
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(this, uri)
    }
}
