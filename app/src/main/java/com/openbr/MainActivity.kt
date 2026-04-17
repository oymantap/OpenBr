package com.openbr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.animation.AlphaAnimation
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var searchLayer: View
    private lateinit var urlDisplayFake: TextView
    private lateinit var urlInputReal: EditText
    private val historyList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        setContentView(R.layout.activity_main)

        // Izin Otomatis
        checkPermissions()

        webView = findViewById(R.id.webview)
        searchLayer = findViewById(R.id.search_focus_layer)
        urlDisplayFake = findViewById(R.id.url_display_fake)
        urlInputReal = findViewById(R.id.url_input_real)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val btnMore = findViewById<ImageButton>(R.id.btn_more)

        // --- WEBVIEW ENGINE ---
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // DARK MODE SUPPORT
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_AUTO)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
            // API UNTUK KAMERA & MIC
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefresh.isRefreshing = false
                urlDisplayFake.text = url
            }
        }

        // --- DOWNLOAD & SHARE ---
        webView.setDownloadListener { url, _, _, _, _ ->
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(i)
        }

        // --- SEARCH FOCUS DENGAN ANIMASI ---
        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            showSearchLayer()
        }

        findViewById<ImageButton>(R.id.btn_back_search).setOnClickListener {
            hideSearchLayer()
        }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val url = if (query.contains(".") && !query.contains(" ")) {
                    if (query.startsWith("http")) query else "https://$query"
                } else "https://www.google.com/search?q=$query"
                webView.loadUrl(url)
                hideSearchLayer()
            }
            true
        }

        // --- MENU TITIK TIGA ---
        btnMore.setOnClickListener { view ->
            val popup = PopupMenu(this, view, Gravity.END)
            popup.menu.add("Bagikan Halaman")
            popup.menu.add("Refresh")
            popup.menu.add("Unduhan")
            popup.setOnMenuItemClickListener {
                when(it.title) {
                    "Bagikan Halaman" -> {
                        val s = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, webView.url)
                        }
                        startActivity(Intent.createChooser(s, "Share"))
                    }
                    "Refresh" -> webView.reload()
                    "Unduhan" -> Toast.makeText(this, "Cek folder Download HP", Toast.LENGTH_SHORT).show()
                }
                true
            }
            popup.show()
        }

        webView.loadUrl("https://www.google.com")
    }

    private fun showSearchLayer() {
        searchLayer.visibility = View.VISIBLE
        val anim = AlphaAnimation(0.0f, 1.0f).apply { duration = 300 }
        searchLayer.startAnimation(anim)
        urlInputReal.requestFocus()
    }

    private fun hideSearchLayer() {
        val anim = AlphaAnimation(1.0f, 0.0f).apply { duration = 200 }
        searchLayer.startAnimation(anim)
        searchLayer.visibility = View.GONE
    }

    private fun checkPermissions() {
        val list = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, list, 1)
        }
    }

    override fun onBackPressed() {
        if (searchLayer.visibility == View.VISIBLE) hideSearchLayer()
        else if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}

