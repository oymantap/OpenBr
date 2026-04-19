package com.openbr

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    private lateinit var webContainer: FrameLayout
    private lateinit var tabLayer: View
    private lateinit var searchLayer: View
    private lateinit var activityLayer: View
    private lateinit var tabCountText: TextView
    private lateinit var urlDisplay: TextView
    private lateinit var urlInputReal: EditText

    private val tabsList = mutableListOf<WebView>()
    private var activeTabIndex = 0
    private val PREFS = "OpenBr_V3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webContainer = findViewById(R.id.webview_container)
        tabLayer = findViewById(R.id.tab_layer)
        searchLayer = findViewById(R.id.search_focus_layer)
        activityLayer = findViewById(R.id.activity_layer)
        tabCountText = findViewById(R.id.tab_count)
        urlDisplay = findViewById(R.id.url_display_fake)
        urlInputReal = findViewById(R.id.url_input_real)

        // Tombol Home
        findViewById<View>(R.id.btn_home).setOnClickListener { 
            if (tabsList.isNotEmpty()) tabsList[activeTabIndex].loadUrl("https://www.google.com")
        }

        // Tombol Tab Manager
        findViewById<View>(R.id.btn_tabs).setOnClickListener {
            tabLayer.visibility = View.VISIBLE
            updateTabList()
        }

        // Trigger Pencarian
        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            urlInputReal.requestFocus()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(urlInputReal, 0)
        }

        // Input URL logic
        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val url = if (query.contains(".") && !query.contains(" ")) "https://$query" else "https://www.google.com/search?q=$query"
                tabsList[activeTabIndex].loadUrl(url)
                searchLayer.visibility = View.GONE
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }

        findViewById<ImageButton>(R.id.btn_more).setOnClickListener { showMenu(it) }
        findViewById<ImageButton>(R.id.btn_close_tab_layer).setOnClickListener { tabLayer.visibility = View.GONE }
        findViewById<ImageButton>(R.id.btn_close_activity).setOnClickListener { activityLayer.visibility = View.GONE }
        findViewById<ImageButton>(R.id.btn_back_search).setOnClickListener { searchLayer.visibility = View.GONE }

        // Start Tab Pertama
        createNewTab("https://www.google.com")
    }

    private fun createNewTab(url: String) {
        val wv = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            
            // TOTAL DARK MODE LOGIC
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                WebSettingsCompat.setForceDark(settings, if (isDark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF)
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

    private fun removeTab(index: Int) {
        if (tabsList.size > 1) {
            tabsList.removeAt(index)
            activeTabIndex = if (activeTabIndex >= tabsList.size) tabsList.size - 1 else activeTabIndex
            switchTab(activeTabIndex)
            updateTabList()
        } else {
            Toast.makeText(this, "Minimal 1 Tab", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTabList() {
        val container = findViewById<LinearLayout>(R.id.tab_items_container) // Lo butuh ScrollView/LinearLayout di XML
        container.removeAllViews()
        
        tabsList.forEachIndexed { index, wv ->
            val view = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, null)
            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)
            
            text1.text = wv.title ?: "Tab Baru"
            text2.text = if (index == activeTabIndex) "AKTIF (Klik tutup X)" else "Klik untuk pindah"
            
            view.setOnClickListener { switchTab(index) }
            view.setOnLongClickListener { removeTab(index); true } // Tahan buat hapus
            container.addView(view)
        }

        // Tombol (+) Tambah Tab di dalam Layer
        val btnAdd = Button(this).apply { text = "+ Tambah Tab Baru" }
        btnAdd.setOnClickListener { createNewTab("https://www.google.com") }
        container.addView(btnAdd)
    }

    private fun showMenu(v: View) {
        val p = PopupMenu(this, v)
        p.menu.add("Refresh Manual")
        p.menu.add("Tab Baru")
        p.menu.add("Ganti Tema (System)")
        p.menu.add("Aktivitas")
        p.setOnMenuItemClickListener {
            when(it.title) {
                "Refresh Manual" -> tabsList[activeTabIndex].reload()
                "Tab Baru" -> createNewTab("https://www.google.com")
                "Aktivitas" -> activityLayer.visibility = View.VISIBLE
                "Ganti Tema (System)" -> {
                    val mode = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) 
                        AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
            true
        }
        p.show()
    }

    override fun onBackPressed() {
        if (tabLayer.visibility == View.VISIBLE) tabLayer.visibility = View.GONE
        else if (searchLayer.visibility == View.VISIBLE) searchLayer.visibility = View.GONE
        else if (tabsList[activeTabIndex].canGoBack()) tabsList[activeTabIndex].goBack()
        else super.onBackPressed()
    }
}

