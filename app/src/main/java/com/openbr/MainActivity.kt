package com.openbr

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    private lateinit var webContainer: FrameLayout
    private lateinit var searchLayer: View
    private lateinit var tabLayer: View
    private lateinit var urlInputReal: EditText
    private lateinit var tabCountText: TextView
    private lateinit var urlDisplay: TextView
    private lateinit var progressBar: ProgressBar

    private val tabsList = mutableListOf<WebView>()
    private var activeTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webContainer = findViewById(R.id.webview_container)
        searchLayer = findViewById(R.id.search_focus_layer)
        tabLayer = findViewById(R.id.tab_layer)
        urlInputReal = findViewById(R.id.url_input_real)
        tabCountText = findViewById(R.id.tab_count)
        urlDisplay = findViewById(R.id.url_display_fake)
        progressBar = findViewById(R.id.progress_bar)

        // Init tombol-tombol
        findViewById<View>(R.id.btn_home).setOnClickListener { 
            if (tabsList.isNotEmpty()) tabsList[activeTabIndex].loadUrl("https://www.google.com")
        }
        findViewById<View>(R.id.btn_tabs).setOnClickListener {
            tabLayer.visibility = View.VISIBLE
            updateTabList()
        }
        findViewById<View>(R.id.btn_add_tab).setOnClickListener { createNewTab("https://www.google.com") }
        findViewById<View>(R.id.btn_close_tab_layer).setOnClickListener { tabLayer.visibility = View.GONE }
        findViewById<View>(R.id.btn_back_search).setOnClickListener { searchLayer.visibility = View.GONE }
        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            urlInputReal.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlInputReal, 0)
        }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty() && tabsList.isNotEmpty()) {
                val url = if (query.contains(".") && !query.contains(" ")) "https://$query" else "https://www.google.com/search?q=$query"
                tabsList[activeTabIndex].loadUrl(url)
                searchLayer.visibility = View.GONE
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }

        findViewById<View>(R.id.btn_more).setOnClickListener { showSettingsMenu(it) }

        createNewTab("https://www.google.com")
    }

    private fun createNewTab(url: String) {
        val wv = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            
            // Fix Dark Mode Website
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                WebSettingsCompat.setForceDark(settings, if (isDark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF)
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, p: Int) {
                    progressBar.progress = p
                    progressBar.visibility = if (p < 100) View.VISIBLE else View.GONE
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    urlDisplay.text = view?.title ?: url
                }
            }
        }
        wv.loadUrl(url)
        tabsList.add(wv)
        switchTab(tabsList.size - 1)
    }

    private fun switchTab(index: Int) {
        if (index !in tabsList.indices) return
        activeTabIndex = index
        webContainer.removeAllViews()
        webContainer.addView(tabsList[index])
        tabCountText.text = tabsList.size.toString()
        tabLayer.visibility = View.GONE
    }

    private fun updateTabList() {
        val listView = findViewById<ListView>(R.id.list_tabs_preview)
        val titles = tabsList.mapIndexed { i, wv -> "${i+1}. ${wv.title ?: "Tab Baru"}" }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titles)
        listView.setOnItemClickListener { _, _, pos, _ -> switchTab(pos) }
        listView.setOnItemLongClickListener { _, _, pos, _ ->
            if (tabsList.size > 1) {
                tabsList.removeAt(pos)
                if (activeTabIndex >= tabsList.size) activeTabIndex = tabsList.size - 1
                switchTab(activeTabIndex)
                updateTabList()
            }
            true
        }
    }

    private fun showSettingsMenu(v: View) {
        val popup = PopupMenu(this, v)
        popup.menu.add("Refresh")
        popup.menu.add("Dark/Light Mode")
        popup.setOnMenuItemClickListener {
            when (it.title) {
                "Refresh" -> tabsList[activeTabIndex].reload()
                "Dark/Light Mode" -> {
                    val mode = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) 
                        AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
                    AppCompatDelegate.setDefaultNightMode(mode)
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
            tabsList.isNotEmpty() && tabsList[activeTabIndex].canGoBack() -> tabsList[activeTabIndex].goBack()
            else -> super.onBackPressed()
        }
    }
}

