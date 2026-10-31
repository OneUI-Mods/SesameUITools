package dev.ryuya.darkapps

import android.content.res.ColorStateList
import android.content.res.Resources.NotFoundException
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Collections
import java.util.HashMap
import java.util.WeakHashMap
import kotlin.jvm.JvmField
import kotlin.jvm.JvmRecord
import kotlin.math.max
import kotlin.math.round

/* Reviewed povo 1.71.0-JP adapters; preserve host geometry and rasterize only known UI glyphs. */
class PovoAppearance {
    @JvmRecord
    private data class Card(
        val get: Method?,
        val set: Method?,
        val strokeColor: Method?,
        val strokeWidth: Method?,
        val getStrokeColor: Method?,
    ) {

        companion object {
            @JvmField
            val NONE: PovoAppearance.Card = PovoAppearance.Card(null, null, null, null, null)
        }

        override fun toString(): String =
            "Card[get=$get, set=$set, strokeColor=$strokeColor, strokeWidth=$strokeWidth, getStrokeColor=$getStrokeColor]"
    }

    @JvmRecord
    private data class Shape(val fill: Field?, val set: Method?) {

        companion object {
            @JvmField val NONE: PovoAppearance.Shape = PovoAppearance.Shape(null, null)
        }

        override fun toString(): String = "Shape[fill=$fill, set=$set]"
    }

    @Throws(ReflectiveOperationException::class)
    fun surface(view: View) {
        profileSurface(view)
        val card = PovoAppearance.CARDS.get(view.javaClass)
        if (card === PovoAppearance.Card.NONE) return
        val old: ColorStateList? = card!!.get!!.invoke(view) as ColorStateList?
        val mapped = ColorLists.card(old)
        if (mapped !== old) card!!.set!!.invoke(view, mapped)
        if (
            card!!.strokeColor != null &&
                card!!.strokeWidth != null &&
                card!!.getStrokeColor != null
        ) {
            val oldStroke: Int = card!!.getStrokeColor.invoke(view) as Int
            if (oldStroke == 0 || DarkPalette.neutral(oldStroke)) {
                if (oldStroke != DarkPalette.CARD_OUTLINE)
                    card!!.strokeColor.invoke(view, DarkPalette.CARD_OUTLINE)
                if (outlined!!.add(view))
                    card!!
                        .strokeWidth
                        .invoke(
                            view,
                            max(1, Math.round(view.getResources().getDisplayMetrics().density)),
                        )
            }
        }
    }

    private fun profileSurface(view: View) {
        // The host's profile is a fragment over the home screen. Its #faf7f7f7 background
        // is almost opaque in light mode, but exposes home content after dark conversion.
        if (name(view) == "profile_list") {
            // Data binding clears the XML tag. Find the fragment's actual root by its container.
            var root: View? = view
            while (true) {
                val parent = root!!.parent as? View ?: break
                if (name(parent) == "help_root_container") {
                    solidColor(root!!, DarkPalette.BACKGROUND)
                    break
                }
                root = parent
            }
            return
        }
        if (
            navigationRow(view) && insideProfile(view)
        ) // Only the reviewed navigation rows, preserving their padding and click handlers.
         solidColor(view, DarkPalette.CARD)
    }

    fun profileContent(view: View): Boolean {
        return name(view) == "profile_list" || (navigationRow(view) && insideProfile(view))
    }

    private fun navigationRow(view: View?): Boolean {
        if (view !is ViewGroup) return false
        var arrow: Boolean = false
        var actionText: Boolean = false
        for (i in 0 until view.getChildCount()) {
            val child = name(view.getChildAt(i))
            arrow = arrow or (child == "ivNavArrow")
            actionText =
                actionText or
                    (child == "tvUnderlinedHelpText" || child == "tvAddress" || child == "tvCardNo")
        }
        return arrow && actionText
    }

    private fun insideProfile(view: View?): Boolean {
        var parent: View? = view
        while (parent != null) {
            if (name(parent) == "profile_list") {
                return true
            }
            parent = parent.parent as? View
        }
        return false
    }

    private fun solidColor(view: View, color: Int) {
        val background = view.background as? ColorDrawable
        if (background != null && background.color != color) {
            background.mutate()
            background.setColor(color)
        }
    }

    @Throws(ReflectiveOperationException::class)
    fun shape(drawable: Drawable) {
        val shape = PovoAppearance.SHAPES.get(drawable.javaClass)
        if (shape === PovoAppearance.Shape.NONE || drawable.getConstantState() == null) return
        val old: ColorStateList? =
            shape!!.fill!!.get(drawable.getConstantState()) as ColorStateList?
        val mapped = ColorLists.map(old, false)
        if (mapped !== old) shape!!.set!!.invoke(drawable, mapped)
    }

    @Throws(ReflectiveOperationException::class)
    fun backgroundColor(view: View): Int? {
        val card = PovoAppearance.CARDS.get(view.javaClass)
        var colors: ColorStateList? = null
        if (card !== PovoAppearance.Card.NONE) colors = card!!.get!!.invoke(view) as ColorStateList?
        else if (view.getBackground() != null) {
            val drawable = view.getBackground()
            val shape = PovoAppearance.SHAPES.get(drawable.javaClass)
            if (shape !== PovoAppearance.Shape.NONE && drawable.getConstantState() != null)
                colors = shape!!.fill!!.get(drawable.getConstantState()) as ColorStateList?
        }
        return if (colors == null) null
        else colors.getColorForState(view.getDrawableState(), colors.getDefaultColor())
    }

    private val outlined: MutableSet<View?>? =
        Collections.newSetFromMap<View?>(WeakHashMap<View?, Boolean?>())
    private val pictures: MutableMap<Drawable?, MutableMap<Long?, Bitmap?>?> =
        WeakHashMap<Drawable?, MutableMap<Long?, Bitmap?>?>()

    private fun rasterizePicture(image: ImageView) {
        val source = image.getDrawable()
        if (source !is PictureDrawable) return
        val width = max(image.getWidth(), source.getIntrinsicWidth())
        val height = max(image.getHeight(), source.getIntrinsicHeight())
        if (width <= 0 || height <= 0 || width > 512 || height > 512) return
        val size: Long = (width.toLong() shl 32) or height.toLong()
        val sizes =
            pictures.computeIfAbsent(source) { ignored: Drawable? -> HashMap<Long?, Bitmap?>() }
        var bitmap = sizes!!.get(size)
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888)
            bitmap.setDensity(image.getResources().getDisplayMetrics().densityDpi)
            val picture = (source as PictureDrawable).getPicture()
            if (picture == null) return
            Canvas(bitmap).drawPicture(picture, Rect(0, 0, width, height))
            sizes!!.put(size, bitmap)
        }
        image.setImageDrawable(BitmapDrawable(image.getResources(), bitmap))
    }

    fun icon(image: ImageView, brightAccent: Boolean) {
        val name = name(image)
        var reviewed = PovoAppearance.MONOCHROME_VIEWS!!.contains(name)
        // The account header reuses ivNavArrow for a yellow info image. Only tint the reviewed
        // navigation chevrons and the address/payment pencils, preserving that colored indicator.
        if (name == "ivNavArrow" && image.getParent() is View)
            reviewed = navigationRow(image.parent as View)
        if (name == "icon") {
            // Only the SMS/call glyphs inside the reviewed usage cards, not arbitrary ImageViews.
            var parent: View? = image
            while (parent != null) {
                if (name(parent) == "telcoDetail") {
                    reviewed = true
                    break
                }
                parent = parent.parent as? View
            }
        }
        if (!reviewed) return
        rasterizePicture(image)
        val color: Int = if (brightAccent) -0xe8dcd9 else DarkPalette.ICON
        val current = image.getImageTintList()
        if (current == null || current.isStateful() || current.getDefaultColor() != color)
            image.setImageTintList(ColorStateList.valueOf(color))
    }

    private fun name(view: View): String? {
        if (view.getId() == View.NO_ID) return ""
        try {
            return view.getResources().getResourceEntryName(view.getId())
        } catch (missing: NotFoundException) {
            return ""
        }
    }

    companion object {
        private val CARDS: ClassValue<PovoAppearance.Card?> =
            object : ClassValue<PovoAppearance.Card?>() {
                override fun computeValue(type: Class<*>): PovoAppearance.Card? {
                    var parent: Class<*>? = type
                    while (parent != null) {
                        if (parent.getName() != "androidx.cardview.widget.CardView") {
                            parent = parent.getSuperclass()
                            continue
                        }
                        try {
                            var strokeColor: Method? = null
                            var strokeWidth: Method? = null
                            var getStrokeColor: Method? = null
                            try {
                                strokeColor =
                                    type.getMethod("setStrokeColor", Int::class.javaPrimitiveType)
                                strokeWidth =
                                    type.getMethod("setStrokeWidth", Int::class.javaPrimitiveType)
                                getStrokeColor = type.getMethod("getStrokeColor")
                            } catch (ordinaryCard: NoSuchMethodException) {
                                /* Surface contrast is sufficient for plain CardView. */
                            }
                            return PovoAppearance.Card(
                                type.getMethod("getCardBackgroundColor"),
                                type.getMethod(
                                    "setCardBackgroundColor",
                                    ColorStateList::class.java,
                                ),
                                strokeColor,
                                strokeWidth,
                                getStrokeColor,
                            )
                        } catch (unavailable: NoSuchMethodException) {
                            return PovoAppearance.Card.NONE
                        }
                        parent = parent.getSuperclass()
                    }
                    return PovoAppearance.Card.NONE
                }
            }

        private val SHAPES: ClassValue<PovoAppearance.Shape?> =
            object : ClassValue<PovoAppearance.Shape?>() {
                override fun computeValue(type: Class<*>): PovoAppearance.Shape? {
                    // This bundled MaterialShapeDrawable was minified by the reviewed povo build.
                    // Its constant-state field c is the fill; method m updates it and invalidates
                    // the
                    // drawable.
                    // HostAppearance's exact version gate must remain in place for this contract.
                    var parent: Class<*>? = type
                    while (parent != null) {
                        if (parent.getName() != "Md.g") {
                            parent = parent.getSuperclass()
                            continue
                        }
                        try {
                            val state = Class.forName("Md.g\$b", false, type.getClassLoader())
                            val fill = state.getField("c")
                            if (fill.getType() != ColorStateList::class.java)
                                return PovoAppearance.Shape.NONE
                            return PovoAppearance.Shape(
                                fill,
                                parent.getMethod("m", ColorStateList::class.java),
                            )
                        } catch (unavailable: ReflectiveOperationException) {
                            return PovoAppearance.Shape.NONE
                        }
                        parent = parent.getSuperclass()
                    }
                    return PovoAppearance.Shape.NONE
                }
            }

        private val MONOCHROME_VIEWS: MutableSet<String?>? =
            java.util.Set.of<String?>(
                "planDetailsIcon",
                "planHistoryIcon",
                "navigation_bar_item_icon_view",
            )
    }
}
