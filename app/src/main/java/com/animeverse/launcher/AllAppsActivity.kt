package com.animeverse.launcher

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Plain ScrollView + rows of focusable tiles: D-pad navigation is native, no adapters, no libraries. */
class AllAppsActivity : Activity() {

    private var W = 0
    private var H = 0
    private lateinit var grid: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dm = resources.displayMetrics
        W = dm.widthPixels
        H = dm.heightPixels

        val root = FrameLayout(this).apply { setBackgroundColor(0xFF0B0C10.toInt()) }

        root.addView(TextView(this).apply {
            text = "All Apps"
            setTextColor(Ui.WARM_WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, H * 0.045f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply {
            leftMargin = (W * 0.05f).toInt()
            topMargin = (H * 0.05f).toInt()
        })

        grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            val p = (W * 0.05f).toInt()
            setPadding(p, (H * 0.03f).toInt(), p, (H * 0.08f).toInt())
        }
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = false
            clipToPadding = false
            isSmoothScrollingEnabled = true
            addView(grid, ViewGroup.LayoutParams(-1, -2))
        }
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1).apply { topMargin = (H * 0.15f).toInt() })
        setContentView(root)

        Apps.load(this) { list -> if (!isDestroyed) fill(list) }
    }

    private fun fill(list: List<AppEntry>) {
        val cols = 6
        val gap = (W * 0.012f).toInt()
        val w = ((W * 0.9f).toInt() - gap * (cols - 1)) / cols
        val h = (w * 0.62f).toInt()

        for (chunk in list.chunked(cols)) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
            }
            for ((i, e) in chunk.withIndex()) {
                val tile = Ui.appTile(this, e, w, h)
                tile.setOnClickListener { Apps.launch(this, e) }
                row.addView(tile, LinearLayout.LayoutParams(w, h).apply { if (i < cols - 1) marginEnd = gap })
            }
            grid.addView(row, LinearLayout.LayoutParams(-2, h).apply { bottomMargin = gap })
        }
        (grid.getChildAt(0) as? ViewGroup)?.getChildAt(0)?.requestFocus()
    }
}
