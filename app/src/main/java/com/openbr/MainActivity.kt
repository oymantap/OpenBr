package com.openbr

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var siteTitle: TextView
    private lateinit var siteUrlDisplay: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        siteTitle = findViewById(R.id.site_title)
        siteUrlDisplay = findViewById(R.id.site_url_display)
        val urlInput = findViewById<EditText>(R.id.url_input)

        // Fitur Tarik Layar buat Reload
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }

        setupWebView(urlInput)

        // Tombol Back di Header
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }

        // Tombol Silang (X)
        findViewById<ImageView>(R.id.btn_clear).setOnClickListener {
            urlInput.text.clear()
        }

        urlInput.setOnEditorActionListener { v, _, _ ->
            val input = v.text.toString().trim()
            if (input.isNotEmpty()) {
                val url = if (input.contains(".") && !input.contains(" ")) {
                    if (input.startsWith("http")) input else "https://$input"
                } else "https://www.google.com/search?q=$input"
                webView.loadUrl(url)
            }
            urlInput.clearFocus()
            true
        }
    }

    private fun setupWebView(urlInput: EditText) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false // Berhenti muter pas kelar reload
                siteTitle.text = view?.title
                siteUrlDisplay.text = url
                urlInput.setText(url)
            }
        }
        webView.loadUrl("https://www.google.com")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
