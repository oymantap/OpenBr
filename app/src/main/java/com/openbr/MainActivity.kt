package com.openbr

import android.Manifest
import android.content.*
import android.content.res.Configuration
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
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
    private var mediaSession: MediaSession? = null
    private val historyFile = "openbr_data.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupMediaSession() // Inisialisasi Media Panel

        webView = findViewById(R.id.webview)
        searchLayer = findViewById(R.id.search_focus_layer)
        urlInputReal = findViewById(R.id.url_input_real)
        progressBar = findViewById(R.id.progress_bar)
        listHistory = findViewById(R.id.list_history)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                findViewById<TextView>(R.id.url_display_fake).text = view?.title ?: url
                updateMediaMetadata(view?.title ?: "Open Br", url ?: "")
                url?.let { saveToData(it, view?.title ?: it, "visit") }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, p: Int) {
                progressBar.progress = p
                progressBar.visibility = if (p < 100) View.VISIBLE else View.GONE
            }
        }

        // --- BUTTONS LOGIC ---
        findViewById<ImageView>(R.id.btn_home).setOnClickListener { webView.loadUrl("https://www.google.com") }

        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            showDataList()
            urlInputReal.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlInputReal, 0)
        }

        findViewById<ImageButton>(R.id.btn_back_search).setOnClickListener { searchLayer.visibility = View.GONE }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val isUrl = query.contains(".") && !query.contains(" ")
                val url = if (isUrl) (if (query.startsWith("http")) query else "https://$query")
                else { saveToData(query, query, "search"); "https://www.google.com/search?q=$query" }
                webView.loadUrl(url)
                searchLayer.visibility = View.GONE
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }

        findViewById<ImageButton>(R.id.btn_more).setOnClickListener { showMenu(it) }
        swipeRefresh.setOnRefreshListener { webView.reload() }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 1)
        webView.loadUrl("https://www.google.com")
    }

    // LOGIKA MEDIA PANEL NOTIFIKASI
    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "OpenBrMedia")
        val state = PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
            .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
            .build()
        
        mediaSession?.setPlaybackState(state)
        mediaSession?.setCallback(object : MediaSession.Callback() {
            override fun onPause() { webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)) }
            override fun onPlay() { webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)) }
        })
        mediaSession?.isActive = true
    }

    private fun updateMediaMetadata(title: String, url: String) {
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Open Br Browser")
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private fun showMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Dark Mode / Light Mode")
        popup.menu.add("Refresh")
        popup.menu.add("Hapus Riwayat")
        popup.setOnMenuItemClickListener {
            when(it.title) {
                "Dark Mode / Light Mode" -> {
                    val mode = if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) 
                        AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
                "Refresh" -> webView.reload()
                "Hapus Riwayat" -> { File(filesDir, historyFile).delete(); showDataList() }
            }
            true
        }
        popup.show()
    }

    private fun saveToData(value: String, title: String, type: String) {
        try {
            val file = File(filesDir, historyFile)
            val json = if (file.exists()) JSONArray(file.readText()) else JSONArray()
            val entry = JSONObject().apply { put("val", value); put("title", title); put("type", type) }
            json.put(entry)
            file.writeText(json.toString())
        } catch (e: Exception) {}
    }

    private fun showDataList() {
        try {
            val file = File(filesDir, historyFile)
            if (!file.exists()) { listHistory.adapter = null; return }
            val json = JSONArray(file.readText())
            val items = mutableListOf<String>()
            val values = mutableListOf<String>()
            for (i in (json.length() - 1) downTo 0) {
                val obj = json.getJSONObject(i)
                items.add(if (obj.getString("type") == "search") "🔍 ${obj.getString("val")}" else "🌐 ${obj.getString("title")}")
                values.add(obj.getString("val"))
            }
            listHistory.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items.distinct())
            listHistory.setOnItemClickListener { _, _, pos, _ ->
                val target = values[pos]
                webView.loadUrl(if (target.startsWith("http")) target else "https://google.com/search?q=$target")
                searchLayer.visibility = View.GONE
            }
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }
}

