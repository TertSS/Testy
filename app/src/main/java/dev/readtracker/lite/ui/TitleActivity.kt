package dev.readtracker.lite.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import dev.readtracker.lite.R
import dev.readtracker.lite.data.ColorRoles
import dev.readtracker.lite.data.FormatType
import dev.readtracker.lite.data.NovelTitle
import dev.readtracker.lite.data.SettingsState
import dev.readtracker.lite.data.TitleStatus
import dev.readtracker.lite.databinding.ActivityTitleBinding

/** Экран тайтла (II-4): просмотр всех данных; кнопки «Редактировать»/«Удалить». */
class TitleActivity : AppCompatActivity() {

    private lateinit var b: ActivityTitleBinding
    private lateinit var st: SettingsState
    private var id: String? = null
    private var descExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        st = Ui.settings(this)
        Ui.applyTheme(this, st.theme)
        super.onCreate(savedInstanceState)
        b = ActivityTitleBinding.inflate(layoutInflater)
        setContentView(b.root)
        id = intent.getStringExtra(Ui.EXTRA_ID)

        b.btnBack.setOnClickListener { finish() }
        b.btnEdit.setOnClickListener { id?.let { Ui.edit(this, it) } }
        b.btnDelete.setOnClickListener {
            val t = Ui.store(this).find(id) ?: return@setOnClickListener
            Ui.confirm(this, getString(R.string.delete_title_q),
                getString(R.string.delete_title_body, t.name.ifBlank { getString(R.string.untitled) })) {
                Ui.store(this).removeTitle(t.id)
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val t = Ui.store(this).find(id)
        if (t == null) { finish(); return }

        b.titleName.text = t.name.ifBlank { getString(R.string.untitled) }
        b.titleAuthor.text = t.author
        b.titleAuthor.visibility = if (t.author.isBlank()) View.GONE else View.VISIBLE

        // обложка
        when {
            t.coverFile.isNotBlank() -> { b.cover.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP; b.cover.load("file://" + t.coverFile); b.coverText.visibility = View.GONE }
            t.coverUrl.isNotBlank() -> { b.cover.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP; b.cover.load(t.coverUrl) { error(t.coverColor) }; b.coverText.visibility = View.GONE }
            else -> { b.cover.setImageDrawable(null); b.cover.setBackgroundColor(t.coverColor); b.coverText.visibility = View.VISIBLE; b.coverText.text = TitleAdapter.initials(t.name) }
        }

        // заголовок+статус и прогресс — по настройкам экрана тайтла
        val headerOn = st.titleScreenHeader
        b.rowStatus.visibility = if (headerOn) View.VISIBLE else View.GONE
        b.rowFormat.visibility = if (headerOn) View.VISIBLE else View.GONE
        setColored(b.rowStatus, getString(R.string.row_status), TitleAdapter.statusLabel(this, t.status), st.statusColor(t.status))
        setColored(b.rowFormat, getString(R.string.row_format), TitleAdapter.formatLabel(this, t.format), st.formatColor(t.format))

        if (st.titleScreenProgress) {
            val p = t.progress(st)
            if (p != null) {
                b.progressBlock.visibility = View.VISIBLE
                b.progressCaption.text = p.caption
                b.progressPill.max = 1000
                b.progressPill.progress = (p.value * 1000).toInt()
                val color = when (p.colorRole) {
                    "accent" -> st.color(ColorRoles.ACCENT)
                    "status" -> st.statusColor(t.status)
                    else -> st.color(ColorRoles.TAG_WEB)
                }
                (b.progressPill.progressDrawable?.mutate() as? android.graphics.drawable.LayerDrawable)
                    ?.findDrawableByLayerId(android.R.id.progress)?.setTint(color)
            } else b.progressBlock.visibility = View.GONE
        } else b.progressBlock.visibility = View.GONE

        // числовые строки
        b.rowWords.visibility = View.VISIBLE
        setPlain(b.rowWords, getString(R.string.row_words), FmtWrap.long(t.effWords(), st) + " " + getString(R.string.words_suffix))
        if (t.hasVolumes()) {
            b.rowVolumes.visibility = View.VISIBLE
            val vc = t.volumesCaption()
            setPlain(b.rowVolumes, getString(R.string.row_volumes),
                if (vc.isNotEmpty()) "$vc ${getString(R.string.volumes_suffix)}" else "—")
        } else b.rowVolumes.visibility = View.GONE
        if (t.format == FormatType.WEB) {
            b.rowChapters.visibility = View.VISIBLE
            val cc = t.chaptersCaption()
            setPlain(b.rowChapters, getString(R.string.row_chapters),
                if (cc.isNotEmpty()) "$cc ${getString(R.string.chapters_suffix)}" else "—")
        } else b.rowChapters.visibility = View.GONE

        if (st.ratingEnabled && t.rating != null) {
            b.rowRating.visibility = View.VISIBLE
            setPlain(b.rowRating, getString(R.string.row_rating), "${t.rating}/${st.ratingScale} ★")
        } else b.rowRating.visibility = View.GONE

        // закладка / причина дропа
        if (st.bookmarksEnabled && t.bookmark.isNotBlank()) {
            b.secBookmark.visibility = View.VISIBLE; b.txtBookmark.visibility = View.VISIBLE
            b.secBookmark.text = getString(R.string.sec_bookmark); b.txtBookmark.text = t.bookmark
        } else { b.secBookmark.visibility = View.GONE; b.txtBookmark.visibility = View.GONE }

        if (t.status == TitleStatus.DROPPED && t.dropReason.isNotBlank()) {
            b.secDrop.visibility = View.VISIBLE; b.txtDrop.visibility = View.VISIBLE
            b.secDrop.text = getString(R.string.sec_drop); b.txtDrop.text = t.dropReason
        } else { b.secDrop.visibility = View.GONE; b.txtDrop.visibility = View.GONE }

        // таблицы томов/глав
        renderRowsTable(b.secVolumes, b.tableVolumes, b.tableSummary,
            getString(R.string.sec_by_volumes), t.volumeRows, t.byVolumeMode && t.format != FormatType.WEB,
            getString(R.string.volume_num), st)
        renderRowsTable(b.secChapters, b.tableChapters, b.tableChaptersSummary,
            getString(R.string.sec_by_chapters), t.chapterRows, t.byChapterMode && t.format == FormatType.WEB,
            getString(R.string.chapter_num), st)

        // жанры
        if (st.genresEnabled && t.genres.isNotEmpty()) {
            b.secGenres.visibility = View.VISIBLE
            b.secGenres.text = getString(R.string.sec_genres)
            b.genresFlow.removeAllViews()
            for (g in t.genres) {
                val tv = TextView(this).apply {
                    text = g; textSize = 12f
                    setTextColor(st.color(ColorRoles.ACCENT))
                    setBackgroundResource(R.drawable.bg_tag)
                    val p = Ui.dp(context, 6f)
                    setPadding(p, Ui.dp(context, 2f), p, Ui.dp(context, 2f))
                }
                val lp = LinearLayout.LayoutParams(-2, -2); lp.marginEnd = Ui.dp(this, 4f); lp.bottomMargin = Ui.dp(this, 4f)
                b.genresFlow.addView(tv, lp)
            }
        } else b.secGenres.visibility = View.GONE

        // описание с «показать все»
        val d = t.description
        if (d.isBlank()) { b.secDesc.visibility = View.GONE; b.txtDesc.visibility = View.GONE; b.btnShowAll.visibility = View.GONE }
        else {
            b.secDesc.visibility = View.VISIBLE; b.txtDesc.visibility = View.VISIBLE
            b.secDesc.text = getString(R.string.sec_description)
            val long = d.length > 280
            b.txtDesc.text = if (!descExpanded && long) d.take(280) + "…" else d
            b.btnShowAll.visibility = if (long) View.VISIBLE else View.GONE
            b.btnShowAll.text = if (descExpanded) getString(R.string.collapse)
                                else getString(R.string.show_all, countLines(d))
            b.btnShowAll.setOnClickListener { descExpanded = !descExpanded; render() }
        }
    }

    private fun countLines(d: String): Int = d.count { it == '\n' } + 1

    private fun setColored(row: LinearLayout, label: String, value: String, color: Int) {
        row.removeAllViews()
        row.addView(labelView(row, label))
        val v = TextView(this).apply {
            text = value; textSize = 13f; setTextColor(color)
        }
        row.addView(v, LinearLayout.LayoutParams(-2, -2))
    }

    private fun setPlain(row: LinearLayout, label: String, value: String) {
        row.removeAllViews()
        row.addView(labelView(row, label))
        val v = TextView(this).apply {
            text = value; textSize = 13f; setTextColor(Ui.themeColor(this@TitleActivity, R.attr.rtlText))
        }
        row.addView(v, LinearLayout.LayoutParams(-2, -2))
    }

    private fun labelView(parent: View, label: String): TextView {
        val w = (200 * parent.resources.displayMetrics.density).toInt()
        return TextView(this).apply {
            text = label; textSize = 13f
            setTextColor(Ui.themeColor(this@TitleActivity, R.attr.rtlHint))
            layoutParams = LinearLayout.LayoutParams(w, -2)
        }
    }

    /** Секция детальной таблицы (том/глава → слов), суммарная строка снизу. */
    private fun renderRowsTable(sec: TextView, table: LinearLayout, summary: TextView,
                                title: String, rows: List<dev.readtracker.lite.data.NumRow>,
                                on: Boolean, numLabel: String, st: SettingsState) {
        if (!on || rows.isEmpty()) { sec.visibility = View.GONE; table.visibility = View.GONE; summary.visibility = View.GONE; return }
        sec.visibility = View.VISIBLE; table.visibility = View.VISIBLE
        sec.text = title
        table.removeAllViews()
        for ((i, r) in rows.withIndex()) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val num = TextView(this).apply {
                text = "$numLabel ${dev.readtracker.lite.data.Fmt.num(r.num)}"
                textSize = 13f; setTextColor(Ui.themeColor(this@TitleActivity, R.attr.rtlText))
                layoutParams = LinearLayout.LayoutParams((150 * resources.displayMetrics.density).toInt(), -2)
            }
            val words = TextView(this).apply {
                text = FmtWrap.long(r.words, st) + " " + getString(R.string.words_suffix)
                textSize = 13f; setTextColor(Ui.themeColor(this@TitleActivity, R.attr.rtlHint))
            }
            line.addView(num); line.addView(words)
            if (i % 2 == 0) line.setBackgroundColor(Ui.withAlpha(Ui.themeColor(this, R.attr.rtlCard), 120))
            table.addView(line)
        }
        val total = rows.sumOf { it.words }
        val avg = if (rows.isNotEmpty()) total / rows.size else 0
        summary.visibility = View.VISIBLE
        summary.text = getString(R.string.total_rows_summary,
            FmtWrap.long(total, st), FmtWrap.long(avg, st),
            getString(if (title == getString(R.string.sec_by_volumes)) R.string.per_volume_word else R.string.per_chapter_word))
    }
}
