package com.rabbihossainltd.app

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.provider.MediaStore
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.rabbihossainltd.app.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // File upload callback
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null
    private var currentPhotoPath: String = ""

    // Activity result launchers
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<Array<String>>

    // Track if WebView has loaded once (for fade-in)
    private var hasLoadedOnce = false

    companion object {
        const val WEBSITE_URL = "https://rabbihossainltd.online"
        private const val CUSTOM_SCHEME = "rabbihossainltd"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActivityResultLaunchers()
        setupWebView()
        setupSwipeRefresh()
        setupRetryButton()
        setupBackPressHandler()

        // Load website or browser-login redirect URL
        if (isNetworkAvailable()) {
            val redirectUrl = getLoadableUrlFromIntent(intent)
            if (redirectUrl != null) {
                loadUrlInWebView(redirectUrl)
            } else {
                loadWebsite()
            }
        } else {
            showOfflinePage()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (!isNetworkAvailable()) {
            showOfflinePage()
            return
        }

        val redirectUrl = getLoadableUrlFromIntent(intent)
        if (redirectUrl != null) {
            loadUrlInWebView(redirectUrl)
        } else {
            loadWebsite()
        }
    }

    // ─── Activity Result Launchers ────────────────────────────────────────────

    private fun setupActivityResultLaunchers() {
        // File chooser (gallery + camera combined intent)
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (filePathCallback == null) return@registerForActivityResult

            val uris: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK) {
                when {
                    result.data?.data != null -> {
                        // Single file selected from gallery
                        arrayOf(result.data!!.data!!)
                    }
                    result.data?.clipData != null -> {
                        // Multiple files selected
                        val clipData = result.data!!.clipData!!
                        Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    }
                    cameraImageUri != null -> {
                        // Camera capture
                        arrayOf(cameraImageUri!!)
                    }
                    else -> null
                }
            } else {
                // User cancelled - check if camera captured something
                if (cameraImageUri != null && File(currentPhotoPath).exists() && File(currentPhotoPath).length() > 0) {
                    arrayOf(cameraImageUri!!)
                } else null
            }

            filePathCallback?.onReceiveValue(uris)
            filePathCallback = null
            cameraImageUri = null
        }

        // Camera permission
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                openFileChooserWithCamera()
            } else {
                openFileChooserGalleryOnly()
            }
        }

        // Storage permissions (Android 13+)
        storagePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            openFileChooserWithCamera()
        }
    }

    // ─── WebView Setup ────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView

        // WebSettings configuration
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.userAgentString = "${settings.userAgentString} RabbiHossainLTD/1.0"

        // Cookie settings
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // WebChromeClient
        webView.webChromeClient = AppWebChromeClient()

        // WebViewClient
        webView.webViewClient = AppWebViewClient()
    }

    // ─── WebChromeClient ──────────────────────────────────────────────────────

    inner class AppWebChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            binding.progressBar.progress = newProgress

            if (newProgress < 100) {
                if (binding.progressBar.visibility != View.VISIBLE) {
                    binding.progressBar.visibility = View.VISIBLE
                }
            } else {
                // Fade out progress bar
                val fadeOut = ObjectAnimator.ofFloat(binding.progressBar, "alpha", 1f, 0f)
                fadeOut.duration = 300
                fadeOut.start()
                fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        binding.progressBar.visibility = View.GONE
                        binding.progressBar.alpha = 1f
                    }
                })
            }
        }

        // Open popup windows / Google OAuth windows in the default browser
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            val popupWebView = WebView(this@MainActivity)
            popupWebView.settings.javaScriptEnabled = true
            popupWebView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val popupUrl = request?.url?.toString() ?: return true
                    openInBrowser(popupUrl)
                    return true
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url != null) openInBrowser(url)
                    return true
                }
            }

            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
            transport.webView = popupWebView
            resultMsg.sendToTarget()
            return true
        }

        // File chooser for upload
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            // Cancel any existing callback
            this@MainActivity.filePathCallback?.onReceiveValue(null)
            this@MainActivity.filePathCallback = filePathCallback

            requestFileChooser(fileChooserParams)
            return true
        }

        // Permission requests from website (microphone, etc.)
        override fun onPermissionRequest(request: PermissionRequest?) {
            request?.let {
                val allowedResources = arrayOf(
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE
                )
                val toGrant = it.resources.filter { res -> res in allowedResources }.toTypedArray()
                if (toGrant.isNotEmpty()) {
                    it.grant(toGrant)
                } else {
                    it.deny()
                }
            }
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            callback?.invoke(origin, true, false)
        }
    }

    // ─── WebViewClient ────────────────────────────────────────────────────────

    inner class AppWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.alpha = 1f
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            binding.swipeRefreshLayout.isRefreshing = false
            CookieManager.getInstance().flush()

            // Fade in WebView on first load
            if (!hasLoadedOnce) {
                hasLoadedOnce = true
                binding.webView.alpha = 0f
                binding.webView.visibility = View.VISIBLE
                val fadeIn = ObjectAnimator.ofFloat(binding.webView, "alpha", 0f, 1f)
                fadeIn.duration = 400
                fadeIn.start()
            }

            // Hide offline page if showing
            if (binding.offlinePage.visibility == View.VISIBLE) {
                binding.offlinePage.visibility = View.GONE
                binding.webView.visibility = View.VISIBLE
            }
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            if (request?.isForMainFrame == true) {
                binding.swipeRefreshLayout.isRefreshing = false
                showOfflinePage()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                binding.swipeRefreshLayout.isRefreshing = false
                showOfflinePage()
            }
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: android.net.http.SslError?
        ) {
            // Show SSL warning dialog instead of blindly proceeding
            if (error?.primaryError == android.net.http.SslError.SSL_UNTRUSTED ||
                error?.primaryError == android.net.http.SslError.SSL_EXPIRED) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Security Warning")
                    .setMessage("This website has an SSL certificate issue. Proceed anyway?")
                    .setPositiveButton("Proceed") { _, _ -> handler?.proceed() }
                    .setNegativeButton("Cancel") { _, _ -> handler?.cancel() }
                    .show()
            } else {
                // For other SSL errors on the trusted domain, proceed
                handler?.proceed()
            }
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request?.isForMainFrame != true) return false
            val url = request.url?.toString() ?: return false
            return handleUrl(url)
        }

        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (url == null) return false
            return handleUrl(url)
        }
    }

    // ─── Browser Login Redirect Handling ──────────────────────────────────────

    private fun getLoadableUrlFromIntent(incomingIntent: Intent?): String? {
        val uri = incomingIntent?.data ?: return null
        val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
        val host = uri.host?.lowercase(Locale.US)

        return when {
            // Normal website/App Link redirect from browser after Google login.
            (scheme == "https" || scheme == "http") && isTrustedWebsiteHost(host) -> {
                uri.toString()
            }

            // Optional custom scheme fallback: rabbihossainltd://auth?url=...
            scheme == CUSTOM_SCHEME && host == "auth" -> {
                uri.getQueryParameter("url") ?: WEBSITE_URL
            }

            else -> null
        }
    }

    private fun isTrustedWebsiteHost(host: String?): Boolean {
        return host == "rabbihossainltd.online" || host == "www.rabbihossainltd.online"
    }

    private fun loadUrlInWebView(url: String) {
        setupDownloadListener()
        binding.offlinePage.visibility = View.GONE
        binding.webView.visibility = View.VISIBLE
        binding.webView.loadUrl(url)
    }

    // ─── URL Handling ─────────────────────────────────────────────────────────

    private fun handleUrl(url: String): Boolean {
        return when {
            // Google OAuth does not work safely inside Android WebView.
            // Open it in the phone's default browser so "Continue with Google" can complete.
            isGoogleAuthUrl(url) -> {
                openInBrowser(url)
                true
            }

            // External app schemes - always open externally
            url.startsWith("tel:") ||
            url.startsWith("mailto:") ||
            url.startsWith("sms:") ||
            url.startsWith("whatsapp:") ||
            url.startsWith("intent:") -> {
                openExternalApp(url)
                true
            }

            // Market links
            url.startsWith("market:") -> {
                openExternalApp(url)
                true
            }

            // External social media and known external domains
            isExternalLink(url) -> {
                openInBrowser(url)
                true
            }

            // Internal links - load in WebView
            url.startsWith("http://") || url.startsWith("https://") -> {
                false // Let WebView handle it
            }

            else -> false
        }
    }

    private fun isGoogleAuthUrl(url: String): Boolean {
        return try {
            val parsed = Uri.parse(url)
            val host = parsed.host?.lowercase() ?: return false
            val path = parsed.path?.lowercase() ?: ""
            val query = parsed.query?.lowercase() ?: ""
            val fullUrl = url.lowercase()

            host == "accounts.google.com" ||
                    host == "myaccount.google.com" ||
                    host == "accounts.youtube.com" ||
                    host == "apis.google.com" ||
                    (host.endsWith(".google.com") && (
                            path.contains("/signin") ||
                                    path.contains("/oauth") ||
                                    path.contains("/o/oauth2") ||
                                    query.contains("client_id=") ||
                                    fullUrl.contains("continue=https://accounts.google.com")
                            )) ||
                    (host.endsWith(".firebaseapp.com") && path.contains("/__/auth"))
        } catch (e: Exception) {
            false
        }
    }

    private fun isExternalLink(url: String): Boolean {
        val externalDomains = listOf(
            "wa.me", "api.whatsapp.com",
            "facebook.com", "fb.com", "m.facebook.com",
            "instagram.com", "www.instagram.com",
            "youtube.com", "youtu.be", "m.youtube.com",
            "twitter.com", "x.com", "t.co",
            "t.me", "telegram.me", "telegram.org",
            "tiktok.com", "vm.tiktok.com",
            "paypal.com", "www.paypal.com",
            "stripe.com",
            "maps.google.com", "goo.gl",
            "play.google.com"
        )
        return try {
            val host = Uri.parse(url).host?.lowercase() ?: return false
            externalDomains.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun openExternalApp(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // If no app found, open in browser
            openInBrowser(url)
        }
    }

    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open browser", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── File Upload / Camera ─────────────────────────────────────────────────

    private fun requestFileChooser(fileChooserParams: WebChromeClient.FileChooserParams?) {
        // Check camera permission first
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> {
                openFileChooserWithCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openFileChooserWithCamera() {
        val chooserIntents = mutableListOf<Intent>()

        // Camera intent
        try {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (cameraIntent.resolveActivity(packageManager) != null) {
                val photoFile = createImageFile()
                cameraImageUri = FileProvider.getUriForFile(
                    this,
                    "com.rabbihossainltd.app.fileprovider",
                    photoFile
                )
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                chooserIntents.add(cameraIntent)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Gallery intent
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryIntent.type = "image/*"

        // Also add general file picker
        val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
        fileIntent.type = "*/*"
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE)

        val chooserIntent = Intent.createChooser(galleryIntent, "Choose File")
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            chooserIntents.toTypedArray()
        )

        fileChooserLauncher.launch(chooserIntent)
    }

    private fun openFileChooserGalleryOnly() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        val chooserIntent = Intent.createChooser(intent, "Choose File")
        fileChooserLauncher.launch(chooserIntent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).also {
            currentPhotoPath = it.absolutePath
        }
    }

    // ─── Download Handling ────────────────────────────────────────────────────

    private fun setupDownloadListener() {
        binding.webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading file...")
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
                )
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this, "⬇ Download started...", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Fallback: open in browser
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } catch (ex: Exception) {
                    Toast.makeText(this, "Cannot download file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ─── Pull to Refresh ──────────────────────────────────────────────────────

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeColors(
                ContextCompat.getColor(this@MainActivity, R.color.neon_green),
                ContextCompat.getColor(this@MainActivity, R.color.neon_cyan)
            )
            setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(this@MainActivity, R.color.surface_dark)
            )
            setOnRefreshListener {
                if (isNetworkAvailable()) {
                    binding.webView.reload()
                } else {
                    isRefreshing = false
                    showOfflinePage()
                }
            }
        }
    }

    // ─── Retry Button ─────────────────────────────────────────────────────────

    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            if (isNetworkAvailable()) {
                hideOfflinePage()
                loadWebsite()
            } else {
                // Shake animation on button
                val shake = ObjectAnimator.ofFloat(
                    binding.retryButton, "translationX",
                    0f, -15f, 15f, -10f, 10f, -5f, 5f, 0f
                )
                shake.duration = 500
                shake.start()
                Toast.makeText(this, "Still offline. Please check your connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── Back Press Handling ──────────────────────────────────────────────────

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.offlinePage.visibility == View.VISIBLE -> {
                        showExitDialog()
                    }
                    binding.webView.canGoBack() -> {
                        binding.webView.goBack()
                    }
                    else -> {
                        showExitDialog()
                    }
                }
            }
        })
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Exit") { _, _ -> finish() }
            .setNegativeButton("Stay") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // ─── Offline / Error Page ─────────────────────────────────────────────────

    private fun showOfflinePage() {
        binding.webView.visibility = View.GONE
        binding.offlinePage.alpha = 0f
        binding.offlinePage.visibility = View.VISIBLE
        val fadeIn = ObjectAnimator.ofFloat(binding.offlinePage, "alpha", 0f, 1f)
        fadeIn.duration = 400
        fadeIn.start()
    }

    private fun hideOfflinePage() {
        val fadeOut = ObjectAnimator.ofFloat(binding.offlinePage, "alpha", 1f, 0f)
        fadeOut.duration = 300
        fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                binding.offlinePage.visibility = View.GONE
                binding.webView.visibility = View.VISIBLE
            }
        })
        fadeOut.start()
    }

    // ─── Network Check ────────────────────────────────────────────────────────

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ─── Load Website ─────────────────────────────────────────────────────────

    private fun loadWebsite() {
        loadUrlInWebView(WEBSITE_URL)
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
