package com.openbr

import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var appBar: AppBarLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var isUiHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // PAKSA HP PAKAI MESIN GRAFIS (Biar gak macet ngerender web berat)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        appBar = findViewById(R.id.app_bar)
        progressBar = findViewById(R.id.progress_bar)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        val urlInput = findViewById<EditText>(R.id.url_input)
        val btnMore = findViewById<ImageButton>(R.id.btn_more)
        val sensorArea = findViewById<FrameLayout>(R.id.gesture_sensor_area)

        // --- SETTING WEBVIEW SUPER POWER ---
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            
            // Biar bisa buka link dari Google Search (Redirect Fix)
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            
            // Setting tampilan biar gak kaku
            useWideViewPort = true
            loadWithOverviewMode = true
            
            // Penting: Biar lancar ngerender gambar & script website modern
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT 
        }

        // BIAR SMOOTH SCROLLING
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = false

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }

        webView.webViewClient = object : WebViewClient() {
            // FIX UTAMA: Tangani Error SSL (Sering bikin macet di awal loading)
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                handler?.proceed() // Lanjut terus meskipun ada peringatan sertifikat (ala browser mod)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // Jangan lempar link ke aplikasi lain, tetep di Open Br
                return false 
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefresh.isRefreshing = false
                urlInput.setText(url)
            }
        }

        // --- DOUBLE TAP DI HEADER SAJA ---
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isUiHidden = true
                appBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "UI Hidden. Tap layar web sekali untuk mengembalikan.", Toast.LENGTH_SHORT).show()
                return true
            }
        })

        sensorArea.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        // Tap di web sekali buat balikin UI
        webView.setOnTouchListener { _, event ->
            if (isUiHidden && event.action == MotionEvent.ACTION_DOWN) {
                appBar.visibility = View.VISIBLE
                isUiHidden = false
            }
            false
        }

        // --- NAVIGASI ---
        swipeRefresh.viewTreeObserver.addOnScrollChangedListener {
            swipeRefresh.isEnabled = webView.scrollY == 0
        }
        swipeRefresh.setOnRefreshListener { webView.reload() }

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

        btnMore.setOnClickListener { view ->
            val popup = PopupMenu(this, view, Gravity.END, 0, R.style.CustomPopupStyle)
            popup.menu.add("Refresh")
            popup.menu.add("DNS (Coming Soon)")
            popup.menu.add("Pengaturan")
            popup.show()
        }

        webView.loadUrl("https://www.google.com")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}

