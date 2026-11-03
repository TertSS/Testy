package dev.readtracker.lite.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import dev.readtracker.lite.R
import dev.readtracker.lite.data.FormatType
import dev.readtracker.lite.data.NovelTitle
import dev.readtracker.lite.data.SettingsState
import dev.readtracker.lite.data.TitleStatus
import dev.readtracker.lite.databinding.ActivityMainBinding

/** Главный экран: вкладки статусов, поиск/фильтр, список карточек, FAB «+», аналитика. */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var st: SettingsState
    private lateinit var adapter: TitleAdapter

    private var statusTab = "all"                 // all | planned | ... | dropped
    private var formatFilter: Set<FormatType> = emptySet() // пусто = без фильтра
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        st = Ui.settings(this)
        Ui.applyTheme(this, st.theme)
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        statusTab = st.lastStatusTab

        adapter = TitleAdapter(st, onClick = { Ui.openCard(this, it.id) }, onLongClick = { showCardMenu(it) })
        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter
        b.list.itemAnimator = null

        b.btnSettings.setOnClickListener { Ui.settings(this) }
        b.btnAnalytics.setOnClickListener { openAnalytics() }
        b.navAnalytics.setOnClickListener { openAnalytics() }
        b.fab.setOnClickListener { Ui.edit(this, null) }

        b.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, i: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, i: Int) {}
            override fun afterTextChanged(s: Editable?) { query = s?.toString().orEmpty(); refresh() }
        })

        buildTabs()
        applyChrome()
    }

    override fun onResume() {
        super.onResume()
        st = Ui.settings(this)
        applyChrome()
        refresh()
    }

    /** Кнопка «+» со свечением (настройка fabGlow). */
    private fun applyChrome() {
        b.bottomBar.visibility = if (st.hideBottomBar) View.GONE else View.VISIBLE
        b.searchRow.visibility = if (st.searchEnabled) View.VISIBLE else View.GONE
        if (st.fabGlow) {
            b.fab.elevation = 14f
            b.fab.outlineColor = st.color(dev.readtracker.lite.data.ColorRoles.ACCENT)
        } else {
            b.fab.elevation = 6f
        }
        buildTabs()
    }

    private fun openAnalytics() {
        val i = Intent(this, SettingsActivity::class.java)
        i.putExtra(SettingsActivity.EXTRA_OPEN, SettingsActivity.TAB_ANALYTICS)
        startActivity(i)
    }

    // ---------- Вкладки и фильтр формата ----------

    private fun buildTabs() {
        val row = b.tabsRow
        row.removeAllViews()
        row.gravity = if (st.centeredHeaders) Gravity.CENTER else Gravity.START
        val tabs = listOf("all" to getString(R.string.tab_all)) +
                TitleStatus.entries.map { it.key to TitleAdapter.statusLabel(this, it) }
        for ((key, label) in tabs) addTabChip(row, key, label, selected = statusTab == key)
        // кнопка фильтра формата
        val fmtTv = TextView(this).apply {
            text = if (formatFilter.isEmpty()) "⚙ " + getString(R.string.filter_format_title)
            else "⚙ " + formatFilter.joinToString(", ") { TitleAdapter.formatLabel(this@MainActivity, it) }
            textSize = 12f
            setPadding(Ui.dp(context, 10f), Ui.dp(context, 5f), Ui.dp(context, 10f), Ui.dp(context, 5f))
            setBackgroundResource(R.drawable.bg_tag)
            setTextColor(if (formatFilter.isEmpty()) st.colors[dev.readtracker.lite.data.ColorRoles.ST_PLANNED]!!
                         else st.color(dev.readtracker.lite.data.ColorRoles.ACCENT))
            setOnClickListener { showFormatFilter() }
        }
        val lp = android.widget.LinearLayout.LayoutParams(-2, -2)
        lp.marginStart = Ui.dp(this, 4f); lp.gravity = Gravity.CENTER_VERTICAL
        row.addView(fmtTv, lp)
    }

    private fun addTabChip(row: android.widget.LinearLayout, key: String, label: String, selected: Boolean) {
        val tv = TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(if (selected) Color.WHITE else st.colors[dev.readtracker.lite.data.ColorRoles.ST_PLANNED]!!.let { Ui.withAlpha(it, 200) })
            setPadding(Ui.dp(context, 12f), Ui.dp(context, 6f), Ui.dp(context, 12f), Ui.dp(context, 6f))
            val bg = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = Ui.dp(context, 16f).toFloat()
                setColor(if (selected) statusTabColor(key) else Ui.withAlpha(
                    Ui.themeColor(context, R.attr.rtlCard), 200))
            }
            background = bg
            setOnClickListener {
                statusTab = key; st.lastStatusTab = key; Ui.store(this@MainActivity).saveSettings()
                buildTabs(); refresh()
            }
        }
        val lp = android.widget.LinearLayout.LayoutParams(-2, -2)
        lp.marginEnd = Ui.dp(this, 4f); lp.gravity = Gravity.CENTER_VERTICAL
        row.addView(tv, lp)
    }

    private fun statusTabColor(key: String): Int = when (key) {
        "all" -> st.color(dev.readtracker.lite.data.ColorRoles.ACCENT)
        else -> st.statusColor(TitleStatus.fromKey(key))
    }

    private fun showFormatFilter() {
        val names = arrayOf(getString(R.string.filter_series), getString(R.string.filter_web), getString(R.string.filter_singles))
        val vals = listOf(FormatType.SERIES, FormatType.WEB, FormatType.SINGLE)
        val checked = BooleanArray(3) { formatFilter.contains(vals[it]) }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.filter_format_title)
            .setMultiChoiceItems(names, checked) { _, i, isChecked -> checked[i] = isChecked }
            .setNeutralButton(R.string.reset) { d, _ -> formatFilter = emptySet(); buildTabs(); refresh(); d.dismiss() }
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.apply) { _, _ ->
                formatFilter = vals.filterIndexed { i, _ -> checked[i] }.toSet()
                buildTabs(); refresh()
            }
            .show()
    }

    // ---------- Список ----------

    private fun filtered(): List<NovelTitle> {
        var list = Ui.store(this).titles.toList()
        if (statusTab != "all") list = list.filter { it.status.key == statusTab }
        if (formatFilter.isNotEmpty()) list = list.filter { formatFilter.contains(it.format) }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { it.name.lowercase().contains(q) || it.author.lowercase().contains(q) }
        }
        if (statusTab == "all" && st.sortAllByStatus) {
            // новый порядок сохраняется, внутри — по статусу
            list = list.sortedBy { TitleStatus.entries.indexOf(it.status) }
        }
        return list
    }

    private fun refresh() {
        val list = filtered()
        adapter.submit(list)
        val empty = list.isEmpty()
        b.emptyView.visibility = if (empty) View.VISIBLE else View.GONE
        if (empty) b.emptyView.text =
            if (Ui.store(this).titles.isEmpty()) getString(R.string.empty_library)
            else getString(R.string.empty_filtered)
    }

    private fun showCardMenu(t: NovelTitle) {
        val items = arrayOf(getString(R.string.edit), getString(R.string.delete))
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(t.name.ifBlank { getString(R.string.untitled) })
            .setItems(items) { d, i ->
                d.dismiss()
                when (i) {
                    0 -> Ui.edit(this, t.id)
                    1 -> confirmDelete(t)
                }
            }
            .show()
    }

    private fun confirmDelete(t: NovelTitle) {
        Ui.confirm(this, getString(R.string.delete_title_q),
            getString(R.string.delete_title_body, t.name.ifBlank { getString(R.string.untitled) })) {
            Ui.store(this).removeTitle(t.id)
            refresh()
        }
    }
}
