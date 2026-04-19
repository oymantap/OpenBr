package com.openbr

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
        restoreTabsState()
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

        // Tombol Tambah Tab
        findViewById<View>(R.id.btn_add_tab_bottom).setOnClickListener {
            createNewTab("https://www.google.com")
            tabLayer.visibility = View.GONE
        }

        // Hapus Log
        findViewById<View>(R.id.btn_clear_history_all).setOnClickListener { clearLog("history", findViewById(R.id.list_history_search)) }
        findViewById<View>(R.id.btn_clear_activity_all).setOnClickListener { clearLog("activity", findViewById(R.id.list_activity_real)) }

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
            // FIX: Sinkronkan teks input dengan URL tab aktif saat dibuka
            urlInputReal.setText(tabsList[activeTabIndex].url)
            showList(findViewById(R.id.list_history_search), "history")
            urlInputReal.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlInputReal, 0)
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
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            // FORCE DARK MODE SELALU ON
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
            }

            setDownloadListener { u, _, cD, mT, _ ->
                val request = DownloadManager.Request(Uri.parse(u))
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(u, cD, mT))
                (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(applicationContext, "Downloading...", Toast.LENGTH_SHORT).show()
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, p: Int) {
                    progressBar.progress = p
                    progressBar.visibility = if (p < 100) View.VISIBLE else View.GONE
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // Update display hanya jika ini tab aktif
                    if (tabsList.indexOf(view) == activeTabIndex) {
                        urlDisplay.text = view?.title ?: url
                    }
                    saveLog("activity", "${view?.title ?: "Web"}|$url")
                }
            }
        }
        wv.loadUrl(url)
        tabsList.add(wv)
        switchTab(tabsList.size - 1)
    }

    private fun switchTab(index: Int) {
        if (index >= tabsList.size) return
        activeTabIndex = index
        webContainer.removeAllViews()
        webContainer.addView(tabsList[index])
        
        // FIX: Update UI Header saat pindah tab agar tidak perlu refresh manual
        urlDisplay.text = tabsList[index].title ?: tabsList[index].url ?: "Cari atau ketik URL"
        tabCountText.text = tabsList.size.toString()
        tabLayer.visibility = View.GONE
    }

    private fun capturePreview() {
        if (webContainer.width > 0 && webContainer.height > 0) {
            val bitmap = Bitmap.createBitmap(webContainer.width, webContainer.height, Bitmap.Config.ARGB_8888)
            webContainer.draw(android.graphics.Canvas(bitmap))
            tabPreviews[activeTabIndex] = bitmap
        }
    }

    private fun showTabs() {
        val container = findViewById<LinearLayout>(R.id.tab_items_container)
        container.removeAllViews()
        tabsList.forEachIndexed { i, wv ->
            val card = layoutInflater.inflate(R.layout.item_tab_card, container, false)
            card.findViewById<TextView>(R.id.tab_title).text = wv.title ?: "Tab Baru"
            card.findViewById<ImageView>(R.id.tab_preview).setImageBitmap(tabPreviews[i])
            card.setOnClickListener { switchTab(i) }
            card.findViewById<View>(R.id.btn_close_this_tab).setOnClickListener {
                if (tabsList.size > 1) {
                    tabsList.removeAt(i)
                    tabPreviews.remove(i)
                    if (activeTabIndex >= tabsList.size) activeTabIndex = tabsList.size - 1
                    showTabs()
                    switchTab(activeTabIndex)
                }
            }
            container.addView(card)
        }
    }

    private fun clearLog(key: String, lv: ListView) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key).apply()
        showList(lv, key)
        Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
    }

    private fun saveLog(key: String, value: String) {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(key, linkedSetOf()) ?: linkedSetOf()
        val newSet = LinkedHashSet<String>(set)
        if (newSet.size > 50) newSet.remove(newSet.first()) // Limit 50 items
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

    private fun showSettings(v: View) {
        val p = PopupMenu(this, v)
        p.menu.add("Refresh")
        p.menu.add("Aktivitas")
        p.setOnMenuItemClickListener {
            when(it.title) {
                "Refresh" -> tabsList[activeTabIndex].reload()
                "Aktivitas" -> {
                    activityLayer.visibility = View.VISIBLE
                    showList(findViewById(R.id.list_activity_real), "activity")
                }
            }
            true
        }
        p.show()
    }

    private fun saveTabsState() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val urls = tabsList.map { it.url ?: "" }.filter { it.isNotEmpty() }.toSet()
        prefs.edit().putStringSet("saved_tabs", urls).apply()
    }

    private fun restoreTabsState() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val urls = prefs.getStringSet("saved_tabs", null)
        urls?.forEach { createNewTab(it) }
    }

    override fun onStop() {
        super.onStop()
        saveTabsState()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.dataString?.let { createNewTab(it) }
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
