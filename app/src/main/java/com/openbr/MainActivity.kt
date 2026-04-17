package com.openbr

import android.os.Bundle
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var appBar: AppBarLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var isUiHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        appBar = findViewById(R.id.app_bar)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        val urlInput = findViewById<EditText>(R.id.url_input)
        val btnMore = findViewById<ImageButton>(R.id.btn_more)
        val webContainer = findViewById<FrameLayout>(R.id.web_container)

        // --- DOUBLE TAP TO HIDE LOGIC ---
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isUiHidden = !isUiHidden
                appBar.visibility = if (isUiHidden) View.GONE else View.VISIBLE
                return true
            }
        })

        // Biar WebView tetep bisa disentuh tapi gesture kedeteksi
        webContainer.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false 
        }

        // --- MENU POPUP ---
        btnMore.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("Refresh")
            popup.menu.add("DNS Bawaan")
            popup.menu.add("Pengaturan")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Refresh" -> webView.reload()
                    "DNS Bawaan" -> Toast.makeText(this, "DNS aktif!", Toast.LENGTH_SHORT).show()
                }
                true
            }
            popup.show()
        }

        // --- WEBVIEW SETUP ---
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefresh.isRefreshing = false
                urlInput.setText(url)
            }
        }

        swipeRefresh.setOnRefreshListener { webView.reload() }
        findViewById<ImageView>(R.id.btn_home).setOnClickListener { webView.loadUrl("https://www.google.com") }

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

        webView.loadUrl("https://www.google.com")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}

