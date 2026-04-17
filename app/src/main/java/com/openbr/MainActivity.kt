package com.openbr

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var siteTitle: TextView
    private lateinit var faviconImg: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        siteTitle = findViewById(R.id.site_title)
        faviconImg = findViewById(R.id.favicon)
        val urlInput = findViewById<EditText>(R.id.url_input)

        // Setup Buttons dari Sketsa Rycl
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { 
            if (webView.canGoBack()) webView.goBack() 
        }
        findViewById<ImageView>(R.id.btn_clear).setOnClickListener { urlInput.text.clear() }
        
        // Setup WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                siteTitle.text = "Loading..."
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                siteTitle.text = view?.title
                urlInput.setText(url)
                // Coba ambil favicon (butuh WebChromeClient buat hasil lebih akurat, tapi ini basic-nya)
                val icon = view?.favicon
                if (icon != null) faviconImg.setImageBitmap(icon)
            }
        }

        webView.loadUrl("https://www.google.com")
        
        // Logika Enter URL (sama kayak sebelumnya)
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
}

