package dev.ryuya.darkapps

import android.app.LocaleManager
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import android.provider.Settings
import android.view.MenuItem
import android.view.MenuItem.OnMenuItemClickListener
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textview.MaterialTextView
import java.util.EnumMap
import kotlin.jvm.JvmField
import kotlin.math.round

/* Material 3 settings with a non-interactive, synthetic color preview. */
class MainActivity : AppCompatActivity() {
    private class PageState {
        @JvmField var status: MaterialTextView? = null
        @JvmField var preview: MaterialCardView? = null
        @JvmField var previewBody: LinearLayout? = null
    }

    private val pages: MutableMap<SupportedApp, MainActivity.PageState?> =
        EnumMap<SupportedApp, MainActivity.PageState?>(SupportedApp::class.java)
    private var tabs: SettingsTabs? = null

    public override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        ConfigurationAccess.grant(this)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false)
        val root = vertical()
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(
            root,
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val bars = insets!!.getInsets(Type.systemBars() or Type.displayCutout())
                v!!.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets!!
            },
        )
        val toolbar = MaterialToolbar(this)
        toolbar.setTitle(R.string.app_name)
        toolbar
            .getMenu()
            .add(R.string.language)
            .setOnMenuItemClickListener(
                OnMenuItemClickListener { item: MenuItem? ->
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.language)
                        .setItems(
                            arrayOf<String>("System / システム", "日本語", "English"),
                            android.content.DialogInterface.OnClickListener {
                                d: DialogInterface?,
                                which: Int ->
                                getSystemService<LocaleManager?>(LocaleManager::class.java)
                                    .setApplicationLocales(
                                        LocaleList.forLanguageTags(
                                            if (which == 1) "ja" else if (which == 2) "en" else ""
                                        )
                                    )
                            },
                        )
                        .show()
                    true
                }
            )
        root.addView(toolbar)
        tabs = SettingsTabs(this, root, saved)
        for (app in SupportedApp.entries) appearance(tabs!!.page(app.title), app)
        about(tabs!!.page(R.string.about_tab))
        tabs!!.restore()
        ViewCompat.requestApplyInsets(root)
    }

    private fun appearance(page: LinearLayout, app: SupportedApp) {
        val section = MainActivity.PageState()
        pages.put(app, section)
        val prefs = app.settings(this)
        val master = card(page, true)
        val enabled = MaterialSwitch(this)
        enabled.setId(View.generateViewId())
        enabled.setText(getString(R.string.enabled, getString(app.title)))
        enabled.setMinHeight(dp(56))
        enabled.setChecked(prefs!!.getBoolean("enabled", true))
        master.addView(enabled, android.widget.LinearLayout.LayoutParams(-1, -2))
        master.addView(text(R.string.enabled_note, false))
        enabled.setOnCheckedChangeListener(
            android.widget.CompoundButton.OnCheckedChangeListener {
                button: CompoundButton?,
                value: Boolean ->
                prefs!!.edit().putBoolean("enabled", value).apply()
                changed(app)
            }
        )
        val theme = card(page, false)
        theme.addView(text(R.string.appearance, true))
        val choices = RadioGroup(this)
        val mode = ThemeChoice.valid(prefs!!.getString("mode", ThemeChoice.SYSTEM))
        val values = arrayOf<String?>(ThemeChoice.SYSTEM, ThemeChoice.DARK, ThemeChoice.LIGHT)
        val labels = intArrayOf(R.string.mode_system, R.string.mode_dark, R.string.mode_light)
        for (i in values.indices) {
            val choice = MaterialRadioButton(this)
            choice.setId(View.generateViewId())
            choice.setTag(values[i])
            choice.setText(labels[i])
            choice.setMinHeight(dp(52))
            choices.addView(choice, android.widget.RadioGroup.LayoutParams(-1, -2))
            if (values[i] == mode) choice.setChecked(true)
        }
        theme.addView(choices)
        choices.setOnCheckedChangeListener(
            android.widget.RadioGroup.OnCheckedChangeListener { group: RadioGroup?, id: Int ->
                val choice = group!!.findViewById<View?>(id)
                if (choice == null) return@OnCheckedChangeListener
                prefs!!.edit().putString("mode", choice.getTag() as String?).apply()
                changed(app)
            }
        )
        theme.addView(text(R.string.restart_note, false))
        val sample = card(page, false)
        sample.addView(text(R.string.preview_title, true))
        section.preview = MaterialCardView(this)
        section.preview!!.setRadius(dp(20).toFloat())
        section.preview!!.setCardElevation(0f)
        section.preview!!.setStrokeWidth(0)
        section.previewBody = vertical()
        section.previewBody!!.setPadding(dp(20), dp(22), dp(20), dp(22))
        section.preview!!.addView(section.previewBody)
        val size = android.widget.LinearLayout.LayoutParams(-1, -2)
        size.topMargin = dp(16)
        size.bottomMargin = dp(12)
        sample.addView(section.preview, size)
        sample.addView(text(R.string.preview_note, false))
        updatePreview(app)
        val state = card(page, false)
        state.addView(text(R.string.status_title, true))
        section.status = text(R.string.status_waiting, false)
        section.status!!.setPadding(0, dp(12), 0, dp(12))
        state.addView(section.status)
        val open = button(R.string.open_target)
        open.setText(getString(R.string.open_target, getString(app.title)))
        open.setOnClickListener(
            android.view.View.OnClickListener { v: View? ->
                val intent = getPackageManager().getLaunchIntentForPackage(app.packageName)
                if (intent != null) startActivity(intent)
                else Toast.makeText(this, R.string.not_installed, Toast.LENGTH_SHORT).show()
            }
        )
        state.addView(open, android.widget.LinearLayout.LayoutParams(-1, -2))
        val info = button(R.string.app_info)
        info.setOnClickListener(
            android.view.View.OnClickListener { v: View? ->
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + app.packageName),
                    )
                )
            }
        )
        state.addView(info, android.widget.LinearLayout.LayoutParams(-1, -2))
        val reset = button(R.string.guard_reset)
        reset.setOnClickListener(
            android.view.View.OnClickListener { v: View? ->
                app.settings(this)!!.edit().putLong("reset", System.currentTimeMillis()).apply()
                changed(app)
            }
        )
        state.addView(reset, android.widget.LinearLayout.LayoutParams(-1, -2))
    }

    private fun changed(app: SupportedApp) {
        app.settings(this)!!.edit().putString("state", "waiting").apply()
        ConfigProvider.changed(this, app)
        updatePreview(app)
        refreshStatus(app)
    }

    private fun updatePreview(app: SupportedApp) {
        val section = pages.get(app)
        if (section!!.preview == null) return
        val prefs = app.settings(this)
        val night: Boolean =
            ((getResources().getConfiguration().uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES)
        val dark =
            ThemeChoice.dark(
                prefs!!.getBoolean("enabled", true),
                prefs!!.getString("mode", ThemeChoice.SYSTEM),
                night,
            )
        section!!.preview!!.setCardBackgroundColor(if (dark) DarkPalette.BACKGROUND else -0x50506)
        section!!.previewBody!!.removeAllViews()
        val title =
            text(
                if (app == SupportedApp.POVO) R.string.povo_preview_name else R.string.preview_name,
                true,
            )
        title.setTextColor(if (dark) DarkPalette.TEXT else -0xe8dcd9)
        section!!.previewBody!!.addView(title)
        val subtitle =
            text(
                if (app == SupportedApp.POVO) R.string.povo_preview_detail
                else R.string.preview_detail,
                false,
            )
        subtitle.setTextColor(if (dark) DarkPalette.SECONDARY else -0xa0938e)
        subtitle.setPadding(0, dp(8), 0, dp(16))
        section!!.previewBody!!.addView(subtitle)
        val line = View(this)
        line.setBackgroundColor(if (app == SupportedApp.POVO) -0x1a00 else -0xd7514f)
        section!!
            .previewBody!!
            .addView(line, android.widget.LinearLayout.LayoutParams(dp(36), dp(4)))
    }

    private fun about(page: LinearLayout) {
        val setup = card(page, false)
        setup.addView(text(R.string.setup_title, true))
        setup.addView(text(R.string.setup_body, false))
        val compatibility = card(page, false)
        compatibility.addView(text(R.string.compatibility, true))
        compatibility.addView(text(R.string.compatibility_body, false))
        val guard = card(page, false)
        guard.addView(text(R.string.guard_title, true))
        guard.addView(text(R.string.guard_body, false))
        val app = card(page, false)
        val version = text(R.string.app_name, true)
        app.addView(version)
        val number = MaterialTextView(this)
        number.setText(BuildConfig.VERSION_NAME)
        app.addView(number)
        app.addView(text(R.string.privacy_body, false))
        val source = button(R.string.github)
        source.setOnClickListener(
            android.view.View.OnClickListener { v: View? ->
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/OneUI-Mods/DarkAppTools"),
                    )
                )
            }
        )
        app.addView(source)
        val licenses = button(R.string.legal_title)
        licenses.setOnClickListener(
            android.view.View.OnClickListener { v: View? ->
                startActivity(Intent(this, LicensesActivity::class.java))
            }
        )
        app.addView(licenses)
    }

    private fun refreshStatus(app: SupportedApp) {
        val section = pages.get(app)
        if (section == null || section.status == null) return
        val state = app.settings(this)!!.getString("state", "waiting")
        val label: Int =
            when (state) {
                "dark" -> R.string.status_dark
                "light" -> R.string.status_light
                "disabled" -> R.string.status_disabled
                "blocked" -> R.string.status_blocked
                "unsupported" -> R.string.status_unsupported
                "error" -> R.string.status_error
                else -> R.string.status_waiting
            }
        section!!.status!!.setText(label)
    }

    override fun onResume() {
        super.onResume()
        for (app in SupportedApp.entries) refreshStatus(app)
    }

    override fun onSaveInstanceState(out: Bundle) {
        tabs!!.save(out!!)
        super.onSaveInstanceState(out!!)
    }

    private fun vertical(): LinearLayout {
        val layout = LinearLayout(this)
        layout.setOrientation(LinearLayout.VERTICAL)
        return layout
    }

    private fun card(parent: LinearLayout, primary: Boolean): LinearLayout {
        val card = MaterialCardView(this)
        card.setRadius(dp(24).toFloat())
        card.setCardElevation(0f)
        card.setStrokeWidth(0)
        card.setCardBackgroundColor(
            MaterialColors.getColor(
                this,
                if (primary) com.google.android.material.R.attr.colorPrimaryContainer
                else com.google.android.material.R.attr.colorSurfaceContainerLow,
                "DarkAppTools",
            )
        )
        val body = vertical()
        body.setPadding(dp(20), dp(16), dp(20), dp(16))
        card.addView(body)
        val size = android.widget.LinearLayout.LayoutParams(-1, -2)
        size.bottomMargin = dp(16)
        parent.addView(card, size)
        return body
    }

    private fun text(label: Int, title: Boolean): MaterialTextView {
        val view = MaterialTextView(this)
        view.setText(label)
        view.setTextAppearance(
            if (title) com.google.android.material.R.style.TextAppearance_Material3_TitleMedium
            else com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
        )
        view.setLineSpacing(dp(3).toFloat(), 1f)
        view.setPadding(0, dp(6), 0, dp(6))
        return view
    }

    private fun button(label: Int): MaterialButton {
        val view =
            MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle,
            )
        view.setText(label)
        view.setMinHeight(dp(48))
        return view
    }

    private fun dp(value: Int): Int {
        return Math.round(value * getResources().getDisplayMetrics().density)
    }
}
