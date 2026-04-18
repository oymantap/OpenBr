package com.openbr

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    private lateinit var webContainer: FrameLayout
    private lateinit var searchLayer: View
    private lateinit var activityLayer: View
    private lateinit var tabLayer: View
    private lateinit var urlInputReal: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tabCountText: TextView
    private lateinit var listHistorySearch: ListView
    private lateinit var listActivityReal: ListView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val tabsList = mutableListOf<WebView>()
    private var activeTabIndex = 0
    private val PREFS_NAME = "OpenBrData"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi semua View
        webContainer = findViewById(R.id.webview_container)
        searchLayer = findViewById(R.id.search_focus_layer)
        activityLayer = findViewById(R.id.activity_layer)
        tabLayer = findViewById(R.id.tab_layer)
        urlInputReal = findViewById(R.id.url_input_real)
        progressBar = findViewById(R.id.progress_bar)
        tabCountText = findViewById(R.id.tab_count)
        listHistorySearch = findViewById(R.id.list_history_search)
        listActivityReal = findViewById(R.id.list_activity_real)
        swipeRefresh = findViewById(R.id.swipe_refresh)

        initButtons()
        createNewTab("https://www.google.com")
    }

    private fun initButtons() {
        findViewById<View>(R.id.btn_home).setOnClickListener {
            tabsList[activeTabIndex].loadUrl("https://www.google.com")
        }

        findViewById<View>(R.id.btn_tabs).setOnClickListener {
            tabLayer.visibility = View.VISIBLE
            updateTabList()
        }

        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            showHistoryList(listHistorySearch, "history")
            urlInputReal.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlInputReal, 0)
        }

        findViewById<ImageButton>(R.id.btn_more).setOnClickListener { showSettingsMenu(it) }
        findViewById<ImageButton>(R.id.btn_close_tab_layer).setOnClickListener { tabLayer.visibility = View.GONE }
        findViewById<ImageButton>(R.id.btn_close_activity).setOnClickListener { activityLayer.visibility = View.GONE }
        findViewById<ImageButton>(R.id.btn_back_search).setOnClickListener { searchLayer.visibility = View.GONE }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val url = if (query.contains(".") && !query.contains(" ")) {
                    if (query.startsWith("http")) query else "https://$query"
                } else {
                    "https://www.google.com/search?q=$query"
                }
                saveData("history", query)
                tabsList[activeTabIndex].loadUrl(url)
                searchLayer.visibility = View.GONE
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
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
            settings.databaseEnabled = true
            settings.setSupportMultipleWindows(true)

            // FITUR DARK MODE WEBSITE
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
                } else {
                    WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, p: Int) {
                    progressBar.progress = p
                    progressBar.visibility = if (p < 100) View.VISIBLE else View.GONE
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    findViewById<TextView>(R.id.url_display_fake).text = view?.title ?: url
                    if (url != null && !url.contains("google.com/search")) {
                        saveData("activity", "${view?.title ?: "Web"}|$url")
                    }
                }
            }
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

    private fun saveData(key: String, value: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dataSet = prefs.getStringSet(key, LinkedHashSet<String>()) ?: LinkedHashSet()
        val newData = LinkedHashSet<String>(dataSet)
        if (newData.size > 50) newData.remove(newData.first()) // Limit 50 data
        newData.add(value)
        prefs.edit().putStringSet(key, newData).apply()
    }

    private fun showHistoryList(listView: ListView, key: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawData = prefs.getStringSet(key, setOf())?.toList()?.reversed() ?: listOf()
        
        val displayData = rawData.map { if (it.contains("|")) it.split("|")[0] else it }
        
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayData)
        listView.setOnItemClickListener { _, _, pos, _ ->
            val selected = rawData[pos]
            val url = if (selected.contains("|")) selected.split("|")[1] else "https://www.google.com/search?q=$selected"
            tabsList[activeTabIndex].loadUrl(url)
            searchLayer.visibility = View.GONE
            activityLayer.visibility = View.GONE
        }
    }

    private fun updateTabList() {
        val titles = tabsList.map { it.title ?: "Tab Baru" }
        findViewById<ListView>(R.id.list_tabs_preview).adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titles)
        findViewById<ListView>(R.id.list_tabs_preview).setOnItemClickListener { _, _, pos, _ -> switchTab(pos) }
    }

    private fun showSettingsMenu(v: View) {
        val popup = PopupMenu(this, v)
        popup.menu.add("Aktivitas Website")
        popup.menu.add("Ganti Tema (Gelap/Terang)")
        popup.menu.add("Share Link")
        popup.setOnMenuItemClickListener {
            when (it.title) {
                "Aktivitas Website" -> {
                    activityLayer.visibility = View.VISIBLE
                    showHistoryList(listActivityReal, "activity")
                }
                "Ganti Tema (Gelap/Terang)" -> {
                    val isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
                    AppCompatDelegate.setDefaultNightMode(if (isDark) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES)
                }
                "Share Link" -> {
                    val i = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, tabsList[activeTabIndex].url)
                    }
                    startActivity(Intent.createChooser(i, "Bagikan link"))
                }
            }
            true
        }
        popup.show()
    }

    override fun onBackPressed() {
        when {
            tabLayer.visibility == View.VISIBLE -> tabLayer.visibility = View.GONE
            searchLayer.visibility == View.VISIBLE -> searchLayer.visibility = View.GONE
            activityLayer.visibility == View.VISIBLE -> activityLayer.visibility = View.GONE
            tabsList[activeTabIndex].canGoBack() -> tabsList[activeTabIndex].goBack()
            else -> super.onBackPressed()
        }
    }
}

