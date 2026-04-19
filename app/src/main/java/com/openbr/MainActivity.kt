package com.openbr

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private lateinit var searchLayer: View
    private lateinit var activityLayer: View
    private lateinit var tabLayer: View
    private lateinit var urlInputReal: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tabCountText: TextView
    private lateinit var urlDisplay: TextView

    private val tabsList = mutableListOf<WebView>()
    private val tabPreviews = mutableMapOf<Int, Bitmap?>()
    private var activeTabIndex = 0
    private val PREFS = "OpenBr_Official_V3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()
        handleIntent(intent)
        if (tabsList.isEmpty()) createNewTab("https://www.google.com")
    }

    private fun initUI() {
        webContainer = findViewById(R.id.webview_container)
        searchLayer = findViewById(R.id.search_focus_layer)
        activityLayer = findViewById(R.id.activity_layer)
        tabLayer = findViewById(R.id.tab_layer)
        urlInputReal = findViewById(R.id.url_input_real)
        btnClearSearch = findViewById(R.id.btn_clear_search)
        progressBar = findViewById(R.id.progress_bar)
        tabCountText = findViewById(R.id.tab_count)
        urlDisplay = findViewById(R.id.url_display_fake)

        // Tombol X di input search: cuma muncul kalo ada teks
        urlInputReal.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        btnClearSearch.setOnClickListener { urlInputReal.text.clear() }

        findViewById<View>(R.id.btn_home).setOnClickListener { tabsList[activeTabIndex].loadUrl("https://www.google.com") }
        findViewById<View>(R.id.btn_tabs).setOnClickListener {
            capturePreview()
            tabLayer.visibility = View.VISIBLE
            showTabs()
        }

        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            showList(findViewById(R.id.list_history_search), "history")
            urlInputReal.requestFocus()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(urlInputReal, 0)
        }

        findViewById<ImageButton>(R.id.btn_more).setOnClickListener { showSettings(it) }
        findViewById<View>(R.id.btn_close_tab_layer).setOnClickListener { tabLayer.visibility = View.GONE }
        findViewById<View>(R.id.btn_close_activity).setOnClickListener { activityLayer.visibility = View.GONE }
        findViewById<View>(R.id.btn_back_search).setOnClickListener { searchLayer.visibility = View.GONE }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val url = when {
                    query.startsWith("http") || query.startsWith("file://") || query.contains("localhost") -> query
                    query.contains(".") && !query.contains(" ") -> "https://$query"
                    else -> "https://www.google.com/search?q=$query"
                }
                saveLog("history", query)
                tabsList[activeTabIndex].loadUrl(url)
                searchLayer.visibility = View.GONE
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }
    }

    private fun createNewTab(url: String) {
        val wv = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false // Biar video/musik gak macet di background
            }

            // DARK MODE TOTAL
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                WebSettingsCompat.setForceDark(settings, if (isNight) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF)
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
                    saveLog("activity", "${view?.title ?: "Web"}|$url")
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

    private fun capturePreview() {
        if (webContainer.width > 0) {
            val bitmap = Bitmap.createBitmap(webContainer.width, webContainer.height, Bitmap.Config.ARGB_8888)
            webContainer.draw(android.graphics.Canvas(bitmap))
            tabPreviews[activeTabIndex] = bitmap
        }
    }

    private fun showTabs() {
        val container = findViewById<LinearLayout>(R.id.tab_items_container)
        container.removeAllViews()
        
        tabsList.forEachIndexed { i, wv ->
            // Pastikan layout name-nya: item_tab_card
            val card = layoutInflater.inflate(R.layout.item_tab_card, container, false)
            
            val title = card.findViewById<TextView>(R.id.tab_title)
            val img = card.findViewById<ImageView>(R.id.tab_preview)
            val close = card.findViewById<ImageButton>(R.id.btn_close_this_tab)

            title.text = wv.title ?: "Tab Baru"
            img.setImageBitmap(tabPreviews[i])
            
            card.setOnClickListener { switchTab(i) }
            close.setOnClickListener {
                if (tabsList.size > 1) {
                    tabsList.removeAt(i)
                    tabPreviews.remove(i)
                    if (activeTabIndex >= tabsList.size) activeTabIndex = tabsList.size - 1
                    showTabs() // Refresh list
                    switchTab(activeTabIndex)
                } else {
                    Toast.makeText(this, "Minimal satu tab terbuka", Toast.LENGTH_SHORT).show()
                }
            }
            container.addView(card)
        }
    }
    

    private fun showSettings(v: View) {
        val p = PopupMenu(this, v)
        p.menu.add("Refresh")
        p.menu.add("Aktivitas")
        p.menu.add("Tambah Tab")
        p.menu.add("Mode Gelap/Terang")
        p.setOnMenuItemClickListener {
            when(it.title) {
                "Refresh" -> tabsList[activeTabIndex].reload()
                "Aktivitas" -> {
                    activityLayer.visibility = View.VISIBLE
                    showList(findViewById(R.id.list_activity_real), "activity")
                }
                "Tambah Tab" -> createNewTab("https://www.google.com")
                "Mode Gelap/Terang" -> {
                    val mode = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
                        AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
            true
        }
        p.show()
    }

    private fun saveLog(key: String, value: String) {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(key, linkedSetOf()) ?: linkedSetOf()
        val newSet = LinkedHashSet<String>(set)
        newSet.add(value)
        prefs.edit().putStringSet(key, newSet).apply()
    }

    private fun showList(lv: ListView, key: String) {
        val data = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(key, setOf())?.toList()?.reversed() ?: listOf()
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, data.map { it.split("|")[0] })
        lv.setOnItemClickListener { _, _, i, _ ->
            val u = if (data[i].contains("|")) data[i].split("|")[1] else "https://google.com/search?q=${data[i]}"
            tabsList[activeTabIndex].loadUrl(u)
            searchLayer.visibility = View.GONE
            activityLayer.visibility = View.GONE
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.dataString?.let { createNewTab(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onBackPressed() {
        when {
            searchLayer.visibility == View.VISIBLE -> searchLayer.visibility = View.GONE
            tabLayer.visibility == View.VISIBLE -> tabLayer.visibility = View.GONE
            activityLayer.visibility == View.VISIBLE -> activityLayer.visibility = View.GONE
            tabsList[activeTabIndex].canGoBack() -> tabsList[activeTabIndex].goBack()
            else -> super.onBackPressed()
        }
    }
}
