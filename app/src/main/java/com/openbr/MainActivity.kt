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
// DUA IMPORT KERAMAT BIAR GAK ERROR LAGI:
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var searchLayer: View
    private lateinit var urlInputReal: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var listHistory: ListView
    private lateinit var tabCountText: TextView // Tambahin ini
    private lateinit var mediaSession: MediaSession
    private val dataFile = "openbr_master.json"
    private var tabCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupMediaSession()
        initUI()
        
        intent?.data?.let { handleIncomingUri(it) }
    }

    private fun initUI() {
        webView = findViewById(R.id.webview)
        searchLayer = findViewById(R.id.search_focus_layer)
        urlInputReal = findViewById(R.id.url_input_real)
        progressBar = findViewById(R.id.progress_bar)
        listHistory = findViewById(R.id.list_history)
        tabCountText = findViewById(R.id.tab_count) // Hubungin ke XML lo
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // DARK MODE FIX: Maksa Website Ikut Gelap
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
                
                // Simpan Aktivitas (Bukan cuma search)
                url?.let { saveMasterData(it, view?.title ?: it, "activity") }
                updateMediaMetadata(view?.title ?: "Open Br")
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // Tiap buka link baru, angka tab nambah biar ga dummy
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

        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            showMasterData()
            urlInputReal.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlInputReal, 0)
        }

        findViewById<ImageButton>(R.id.btn_back_search).setOnClickListener { searchLayer.visibility = View.GONE }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val url = when {
                    query.startsWith("http") || query.startsWith("file://") -> query
                    query.contains("localhost") -> "http://$query"
                    query.contains(".") && !query.contains(" ") -> "https://$query"
                    else -> {
                        saveMasterData(query, "Pencarian Google", "search")
                        "https://www.google.com/search?q=$query"
                    }
                }
                webView.loadUrl(url)
                searchLayer.visibility = View.GONE
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }

        swipeRefresh.setOnRefreshListener { webView.reload() }
    }

    private fun handleIncomingUri(uri: Uri) {
        webView.loadUrl(uri.toString())
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "OpenBrMedia")
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)) }
            override fun onPause() { webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)) }
        })
        val state = PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
            .setState(PlaybackState.STATE_PLAYING, 0, 1.0f).build()
        mediaSession.setPlaybackState(state)
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

    private fun showMasterData() {
        try {
            val file = File(filesDir, dataFile)
            if (!file.exists()) return
            val json = JSONArray(file.readText())
            val items = mutableListOf<String>()
            val rawUrls = mutableListOf<String>()
            for (i in (json.length() - 1) downTo 0) {
                val obj = json.getJSONObject(i)
                val type = obj.getString("type")
                val display = if (type == "search") "🔍 Pencarian: ${obj.getString("val")}" 
                              else "🌐 ${obj.getString("title")}\n${obj.getString("val")}"
                items.add(display)
                rawUrls.add(obj.getString("val"))
            }
            listHistory.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items.distinct().take(30))
            listHistory.setOnItemClickListener { _, _, pos, _ ->
                val target = rawUrls[pos]
                webView.loadUrl(if (target.startsWith("http") || target.startsWith("file")) target else "https://google.com/search?q=$target")
                searchLayer.visibility = View.GONE
            }
        } catch (e: Exception) {}
    }

    override fun onBackPressed() {
        if (searchLayer.visibility == View.VISIBLE) searchLayer.visibility = View.GONE
        else if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
