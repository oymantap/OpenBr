package com.openbr

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView = findViewById<WebView>(R.id.webview)
        val urlInput = findViewById<EditText>(R.id.url_input)

        // Setting agar web lancar & modern
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()

        // Buka Google pas pertama start
        webView.loadUrl("https://google.com")

        urlInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val input = v.text.toString()
                val url = if (input.contains(".")) {
                    if (input.startsWith("http")) input else "https://$input"
                } else {
                    "https://duckduckgo.com/?q=$input"
                }
                webView.loadUrl(url)
                true
            } else false
        }
    }
}

