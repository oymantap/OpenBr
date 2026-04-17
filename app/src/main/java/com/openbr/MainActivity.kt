package com.openbr

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var searchLayer: View
    private lateinit var urlDisplayFake: TextView
    private lateinit var urlInputReal: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Binding UI
        webView = findViewById(R.id.webview)
        searchLayer = findViewById(R.id.search_focus_layer)
        urlDisplayFake = findViewById(R.id.url_display_fake)
        urlInputReal = findViewById(R.id.url_input_real)
        progressBar = findViewById(R.id.progress_bar)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val btnHome = findViewById<ImageView>(R.id.btn_home)
        val btnClear = findViewById<ImageButton>(R.id.btn_clear_text)
        val btnBackSearch = findViewById<ImageButton>(R.id.btn_back_search)
        val btnMore = findViewById<ImageButton>(R.id.btn_more)

        // --- ENGINE SETTINGS (Fix Loading) ---
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                urlDisplayFake.text = url
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false // Biar tetep buka di dalam aplikasi
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
            }
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        // --- LOGIC TOMBOL ---

        // Tombol Home (Kembali ke Google atau awal)
        btnHome.setOnClickListener {
            webView.loadUrl("https://www.google.com")
        }

        // Buka Search Focus
        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            urlInputReal.setText(webView.url)
            urlInputReal.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlInputReal, InputMethodManager.SHOW_IMPLICIT)
        }

        // Tombol X (Hapus Teks) - FIX
        btnClear.setOnClickListener {
            urlInputReal.text.clear()
        }

        // Tombol Back di Search - FIX
        btnBackSearch.setOnClickListener {
            searchLayer.visibility = View.GONE
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(urlInputReal.windowToken, 0)
        }

        // Input URL / Search
        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val url = if (query.contains(".") && !query.contains(" ")) {
                    if (query.startsWith("http")) query else "https://$query"
                } else "https://www.google.com/search?q=$query"
                webView.loadUrl(url)
                searchLayer.visibility = View.GONE
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(urlInputReal.windowToken, 0)
            }
            true
        }

        // Menu Titik Tiga
        btnMore.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("Bagikan")
            popup.menu.add("Refresh")
            popup.setOnMenuItemClickListener {
                if (it.title == "Bagikan") {
                    val i = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, webView.url)
                    }
                    startActivity(Intent.createChooser(i, "Share"))
                } else { webView.reload() }
                true
            }
            popup.show()
        }

        // Download
        webView.setDownloadListener { url, _, _, _, _ ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        swipeRefresh.setOnRefreshListener { webView.reload() }

        // Load awal
        webView.loadUrl("https://www.google.com")
        
        // Minta Izin Kamera/Mic
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 1)
    }

    override fun onBackPressed() {
        if (searchLayer.visibility == View.VISIBLE) {
            searchLayer.visibility = View.GONE
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
