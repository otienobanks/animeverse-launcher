package com.animeverse.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.util.concurrent.Executors

class AppEntry(
    val pkg: String,
    val label: String,
    val intent: Intent,
    val art: Drawable?,
    val banner: Boolean,
)

/** Which apps appear on the Home dock. Stored as a comma-separated package list. */
object Pins {
    val DEFAULT = listOf(
        "com.netflix.ninja",
        "com.google.android.youtube.tv",
        "com.disney.disneyplus",
        "com.amazon.amazonvideo.livingroom",
        "com.apple.atve.androidtv.appletv",
        "com.wbd.stream",
        "com.hbo.hbonow",
        "com.crunchyroll.crunchyroid",
        "com.spotify.tv.android",
    )
    const val MAX = 8

    private fun sp(c: Context) = c.getSharedPreferences("av", Context.MODE_PRIVATE)

    fun get(c: Context): List<String> =
        sp(c).getString("pins", null)?.split(',')?.filter { it.isNotBlank() } ?: DEFAULT

    /** Returns true if the package is pinned after the toggle. */
    fun toggle(c: Context, pkg: String): Boolean {
        val list = get(c).toMutableList()
        val pinned = if (list.remove(pkg)) false else { list.add(pkg); true }
        sp(c).edit().putString("pins", list.joinToString(",")).apply()
        return pinned
    }
}

object Apps {
    private val scanner = Executors.newSingleThreadExecutor()
    private val cleaner = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** Scans launchable apps off the main thread, delivers the sorted list on the main thread. */
    fun load(ctx: Context, done: (List<AppEntry>) -> Unit) {
        val app = ctx.applicationContext
        scanner.execute {
            val list = scan(app)
            main.post { done(list) }
        }
    }

    private fun scan(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val seen = HashSet<String>()
        val out = ArrayList<AppEntry>()
        for (cat in arrayOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(q, 0)) {
                val ai = ri.activityInfo
                if (ai.packageName == ctx.packageName || !seen.add(ai.packageName)) continue
                val banner = ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm)
                val art = banner ?: ri.loadIcon(pm)
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(cat)
                    .setClassName(ai.packageName, ai.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                out.add(AppEntry(ai.packageName, ri.loadLabel(pm).toString(), intent, art, banner != null))
            }
        }
        out.sortBy { it.label.lowercase() }
        return out
    }

    /** Frees RAM first (async), then starts the app. */
    fun launch(ctx: Context, e: AppEntry) {
        clean(ctx, null)
        try {
            ctx.startActivity(e.intent)
        } catch (_: Exception) {
            Toast.makeText(ctx, "Cannot open ${e.label}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Asks Android to kill cached background processes of user-installed apps.
     * Only touches processes the system considers safe to kill; the foreground app and
     * anything playing audio are left alone. System apps are skipped.
     */
    fun clean(ctx: Context, onDone: ((freedBytes: Long) -> Unit)?) {
        val app = ctx.applicationContext
        cleaner.execute {
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            val before = mi.availMem
            for (ai in app.packageManager.getInstalledApplications(0)) {
                val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                if (isSystem || ai.packageName == app.packageName) continue
                try { am.killBackgroundProcesses(ai.packageName) } catch (_: Exception) {}
            }
            if (onDone != null) {
                Thread.sleep(500)
                am.getMemoryInfo(mi)
                val freed = (mi.availMem - before).coerceAtLeast(0L)
                main.post { onDone(freed) }
            }
        }
    }
}
