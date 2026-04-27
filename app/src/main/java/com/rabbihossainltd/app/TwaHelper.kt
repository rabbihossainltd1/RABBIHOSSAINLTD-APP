package com.rabbihossainltd.app

import android.content.ComponentName
import android.content.Context
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession

/**
 * TwaHelper manages the CustomTabsSession needed to launch a Trusted Web Activity.
 *
 * For a TWA to show without the browser toolbar, it needs:
 * 1. A valid CustomTabsSession
 * 2. Verified Digital Asset Links (.well-known/assetlinks.json on your server)
 *
 * If Chrome is not installed or the session cannot be created, the app falls back
 * to Custom Tabs (still browser-based, Google login still works, toolbar visible).
 */
object TwaHelper {

    private var session: CustomTabsSession? = null
    private var connection: CustomTabsServiceConnection? = null

    /**
     * Returns an existing session or creates a new one synchronously.
     * Returns null if Chrome Custom Tabs service is not available.
     */
    fun getOrCreateSession(context: Context): CustomTabsSession? {
        if (session != null) return session

        val packageName = CustomTabsClient.getPackageName(context, null) ?: return null

        var client: CustomTabsClient? = null
        val latch = java.util.concurrent.CountDownLatch(1)

        val conn = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, c: CustomTabsClient) {
                client = c
                c.warmup(0)
                session = c.newSession(null)
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                session = null
                client = null
            }
        }

        connection = conn
        val bound = CustomTabsClient.bindCustomTabsService(context, packageName, conn)

        if (bound) {
            // Wait up to 1 second for the connection
            latch.await(1, java.util.concurrent.TimeUnit.SECONDS)
        }

        return session
    }

    fun unbind(context: Context) {
        connection?.let {
            try {
                context.unbindService(it)
            } catch (e: Exception) {
                // ignore if not bound
            }
            connection = null
        }
        session = null
    }
}
