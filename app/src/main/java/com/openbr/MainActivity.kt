package com.openbr

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlInput: EditText
    private lateinit var btnClear: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        webView = findViewById(R.id.webview)
        urlInput = findViewById(R.id.url_input)
        btnClear = findViewById(R.id.btn_clear)

        setupWebView()

        // Tombol Silang (X) buat hapus semua
        btnClear.setOnClickListener {
            urlInput.text.clear()
        }

        urlInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val input = v.text.toString().trim()
                val url = if (input.contains(".") && !input.contains(" ")) {
                    if (input.startsWith("http")) input else "https://$input"
                } else {
                    "https://www.google.com/search?q=$input"
                }
                webView.loadUrl(url)
                urlInput.clearFocus() // Tutup keyboard
                true
            } else false
        }
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                supportActionBar?.title = view?.title
                
                // LOGIKA PINTAR: Jika URL search, tampilin query-nya aja
                if (url != null && url.contains("google.com/search?q=")) {
                    val uri = Uri.parse(url)
                    val query = uri.getQueryParameter("q")
                    urlInput.setText(query)
                } else {
                    urlInput.setText(url)
                }
            }
        }
        webView.loadUrl("https://www.google.com")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}

