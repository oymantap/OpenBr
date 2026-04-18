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

class MainActivity : AppCompatActivity() {

    private lateinit var webContainer: FrameLayout
    private lateinit var searchLayer: View
    private lateinit var activityLayer: View
    private lateinit var tabLayer: View
    private lateinit var urlInputReal: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tabCountText: TextView
    private lateinit var appBar: View
    
    private val tabsList = mutableListOf<WebView>()
    private var activeTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initUI()
        createNewTab("https://www.google.com")
    }

    private fun initUI() {
        webContainer = findViewById(R.id.webview_container)
        searchLayer = findViewById(R.id.search_focus_layer)
        activityLayer = findViewById(R.id.activity_layer)
        tabLayer = findViewById(R.id.tab_layer)
        urlInputReal = findViewById(R.id.url_input_real)
        progressBar = findViewById(R.id.progress_bar)
        tabCountText = findViewById(R.id.tab_count)
        appBar = findViewById(R.id.app_bar_layout)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        findViewById<View>(R.id.btn_home).setOnClickListener { createNewTab("https://www.google.com") }
        
        findViewById<View>(R.id.btn_tabs).setOnClickListener {
            tabLayer.visibility = View.VISIBLE
            updateTabList()
        }

        findViewById<View>(R.id.search_container_trigger).setOnClickListener {
            searchLayer.visibility = View.VISIBLE
            urlInputReal.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlInputReal, 0)
        }

        findViewById<ImageButton>(R.id.btn_more).setOnClickListener { showSettingsMenu(it) }
        findViewById<ImageButton>(R.id.btn_close_tab_layer).setOnClickListener { tabLayer.visibility = View.GONE }
        findViewById<ImageButton>(R.id.btn_close_activity).setOnClickListener { activityLayer.visibility = View.GONE }
        findViewById<ImageButton>(R.id.btn_back_search).setOnClickListener { searchLayer.visibility = View.GONE }

        urlInputReal.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            val url = if (query.contains(".") && !query.contains(" ")) "https://$query" else "https://www.google.com/search?q=$query"
            tabsList[activeTabIndex].loadUrl(url)
            searchLayer.visibility = View.GONE
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
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
                    findViewById<TextView>(R.id.url_display_fake).text = view?.title ?: url
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

    private fun applyDynamicColor(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val color = palette?.getDominantColor(Color.parseColor("#FFFFFF")) ?: return@generate
            appBar.setBackgroundColor(color)
            window.statusBarColor = color
        }
    }

    private fun updateTabList() {
        val listView = findViewById<ListView>(R.id.list_tabs_preview)
        val adapter = object : ArrayAdapter<WebView>(this, android.R.layout.simple_list_item_1, tabsList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.text = tabsList[position].title ?: "New Tab"
                return view
            }
        }
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ -> switchTab(position) }
    }

    private fun showSettingsMenu(v: View) {
        val popup = PopupMenu(this, v)
        popup.menu.add("Refresh")
        popup.menu.add("Aktivitas")
        popup.menu.add("Share")
        popup.setOnMenuItemClickListener {
            when(it.title) {
                "Refresh" -> tabsList[activeTabIndex].reload()
                "Aktivitas" -> activityLayer.visibility = View.VISIBLE
                "Share" -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, tabsList[activeTabIndex].url)
                    }
                    startActivity(Intent.createChooser(intent, "Share via Open Br"))
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

