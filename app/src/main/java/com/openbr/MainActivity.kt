package com.openbr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.palette.graphics.Palette
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONArray
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private lateinit var tabLayer: View
    private lateinit var tabList: ListView
    private lateinit var header: View
    
    // Simpan list tab aktif
    private val tabs = mutableListOf<WebView>()
    private var currentTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        container = findViewById(R.id.webview_container)
        tabLayer = findViewById(R.id.tab_layer)
        tabList = findViewById(R.id.list_tabs_preview)
        header = findViewById(R.id.app_bar_layout) // Pastiin ID ini ada di XML

        createNewTab("https://www.google.com")
        initControls()
    }

    private fun createNewTab(url: String) {
        val newWebView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    updateDynamicTheme(view)
                    findViewById<TextView>(R.id.url_display_fake).text = view?.title
                }
            }
            
            // FIX: Web Share API Support
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    // Handle Fullscreen video
                }
            }
        }
        
        // Custom Interface buat Share API
        newWebView.addJavascriptInterface(object {
            @JavascriptInterface
            fun share(title: String, text: String, url: String) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, title)
                    putExtra(Intent.EXTRA_TEXT, "$text \n $url")
                }
                startActivity(Intent.createChooser(intent, "Share via Open Br"))
            }
        }, "AndroidShare")

        newWebView.loadUrl(url)
        tabs.add(newWebView)
        switchTab(tabs.size - 1)
    }

    private fun switchTab(index: Int) {
        container.removeAllViews()
        container.addView(tabs[index])
        currentTabIndex = index
        findViewById<TextView>(R.id.tab_count).text = tabs.size.toString()
    }

    private fun updateDynamicTheme(view: WebView?) {
        val bitmap = view?.favicon ?: return
        Palette.from(bitmap).generate { palette ->
            val color = palette?.getDominantColor(Color.parseColor("#121212")) ?: return@generate
            header.setBackgroundColor(color)
            window.statusBarColor = color
        }
    }

    private fun initControls() {
        // Tombol buka tab layer
        findViewById<View>(R.id.btn_tabs).setOnClickListener {
            tabLayer.visibility = View.VISIBLE
            showTabPreview()
        }

        // Tombol Close di Layer (Bukan keluar app!)
        findViewById<ImageButton>(R.id.btn_close_tab_layer).setOnClickListener {
            tabLayer.visibility = View.GONE
        }
        
        // Tombol Home
        findViewById<View>(R.id.btn_home).setOnClickListener { createNewTab("https://www.google.com") }
    }

    private fun showTabPreview() {
        val adapter = object : ArrayAdapter<WebView>(this, R.layout.item_tab_card, tabs) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = layoutInflater.inflate(R.layout.item_tab_card, null)
                val title = v.findViewById<TextView>(R.id.tab_card_title)
                val close = v.findViewById<ImageButton>(R.id.btn_close_single_tab)
                
                title.text = tabs[position].title ?: "New Tab"
                v.setOnClickListener { switchTab(position); tabLayer.visibility = View.GONE }
                close.setOnClickListener { 
                    if(tabs.size > 1) {
                        tabs.removeAt(position)
                        notifyDataSetChanged()
                        if(currentTabIndex >= tabs.size) switchTab(tabs.size - 1)
                    }
                }
                return v
            }
        }
        tabList.adapter = adapter
    }

    override fun onBackPressed() {
        if (tabLayer.visibility == View.VISIBLE) {
            tabLayer.visibility = View.GONE // FIX: Tutup layer, bukan app
        } else if (tabs[currentTabIndex].canGoBack()) {
            tabs[currentTabIndex].goBack()
        } else {
            super.onBackPressed()
        }
    }
}
