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

        // Init MediaSession supaya kontrol notif muncul saat muter video/musik
        setupMediaSession()

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
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false 
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
        }

        webView.addJavascriptInterface(WebShareInterface(this), "AndroidShare")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                findViewById<TextView>(R.id.url_display_fake).text = view?.title ?: url
                
                // Update Metadata Media saat halaman berubah
                updateMediaInfo(view?.title ?: "Open Br", url ?: "")
                url?.let { saveToData(it, view?.title ?: it, "visit") }
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, p: Int) {
                progressBar.progress = p
                progressBar.visibility = if (p < 100) View.VISIBLE else View.GONE
            }
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        findViewById<ImageView>(R.id.btn_home).setOnClickListener { webView.loadUrl("https://www.google.com") }

        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            showDataList()
            urlInputReal.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlInputReal, 0)
        }

        findViewById<ImageButton>(R.id.btn_back_search).setOnClickListener { searchLayer.visibility = View.GONE }
        findViewById<ImageButton>(R.id.btn_clear_text).setOnClickListener { urlInputReal.text.clear() }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val isUrl = query.contains(".") && !query.contains(" ")
                val url = if (isUrl) {
                    if (query.startsWith("http")) query else "https://$query"
                } else {
                    saveToData(query, query, "search")
                    "https://www.google.com/search?q=$query"
                }
                webView.loadUrl(url)
                searchLayer.visibility = View.GONE
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }

        findViewById<ImageButton>(R.id.btn_more).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("Refresh")
            popup.menu.add("Dark Mode / Light Mode")
            popup.menu.add("Bagikan")
            popup.menu.add("Hapus Riwayat")
            popup.setOnMenuItemClickListener {
                when(it.title) {
                    "Refresh" -> webView.reload()
                    "Dark Mode / Light Mode" -> toggleTheme()
                    "Bagikan" -> shareUrl()
                    "Hapus Riwayat" -> { File(filesDir, historyFile).delete(); showDataList() }
                }
                true
            }
            popup.show()
        }

        swipeRefresh.setOnRefreshListener { webView.reload() }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 1)
        webView.loadUrl("https://www.google.com")
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "OpenBrSession")
        mediaSession?.isActive = true
        
        val state = PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT)
            .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()
        mediaSession?.setPlaybackState(state)
    }

    private fun updateMediaInfo(title: String, url: String) {
        val meta = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Open Br Browser")
            .build()
        mediaSession?.setMetadata(meta)
    }

    private fun toggleTheme() {
        val currentMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val mode = if (currentMode == Configuration.UI_MODE_NIGHT_YES) 
            AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun shareUrl() {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, webView.url)
        }
        startActivity(Intent.createChooser(i, "Bagikan via Open Br"))
    }

    private fun saveToData(value: String, title: String, type: String) {
        try {
            val file = File(filesDir, historyFile)
            val json = if (file.exists()) JSONArray(file.readText()) else JSONArray()
            for (i in 0 until json.length()) {
                if (json.getJSONObject(i).getString("val") == value) return
            }
            val entry = JSONObject().apply {
                put("val", value); put("title", title); put("type", type); put("time", System.currentTimeMillis())
            }
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
            val rawValues = mutableListOf<String>()
            for (i in (json.length() - 1) downTo 0) {
                val obj = json.getJSONObject(i)
                val type = obj.getString("type")
                val label = if (type == "search") "🔍 ${obj.getString("val")}" else "🌐 ${obj.getString("title")}"
                items.add(label)
                rawValues.add(obj.getString("val"))
            }
            listHistory.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items.distinct().take(20))
            listHistory.setOnItemClickListener { _, _, pos, _ ->
                val target = rawValues[pos]
                webView.loadUrl(if (target.startsWith("http")) target else "https://www.google.com/search?q=$target")
                searchLayer.visibility = View.GONE
            }
        } catch (e: Exception) {}
    }

    class WebShareInterface(private val context: Context) {
        @JavascriptInterface
        fun share(t: String, txt: String, u: String) {
            val i = Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT, "$txt\n$u") }
            context.startActivity(Intent.createChooser(i, "Share"))
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (searchLayer.visibility == View.VISIBLE) searchLayer.visibility = View.GONE
        else if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}

