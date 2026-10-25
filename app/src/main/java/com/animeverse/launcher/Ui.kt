package com.animeverse.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

object Ui {
    val WARM_WHITE = 0xFFFFF1E0.toInt()
    val PEACH = 0xFFFFC9A0.toInt()
    val ORANGE = 0xFFE8650A.toInt()
    val CHARCOAL = 0xFF1C1F26.toInt()

    private val brand = mapOf(
        "com.netflix.ninja" to 0xFF000000.toInt(),
        "com.google.android.youtube.tv" to 0xFF0F0F0F.toInt(),
        "com.disney.disneyplus" to 0xFF0B1E5B.toInt(),
        "com.amazon.amazonvideo.livingroom" to 0xFF1A98FF.toInt(),
        "com.apple.atve.androidtv.appletv" to 0xFF000000.toInt(),
        "com.wbd.stream" to 0xFF3A2BD8.toInt(),
        "com.hbo.hbonow" to 0xFF3A2BD8.toInt(),
        "com.crunchyroll.crunchyroid" to 0xFFF47521.toInt(),
        "com.spotify.tv.android" to 0xFF000000.toInt(),
    )

    fun rounded(fill: Int, radius: Float, stroke: Int = 0, strokePx: Int = 0) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = radius
        if (strokePx > 0) setStroke(strokePx, stroke)
    }

    fun states(normal: Drawable, focused: Drawable, selected: Drawable? = null) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), focused)
        if (selected != null) addState(intArrayOf(android.R.attr.state_selected), selected)
        addState(intArrayOf(), normal)
    }

    /** Apple TV style focus: gentle lift + scale, 110 ms, no blur or glow layers. */
    fun pop(v: View) {
        v.setOnFocusChangeListener { view, has ->
            val s = if (has) 1.1f else 1f
            view.animate().cancel()
            view.animate().scaleX(s).scaleY(s).translationZ(if (has) 16f else 0f).setDuration(110).start()
        }
    }

    private fun tileBase(ctx: Context, bg: Int, w: Int, h: Int): FrameLayout {
        val r = h * 0.12f
        return FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(w, h)
            background = rounded(bg, r)
            clipToOutline = true
            foreground = states(
                ColorDrawable(Color.TRANSPARENT),
                rounded(Color.TRANSPARENT, r, WARM_WHITE, maxOf(3, (h * 0.035f).toInt())),
            )
            isFocusable = true
            isClickable = true
            isLongClickable = true
            pop(this)
        }
    }

    private fun label(ctx: Context, text: String, h: Int) = TextView(ctx).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, h * 0.13f)
        gravity = Gravity.CENTER
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    fun appTile(ctx: Context, e: AppEntry, w: Int, h: Int): FrameLayout {
        val f = tileBase(ctx, brand[e.pkg] ?: CHARCOAL, w, h)
        val img = ImageView(ctx).apply { setImageDrawable(e.art) }
        if (e.banner) {
            img.scaleType = ImageView.ScaleType.CENTER_CROP
            f.addView(img, FrameLayout.LayoutParams(-1, -1))
        } else {
            val s = (h * 0.46f).toInt()
            img.scaleType = ImageView.ScaleType.FIT_CENTER
            f.addView(img, FrameLayout.LayoutParams(s, s, Gravity.CENTER_HORIZONTAL or Gravity.TOP).apply {
                topMargin = (h * 0.12f).toInt()
            })
            f.addView(label(ctx, e.label, h), FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
                bottomMargin = (h * 0.08f).toInt()
            })
        }
        return f
    }

    fun iconTile(ctx: Context, res: Int, text: String, w: Int, h: Int): FrameLayout {
        val f = tileBase(ctx, 0xFF23262D.toInt(), w, h)
        val s = (h * 0.42f).toInt()
        f.addView(ImageView(ctx).apply { setImageResource(res) },
            FrameLayout.LayoutParams(s, s, Gravity.CENTER_HORIZONTAL or Gravity.TOP).apply { topMargin = (h * 0.14f).toInt() })
        f.addView(label(ctx, text, h), FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            bottomMargin = (h * 0.08f).toInt()
        })
        return f
    }

    fun pinDot(ctx: Context, h: Int): View = View(ctx).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ORANGE)
            setStroke(2, PEACH)
        }
    }
}
