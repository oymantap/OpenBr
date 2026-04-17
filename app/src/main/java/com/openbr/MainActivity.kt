package com.openbr

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val homeUrl = "https://www.google.com" // Link Home Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        webView = findViewById(R.id.webview)
        val urlInput = findViewById<EditText>(R.id.url_input)

        setupWebView()

        // Buka Google pas pertama start
        webView.loadUrl(homeUrl)

        urlInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val input = v.text.toString()
                val url = if (input.contains(".")) {
                    if (input.startsWith("http")) input else "https://$input"
                } else {
                    "https://www.google.com/search?q=$input" // Default search Google
                }
                webView.loadUrl(url)
                true
            } else false
        }
    }

    private fun setupWebView() {
        val settings = webView.settings
        
        // --- BIAR LOGIN AWET (COOKIES) ---
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        
        // Penting: Izinkan pihak ketiga (Google) simpan cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                supportActionBar?.title = view?.title
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Tambahkan tombol Home dan Reload
        menu?.add(0, 0, 0, "Home")?.setIcon(android.R.drawable.ic_menu_today)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu?.add(0, 1, 1, "Reload")
        menu?.add(0, 2, 2, "Settings")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            0 -> { // Fungsi Tombol Home
                webView.loadUrl(homeUrl)
                true
            }
            1 -> {
                webView.reload()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}

