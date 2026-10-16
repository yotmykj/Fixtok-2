package com.fixtok.tv

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Fresh, minimal Android TV wrapper for TikTok.
 * Uses the system WebView, so TikTok website compatibility depends on the
 * installed Android System WebView/Chrome version and TikTok's current site.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var root: FrameLayout
    private var lastUrl = HOME_URL
    private var loadingError = false

    companion object {
        private const val HOME_URL = "https://www.tiktok.com/"
        private const val LOGIN_URL = "https://www.tiktok.com/login"
        private const val PREFS = "fixtok_preferences"
        private const val KEY_ZOOM = "page_zoom"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        enterImmersiveMode()

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                textZoom = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_ZOOM, 100)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
            }
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val scheme = uri.scheme?.lowercase()
                val host = uri.host?.lowercase()
                if (scheme != "https" && scheme != "http") return true
                val isTikTok = host == "tiktok.com" || host?.endsWith(".tiktok.com") == true
                if (!isTikTok) {
                    Toast.makeText(this@MainActivity, "Внешняя ссылка заблокирована", Toast.LENGTH_SHORT).show()
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                lastUrl = url
                loadingError = false
                status.text = "Загрузка…"
                status.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                lastUrl = url
                status.text = "FixTok  •  OK"
                status.postDelayed({ if (!loadingError) status.visibility = View.GONE }, 2500)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showError()
            }

            override fun onSafeBrowsingHit(
                view: WebView,
                request: WebResourceRequest,
                threatType: Int,
                callback: SafeBrowsingResponse
            ) {
                callback.backToSafety(true)
                showError("Страница заблокирована защитой Android")
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress < 100) {
                    status.visibility = View.VISIBLE
                    status.text = "Загрузка $newProgress%"
                }
            }
        }

        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(0xDD111116.toInt())
            gravity = android.view.Gravity.CENTER_VERTICAL
            elevation = dp(8).toFloat()
        }
        val brand = TextView(this).apply {
            text = "FixTok"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(dp(8), 0, dp(16), 0)
        }
        status = TextView(this).apply {
            text = "Готово"
            textSize = 13f
            setTextColor(0xFFBDBDC7.toInt())
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        bar.addView(brand)
        bar.addView(status)
        bar.addView(makeButton("Главная") { load(HOME_URL) })
        bar.addView(makeButton("Войти") { load(LOGIN_URL) })
        bar.addView(makeButton("Обновить") { webView.reload() })
        bar.addView(makeButton("Настройки") { showSettings() })
        val barParams = FrameLayout.LayoutParams(-1, dp(58), android.view.Gravity.TOP)
        root.addView(bar, barParams)
        setContentView(root)

        if (savedInstanceState != null) {
            val restored = webView.restoreState(savedInstanceState)
            if (restored == null) webView.loadUrl(HOME_URL)
        } else {
            webView.loadUrl(HOME_URL)
        }
        webView.requestFocus()
    }

    private fun makeButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isFocusable = true
            isFocusableInTouchMode = true
            setTextColor(Color.WHITE)
            setOnClickListener { action() }
            contentDescription = label
        }

    private fun load(url: String) {
        loadingError = false
        webView.loadUrl(url)
        webView.requestFocus()
    }

    private fun showError(message: String = "Нет соединения или сайт недоступен") {
        loadingError = true
        status.text = "$message  •  нажми Обновить"
        status.visibility = View.VISIBLE
    }

    private fun showSettings() {
        val options = arrayOf("Масштаб: 80%", "Масштаб: 90%", "Масштаб: 100%", "Очистить cookies / выйти", "Перезагрузить страницу")
        AlertDialog.Builder(this)
            .setTitle("Настройки FixTok")
            .setItems(options) { _, which ->
                when (which) {
                    0, 1, 2 -> {
                        val zoom = when (which) { 0 -> 80; 1 -> 90; else -> 100 }
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_ZOOM, zoom).apply()
                        webView.settings.textZoom = zoom
                        Toast.makeText(this, "Масштаб текста: $zoom%", Toast.LENGTH_SHORT).show()
                    }
                    3 -> confirmLogout()
                    4 -> webView.reload()
                }
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Выйти из аккаунта?")
            .setMessage("Будут удалены cookies WebView, включая сохранённые входы на сайтах.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Выйти") { _, _ ->
                CookieManager.getInstance().removeAllCookies {
                    CookieManager.getInstance().flush()
                    runOnUiThread {
                        webView.clearHistory()
                        webView.loadUrl(HOME_URL)
                        Toast.makeText(this, "Cookies очищены", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MENU -> { showSettings(); return true }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    webView.evaluateJavascript(
                        "(function(){var v=document.querySelector('video');if(v){if(v.paused)v.play();else v.pause();}})();",
                        null
                    )
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (webView.canGoBack()) {
                        webView.goBack()
                        return true
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Закрыть FixTok?")
                        .setNegativeButton("Остаться", null)
                        .setPositiveButton("Выйти") { _, _ -> finish() }
                        .show()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
            webView.resumeTimers()
        }
        enterImmersiveMode()
    }

    override fun onPause() {
        if (::webView.isInitialized) {
            webView.onPause()
            webView.pauseTimers()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.removeAllViews()
            webView.destroy()
        }
        super.onDestroy()
    }
}
