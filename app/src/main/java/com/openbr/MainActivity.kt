package com.openbr

import android.Manifest
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var searchLayer: View
    private lateinit var urlInputReal: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var listHistory: ListView
    private lateinit var mediaSession: MediaSession
    private val dataFile = "openbr_master.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupMediaSession()
        initUI()
        
        // Handle Open File dari luar (ZArchiver dll)
        intent?.data?.let { handleIncomingUri(it) }
    }

    private fun initUI() {
        webView = findViewById(R.id.webview)
        searchLayer = findViewById(R.id.search_focus_layer)
        urlInputReal = findViewById(R.id.url_input_real)
        progressBar = findViewById(R.id.progress_bar)
        listHistory = findViewById(R.id.list_history)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // Force Dark Mode untuk konten Website
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
                
                // Simpan sebagai Aktivitas Website (Bukan Pencarian)
                url?.let { saveMasterData(it, view?.title ?: it, "activity", view?.favicon) }
                updateMediaMetadata(view?.title ?: "Open Br")
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
        }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val url = when {
                    query.startsWith("http") || query.startsWith("file://") -> query
                    query.contains("localhost") -> "http://$query"
                    query.contains(".") && !query.contains(" ") -> "https://$query"
                    else -> {
                        saveMasterData(query, "Pencarian Google", "search", null)
                        "https://www.google.com/search?q=$query"
                    }
                }
                webView.loadUrl(url)
                searchLayer.visibility = View.GONE
            }
            true
        }
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
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT)
            .setState(PlaybackState.STATE_PLAYING, 0, 1.0f).build()
        mediaSession.setPlaybackState(state)
        mediaSession.isActive = true
    }

    private fun updateMediaMetadata(title: String) {
        val meta = MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title).build()
        mediaSession.setMetadata(meta)
    }

    private fun saveMasterData(valStr: String, title: String, type: String, favicon: Any?) {
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
            for (i in (json.length() - 1) downTo 0) {
                val obj = json.getJSONObject(i)
                val prefix = if (obj.getString("type") == "search") "🔍" else "🌐"
                items.add("$prefix ${obj.getString("title")}\n${obj.getString("val")}")
            }
            listHistory.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items.distinct().take(30))
        } catch (e: Exception) {}
    }
}

