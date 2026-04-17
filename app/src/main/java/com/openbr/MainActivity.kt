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
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        appBar = findViewById(R.id.app_bar)
        progressBar = findViewById(R.id.progress_bar)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        val urlInput = findViewById<EditText>(R.id.url_input)
        val btnMore = findViewById<ImageButton>(R.id.btn_more)
        val sensorArea = findViewById<FrameLayout>(R.id.gesture_sensor_area)

        // --- 1. SETTING WEBVIEW BIAR GAK MACET ---
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowContentAccess = true
            allowFileAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true) // Penting buat link dari Google
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false // Biar semua link dibuka di dalam Open Br, bukan pindah app
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefresh.isRefreshing = false
                urlInput.setText(url)
            }
        }

        // --- 2. DOUBLE TAP HANYA DI HEADER ---
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isUiHidden = true
                appBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Ketuk layar mana saja untuk memunculkan UI", Toast.LENGTH_SHORT).show()
                return true
            }
        })

        // Sensor area cuma di header (Gak ganggu game/web)
        sensorArea.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        // Ketuk bebas di WebView buat balikin UI kalau lagi hidden
        webView.setOnTouchListener { _, event ->
            if (isUiHidden && event.action == MotionEvent.ACTION_DOWN) {
                appBar.visibility = View.VISIBLE
                isUiHidden = false
            }
            false
        }

        // --- 3. FIX SWIPE REFRESH ---
        swipeRefresh.viewTreeObserver.addOnScrollChangedListener {
            swipeRefresh.isEnabled = webView.scrollY == 0
        }
        swipeRefresh.setOnRefreshListener { webView.reload() }

        // --- 4. ENTER URL & SEARCH ---
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

        // --- 5. MENU PREMIUM (Radius) ---
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

