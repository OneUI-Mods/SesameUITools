package dev.ryuya.darkapps

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import java.util.Collections
import java.util.IdentityHashMap
import java.util.WeakHashMap

/* Styles existing native views without replacing listeners, content or layout parameters. */
class NativeAppearance(icons: NeutralIcons, app: SupportedApp?) {
    private val detached: MutableSet<Drawable?>? =
        Collections.newSetFromMap<Drawable?>(WeakHashMap<Drawable?, Boolean?>())
    private val visited: MutableSet<Drawable?>? =
        Collections.newSetFromMap<Drawable?>(IdentityHashMap<Drawable?, Boolean?>())
    private val povo: PovoAppearance?
    private val sesame: SesameAppearance?
    private var applying: Boolean = false
    private val icons: NeutralIcons

    init {
        this.icons = icons
        povo = if (app == SupportedApp.POVO) PovoAppearance() else null
        sesame = if (app == SupportedApp.SESAME) SesameAppearance() else null
    }

    @Throws(ReflectiveOperationException::class)
    fun tree(view: View?) {
        if (applying) return
        applying = true
        visited!!.clear()
        try {
            visit(view)
        } finally {
            visited!!.clear()
            applying = false
        }
    }

    fun profileContent(view: View?): Boolean {
        return povo != null && povo.profileContent(view!!)
    }

    @Throws(ReflectiveOperationException::class)
    private fun visit(view: View?) {
        if (view == null) return
        // Bitmaps, QR codes, camera previews and embedded Chromium surfaces are never recolored.
        if (povo != null) povo.surface(view)
        if (sesame != null) sesame.surface(view)
        background(view.getBackground())
        if (view.getBackgroundTintList() != null) {
            val old = view.getBackgroundTintList()
            val mapped = ColorLists.map(old, false)
            if (mapped !== old) view.setBackgroundTintList(mapped)
        }
        val brightAccent = brightAccent(view)
        if (view is ImageView) {
            icons.contrast(view.getDrawable(), brightAccent)
            val tint = view.getImageTintList()
            if (tint != null && !brightAccent) {
                val mapped: ColorStateList? =
                    if (povo == null) ColorLists.map(tint, true) else ColorLists.icon(tint)
                if (mapped !== tint) view.setImageTintList(mapped)
            }
            if (povo != null) povo.icon(view, brightAccent)
        }
        if (view is TextView) {
            for (icon in view.getCompoundDrawables()) icons.contrast(icon, brightAccent)
            var old = view.getTextColors()
            var mapped: ColorStateList? = if (brightAccent) old else ColorLists.map(old, true)
            if (mapped !== old) view.setTextColor(mapped)
            old = view.getHintTextColors()
            mapped = if (brightAccent) old else ColorLists.map(old, true)
            if (mapped !== old) view.setHintTextColor(mapped)
            old = view.getLinkTextColors()
            mapped = if (brightAccent) old else ColorLists.map(old, true)
            if (mapped !== old) view.setLinkTextColor(mapped)
        }
        if (view is WebView) {
            view.setBackgroundColor(DarkPalette.BACKGROUND)
            if (!view.getSettings().isAlgorithmicDarkeningAllowed())
                view.getSettings().setAlgorithmicDarkeningAllowed(true)
            return
        }
        if (view is ViewGroup) for (i in 0 until view.getChildCount()) visit(view.getChildAt(i))
    }

    @Throws(ReflectiveOperationException::class)
    private fun brightAccent(view: View?): Boolean {
        // Dark text on a yellow call-to-action must remain readable; photographs are not sampled.
        var view: View? = view
        var depth: Int = 0
        while (view != null && depth < 5) {
            val tint = view.getBackgroundTintList()
            if (tint != null) {
                val color = tint.getColorForState(view.getDrawableState(), tint.getDefaultColor())
                if ((color ushr 24) > 230)
                    return !DarkPalette.neutral(color) && ColorUtils.calculateLuminance(color) > 0.5
            }
            val d = view.getBackground()
            val hostColor: Int? = if (povo == null) null else povo.backgroundColor(view)
            val color: Int? =
                if (hostColor != null) hostColor
                else if (d is ColorDrawable) d.getColor()
                else if (d is GradientDrawable && d.getColor() != null)
                    d.getColor()!!.getDefaultColor()
                else null
            if (color != null && (color ushr 24) > 230) {
                if (DarkPalette.neutral(color)) return false
                return ColorUtils.calculateLuminance(color) > 0.5
            }
            view = view.parent as? View
            depth++
        }
        return false
    }

    @Throws(ReflectiveOperationException::class)
    private fun background(drawable: Drawable?) {
        if (drawable == null || !visited!!.add(drawable)) return
        if (detached!!.add(drawable)) drawable.mutate()
        if (povo != null) povo.shape(drawable)
        if (drawable is ColorDrawable) {
            val mapped = DarkPalette.background(drawable.getColor())
            if (mapped != drawable.getColor()) drawable.setColor(mapped)
        } else if (drawable is GradientDrawable) {
            if (drawable.getColor() != null) {
                val old = drawable.getColor()
                val mapped = ColorLists.map(old, false)
                if (mapped !== old) drawable.setColor(mapped)
            }
            val colors = drawable.getColors()
            if (colors != null) {
                var changed: Boolean = false
                for (i in colors.indices) {
                    val mapped = DarkPalette.background(colors[i])
                    changed = changed or (mapped != colors[i])
                    colors[i] = mapped
                }
                if (changed) drawable.setColors(colors)
            }
        } else if (drawable is LayerDrawable) {
            for (i in 0 until drawable.getNumberOfLayers()) background(drawable.getDrawable(i))
        } else if (drawable is StateListDrawable) {
            for (i in 0 until drawable.getStateCount()) background(drawable.getStateDrawable(i))
        } else if (drawable is InsetDrawable) background(drawable.getDrawable())
        else if (drawable is DrawableWrapper) background(drawable.getDrawable())
    }
}
