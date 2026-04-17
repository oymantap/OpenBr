package com.openbr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var searchLayer: View
    private lateinit var downloadLayer: View
    private val searchHistory = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi View baru
        searchLayer = findViewById(R.id.search_focus_layer)
        downloadLayer = findViewById(R.id.download_layer)
        val urlInput = findViewById<EditText>(R.id.url_input)
        val searchFocusInput = findViewById<EditText>(R.id.search_input_focus)
        val btnClear = findViewById<ImageButton>(R.id.btn_clear_search)
        val btnBackSearch = findViewById<ImageButton>(R.id.btn_back_search)
        val listHistory = findViewById<ListView>(R.id.list_history)

        webView = findViewById(R.id.webview)
        
        // --- 1. MEDIA SESSION & NOTIF PANEL ---
        // Setting agar media terdeteksi sistem
        webView.settings.mediaPlaybackRequiresUserGesture = false

        // --- 2. DOWNLOAD MANAGER ---
        webView.setDownloadListener { url, _, _, _, _ ->
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
            Toast.makeText(this, "Mendownload...", Toast.LENGTH_SHORT).show()
        }

        // --- 3. SEARCH FOCUS MODE ---
        urlInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchLayer.visibility = View.VISIBLE
                searchFocusInput.requestFocus()
                // Tampilkan history
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, searchHistory)
                listHistory.adapter = adapter
            }
        }

        btnBackSearch.setOnClickListener {
            searchLayer.visibility = View.GONE
            urlInput.clearFocus()
        }

        btnClear.setOnClickListener { searchFocusInput.text.clear() }

        searchFocusInput.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString()
            if (query.isNotEmpty()) {
                searchHistory.add(0, query) // Simpan history
                val url = if (query.contains(".")) "https://$query" else "https://www.google.com/search?q=$query"
                webView.loadUrl(url)
                searchLayer.visibility = View.GONE
            }
            true
        }

        // --- 4. API SHARE (Di Tombol More) ---
        findViewById<ImageButton>(R.id.btn_more).setOnClickListener { view ->
            val popup = PopupMenu(this, view, Gravity.END, 0, R.style.CustomPopupStyle)
            popup.menu.add("Bagikan Halaman")
            popup.menu.add("Unduhan")
            popup.menu.add("Refresh")
            
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Bagikan Halaman" -> {
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "text/plain"
                        intent.putExtra(Intent.EXTRA_TEXT, webView.url)
                        startActivity(Intent.createChooser(intent, "Share Link"))
                    }
                    "Unduhan" -> downloadLayer.visibility = View.VISIBLE
                    "Refresh" -> webView.reload()
                }
                true
            }
            popup.show()
        }
    }

    override fun onBackPressed() {
        if (downloadLayer.visibility == View.VISIBLE) {
            downloadLayer.visibility = View.GONE
        } else if (searchLayer.visibility == View.VISIBLE) {
            searchLayer.visibility = View.GONE
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

