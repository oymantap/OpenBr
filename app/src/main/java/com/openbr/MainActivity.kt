package com.openbr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.palette.graphics.Palette
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var webContainer: FrameLayout
    private lateinit var tabLayer: View
    private lateinit var searchLayer: View
    private lateinit var activityLayer: View
    private lateinit var urlInputReal: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tabCountText: TextView
    private lateinit var appBar: View
    
    // Multi-Tab System
    private val tabsList = mutableListOf<WebView>()
    private var activeTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()
        // Buka tab pertama
        createNewTab("https://www.google.com")
    }

    private fun initUI() {
        webContainer = findViewById(R.id.webview_container) // Ganti webview di XML ke FrameLayout
        tabLayer = findViewById(R.id.tab_layer) // Tambahin di XML
        searchLayer = findViewById(R.id.search_focus_layer)
        activityLayer = findViewById(R.id.activity_layer)
        urlInputReal = findViewById(R.id.url_input_real)
        progressBar = findViewById(R.id.progress_bar)
        tabCountText = findViewById(R.id.tab_count)
        appBar = findViewById(R.id.app_bar_layout)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        // Tombol Home
        findViewById<View>(R.id.btn_home).setOnClickListener { createNewTab("https://www.google.com") }

        // Tombol Tab (Buka Layer Tab)
        findViewById<View>(R.id.btn_tabs).setOnClickListener {
            tabLayer.visibility = View.VISIBLE
            updateTabList()
        }

        // Tombol More (Settings)
        findViewById<View>(R.id.btn_more).setOnClickListener { showSettingsMenu(it) }

        // Logic Search
        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            urlInputReal.requestFocus()
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(urlInputReal, 0)
        }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString()
            val url = if (query.contains(".") && !query.contains(" ")) "https://$query" else "https://www.google.com/search?q=$query"
            tabsList[activeTabIndex].loadUrl(url)
            searchLayer.visibility = View.GONE
            true
        }

        swipeRefresh.setOnRefreshListener { 
            tabsList[activeTabIndex].reload()
            swipeRefresh.isRefreshing = false
        }
    }

    private fun createNewTab(url: String) {
        val wv = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            
            // Fix Share API
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, p: Int) {
                    progressBar.progress = p
                    progressBar.visibility = if (p < 100) View.VISIBLE else View.GONE
                }
                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    icon?.let { applyDynamicColor(it) }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    findViewById<TextView>(R.id.url_display_fake).text = view?.title
                }
            }
            
            // Jembatan buat Share ke Android
            addJavascriptInterface(object {
                @JavascriptInterface
                fun share(title: String, url: String) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "$title\n$url")
                    }
                    startActivity(Intent.createChooser(intent, "Share via Open Br"))
                }
            }, "NativeShare")
        }

        wv.loadUrl(url)
        tabsList.add(wv)
        switchTab(tabsList.size - 1)
    }

    private fun switchTab(index: Int) {
        activeTabIndex = index
        webContainer.removeAllViews()
        webContainer.addView(tabsList[index])
        tabCountText.text = tabsList.size.toString()
        tabLayer.visibility = View.GONE
    }

    private fun applyDynamicColor(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val color = palette?.getDominantColor(Color.parseColor("#FFFFFF")) ?: return@generate
            appBar.setBackgroundColor(color)
            window.statusBarColor = color
            // Cek kontras biar icon tetep keliatan
            val tint = if (Palette.from(bitmap).generate().dominantSwatch?.bodyTextColor ?: Color.BLACK == Color.WHITE) Color.WHITE else Color.BLACK
            // (Opsional: Loop icon buat ganti tint)
        }
    }

    private fun updateTabList() {
        val listView = findViewById<ListView>(R.id.list_tabs_preview)
        val adapter = object : ArrayAdapter<WebView>(this, R.layout.item_tab_card, tabsList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = layoutInflater.inflate(R.layout.item_tab_card, null)
                val title = view.findViewById<TextView>(R.id.tab_card_title)
                val btnClose = view.findViewById<ImageButton>(R.id.btn_close_single_tab)
                
                title.text = tabsList[position].title ?: "New Tab"
                view.setOnClickListener { switchTab(position) }
                btnClose.setOnClickListener {
                    if (tabsList.size > 1) {
                        tabsList.removeAt(position)
                        if (activeTabIndex >= tabsList.size) switchTab(tabsList.size - 1)
                        updateTabList()
                    }
                }
                return view
            }
        }
        listView.adapter = adapter
    }

    private fun showSettingsMenu(v: View) {
        val popup = PopupMenu(this, v)
        popup.menu.add("Refresh")
        popup.menu.add("Aktivitas")
        popup.menu.add("Share Halaman")
        popup.setOnMenuItemClickListener {
            when(it.title) {
                "Refresh" -> tabsList[activeTabIndex].reload()
                "Aktivitas" -> activityLayer.visibility = View.VISIBLE
                "Share Halaman" -> {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, tabsList[activeTabIndex].url)
                    startActivity(Intent.createChooser(shareIntent, "Share"))
                }
            }
            true
        }
        popup.show()
    }

    override fun onBackPressed() {
        if (tabLayer.visibility == View.VISIBLE) tabLayer.visibility = View.GONE
        else if (searchLayer.visibility == View.VISIBLE) searchLayer.visibility = View.GONE
        else if (activityLayer.visibility == View.VISIBLE) activityLayer.visibility = View.GONE
        else if (tabsList[activeTabIndex].canGoBack()) tabsList[activeTabIndex].goBack()
        else super.onBackPressed()
    }
}
