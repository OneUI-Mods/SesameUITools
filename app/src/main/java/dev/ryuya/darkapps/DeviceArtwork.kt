package dev.ryuya.darkapps

import android.content.res.Resources
import android.content.res.Resources.NotFoundException
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.util.LinkedHashMap
import kotlin.jvm.JvmRecord
import kotlin.jvm.Synchronized

/* Darkens only the reviewed, bundled UI artwork, never user photos or QR codes. */
class DeviceArtwork(app: SupportedApp?) {
    private val names: MutableSet<String?>
    private val povo: Boolean

    @JvmRecord
    private data class Key(val resource: Int, val width: Int, val height: Int, val density: Int) {
        override fun toString(): String =
            "Key[resource=$resource, width=$width, height=$height, density=$density]"
    }

    private val cache: MutableMap<DeviceArtwork.Key?, Bitmap?> =
        object : LinkedHashMap<DeviceArtwork.Key?, Bitmap?>(32, .75f, true) {
            override fun removeEldestEntry(
                entry: MutableMap.MutableEntry<DeviceArtwork.Key?, Bitmap?>?
            ): Boolean {
                return size > 32
            }
        }

    init {
        povo = app == SupportedApp.POVO
        names = (if (povo) DeviceArtwork.POVO_NAMES else DeviceArtwork.SESAME_NAMES)!!
    }

    @Synchronized
    fun map(resources: Resources, id: Int, drawable: Drawable?): Drawable? {
        if (drawable !is BitmapDrawable) return drawable
        try {
            if (!names.contains(resources.getResourceEntryName(id))) return drawable
        } catch (missing: NotFoundException) {
            return drawable
        }
        val source = drawable.getBitmap()
        if (source == null || source.getWidth() * source.getHeight().toLong() > 1048576)
            return drawable
        val key = DeviceArtwork.Key(id, source.getWidth(), source.getHeight(), source.getDensity())
        var result = cache.get(key)
        if (result == null) {
            val readable: Bitmap? =
                if (source.getConfig() == Config.HARDWARE) source.copy(Config.ARGB_8888, false)
                else source
            if (readable == null) return drawable
            val pixels = IntArray(readable.getWidth() * readable.getHeight())
            readable.getPixels(
                pixels,
                0,
                readable.getWidth(),
                0,
                0,
                readable.getWidth(),
                readable.getHeight(),
            )
            for (i in pixels.indices) {
                val pixelColor: Int = pixels[i]
                var mapped = DarkPalette.artwork(pixelColor)
                if (povo && DarkPalette.neutral(pixelColor) && (pixelColor ushr 24) != 0)
                    mapped =
                        ((pixelColor and -0x1000000) or
                            ((if ((pixelColor and 255) >= 245) DarkPalette.CARD
                            else DarkPalette.ICON) and 0x00ffffff))
                pixels[i] = mapped
            }
            result =
                Bitmap.createBitmap(
                    pixels,
                    readable.getWidth(),
                    readable.getHeight(),
                    Config.ARGB_8888,
                )
            result.setDensity(source.getDensity())
            if (readable != source) readable.recycle()
            cache.put(key, result)
        }
        return BitmapDrawable(resources, result)
    }

    companion object {
        private val SESAME_NAMES: MutableSet<String?>? =
            java.util.Set.of<String?>(
                "icon_lock",
                "icon_unlock",
                "icon_receiveblee",
                "icon_waitgatt",
                "icon_logining",
                "icon_nosignal",
                "icon_nosetting",
                "icon_opensensor",
                "swtich_no_ble",
                "swtich_receive_ble",
                "swtich_waitgatt",
                "swtich_logining",
                "swtich_locked",
                "swtich_unlocked",
            )

        private val POVO_NAMES: MutableSet<String?>? =
            java.util.Set.of<String?>(
                "ic_menu_home",
                "ic_menu_shop",
                "ic_menu_faq",
                "ic_menu_help",
                "ic_menu_games",
                "ic_menu_discover",
                "ic_menu_not",
                "ic_toolbar_profile",
                "toolbar_profile_icon",
                "icon_bell",
                "ud_info_icon",
                "ic_profile_placeholder",
            )
    }
}
