package com.animeverse.launcher

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextClock
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * ANIMEVERSE Home. The artwork (wallpaper.jpg) already contains the painted menu and app tiles,
 * so this screen draws nothing except a live clock. Invisible focusable areas sit exactly over
 * each painted tile/menu row and show a warm-white focus ring when the remote lands on them.
 * Result: the exact theme, very little RAM, no per-frame work.
 */
class MainActivity : Activity() {

    // Design space of the artwork (1672 x 941). Everything is positioned in these units.
    private val DW = 1672f
    private val DH = 941f

    private class AppSlot(val name: String, val x1: Int, val x2: Int, vararg val pkgs: String)

    private val ui = Handler(Looper.getMainLooper())
    private var W = 0
    private var H = 0
    private var sx = 1f
    private var sy = 1f
    private lateinit var root: FrameLayout
    private lateinit var wall: ImageView
    private var firstTile: View? = null
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dm = resources.displayMetrics
        W = dm.widthPixels
        H = dm.heightPixels
        sx = W / DW
        sy = H / DH
        getExternalFilesDir(null) // creates the folder used for the optional wallpaper override
        setContentView(build())
    }

    override fun onStart() {
        super.onStart()
        started = true
        loadWallpaper()
    }

    override fun onResume() {
        super.onResume()
        if (currentFocus == null) firstTile?.requestFocus()
        val sp = getSharedPreferences("av", MODE_PRIVATE)
        if (!isDefaultHome() && !sp.getBoolean("asked_home", false)) {
            sp.edit().putBoolean("asked_home", true).apply()
            ui.postDelayed({ if (started) requestDefaultHome() }, 600)
        }
    }

    override fun onStop() {
        super.onStop()
        started = false
        wall.setImageDrawable(null) // free the bitmap while another app is in front
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        firstTile?.requestFocus() // Home pressed while already home
    }

    @Deprecated("Launcher must never exit")
    override fun onBackPressed() { /* stay on Home */ }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) { // remote MENU button = clean memory
            cleanNow()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ---------------------------------------------------------------- UI

    private fun build(): View {
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        wall = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY // 16:9 art on a 16:9 TV; keeps overlay math exact
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFF7A3A0A.toInt(), 0xFF1A2F7A.toInt(), 0xFF7A1020.toInt(), 0xFFD98BAA.toInt()),
            )
        }
        root.addView(wall, FrameLayout.LayoutParams(-1, -1))

        // ---- App dock (painted tiles, y 711..799)
        val dock = listOf(
            AppSlot("Netflix", 248, 385, "com.netflix.ninja"),
            AppSlot("YouTube", 398, 553, "com.google.android.youtube.tv", "com.google.android.youtube"),
            AppSlot("Disney+", 566, 694, "com.disney.disneyplus"),
            AppSlot("Prime Video", 706, 833, "com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"),
            AppSlot("Apple TV", 845, 962, "com.apple.atve.androidtv.appletv"),
            AppSlot("Max", 974, 1092, "com.wbd.stream", "com.hbo.hbonow"),
            AppSlot("Crunchyroll", 1104, 1237, "com.crunchyroll.crunchyroid"),
            AppSlot("Spotify", 1249, 1393, "com.spotify.tv.android", "com.spotify.music"),
        )
        for ((i, a) in dock.withIndex()) {
            val v = slot(a.x1, 711, a.x2, 799, 10f, { open(a.name, *a.pkgs) })
            if (i == 0) firstTile = v
        }
        slot(1408, 711, 1613, 799, 10f, { openAllApps() }) // More Apps

        // ---- Left menu (painted panel, rows 52 px tall)
        slot(50, 418, 292, 470, 14f, { firstTile?.requestFocus() }, { requestDefaultHome() }) // Home (hold OK = set default)
        slot(50, 478, 292, 530, 14f, { open("Netflix", "com.netflix.ninja") })                // TV Shows
        slot(50, 534, 292, 586, 14f, { open("Prime Video", "com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient") }) // Movies
        slot(50, 589, 292, 641, 14f, { open("Crunchyroll", "com.crunchyroll.crunchyroid") })  // Anime
        slot(50, 644, 292, 696, 14f, { openAllApps() })                                        // Apps
        slot(50, 699, 292, 751, 14f, { openSettings() })                                       // Settings

        // ---- Top-right: search + settings are painted, clock is live
        slot(1338, 28, 1376, 66, 19f, { openSearch() })
        slot(1406, 28, 1444, 66, 19f, { openSettings() })
        val clock = TextClock(this).apply {
            format12Hour = "h:mm a"
            format24Hour = "h:mm a"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 23f * sy)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        root.addView(clock, FrameLayout.LayoutParams(((1642 - 1500) * sx).toInt(), (34 * sy).toInt()).apply {
            leftMargin = (1500 * sx).toInt()
            topMargin = (30 * sy).toInt()
        })
        return root
    }

    /** Invisible focus target at design coordinates; shows a ring + faint wash when focused. */
    private fun slot(
        x1: Int, y1: Int, x2: Int, y2: Int, radius: Float,
        onClick: () -> Unit, onLong: (() -> Unit)? = null,
    ): View {
        val r = radius * sy
        val v = View(this).apply {
            background = Ui.states(
                ColorDrawable(0),
                Ui.rounded(0x26FFF1E0, r, Ui.WARM_WHITE, maxOf(3, (3 * sy).toInt())),
            )
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
            if (onLong != null) setOnLongClickListener { onLong(); true }
        }
        root.addView(v, FrameLayout.LayoutParams(((x2 - x1) * sx).toInt(), ((y2 - y1) * sy).toInt()).apply {
            leftMargin = (x1 * sx).toInt()
            topMargin = (y1 * sy).toInt()
        })
        return v
    }

    // ---------------------------------------------------------------- Actions

    private fun open(name: String, vararg pkgs: String) {
        for (p in pkgs) {
            val i = packageManager.getLeanbackLaunchIntentForPackage(p)
                ?: packageManager.getLaunchIntentForPackage(p)
            if (i != null) {
                Apps.clean(this, null) // free RAM first, off the main thread
                try {
                    startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {
                    Toast.makeText(this, "Cannot open $name", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
        Toast.makeText(this, "$name is not installed", Toast.LENGTH_SHORT).show()
        try { // offer it in the Play Store if the TV has one
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${pkgs[0]}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }

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

    private fun cleanNow() {
        Toast.makeText(this, "Cleaning…", Toast.LENGTH_SHORT).show()
        Apps.clean(this) { freed ->
            Toast.makeText(this, "Freed ${freed / 1048576} MB", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------------------------------------------------------- Default Home

    private fun isDefaultHome(): Boolean {
        val ri = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return ri?.activityInfo?.packageName == packageName
    }

    /** Tries the Android "make this your Home app" dialog, then the Home settings screen. */
    private fun requestDefaultHome() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val rm = getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 1)
                    return
                }
            }
        } catch (_: Exception) {}
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            return
        } catch (_: Exception) {}
        Toast.makeText(this, "This TV does not allow changing the Home app from here", Toast.LENGTH_LONG).show()
    }

    // ---------------------------------------------------------------- Wallpaper

    /**
     * Artwork source, first match wins:
     *  1. /sdcard/Android/data/com.animeverse.launcher/files/wallpaper.jpg  (push over adb, no rebuild;
     *     must contain the same painted menu/dock layout or the focus areas will not line up)
     *  2. bundled assets/wallpaper.jpg (the ANIMEVERSE theme)
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
            ui.post { if (started && !isDestroyed && bmp != null) wall.setImageBitmap(bmp) }
        }.start()
    }
}
