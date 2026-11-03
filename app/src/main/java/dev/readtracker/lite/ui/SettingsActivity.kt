package dev.readtracker.lite.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.readtracker.lite.R
import dev.readtracker.lite.data.ColorRoles
import dev.readtracker.lite.data.SettingsState
import dev.readtracker.lite.databinding.ActivitySettingsBinding

/** Экран настроек с вкладками: Вид / Список / Цифры / Аналитика / Данные (V). */
class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding
    private lateinit var st: SettingsState
    private var tab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        st = Ui.settings(this)
        Ui.applyTheme(this, st.theme)
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        when (intent.getStringExtra(EXTRA_OPEN)) {
            TAB_ANALYTICS -> tab = 3
            else -> tab = st.lastSettingsTab
        }

        b.btnBack.setOnClickListener { finish() }
        buildTabs()
        render()
    }

    private fun buildTabs() {
        val row = b.tabsRow
        row.removeAllViews()
        row.gravity = if (st.centeredHeaders) Gravity.CENTER else Gravity.START
        val names = listOf(
            getString(R.string.tab_view), getString(R.string.tab_list),
            getString(R.string.tab_numbers), getString(R.string.analytics_short),
            getString(R.string.tab_data),
        )
        for ((i, n) in names.withIndex()) {
            val tv = TextView(this).apply {
                text = n; textSize = 12f
                setTextColor(if (tab == i) 0xFF000000.toInt() else st.color(ColorRoles.ACCENT))
                setPadding(Ui.dp(context, 10f), Ui.dp(context, 5f), Ui.dp(context, 10f), Ui.dp(context, 5f))
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = Ui.dp(context, 14f).toFloat()
                    setColor(if (tab == i) st.color(ColorRoles.ACCENT) else Ui.withAlpha(st.color(ColorRoles.ACCENT), 28))
                }
                background = bg
                setOnClickListener { tab = i; st.lastSettingsTab = i; Ui.store(this@SettingsActivity).saveSettings(); buildTabs(); render() }
            }
            val lp = LinearLayout.LayoutParams(-2, -2); lp.marginEnd = Ui.dp(this, 5f); lp.gravity = Gravity.CENTER_VERTICAL
            row.addView(tv, lp)
        }
    }

    private fun render() {
        val box = b.content
        box.removeAllViews()
        when (tab) {
            0 -> renderView(box)
            1 -> renderList(box)
            2 -> renderNumbers(box)
            3 -> Analytics.render(this, box, st) { save() }
            4 -> renderData(box)
        }
    }

    private fun save() {
        Ui.store(this).saveSettings()
        // тема/анимации применяются после перезапуска активностей — мягкий рестарт стека
    }

    // ---------- V-1 Вид ----------

    private fun renderView(box: LinearLayout) {
        section(box, getString(R.string.sec_theme))
        choice(box, getString(R.string.theme_mode), st.theme.name, st.theme.label(this)) {
            Ui.choose(this, getString(R.string.theme_mode), dev.readtracker.lite.data.AppTheme.entries.toList(),
                { it.label(this) }, st.theme) { st.theme = it; save(); recreate() }
        }
        toggle(box, getString(R.string.animations), "", st.animations) { st.animations = it; save() }
        toggle(box, getString(R.string.centered_headers), "", st.centeredHeaders) { st.centeredHeaders = it; save(); buildTabs(); render() }
        toggle(box, getString(R.string.fab_glow), "", st.fabGlow) { st.fabGlow = it; save() }
        toggle(box, getString(R.string.hide_bottom_bar), "", st.hideBottomBar) { st.hideBottomBar = it; save() }

        section(box, getString(R.string.sec_colors))
        colorRow(box, ColorRoles.BG_DARK)
        colorRow(box, ColorRoles.CARD_DARK)
        colorRow(box, ColorRoles.TEXT_ON_DARK)
        colorRow(box, ColorRoles.HINT_ON_DARK)
        colorRow(box, ColorRoles.BG_LIGHT)
        colorRow(box, ColorRoles.CARD_LIGHT)
        colorRow(box, ColorRoles.TEXT_ON_LIGHT)
        colorRow(box, ColorRoles.HINT_ON_LIGHT)
        colorRow(box, ColorRoles.ACCENT)
        colorRow(box, ColorRoles.TAG_SERIES)
        colorRow(box, ColorRoles.TAG_WEB)
        colorRow(box, ColorRoles.TAG_SINGLE)
        colorRow(box, ColorRoles.TAG_ONGOING)
        colorRow(box, ColorRoles.ST_PLANNED)
        colorRow(box, ColorRoles.ST_READING)
        colorRow(box, ColorRoles.ST_PAUSED)
        colorRow(box, ColorRoles.ST_COMPLETED)
        colorRow(box, ColorRoles.ST_DROPPED)
        actionRow(box, getString(R.string.reset_colors)) {
            Ui.confirm(this, getString(R.string.reset_colors), getString(R.string.reset_colors_q), okText = R.string.reset) {
                st.colors.clear(); save(); render()
            }
        }

        section(box, getString(R.string.sec_progress_style))
        choice(box, getString(R.string.progress_color), st.progressColorBy.name, st.progressColorBy.label(this)) {
            Ui.choose(this, getString(R.string.progress_color), dev.readtracker.lite.data.ProgressColorBy.entries.toList(),
                { it.label(this) }, st.progressColorBy) { st.progressColorBy = it; save(); render() }
        }
        toggle(box, getString(R.string.show_percent), "", st.showPercent) { st.showPercent = it; save() }
        toggle(box, getString(R.string.show_totals), "", st.showTotals) { st.showTotals = it; save() }
        toggle(box, getString(R.string.shorten_numbers), "", st.shortenNumbers) { st.shortenNumbers = it; save() }
    }

    private val colorRoleLabels = mapOf(
        ColorRoles.BG_DARK to "Фон · тёмная", ColorRoles.CARD_DARK to "Карточка · тёмная",
        ColorRoles.TEXT_ON_DARK to "Текст · тёмная", ColorRoles.HINT_ON_DARK to "Подсказки · тёмная",
        ColorRoles.BG_LIGHT to "Фон · светлая", ColorRoles.CARD_LIGHT to "Карточка · светлая",
        ColorRoles.TEXT_ON_LIGHT to "Текст · светлая", ColorRoles.HINT_ON_LIGHT to "Подсказки · светлая",
        ColorRoles.ACCENT to "Акцент", ColorRoles.TAG_SERIES to "Теги · Серия",
        ColorRoles.TAG_WEB to "Теги · Веб", ColorRoles.TAG_SINGLE to "Теги · Одиночка",
        ColorRoles.TAG_ONGOING to "Теги · Онгоинг", ColorRoles.ST_PLANNED to "Статус · В планах",
        ColorRoles.ST_READING to "Статус · Читаю", ColorRoles.ST_PAUSED to "Статус · Отложено",
        ColorRoles.ST_COMPLETED to "Статус · Прочитано", ColorRoles.ST_DROPPED to "Статус · Брошено",
    )

    private fun colorRow(box: LinearLayout, role: String) {
        val label = colorRoleLabels[role] ?: role
        val cur = st.colors[role] ?: 0xFF888888.toInt()
        val row = baseRow(box)
        row.addView(text(row, label, 14f, st.color(ColorRoles.TEXT_ON_DARK), weight = true))
        val sw = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(Ui.dp(this@SettingsActivity, 26f), Ui.dp(this@SettingsActivity, 26f))
            background = rounded(cur)
        }
        row.addView(sw)
        val hex = TextView(this).apply {
            text = FmtK.hex(cur); textSize = 12f
            setTextColor(st.color(ColorRoles.HINT_ON_DARK))
            setPadding(Ui.dp(this@SettingsActivity, 8f), 0, 0, 0)
        }
        row.addView(hex)
        row.setOnClickListener { openColorPicker(role, cur) { save(); render() } }
    }

    private fun openColorPicker(role: String, current: Int, onDone: () -> Unit) {
        val grid = GridLayout(this).apply {
            columnCount = 6
            setPadding(Ui.dp(this@SettingsActivity, 16f), Ui.dp(this@SettingsActivity, 8f), Ui.dp(this@SettingsActivity, 16f), 0)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(colorRoleLabels[role] ?: role)
            .setView(grid)
            .setNeutralButton(R.string.reset) { _, _ -> st.colors.remove(role); onDone() }
            .setNegativeButton(R.string.cancel, null)
            .create()
        for (c in PALETTE) {
            val cell = View(this).apply {
                val d = Ui.dp(this@SettingsActivity, 34f)
                layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams(d, d).also {
                    it.setMargins(Ui.dp(context, 3f), Ui.dp(context, 3f), Ui.dp(context, 3f), Ui.dp(context, 3f))
                }
                background = rounded(c)
                setOnClickListener { st.colors[role] = c; dialog.dismiss(); onDone() }
            }
            grid.addView(cell)
        }
        dialog.show()
    }

    private fun rounded(color: Int) = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        cornerRadius = Ui.dp(this@SettingsActivity, 7f).toFloat()
        setColor(color)
        setStroke(Ui.dp(this@SettingsActivity, 1f), Ui.withAlpha(0xFFFFFFFF.toInt(), 60))
    }

    // ---------- V-2 Список ----------

    private fun renderList(box: LinearLayout) {
        section(box, getString(R.string.sec_card_style))
        choice(box, getString(R.string.card_style), st.cardStyle.name, st.cardStyle.label(this)) {
            Ui.choose(this, getString(R.string.card_style), dev.readtracker.lite.data.CardStyle.entries.toList(),
                { it.label(this) }, st.cardStyle) { st.cardStyle = it; save() }
        }
        toggle(box, getString(R.string.card_arrow), "", st.cardArrow) { st.cardArrow = it; save() }
        toggle(box, getString(R.string.title_one_line), "", st.titleOneLine) { st.titleOneLine = it; save() }
        toggle(box, getString(R.string.statusTextInList), "", st.statusTextInList) { st.statusTextInList = it; save() }
        toggle(box, getString(R.string.sort_all_by_status), "", st.sortAllByStatus) { st.sortAllByStatus = it; save() }

        section(box, getString(R.string.sec_search_filter))
        toggle(box, getString(R.string.search_enabled), "", st.searchEnabled) { st.searchEnabled = it; save() }

        section(box, getString(R.string.sec_title_screen))
        toggle(box, getString(R.string.title_screen_enabled), "", st.titleScreenEnabled) { st.titleScreenEnabled = it; save() }
        toggle(box, getString(R.string.title_screen_header), "", st.titleScreenHeader) { st.titleScreenHeader = it; save() }
        toggle(box, getString(R.string.title_screen_progress), "", st.titleScreenProgress) { st.titleScreenProgress = it; save() }

        section(box, getString(R.string.sec_bookmarks))
        toggle(box, getString(R.string.bookmarks_enabled), "", st.bookmarksEnabled) { st.bookmarksEnabled = it; save(); render() }
        if (st.bookmarksEnabled) {
            choice(box, getString(R.string.bookmark_position), st.bookmarkPosition.name, st.bookmarkPosition.label(this)) {
                Ui.choose(this, getString(R.string.bookmark_position), dev.readtracker.lite.data.BookmarkPos.entries.toList(),
                    { it.label(this) }, st.bookmarkPosition) { st.bookmarkPosition = it; save() }
            }
        }

        section(box, getString(R.string.sec_drop_reason))
        toggle(box, getString(R.string.drop_reason_in_cards), "", st.dropReasonInCards) { st.dropReasonInCards = it; save() }

        section(box, getString(R.string.sec_extra_fields))
        toggle(box, getString(R.string.rating_enabled), "", st.ratingEnabled) { st.ratingEnabled = it; save(); render() }
        if (st.ratingEnabled) {
            choice(box, getString(R.string.rating_scale), st.ratingScale.toString(), "${st.ratingScale} ★") {
                Ui.choose(this, getString(R.string.rating_scale), listOf(5, 10), { "$it ★" }, st.ratingScale) {
                    st.ratingScale = it
                    // при уменьшении шкалы обрезаем оценки (V-2)
                    for (t in Ui.store(this).titles) t.rating?.let { r -> if (r > it) t.rating = it }
                    Ui.store(this).saveTitles(); save()
                }
            }
        }
        toggle(box, getString(R.string.total_words_enabled), "", st.totalWordsEnabled) { st.totalWordsEnabled = it; save() }
        toggle(box, getString(R.string.genres_enabled), "", st.genresEnabled) { st.genresEnabled = it; save() }

        section(box, getString(R.string.sec_web_specifics))
        toggle(box, getString(R.string.web_chapters_on_cards), "", st.webChaptersOnCards) { st.webChaptersOnCards = it; save() }
        toggle(box, getString(R.string.chapter_search_in_list), "", st.chapterSearchInList) { st.chapterSearchInList = it; save() }
        toggle(box, getString(R.string.hide_ongoing_tag), "", st.hideOngoingTag) { st.hideOngoingTag = it; save() }

        section(box, getString(R.string.sec_add_flow))
        toggle(box, getString(R.string.simple_add), "", st.simpleAdd) { st.simpleAdd = it; save() }
    }

    // ---------- V-3 Цифры ----------

    private fun renderNumbers(box: LinearLayout) {
        section(box, getString(R.string.sec_volume_display))
        choice(box, getString(R.string.volume_display), st.volumeDisplay.name, st.volumeDisplay.label(this)) {
            Ui.choose(this, getString(R.string.volume_display), dev.readtracker.lite.data.VolumeDisplay.entries.toList(),
                { it.label(this) }, st.volumeDisplay) { st.volumeDisplay = it; save() }
        }
        toggle(box, getString(R.string.volumes_as_range), "", st.volumesAsRange) { st.volumesAsRange = it; save() }
        toggle(box, getString(R.string.volumes_count_only), "", st.volumesCountOnly) { st.volumesCountOnly = it; save() }

        section(box, getString(R.string.sec_chapter_display))
        toggle(box, getString(R.string.chapters_as_range), "", st.chaptersAsRange) { st.chaptersAsRange = it; save() }
        toggle(box, getString(R.string.chapters_count_only), "", st.chaptersCountOnly) { st.chaptersCountOnly = it; save() }

        section(box, getString(R.string.sec_number_format))
        choice(box, getString(R.string.decimal_sep), st.decimalSep.name, st.decimalSep.label(this)) {
            Ui.choose(this, getString(R.string.decimal_sep), dev.readtracker.lite.data.DecimalSep.entries.toList(),
                { it.label(this) }, st.decimalSep) { st.decimalSep = it; save() }
        }
        choice(box, getString(R.string.thousand_sep), st.thousandSep.name, st.thousandSep.label(this)) {
            Ui.choose(this, getString(R.string.thousand_sep), dev.readtracker.lite.data.ThousandSep.entries.toList(),
                { it.label(this) }, st.thousandSep) { st.thousandSep = it; save() }
        }
        toggle(box, getString(R.string.spaces_before_suffix), "", st.spacesBeforeSuffix) { st.spacesBeforeSuffix = it; save() }
        toggle(box, getString(R.string.shorten_numbers), "", st.shortenNumbers) { st.shortenNumbers = it; save() }

        section(box, getString(R.string.sec_preview))
        val sample = dev.readtracker.lite.data.Fmt.smart(1234567L, st.shortenNumbers) + " " +
                getString(R.string.words_suffix) + " · " + dev.readtracker.lite.data.Fmt.num(12.5) + " " +
                getString(R.string.volumes_suffix)
        infoRow(box, sample)
    }

    private fun infoRow(box: LinearLayout, text: String) {
        val row = baseRow(box)
        row.addView(text(row, text, 14f, st.color(ColorRoles.TEXT_ON_DARK), weight = true))
    }

    // ---------- V-5 Данные ----------

    private fun renderData(box: LinearLayout) {
        val store = Ui.store(this)
        section(box, getString(R.string.sec_stats))
        infoRow(box, getString(R.string.stats_titles, store.titles.size))
        infoRow(box, getString(R.string.stats_words, dev.readtracker.lite.data.Fmt.smart(store.totalWordsRead(), st.shortenNumbers)))
        infoRow(box, getString(R.string.storage_used, FmtK.bytes(store.storageUsed())))

        section(box, getString(R.string.sec_backup))
        actionRow(box, getString(R.string.export_json)) { Export.importExport(this, store, export = true) { save(); render() } }
        actionRow(box, getString(R.string.import_json)) { Export.importExport(this, store, export = false) { save(); render() } }

        section(box, getString(R.string.sec_reset))
        actionRow(box, getString(R.string.wipe_settings)) {
            Ui.confirm(this, getString(R.string.wipe_settings), getString(R.string.wipe_settings_q), okText = R.string.reset) {
                Store.resetSettings(this); recreate()
            }
        }
        actionRow(box, getString(R.string.wipe_all)) {
            Ui.confirm(this, getString(R.string.wipe_all), getString(R.string.wipe_all_q), okText = R.string.reset) {
                Store.wipeAll(this); recreate()
            }
        }
    }

    // ---------- виджеты ----------

    private fun section(box: LinearLayout, title: String) {
        val tv = TextView(this).apply {
            text = title.uppercase(); textSize = 12f
            setTextColor(st.color(ColorRoles.ACCENT))
            letterSpacing = 0.05f
            setPadding(0, Ui.dp(this@SettingsActivity, 18f), 0, Ui.dp(this@SettingsActivity, 6f))
        }
        box.addView(tv)
    }

    private fun baseRow(box: LinearLayout): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_field)
            setPadding(Ui.dp(this@SettingsActivity, 12f), Ui.dp(this@SettingsActivity, 10f), Ui.dp(this@SettingsActivity, 12f), Ui.dp(this@SettingsActivity, 10f))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = Ui.dp(this@SettingsActivity, 6f) }
        }
        return row
    }

    private fun text(parent: LinearLayout, s: String, size: Float, color: Int, weight: Boolean): TextView {
        val tv = TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
        }
        val lp = LinearLayout.LayoutParams(if (weight) 0 else -2, -2)
        if (weight) lp.weight = 1f
        parent.addView(tv, lp)
        return tv
    }

    private fun toggle(box: LinearLayout, title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
        val row = baseRow(box)
        row.addView(text(row, title, 14f, st.color(ColorRoles.TEXT_ON_DARK), weight = true))
        val sw = com.google.android.material.switchmaterial.SwitchMaterial(this).apply { isChecked = value }
        sw.setOnCheckedChangeListener { _, c -> onChange(c) }
        row.addView(sw)
        if (sub.isNotEmpty()) { /* подпись не используется в compact-режиме */ }
        box.addView(row)
    }

    private fun choice(box: LinearLayout, title: String, key: String, value: String, onClick: () -> Unit) {
        val row = baseRow(box)
        row.addView(text(row, title, 14f, st.color(ColorRoles.TEXT_ON_DARK), weight = true))
        row.addView(text(row, value, 13f, st.color(ColorRoles.ACCENT), weight = false))
        row.findViewById<View?>(null)
        row.setOnClickListener { onClick() }
        box.addView(row)
    }

    private fun actionRow(box: LinearLayout, title: String, onClick: () -> Unit) {
        val row = baseRow(box)
        val tv = text(row, title, 14f, st.color(ColorRoles.ACCENT), weight = true)
        tv.gravity = Gravity.CENTER
        row.setOnClickListener { onClick() }
        box.addView(row)
    }

    companion object {
        const val EXTRA_OPEN = "open_tab"
        const val TAB_ANALYTICS = "analytics"

        private val PALETTE = listOf(
            // ряды: нейтральные → яркие (как палитра макета)
            0xFFFFFFFF.toInt(), 0xFFBDBDBD.toInt(), 0xFF9E9E9E.toInt(), 0xFF757575.toInt(), 0xFF424242.toInt(), 0xFF1C1C1E.toInt(),
            0xFFFFFDE7.toInt(), 0xFFFDE68A.toInt(), 0xFFFACC15.toInt(), 0xFFCA8A04.toInt(), 0xFF854D0E.toInt(), 0xFF78350F.toInt(),
            0xFFFFE4E6.toInt(), 0xFFFDA4AF.toInt(), 0xFFF87171.toInt(), 0xFFDC2626.toInt(), 0xFF7F1D1D.toInt(), 0xFF831843.toInt(),
            0xFFFFEDD5.toInt(), 0xFFFDBA74.toInt(), 0xFFF97316.toInt(), 0xFFEA580C.toInt(), 0xFF7C2D12.toInt(), 0xFF431407.toInt(),
            0xFFECFCCB.toInt(), 0xFFBEF264.toInt(), 0xFF84CC16.toInt(), 0xFF4D7C0F.toInt(), 0xFF1A2E05.toInt(), 0xFF14532D.toInt(),
            0xFFD1FAE5.toInt(), 0xFF6EE7B7.toInt(), 0xFF10B981.toInt(), 0xFF059669.toInt(), 0xFF064E3B.toInt(), 0xFF022C22.toInt(),
            0xFFCFFAFE.toInt(), 0xFF67E8F9.toInt(), 0xFF06B6D4.toInt(), 0xFF0891B2.toInt(), 0xFF164E63.toInt(), 0xFF083344.toInt(),
            0xFFDBEAFE.toInt(), 0xFF93C5FD.toInt(), 0xFF3B82F6.toInt(), 0xFF2563EB.toInt(), 0xFF1E3A8A.toInt(), 0xFF172554.toInt(),
            0xFFEDE9FE.toInt(), 0xFFC4B5FD.toInt(), 0xFF8B5CF6.toInt(), 0xFF7C3AED.toInt(), 0xFF4C1D95.toInt(), 0xFF2E1065.toInt(),
            0xFFFCE7F3.toInt(), 0xFFF9A8D4.toInt(), 0xFFEC4899.toInt(), 0xFFDB2777.toInt(), 0xFF831843.toInt(), 0xFF500724.toInt(),
            0xFF30336B.toInt(), 0xFF1F2937.toInt(), 0xFF6B7280.toInt(), 0xFF374151.toInt(), 0xFF111827.toInt(), 0xFF000000.toInt(),
        )
    }
}
