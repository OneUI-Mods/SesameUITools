package dev.ryuya.darkapps

import android.content.res.ColorStateList
import android.content.res.Resources.NotFoundException
import android.view.View
import android.widget.TextView

/* Reviewed Sesame search header; tint existing shapes without changing scrolling or geometry. */
class SesameAppearance {
    fun surface(view: View) {
        val name: String?
        try {
            name = view.getResources().getResourceEntryName(view.getId())
        } catch (unnamed: NotFoundException) {
            return
        }
        if (name == "appBarLayout") {
            // AppBarLayout replaces the XML ColorDrawable with its bundled Material shape.
            // View tint covers that fill through its public API, preserving the host drawable.
            background(view, DarkPalette.BACKGROUND)
        } else if (name == "searchEditText" && view is TextView) {
            background(view, DarkPalette.SURFACE)
            if (view.getCurrentHintTextColor() != DarkPalette.SECONDARY)
                view.setHintTextColor(DarkPalette.SECONDARY)
            val current = view.getCompoundDrawableTintList()
            if (
                current == null ||
                    current.isStateful() ||
                    current.getDefaultColor() != DarkPalette.ICON
            )
                view.setCompoundDrawableTintList(ColorStateList.valueOf(DarkPalette.ICON))
        }
    }

    private fun background(view: View, color: Int) {
        val current = view.getBackgroundTintList()
        if (current == null || current.isStateful() || current.getDefaultColor() != color)
            view.setBackgroundTintList(ColorStateList.valueOf(color))
    }
}
