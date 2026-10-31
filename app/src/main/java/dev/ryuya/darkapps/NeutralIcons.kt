package dev.ryuya.darkapps

import android.content.res.Resources
import android.graphics.drawable.Drawable
import java.util.Collections
import java.util.HashMap
import java.util.WeakHashMap
import kotlin.jvm.Synchronized
import org.xmlpull.v1.XmlPullParser

/* Only monochrome XML vector UI icons are tinted; multicolor assets and bitmaps stay intact. */
class NeutralIcons {
    private var foreground: Int = DarkPalette.TEXT

    fun foreground(color: Int) {
        foreground = color
    }

    private val monochrome: MutableMap<Int, Boolean> = HashMap<Int, Boolean>()
    private val mapped: MutableSet<Drawable?>? =
        Collections.newSetFromMap<Drawable?>(WeakHashMap<Drawable?, Boolean?>())

    @Synchronized
    fun map(resources: Resources, id: Int, drawable: Drawable?): Drawable? {
        if (drawable == null || !monochrome.getOrPut(id) { inspect(resources, id) }) return drawable
        val state = drawable.getConstantState()
        if (state == null) return drawable
        val copy = state.newDrawable(resources).mutate()
        copy.setTint(foreground)
        mapped!!.add(copy)
        return copy
    }

    @Synchronized
    fun contrast(drawable: Drawable?, brightBackground: Boolean) {
        if (drawable != null && mapped!!.contains(drawable))
            drawable.setTint(if (brightBackground) -0xe8dcd9 else foreground)
    }

    private fun inspect(resources: Resources, id: Int): Boolean {
        try {
            val name = resources.getResourceEntryName(id)
            if (
                !(name.startsWith("ic_") ||
                    name.startsWith("abc_ic_") ||
                    name == "svg_arrow_down") || name.contains("qr")
            )
                return false
            resources.getXml(id).use { xml ->
                var vector: Boolean = false
                var visible: Boolean = false
                var event = xml.getEventType()
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event != XmlPullParser.START_TAG) {
                        event = xml.next()
                        continue
                    }
                    if (xml.getName() == "vector") vector = true
                    if (!vector) return false
                    for (i in 0 until xml.getAttributeCount()) {
                        val attr = xml.getAttributeName(i)
                        if (!(attr == "fillColor" || attr == "strokeColor")) continue
                        // Resource references and theme-dependent selectors are left to the host
                        // theme.
                        if (xml.getAttributeResourceValue(i, 0) != 0) return false
                        val color = xml.getAttributeIntValue(i, 0)
                        if ((color ushr 24) == 0) continue
                        if (!DarkPalette.neutral(color)) return false
                        visible = true
                    }
                    event = xml.next()
                }
                return vector && visible
            }
        } catch (unsupported: Exception) {
            return false
        }
    }
}
