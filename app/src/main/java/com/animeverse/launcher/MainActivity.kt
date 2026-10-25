package com.animeverse.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class MainActivity : Activity() {

    private val ui = Handler(Looper.getMainLooper())
    private var W = 0
    private var H = 0
    private lateinit var wall: ImageView
    private lateinit var dock: LinearLayout
    private lateinit var wifi: ImageView
    private var apps: List<AppEntry> = emptyList()
    private var dockKey = ""
    private var started = false
    private var netCb: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dm = resources.displayMetrics
        W = dm.widthPixels
        H = dm.heightPixels
        getExternalFilesDir(null) // creates the folder used for the wallpaper override
        setContentView(build())
    }

    override fun onStart() {
        super.onStart()
        started = true
        loadWallpaper()
        watchNetwork()
    }

    override fun onResume() {
        super.onResume()
        Apps.load(this) { list ->
            if (isDestroyed) return@load
            apps = list
            fillDock()
        }
    }

    override fun onStop() {
        super.onStop()
        started = false
        // Free the big bitmap while another app is in front.
        wall.setImageDrawable(null)
        val cm = getSystemService(ConnectivityManager::class.java)
        netCb?.let { try { cm.unregisterNetworkCallback(it) } catch (_: Exception) {} }
        netCb = null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Home pressed while already home: jump back to the first tile.
        if (::dock.isInitialized) dock.getChildAt(0)?.requestFocus()
    }

    @Deprecated("Launcher must never exit")
    override fun onBackPressed() { /* stay on Home */ }

    // ---------------------------------------------------------------- UI

    private fun build(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            clipChildren = false
            clipToPadding = false
        }

        wall = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            // Themed fallback (sunset / blue / crimson / blossom) when no wallpaper file is present.
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFF7A3A0A.toInt(), 0xFF1A2F7A.toInt(), 0xFF1B2A6B.toInt(), 0xFF7A1020.toInt(), 0xFFD98BAA.toInt()),
            )
        }
        root.addView(wall, FrameLayout.LayoutParams(-1, -1))

        // Bottom scrim keeps tiles and text legible on any artwork.
        root.addView(View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x00000000, 0x00000000, 0xD0000000.toInt()),
            )
        }, FrameLayout.LayoutParams(-1, -1))

        root.addView(statusBar(), FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply {
            topMargin = (H * 0.025f).toInt()
            marginEnd = (W * 0.025f).toInt()
        })
        root.addView(navPanel(), FrameLayout.LayoutParams((W * 0.15f).toInt(), -2, Gravity.TOP or Gravity.START).apply {
            leftMargin = (W * 0.02f).toInt()
            topMargin = (H * 0.44f).toInt()
        })
        root.addView(titleBlock(), FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply {
            topMargin = (H * 0.575f).toInt()
            leftMargin = (W * 0.17f).toInt()
            rightMargin = (W * 0.17f).toInt()
        })

        dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipChildren = false
            clipToPadding = false
        }
        root.addView(dock, FrameLayout.LayoutParams((W * 0.79f).toInt(), -2, Gravity.BOTTOM or Gravity.START).apply {
            leftMargin = (W * 0.17f).toInt()
            bottomMargin = (H * 0.09f).toInt()
        })
        return root
    }

    private fun statusBar(): View {
        val icon = (H * 0.032f).toInt()
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        bar.addView(iconButton(R.drawable.ic_search, icon) { openSearch() })
        bar.addView(iconButton(R.drawable.ic_settings, icon) { openSettings() })
        wifi = ImageView(this).apply {
            setImageResource(R.drawable.ic_wifi)
            alpha = 0.3f
        }
        bar.addView(wifi, LinearLayout.LayoutParams(icon, icon).apply {
            marginStart = (icon * 0.3f).toInt()
            marginEnd = (icon * 0.5f).toInt()
        })
        bar.addView(TextClock(this).apply {
            format12Hour = "h:mm a"
            format24Hour = "h:mm a"
            setTextColor(Ui.WARM_WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, H * 0.027f)
            setShadowLayer(6f, 0f, 2f, 0xAA000000.toInt())
        })
        return bar
    }

    private fun iconButton(res: Int, size: Int, onClick: () -> Unit): View {
        val pad = (size * 0.3f).toInt()
        return ImageView(this).apply {
            setImageResource(res)
            setPadding(pad, pad, pad, pad)
            background = Ui.states(ColorDrawable(0), Ui.rounded(0x55FFFFFF, size.toFloat()))
            isFocusable = true
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(size + pad * 2, size + pad * 2)
            setOnClickListener { onClick() }
        }
    }

    private fun navPanel(): View {
        val pad = (H * 0.012f).toInt()
        val rowH = (H * 0.066f).toInt()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            background = Ui.rounded(0xAA000000.toInt(), H * 0.014f)
        }
        panel.addView(navRow(R.drawable.ic_home, "Home", true, rowH) { dock.getChildAt(0)?.requestFocus() })
        panel.addView(navRow(R.drawable.ic_apps, "Apps", false, rowH) { openAllApps() })
        panel.addView(navRow(R.drawable.ic_bolt, "Clean", false, rowH) {
            Toast.makeText(this, "Cleaning…", Toast.LENGTH_SHORT).show()
            Apps.clean(this) { freed ->
                Toast.makeText(this, "Freed ${freed / 1048576} MB", Toast.LENGTH_SHORT).show()
            }
        })
        panel.addView(navRow(R.drawable.ic_settings, "Settings", false, rowH) { openSettings() })
        return panel
    }

    private fun navRow(res: Int, text: String, active: Boolean, rowH: Int, onClick: () -> Unit): View {
        val r = H * 0.012f
        val icon = (H * 0.028f).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val p = (H * 0.016f).toInt()
            setPadding(p, 0, p, 0)
            background = Ui.states(
                ColorDrawable(0),
                Ui.rounded(0x99CC5500.toInt(), r, Ui.PEACH, 2),
                Ui.rounded(0x44CC5500, r),
            )
            isSelected = active
            isFocusable = true
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(-1, rowH)
            addView(ImageView(this@MainActivity).apply { setImageResource(res) }, LinearLayout.LayoutParams(icon, icon))
            addView(TextView(this@MainActivity).apply {
                this.text = text
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, H * 0.024f)
                setSingleLine()
                setPadding((H * 0.016f).toInt(), 0, 0, 0)
            })
            setOnClickListener { onClick() }
        }
    }

    private fun titleBlock(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        box.addView(TextView(this).apply {
            text = "アニメの世界"
            setTextColor(Ui.WARM_WHITE)
            typeface = Typeface.create("serif", Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, H * 0.058f)
            setShadowLayer(14f, 0f, 4f, 0xCC000000.toInt())
            gravity = Gravity.CENTER
        })
        box.addView(TextView(this).apply {
            text = "DIFFERENT WORLDS · ONE SCREEN"
            setTextColor(0xCCFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_PX, H * 0.015f)
            letterSpacing = 0.35f
            setShadowLayer(6f, 0f, 2f, 0xCC000000.toInt())
            gravity = Gravity.CENTER
        })
        box.addView(View(this).apply {
            background = Ui.rounded(0xFFD81E2B.toInt(), 6f)
        }, LinearLayout.LayoutParams((W * 0.11f).toInt(), maxOf(3, (H * 0.004f).toInt())).apply {
            topMargin = (H * 0.012f).toInt()
        })
        return box
    }

    // ---------------------------------------------------------------- Dock

    private fun fillDock() {
        val byPkg = apps.associateBy { it.pkg }
        val items = Pins.get(this).mapNotNull { byPkg[it] }.take(Pins.MAX)
        val key = items.joinToString(",") { it.pkg }
        if (key == dockKey) return // nothing changed: don't rebuild, don't lose focus
        dockKey = key

        val keepFocus = dock.hasFocus() || currentFocus == null
        val idx = dock.indexOfChild(dock.focusedChild).coerceAtLeast(0)
        dock.removeAllViews()

        val n = items.size + 1
        val gap = (W * 0.008f).toInt()
        val w = minOf((W * 0.79f).toInt().minus(gap * (n - 1)) / n, (W * 0.115f).toInt())
        val h = (w * 0.62f).toInt()

        for (e in items) {
            val tile = Ui.appTile(this, e, w, h)
            tile.setOnClickListener { Apps.launch(this, e) }
            tile.setOnLongClickListener {
                Pins.toggle(this, e.pkg) // long-press a dock tile to remove it
                dockKey = ""
                fillDock()
                true
            }
            dock.addView(tile, LinearLayout.LayoutParams(w, h).apply { marginEnd = gap })
        }
        val more = Ui.iconTile(this, R.drawable.ic_apps, "More Apps", w, h)
        more.setOnClickListener { openAllApps() }
        dock.addView(more, LinearLayout.LayoutParams(w, h))

        if (keepFocus) dock.getChildAt(idx.coerceAtMost(dock.childCount - 1))?.requestFocus()
    }

    // ---------------------------------------------------------------- Actions

    private fun openAllApps() = startActivity(Intent(this, AllAppsActivity::class.java))

    private fun openSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(this, "Settings not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSearch() {
        try {
            startActivity(Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(this, "Search not available", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------------------------------------------------------- Wallpaper

    /**
     * Wallpaper source, first match wins:
     *  1. /sdcard/Android/data/com.animeverse.launcher/files/wallpaper.jpg  (pushed over adb, no rebuild)
     *  2. app/src/main/assets/wallpaper.jpg | .webp | .png                  (bundled)
     */
    private fun openWallpaper(): InputStream? {
        val ext = File(getExternalFilesDir(null), "wallpaper.jpg")
        if (ext.exists()) return FileInputStream(ext)
        for (n in arrayOf("wallpaper.jpg", "wallpaper.webp", "wallpaper.png")) {
            try { return assets.open(n) } catch (_: IOException) {}
        }
        return null
    }

    private fun decodeWallpaper(): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val first = openWallpaper() ?: return null
        first.use { BitmapFactory.decodeStream(it, null, o) }
        if (o.outWidth <= 0) return null
        var s = 1
        while (o.outWidth / (s * 2) >= 1920) s *= 2 // never hold more than ~1080p in RAM
        o.inJustDecodeBounds = false
        o.inSampleSize = s
        return openWallpaper()?.use { BitmapFactory.decodeStream(it, null, o) }
    }

    private fun loadWallpaper() {
        Thread {
            val bmp = try { decodeWallpaper() } catch (_: Throwable) { null }
            ui.post {
                if (started && !isDestroyed && bmp != null) wall.setImageBitmap(bmp)
            }
        }.start()
    }

    // ---------------------------------------------------------------- Network indicator

    private fun watchNetwork() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { ui.post { wifi.alpha = 1f } }
            override fun onLost(network: Network) { ui.post { wifi.alpha = 0.3f } }
        }
        try {
            cm.registerDefaultNetworkCallback(cb)
            netCb = cb
        } catch (_: Exception) {
            wifi.alpha = 1f
        }
    }
}
