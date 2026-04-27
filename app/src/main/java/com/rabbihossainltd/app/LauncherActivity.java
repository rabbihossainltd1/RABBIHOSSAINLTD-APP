package com.rabbihossainltd.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.browser.customtabs.CustomTabsIntent;

/**
 * Simple Chrome Custom Tabs launcher.
 * No WebView is used, so Google Login runs in the browser/Chrome session.
 */
public class LauncherActivity extends Activity {

    private static final String SITE_URL = "https://rabbihossainltd.online";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openFromIntent(intent);
    }

    private void openFromIntent(Intent intent) {
        Uri uri = Uri.parse(SITE_URL);
        if (intent != null && intent.getData() != null) {
            uri = intent.getData();
        }

        openInBrowserContext(uri);
        finish();
    }

    private void openInBrowserContext(Uri uri) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setToolbarColor(Color.parseColor("#1a237e"))
                .setShowTitle(false)
                .build();

        try {
            customTabsIntent.launchUrl(this, uri);
        } catch (Exception e) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(browserIntent);
        }
    }
}
