package com.openbr

import android.graphics.Bitmap
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        urlInput = findViewById(R.id.url_input)

        setupWebView()

        // Fungsi Load URL saat di-Enter
        urlInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = v.text.toString().trim()
                if (input.isNotEmpty()) {
                    val url = if (input.contains(".") && !input.contains(" ")) {
                        if (input.startsWith("http")) input else "https://$input"
                    } else {
                        "https://duckduckgo.com/?q=${input.replace(" ", "+")}"
                    }
                    webView.loadUrl(url)
                }
                true
            } else false
        }
    }

    private fun setupWebView() {
        val settings = webView.settings
        
        // --- AKTIFKAN PENYIMPANAN DATA (Biar folder com.openbr isi data) ---
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true    // WAJIB buat web modern
        settings.databaseEnabled = true       // Bikin folder databases
        settings.allowFileAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT // Simpan cache di disk
        
        // User Agent modern agar tidak dianggap bot
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                urlInput.setText(url) // Update bar URL saat loading mulai
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Pastikan cache beneran ketulis ke disk saat page selesai
                webView.requestLayout()
            }
        }

        webView.loadUrl("https://www.google.com")
    }

    // Tombol BACK biar navigasi web lancar
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

