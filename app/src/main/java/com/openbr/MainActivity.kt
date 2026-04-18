package com.openbr

import android.content.*
import android.content.res.Configuration
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var searchLayer: View
    private lateinit var activityLayer: View
    private lateinit var urlInputReal: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tabCountText: TextView
    private lateinit var listHistorySearch: ListView
    private lateinit var listActivityReal: ListView
    private lateinit var mediaSession: MediaSession
    private val dataFile = "openbr_v2.json"
    private var tabCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupMediaSession()
        initUI()
        
        // Load awal (Google atau file dari luar)
        val startUrl = intent?.data?.toString() ?: "https://www.google.com"
        webView.loadUrl(startUrl)
    }

    private fun initUI() {
        webView = findViewById(R.id.webview)
        searchLayer = findViewById(R.id.search_focus_layer)
        activityLayer = findViewById(R.id.activity_layer)
        urlInputReal = findViewById(R.id.url_input_real)
        progressBar = findViewById(R.id.progress_bar)
        tabCountText = findViewById(R.id.tab_count)
        listHistorySearch = findViewById(R.id.list_history_search)
        listActivityReal = findViewById(R.id.list_activity_real)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            
            // Fix Dark Header / Website Content
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
                
                // Simpan Aktivitas Website (🌐)
                url?.let { 
                    if (!it.contains("google.com/search")) saveMasterData(it, view?.title ?: it, "activity") 
                }
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                tabCount++; tabCountText.text = tabCount.toString()
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, p: Int) {
                progressBar.progress = p
                progressBar.visibility = if (p < 100) View.VISIBLE else View.GONE
            }
        }

        // --- TOMBOL-TOMBOL ---
        findViewById<ImageView>(R.id.btn_home).setOnClickListener { webView.loadUrl("https://www.google.com") }
        
        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            showHistoryList(listHistorySearch, "search")
            urlInputReal.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlInputReal, 0)
        }

        findViewById<ImageButton>(R.id.btn_more).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_refresh -> webView.reload()
                    R.id.menu_activity -> {
                        activityLayer.visibility = View.VISIBLE
                        showHistoryList(listActivityReal, "activity")
                    }
                }
                true
            }
            popup.show()
        }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val url = if (query.contains(".") && !query.contains(" ")) "https://$query" else "https://www.google.com/search?q=$query"
                if (!query.contains(".")) saveMasterData(query, "Pencarian", "search")
                webView.loadUrl(url)
                searchLayer.visibility = View.GONE
            }
            true
        }

        findViewById<ImageButton>(R.id.btn_back_search).setOnClickListener { searchLayer.visibility = View.GONE }
        findViewById<ImageButton>(R.id.btn_close_activity).setOnClickListener { activityLayer.visibility = View.GONE }
        swipeRefresh.setOnRefreshListener { webView.reload() }
    }

    private fun saveMasterData(valStr: String, title: String, type: String) {
        try {
            val file = File(filesDir, dataFile)
            val json = if (file.exists()) JSONArray(file.readText()) else JSONArray()
            json.put(JSONObject().apply { put("val", valStr); put("title", title); put("type", type) })
            file.writeText(json.toString())
        } catch (e: Exception) {}
    }

    private fun showHistoryList(listView: ListView, filter: String) {
        val file = File(filesDir, dataFile)
        if (!file.exists()) return
        val json = JSONArray(file.readText())
        val items = mutableListOf<String>()
        val urls = mutableListOf<String>()
        for (i in (json.length() - 1) downTo 0) {
            val obj = json.getJSONObject(i)
            if (obj.getString("type") == filter) {
                items.add(obj.getString("title") + "\n" + obj.getString("val"))
                urls.add(obj.getString("val"))
            }
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items.distinct())
        listView.setOnItemClickListener { _, _, pos, _ ->
            webView.loadUrl(if (urls[pos].startsWith("http")) urls[pos] else "https://google.com/search?q=${urls[pos]}")
            searchLayer.visibility = View.GONE; activityLayer.visibility = View.GONE
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "OpenBr")
        mediaSession.isActive = true
    }
}

