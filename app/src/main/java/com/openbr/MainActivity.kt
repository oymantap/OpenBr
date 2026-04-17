package com.openbr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout

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

        // Binding
        webView = findViewById(R.id.webview)
        searchLayer = findViewById(R.id.search_focus_layer)
        urlDisplayFake = findViewById(R.id.url_display_fake)
        urlInputReal = findViewById(R.id.url_input_real)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val btnMore = findViewById<ImageButton>(R.id.btn_more)

        // --- WEBVIEW SETTINGS ---
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"
            mediaPlaybackRequiresUserGesture = false // Biar notif panel peka
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefresh.isRefreshing = false
                urlDisplayFake.text = url
            }
        }

        // --- DOWNLOAD & SHARE API ---
        webView.setDownloadListener { url, _, _, _, _ ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // --- SEARCH FOCUS MODE LOGIC ---
        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            urlInputReal.requestFocus()
            urlInputReal.setText(webView.url)
            urlInputReal.selectAll()
        }

        findViewById<ImageButton>(R.id.btn_back_search).setOnClickListener {
            searchLayer.visibility = View.GONE
        }

        findViewById<ImageButton>(R.id.btn_clear_text).setOnClickListener {
            urlInputReal.text.clear()
        }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                historyList.add(0, query)
                val url = if (query.contains(".") && !query.contains(" ")) {
                    if (query.startsWith("http")) query else "https://$query"
                } else "https://www.google.com/search?q=$query"
                
                webView.loadUrl(url)
                searchLayer.visibility = View.GONE
            }
            true
        }

        // --- MENU MORE ---
        btnMore.setOnClickListener { view ->
            val popup = PopupMenu(this, view, Gravity.END, 0, R.style.CustomPopupStyle)
            popup.menu.add("Bagikan Halaman")
            popup.menu.add("Refresh")
            popup.setOnMenuItemClickListener {
                if (it.title == "Bagikan Halaman") {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, webView.url)
                    }
                    startActivity(Intent.createChooser(intent, "Share via Open Br"))
                } else if (it.title == "Refresh") webView.reload()
                true
            }
            popup.show()
        }

        webView.loadUrl("https://www.google.com")
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

