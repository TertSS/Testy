package dev.readtracker.lite.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.readtracker.lite.R
import dev.readtracker.lite.data.ColorRoles
import dev.readtracker.lite.data.FormatType
import dev.readtracker.lite.data.NovelTitle
import dev.readtracker.lite.data.NumRow
import dev.readtracker.lite.data.SettingsState
import dev.readtracker.lite.data.TitleStatus
import dev.readtracker.lite.databinding.ActivityEditBinding

/**
 * Форма добавления/редактирования (II-3). Шаги: Тайтл → Статус → Данные.
 * Упрощённое добавление (simpleAdd) показывает только первый шаг + сохранение.
 */
class EditActivity : AppCompatActivity() {

    private lateinit var b: ActivityEditBinding
    private lateinit var st: SettingsState
    private lateinit var t: NovelTitle          // рабочая копия
    private var isNew = false
    private var step = 0

    private val pickCover = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val path = Ui.persistCover(this, uri)
            if (path != null) {
                t.coverFile = path; t.coverUrl = ""   // выбор файла стирает url
                b.coverUrlInput.setText("")
                showCover()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        st = Ui.settings(this)
        Ui.applyTheme(this, st.theme)
        super.onCreate(savedInstanceState)
        b = ActivityEditBinding.inflate(layoutInflater)
        setContentView(b.root)

        val id = intent.getStringExtra(Ui.EXTRA_ID)
        val src = Ui.store(this).find(id)
        if (src != null) {
            t = src.copy(
                genres = src.genres.toMutableList(),
                volumeRows = src.volumeRows.map { NumRow(it.num, it.words) }.toMutableList(),
                chapterRows = src.chapterRows.map { NumRow(it.num, it.words) }.toMutableList(),
            )
            isNew = false
        } else {
            t = NovelTitle(id = System.currentTimeMillis().toString())
            isNew = true
        }

        b.btnBack.setOnClickListener { onStepBack() }
        b.btnSaveTop.setOnClickListener { saveAndFinish() }
        b.btnNext.setOnClickListener { goNext() }
        b.btnDelete.setOnClickListener { confirmDelete() }

        setupBasics()
        setupStatus()
        setupData()
        applySimpleAdd()
        showStep(0)
    }

    private fun applySimpleAdd() {
        b.stepTabs.visibility = if (st.simpleAdd && isNew) View.GONE else View.VISIBLE
        b.btnNext.visibility = if (st.simpleAdd && isNew) View.GONE else View.VISIBLE
    }

    // ---------- Шаг 1: тайтл ----------

    private fun setupBasics() {
        b.nameInput.setText(t.name)
        b.authorInput.setText(t.author)
        b.descInput.setText(t.description)
        b.coverUrlInput.setText(t.coverUrl)
        b.coverUrlInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, i: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, i: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val v = s?.toString().orEmpty()
                if (v != t.coverUrl) {           // ввод url стирает локальный файл
                    t.coverUrl = v
                    if (v.isNotBlank() && t.coverFile.isNotBlank()) { t.coverFile = ""; showCover() }
                }
            }
        })
        b.btnPickCover.setOnClickListener { pickCover.launch("image/*") }
        b.btnClearCover.setOnClickListener {
            t.coverUrl = ""; t.coverFile = ""
            b.coverUrlInput.setText(""); showCover()
        }
        b.btnCoverColor.setOnClickListener {
            Ui.choose(this, getString(R.string.pick_color), COVER_SWATCHES, { "●" }, t.coverColor) {
                t.coverColor = it; showCover()
            }
        }
        showCover()
    }

    private fun showCover() {
        when {
            t.coverFile.isNotBlank() -> { b.coverPrev.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP; b.coverPrev.load("file://" + t.coverFile); b.coverPrevText.visibility = View.GONE }
            t.coverUrl.isNotBlank() -> { b.coverPrev.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP; b.coverPrev.load(t.coverUrl) { error(t.coverColor) }; b.coverPrevText.visibility = View.GONE }
            else -> { b.coverPrev.setImageDrawable(null); b.coverPrev.setBackgroundColor(t.coverColor); b.coverPrevText.visibility = View.VISIBLE; b.coverPrevText.text = TitleAdapter.initials(b.nameInput.text.toString().ifBlank { t.name }.ifBlank { "?" }) }
        }
    }

    // ---------- Шаг 2: статус ----------

    private fun setupStatus() {
        val labels = TitleStatus.entries.map { TitleAdapter.statusLabel(this, it) }
        b.statusSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        b.statusSpinner.setSelection(t.status.ordinal)
        b.statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                t.status = TitleStatus.entries[pos]
                b.dropBox.visibility = if (t.status == TitleStatus.DROPPED) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        val flabels = FormatType.entries.map { formatFullLabel(this, it) }
        b.formatSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, flabels)
        b.formatSpinner.setSelection(t.format.ordinal)
        b.formatSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val nf = FormatType.entries[pos]
                if (nf == t.format) return
                if (hasProgressData()) {
                    // откат выбора до подтверждения
                    b.formatSpinner.setSelection(t.format.ordinal)
                    MaterialAlertDialogBuilder(this@EditActivity)
                        .setTitle(R.string.change_format_q)
                        .setMessage(R.string.change_format_body)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.change) { _, _ -> applyFormat(nf) }
                        .show()
                } else applyFormat(nf)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        b.dropReasonInput.setText(t.dropReason)
        b.dropBox.visibility = if (t.status == TitleStatus.DROPPED) View.VISIBLE else View.GONE

        b.bookmarkInput.setText(t.bookmark)
        b.bookmarkBox.visibility = if (st.bookmarksEnabled) View.VISIBLE else View.GONE

        ratingSetup()
    }

    private fun formatFullLabel(ctx: android.content.Context, f: FormatType): String = ctx.getString(
        when (f) { FormatType.SERIES -> R.string.format_series; FormatType.WEB -> R.string.format_web; FormatType.SINGLE -> R.string.format_single }
    )

    private fun hasProgressData(): Boolean =
        t.words > 0 || t.volumes > 0 || t.chaptersRead > 0 || t.volumeRows.isNotEmpty() || t.chapterRows.isNotEmpty()

    private fun applyFormat(f: FormatType) {
        val old = t.format
        t.format = f
        // при смене формата очищаем неприменимые данные
        if (old != f) {
            if (f == FormatType.WEB) { t.volumes = 0; t.totalVolumesInSeries = null; t.byVolumeMode = false; t.volumeRows.clear(); t.countVolumes = false }
            else { t.chaptersRead = 0; t.totalChapters = null; t.byChapterMode = false; t.chapterRows.clear(); t.countVolumes = true }
            t.words = 0L
        }
        b.formatSpinner.setSelection(f.ordinal)
        setupData()
    }

    private fun ratingSetup() {
        b.ratingBox.visibility = if (st.ratingEnabled) View.VISIBLE else View.GONE
        if (!st.ratingEnabled) return
        rebuildRatingStars()
        b.btnRatingReset.setOnClickListener { t.rating = null; rebuildRatingStars() }
    }

    private fun rebuildRatingStars() {
        val row = b.ratingStars
        row.removeAllViews()
        for (i in 1..st.ratingScale) {
            val star = TextView(this).apply {
                text = if (t.rating != null && i <= t.rating!!) "★" else "☆"
                textSize = 26f
                setTextColor(st.color(ColorRoles.ACCENT))
                setPadding(Ui.dp(context, 2f), 0, Ui.dp(context, 2f), 0)
                setOnClickListener { t.rating = if (t.rating == i) null else i; rebuildRatingStars() }
            }
            row.addView(star)
        }
        b.ratingValue.text = t.rating?.toString() ?: "—"
    }

    // ---------- Шаг 3: данные ----------

    private fun setupData() {
        // сброс слушателей перестройкой — проще: читаем в момент showStep/save
        b.ongoingSwitch.setOnCheckedChangeListener(null)
        b.countVolumesSwitch.setOnCheckedChangeListener(null)
        b.calcSwitch.setOnCheckedChangeListener(null)

        val web = t.format == FormatType.WEB

        b.volumesBox.visibility = if (!web) View.VISIBLE else View.GONE
        b.chaptersBox.visibility = if (web) View.VISIBLE else View.GONE
        b.totalWordsBox.visibility = if (st.totalWordsEnabled) View.VISIBLE else View.GONE

        // «считать тома» недоступно у Одиночки и Веба
        b.countVolumesSwitch.isEnabled = t.format == FormatType.SERIES
        b.countVolumesSwitch.isChecked = t.countVolumes
        b.countVolumesSwitch.setOnCheckedChangeListener { _, c -> t.countVolumes = c; syncCalcVisibility() }

        b.ongoingSwitch.isChecked = t.ongoing
        b.ongoingSwitch.setOnCheckedChangeListener { _, c ->
            t.ongoing = c
            b.ongoingCaption.text = getString(if (c) R.string.ongoing_caption_5q else R.string.ongoing_caption_known)
            b.totalVolumesInput.isEnabled = !c
            syncCalcVisibility()
        }
        b.ongoingCaption.text = getString(if (t.ongoing) R.string.ongoing_caption_5q else R.string.ongoing_caption_known)

        b.totalVolumesInput.setText(t.totalVolumesInSeries?.toString() ?: "")
        b.totalVolumesInput.isEnabled = !t.ongoing
        b.totalChaptersInput.setText(t.totalChapters?.toString() ?: "")
        b.totalWordsInput.setText(t.totalWordsInTitle?.toString() ?: "")
        b.wordsInput.setText(if (t.words > 0) t.words.toString() else "")
        b.volumesInput.setText(if (t.volumes > 0) t.volumes.toString() else "")
        b.chaptersReadInput.setText(if (t.chaptersRead > 0) t.chaptersRead.toString() else "")

        // расчёт по томам/главам
        b.calcSwitch.text = getString(if (web) R.string.by_chapters_switch else R.string.by_volumes_switch)
        b.calcSwitch.isChecked = if (web) t.byChapterMode else t.byVolumeMode
        b.calcSwitch.setOnCheckedChangeListener { _, c ->
            if (web) t.byChapterMode = c else t.byVolumeMode = c
            syncCalcVisibility()
        }
        b.rowsBox.visibility = if (b.calcSwitch.isChecked) View.VISIBLE else View.GONE
        b.summaryBox.visibility = if (b.calcSwitch.isChecked) View.GONE else View.VISIBLE
        b.btnAddRow.text = getString(if (web) R.string.add_chapter else R.string.add_volume)
        b.btnAddRow.setOnClickListener { addRow(web) }
        b.btnOpenChapters.visibility = if (web) View.VISIBLE else View.GONE
        b.btnOpenChapters.setOnClickListener { openChaptersScreen() }
        rebuildRowsTable(web)
        syncCalcVisibility()

        // жанры
        b.genresBox.visibility = if (st.genresEnabled) View.VISIBLE else View.GONE
        rebuildGenreChips()
        b.genreAddBtn.setOnClickListener { commitGenreInput() }
        b.genreInput.setOnEditorActionListener { _, _, _ -> commitGenreInput(); true }
    }

    private fun syncCalcVisibility() {
        val web = t.format == FormatType.WEB
        val detail = if (web) t.byChapterMode else t.byVolumeMode
        b.rowsBox.visibility = if (detail) View.VISIBLE else View.GONE
        b.summaryBox.visibility = if (detail) View.GONE else View.VISIBLE
        b.calcSwitch.isEnabled = if (web) true else t.format != FormatType.SINGLE
        if (!web && t.format == FormatType.SINGLE) { b.calcSwitch.isChecked = false; t.byVolumeMode = false }
    }

    private fun rows(): MutableList<NumRow> =
        if (t.format == FormatType.WEB) t.chapterRows else t.volumeRows

    private fun addRow(web: Boolean) {
        val next = (rows().maxOfOrNull { it.num } ?: 0.0) + 1.0
        rows().add(NumRow(next, 0))
        rebuildRowsTable(web)
    }

    /** Редактирование таблицы прямо в форме: номер + слова, кнопка удаления строки. */
    private fun rebuildRowsTable(web: Boolean) {
        val box = b.rowsTable
        box.removeAllViews()
        val list = rows()
        for ((idx, r) in list.withIndex()) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
            val num = androidx.appcompat.widget.AppCompatEditText(this).apply {
                setText(dev.readtracker.lite.data.Fmt.num(r.num))
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                textSize = 13f; maxLines = 1
                layoutParams = LinearLayout.LayoutParams(Ui.dp(this@EditActivity, 64f), -2)
                addTextChangedListener(simpleWatcher {
                    val v = text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (v > 0 && list.any { x -> x !== r && x.num == v }) {
                        MaterialAlertDialogBuilder(this@EditActivity)
                            .setMessage(getString(R.string.exists_error, dev.readtracker.lite.data.Fmt.num(v)))
                            .setPositiveButton(R.string.ok, null).show()
                        setText(dev.readtracker.lite.data.Fmt.num(r.num)); setSelection(text.length)
                    } else r.num = v
                })
            }
            val words = androidx.appcompat.widget.AppCompatEditText(this).apply {
                setText(if (r.words > 0) r.words.toString() else "")
                hint = getString(R.string.words_label)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                textSize = 13f; maxLines = 1
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                addTextChangedListener(simpleWatcher { r.words = text.toString().toLongOrNull() ?: 0L })
            }
            val del = TextView(this).apply {
                text = "✕"; textSize = 18f
                setTextColor(Ui.themeColor(this@EditActivity, R.attr.rtlHint))
                setPadding(Ui.dp(context, 10f), Ui.dp(context, 6f), Ui.dp(context, 10f), Ui.dp(context, 6f))
                setOnClickListener { list.removeAt(box.indexOfChild(line)); rebuildRowsTable(web) }
            }
            line.addView(num); line.addView(words); line.addView(del)
            box.addView(line)
        }
        updateRowsSummary(web)
    }

    private fun updateRowsSummary(web: Boolean) {
        val list = rows()
        b.rowsSummary.text = if (list.isEmpty()) "" else {
            val total = list.sumOf { it.words }
            val avg = total / list.size
            getString(R.string.total_rows_summary, FmtWrap.long(total, st), FmtWrap.long(avg, st),
                getString(if (web) R.string.per_chapter_word else R.string.per_volume_word))
        }
    }

    private fun simpleWatcher(after: () -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, i: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, c: Int, i: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) { after(); updateRowsSummary(t.format == FormatType.WEB) }
    }

    private fun openChaptersScreen() {
        if (isNew) { Ui.alert(this, getString(R.string.chapters_screen), getString(R.string.err_save_first)); return }
        // таблица глав живёт в ChaptersActivity поверх рабочей копии: сохраняем текущее состояние
        stashToDraft()
        val i = Intent(this, ChaptersActivity::class.java).putExtra(Ui.EXTRA_ID, t.id)
        startActivityForResult(i, REQ_CHAPTERS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CHAPTERS && resultCode == Activity.RESULT_OK) {
            val saved = Ui.store(this).find(t.id)
            if (saved != null) {
                t.chapterRows = saved.chapterRows.map { NumRow(it.num, it.words) }.toMutableList()
                t.byChapterMode = saved.byChapterMode
                t.words = saved.words
                rebuildRowsTable(true)
            }
        }
    }

    /** Временная запись для экранов таблиц (только для уже сохранённых тайтлов). */
    private fun stashToDraft() {
        if (isNew) return
        val store = Ui.store(this)
        val i = store.titles.indexOfFirst { it.id == t.id }
        if (i >= 0) store.titles[i] = t
        store.saveTitles()
    }

    private fun commitGenreInput() {
        val raw = b.genreInput.text.toString()
        for (part in raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }) {
            if (t.genres.none { it.equals(part, true) }) t.genres.add(part)
        }
        b.genreInput.setText("")
        rebuildGenreChips()
    }

    private fun rebuildGenreChips() {
        val flow = b.genreChips
        flow.removeAllViews()
        for (g in t.genres.toList()) {
            val tv = TextView(this).apply {
                text = "$g ✕"; textSize = 12f
                setTextColor(st.color(ColorRoles.ACCENT))
                setBackgroundResource(R.drawable.bg_tag)
                val p = Ui.dp(context, 7f)
                setPadding(p, Ui.dp(context, 3f), p, Ui.dp(context, 3f))
                setOnClickListener { t.genres.remove(g); rebuildGenreChips() }
            }
            val lp = LinearLayout.LayoutParams(-2, -2)
            lp.marginEnd = Ui.dp(this, 5f); lp.bottomMargin = Ui.dp(this, 5f)
            flow.addView(tv, lp)
        }
    }

    // ---------- Навигация по шагам ----------

    private fun showStep(i: Int) {
        step = i.coerceIn(0, 2)
        b.pageBasics.visibility = if (step == 0) View.VISIBLE else View.GONE
        b.pageStatus.visibility = if (step == 1) View.VISIBLE else View.GONE
        b.pageData.visibility = if (step == 2) View.VISIBLE else View.GONE
        b.tab0.setTextColor(tabColor(0)); b.tab1.setTextColor(tabColor(1)); b.tab2.setTextColor(tabColor(2))
        b.tab0.setBackgroundColor(if (step == 0) st.color(ColorRoles.ACCENT) else 0)
        b.tab1.setBackgroundColor(if (step == 1) st.color(ColorRoles.ACCENT) else 0)
        b.tab2.setBackgroundColor(if (step == 2) st.color(ColorRoles.ACCENT) else 0)
        b.btnNext.visibility = if (step == 2 || (st.simpleAdd && isNew)) View.GONE else View.VISIBLE
        b.btnDelete.visibility = if (isNew) View.GONE else View.VISIBLE
    }

    private fun tabColor(i: Int) =
        if (step == i) 0xFF000000.toInt() else Ui.themeColor(this, R.attr.rtlHint)

    private fun collectBasics() {
        t.name = b.nameInput.text.toString().trim()
        t.author = b.authorInput.text.toString().trim()
        t.description = b.descInput.text.toString()
    }

    private fun collectStatus() {
        t.dropReason = b.dropReasonInput.text.toString().trim()
        t.bookmark = b.bookmarkInput.text.toString().trim()
    }

    private fun collectData() {
        val web = t.format == FormatType.WEB
        if (!web) {
            t.volumes = b.volumesInput.text.toString().toLongOrNull()?.toInt() ?: 0
        } else {
            t.chaptersRead = b.chaptersReadInput.text.toString().toLongOrNull()?.toInt() ?: 0
        }
        if (!(if (web) t.byChapterMode else t.byVolumeMode))
            t.words = b.wordsInput.text.toString().toLongOrNull() ?: 0L
        t.totalVolumesInSeries = b.totalVolumesInput.text.toString().toLongOrNull()?.takeIf { it > 0 }?.toInt()
        t.totalChapters = b.totalChaptersInput.text.toString().toLongOrNull()?.takeIf { it > 0 }?.toInt()
        t.totalWordsInTitle = b.totalWordsInput.text.toString().toLongOrNull()?.takeIf { it > 0 }
        commitGenreInput()
    }

    private fun goNext() {
        when (step) {
            0 -> { collectBasics(); if (t.name.isBlank()) { Ui.alert(this, getString(R.string.field_name), getString(R.string.err_no_name)); return }; showStep(1) }
            1 -> { collectStatus(); showStep(2) }
            else -> saveAndFinish()
        }
    }

    private fun onStepBack() {
        if (step > 0) {
            when (step) { 1 -> collectStatus(); 2 -> collectData() }
            showStep(step - 1)
        } else finish()
    }

    /** Проверки противоречий (II-3): прочитано больше, чем «всего». */
    private fun validate(): String? {
        val tc = t.totalChapters
        if (t.format == FormatType.WEB && tc != null && tc > 0) {
            val read = if (t.byChapterMode) t.chapterRows.size else t.chaptersRead
            if (read > tc) return getString(R.string.err_chapters_more)
        }
        val tw = t.totalWordsInTitle
        if (st.totalWordsEnabled && tw != null && tw > 0 && t.effWords() > tw)
            return getString(R.string.err_words_more)
        val tv = t.totalVolumesInSeries
        if (!t.ongoing && t.hasVolumes() && tv != null && tv > 0 && t.effVolumes() > tv)
            return getString(R.string.err_volumes_more)
        return null
    }

    private fun saveAndFinish() {
        when (step) {
            0 -> collectBasics()
            1 -> { collectBasics(); collectStatus() }
            else -> { collectBasics(); collectStatus(); collectData() }
        }
        val err = validate()
        if (err != null) { Ui.alert(this, getString(R.string.save), err); return }
        if (t.name.isBlank()) t.name = getString(R.string.untitled)
        val store = Ui.store(this)
        if (isNew) store.addTitle(t) else store.updateTitle(t)
        setResult(RESULT_OK)
        finish()
    }

    private fun confirmDelete() {
        Ui.confirm(this, getString(R.string.delete_title_q),
            getString(R.string.delete_title_body, t.name.ifBlank { getString(R.string.untitled) })) {
            Ui.store(this).removeTitle(t.id)
            finish()
        }
    }

    companion object {
        const val REQ_CHAPTERS = 41
        private val COVER_SWATCHES = listOf(
            0xFF30336B.toInt(), 0xFF7C3AED.toInt(), 0xFF2563EB.toInt(), 0xFF0891B2.toInt(),
            0xFF059669.toInt(), 0xFF65A30D.toInt(), 0xFFCA8A04.toInt(), 0xFFEA580C.toInt(),
            0xFFDC2626.toInt(), 0xFFDB2777.toInt(), 0xFF6B7280.toInt(), 0xFF1F2937.toInt(),
        )
    }
}
