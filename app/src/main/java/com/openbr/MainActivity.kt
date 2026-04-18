package com.openbr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var searchLayer: View
    private lateinit var urlDisplayFake: TextView
    private lateinit var urlInputReal: EditText
    private lateinit var progressBar: ProgressBar
    private val historyFileName = "history.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        searchLayer = findViewById(R.id.search_focus_layer)
        urlDisplayFake = findViewById(R.id.url_display_fake)
        urlInputReal = findViewById(R.id.url_input_real)
        progressBar = findViewById(R.id.progress_bar)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        // --- ENGINE SETTINGS (Support Multiplayer & Media) ---
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

        // Bridge Share API: Website bisa manggil AndroidShare.share(...)
        webView.addJavascriptInterface(WebShareInterface(this), "AndroidShare")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                urlDisplayFake.text = url
                url?.let { saveToHistory(it) }
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.startsWith("http")) return false
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources) // Penting buat testing multiplayer
            }
        }

        // --- BUTTON LOGIC ---
        findViewById<ImageView>(R.id.btn_home).setOnClickListener { webView.loadUrl("https://www.google.com") }

        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            urlInputReal.setText(webView.url)
            urlInputReal.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlInputReal, 0)
        }

        findViewById<ImageButton>(R.id.btn_back_search).setOnClickListener { searchLayer.visibility = View.GONE }
        findViewById<ImageButton>(R.id.btn_clear_text).setOnClickListener { urlInputReal.text.clear() }
        
        findViewById<ImageButton>(R.id.btn_more).setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menu.add("Refresh")
            popup.menu.add("Bagikan")
            popup.setOnMenuItemClickListener { item ->
                when(item.title) {
                    "Refresh" -> webView.reload()
                    "Bagikan" -> {
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, webView.url)
                        }
                        startActivity(Intent.createChooser(i, "Share URL"))
                    }
                }
                true
            }
            popup.show()
        }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val url = if (query.contains(".") && !query.contains(" ")) {
                    if (query.startsWith("http")) query else "https://$query"
                } else "https://www.google.com/search?q=$query"
                webView.loadUrl(url)
                searchLayer.visibility = View.GONE
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }

        swipeRefresh.setOnRefreshListener { webView.reload() }
        
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 1)
        webView.loadUrl("https://www.google.com")
    }

    private fun saveToHistory(url: String) {
        try {
            val file = File(filesDir, historyFileName)
            val jsonArray = if (file.exists()) JSONArray(file.readText()) else JSONArray()
            val entry = JSONObject().apply {
                put("url", url)
                put("time", System.currentTimeMillis())
            }
            jsonArray.put(entry)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) { e.printStackTrace() }
    }

    class WebShareInterface(private val context: Context) {
        @JavascriptInterface
        fun share(title: String, text: String, url: String) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$text \n $url")
            }
            context.startActivity(Intent.createChooser(intent, "Share via Open Br"))
        }
    }

    override fun onBackPressed() {
        if (searchLayer.visibility == View.VISIBLE) searchLayer.visibility = View.GONE
        else if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}

