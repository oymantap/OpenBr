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
import androidx.core.app.ActivityCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
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

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"
            
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_AUTO)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            // FIX: Biar nggak mental ke browser luar atau stuck
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                urlDisplayFake.text = url
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        btnHome.setOnClickListener { webView.loadUrl("https://www.google.com") }

        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            urlInputReal.setText(webView.url)
            urlInputReal.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlInputReal, InputMethodManager.SHOW_IMPLICIT)
        }

        btnClear.setOnClickListener { urlInputReal.text.clear() }

        btnBackSearch.setOnClickListener {
            searchLayer.visibility = View.GONE
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(urlInputReal.windowToken, 0)
        }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val url = if (query.contains(".") && !query.contains(" ")) {
                    if (query.startsWith("http")) query else "https://$query"
                } else "https://www.google.com/search?q=$query"
                
                webView.loadUrl(url)
                searchLayer.visibility = View.GONE
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }

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
                } else webView.reload()
                true
            }
            popup.show()
        }

        webView.setDownloadListener { url, _, _, _, _ ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        swipeRefresh.setOnRefreshListener { webView.reload() }

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 1)
        
        if (webView.url == null) {
            webView.loadUrl("https://www.google.com")
        }
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

