package com.openbr

import android.os.Bundle
import android.view.*
import android.webkit.WebChromeClient
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
    private lateinit var progressBar: ProgressBar
    private var isUiHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inisialisasi View
        webView = findViewById(R.id.webview)
        appBar = findViewById(R.id.app_bar)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        progressBar = findViewById(R.id.progress_bar)
        val urlInput = findViewById<EditText>(R.id.url_input)
        val btnMore = findViewById<ImageButton>(R.id.btn_more)
        val webContainer = findViewById<FrameLayout>(R.id.web_container)
        val btnHome = findViewById<ImageView>(R.id.btn_home)
        val rightButtons = findViewById<LinearLayout>(R.id.right_buttons)

        // 2. Fix Swipe Refresh (Biar gak ganggu pas scroll web di tengah)
        swipeRefresh.viewTreeObserver.addOnScrollChangedListener {
            swipeRefresh.isEnabled = webView.scrollY == 0
        }
        swipeRefresh.setOnRefreshListener { webView.reload() }

        // 3. Webview Settings & Barload (ProgressBar)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        
        // Logika Barload (Garis Biru lari)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefresh.isRefreshing = false
                urlInput.setText(url)
            }
        }

        // 4. Fokus Search ala Chrome (Input membesar)
        urlInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                btnHome.visibility = View.GONE
                rightButtons.visibility = View.GONE
            } else {
                btnHome.visibility = View.VISIBLE
                rightButtons.visibility = View.VISIBLE
            }
        }

        // 5. Double Tap to Hide UI (Focus Mode)
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isUiHidden = !isUiHidden
                appBar.visibility = if (isUiHidden) View.GONE else View.VISIBLE
                return true
            }
        })

        webContainer.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false 
        }

        // 6. Menu Popup Titik Tiga
        btnMore.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("Refresh")
            popup.menu.add("DNS Bawaan")
            popup.menu.add("Pengaturan")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Refresh" -> webView.reload()
                }
                true
            }
            popup.show()
        }

        // 7. Navigasi & Enter URL
        btnHome.setOnClickListener { webView.loadUrl("https://www.google.com") }

        urlInput.setOnEditorActionListener { v, _, _ ->
            val input = v.text.toString().trim()
            if (input.isNotEmpty()) {
                val url = if (input.contains(".") && !input.contains(" ")) {
                    if (input.startsWith("http")) input else "https://$input"
                } else "https://www.google.com/search?q=$input"
                webView.loadUrl(url)
                urlInput.clearFocus()
            }
            true
        }

        webView.loadUrl("https://www.google.com")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
