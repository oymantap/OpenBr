package com.openbr

import android.Manifest
import android.content.*
import android.content.res.Configuration
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var searchLayer: View
    private lateinit var activityLayer: View // Layer Khusus Aktivitas
    private lateinit var urlInputReal: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tabCountText: TextView
    private lateinit var mediaSession: MediaSession
    private val dataFile = "openbr_v2.json"
    private var tabCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupMediaSession()
        initUI()
        
        // FIX: Biar pas buka gak kosong!
        val targetUrl = intent?.data?.toString() ?: "https://www.google.com"
        webView.loadUrl(targetUrl)
    }

    private fun initUI() {
        webView = findViewById(R.id.webview)
        searchLayer = findViewById(R.id.search_focus_layer)
        activityLayer = findViewById(R.id.activity_layer) // Tambahin di XML nanti
        urlInputReal = findViewById(R.id.url_input_real)
        progressBar = findViewById(R.id.progress_bar)
        tabCountText = findViewById(R.id.tab_count)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_ON)
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                findViewById<TextView>(R.id.url_display_fake).text = view?.title ?: url
                
                // Simpan Aktivitas (🌐)
                url?.let { if(!it.contains("google.com/search")) saveMasterData(it, view?.title ?: it, "activity") }
                updateMediaMetadata(view?.title ?: "Open Br")
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                tabCount++
                tabCountText.text = tabCount.toString()
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, p: Int) {
                progressBar.progress = p
                progressBar.visibility = if (p < 100) View.VISIBLE else View.GONE
            }
        }

        // TOMBOL HOME (Balik ke Google)
        findViewById<ImageView>(R.id.btn_home).setOnClickListener {
            webView.loadUrl("https://www.google.com")
        }

        // SEARCH BAR CLICK
        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            showHistoryList(findViewById(R.id.list_history_search), "search")
            urlInputReal.requestFocus()
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(urlInputReal, 0)
        }

        // TOMBOL SETTINGS (MENU MODERN)
        findViewById<ImageButton>(R.id.btn_more).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_refresh -> webView.reload()
                    R.id.menu_activity -> {
                        activityLayer.visibility = View.VISIBLE
                        showHistoryList(findViewById(R.id.list_activity_real), "activity")
                    }
                    R.id.menu_dark_mode -> {
                        val mode = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) 
                            AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
                        AppCompatDelegate.setDefaultNightMode(mode)
                    }
                }
                true
            }
            popup.show()
        }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val url = when {
                    query.startsWith("http") || query.startsWith("file://") -> query
                    query.contains("localhost") -> "http://$query"
                    query.contains(".") && !query.contains(" ") -> "https://$query"
                    else -> {
                        saveMasterData(query, "Pencarian", "search")
                        "https://www.google.com/search?q=$query"
                    }
                }
                webView.loadUrl(url)
                searchLayer.visibility = View.GONE
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }

        findViewById<ImageButton>(R.id.btn_close_activity).setOnClickListener { activityLayer.visibility = View.GONE }
        findViewById<ImageButton>(R.id.btn_back_search).setOnClickListener { searchLayer.visibility = View.GONE }
        swipeRefresh.setOnRefreshListener { webView.reload() }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "OpenBrMedia")
        mediaSession.isActive = true
    }

    private fun updateMediaMetadata(title: String) {
        val meta = MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title).build()
        mediaSession.setMetadata(meta)
    }

    private fun saveMasterData(valStr: String, title: String, type: String) {
        try {
            val file = File(filesDir, dataFile)
            val json = if (file.exists()) JSONArray(file.readText()) else JSONArray()
            val obj = JSONObject().apply {
                put("val", valStr); put("title", title); put("type", type); put("time", System.currentTimeMillis())
            }
            json.put(obj)
            file.writeText(json.toString())
        } catch (e: Exception) {}
    }

    private fun showHistoryList(listView: ListView, filterType: String) {
        try {
            val file = File(filesDir, dataFile)
            if (!file.exists()) return
            val json = JSONArray(file.readText())
            val items = mutableListOf<String>()
            val rawUrls = mutableListOf<String>()
            
            for (i in (json.length() - 1) downTo 0) {
                val obj = json.getJSONObject(i)
                if (obj.getString("type") == filterType) {
                    val icon = if (filterType == "search") "🔍 " else "🌐 "
                    items.add("$icon${obj.getString("title")}\n${obj.getString("val")}")
                    rawUrls.add(obj.getString("val"))
                }
            }
            listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items.distinct())
            listView.setOnItemClickListener { _, _, pos, _ ->
                val target = rawUrls[pos]
                webView.loadUrl(if (target.startsWith("http")) target else "https://google.com/search?q=$target")
                searchLayer.visibility = View.GONE
                activityLayer.visibility = View.GONE
            }
        } catch (e: Exception) {}
    }

    override fun onBackPressed() {
        if (activityLayer.visibility == View.VISIBLE) activityLayer.visibility = View.GONE
        else if (searchLayer.visibility == View.VISIBLE) searchLayer.visibility = View.GONE
        else if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}

