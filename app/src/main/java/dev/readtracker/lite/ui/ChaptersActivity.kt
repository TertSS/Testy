package dev.readtracker.lite.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.readtracker.lite.R
import dev.readtracker.lite.data.ColorRoles
import dev.readtracker.lite.data.NovelTitle
import dev.readtracker.lite.data.NumRow
import dev.readtracker.lite.data.SettingsState
import dev.readtracker.lite.databinding.ActivityChaptersBinding

/** Экран «Главы» (II-3): таблица глав веб-новеллы с автозаполнением и массовыми операциями. */
class ChaptersActivity : AppCompatActivity() {

    private lateinit var b: ActivityChaptersBinding
    private lateinit var st: SettingsState
    private var titleId: String? = null
    private var t: NovelTitle? = null
    private val rows get() = t?.chapterRows ?: mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        st = Ui.settings(this)
        Ui.applyTheme(this, st.theme)
        super.onCreate(savedInstanceState)
        b = ActivityChaptersBinding.inflate(layoutInflater)
        setContentView(b.root)

        titleId = intent.getStringExtra(Ui.EXTRA_ID)
        t = Ui.store(this).find(titleId)
        if (t == null) { finish(); return }
        val tt = t!!

        b.btnBack.setOnClickListener { saveAndFinish() }
        b.titleName.text = tt.name
        b.byChaptersSwitch.isChecked = tt.byChapterMode
        b.byChaptersSwitch.setOnCheckedChangeListener { _, c -> tt.byChapterMode = c; refreshSummary() }

        b.btnAdd.setOnClickListener { addOne() }
        b.btnFill.setOnClickListener { showFillDialog() }
        b.btnShift.setOnClickListener { showShiftDialog() }
        b.btnClear.setOnClickListener {
            Ui.confirm(this, getString(R.string.chapters_clear), getString(R.string.chapters_clear_q), okText = R.string.reset) {
                rows.clear(); adapter.notifyDataSetChanged(); refreshSummary()
            }
        }

        adapter = RowAdapter()
        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter

        b.searchBox.visibility = if (st.chapterSearchInList) View.VISIBLE else View.GONE
        b.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, i: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, i: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter() }
        })
        b.btnSortByNum.setOnClickListener { sortByNum() }
        b.btnSortByWords.setOnClickListener { sortByWords() }
        b.btnCompact.setOnClickListener {
            compactMode = !compactMode
            adapter.notifyDataSetChanged()
        }
        refreshSummary()
        applyFilter()
    }

    private fun saveAndFinish() {
        val tt = t ?: return
        tt.words = if (tt.byChapterMode) rows.sumOf { it.words } else tt.words
        val store = Ui.store(this)
        val i = store.titles.indexOfFirst { it.id == tt.id }
        if (i >= 0) store.titles[i] = tt
        store.saveTitles()
        setResult(RESULT_OK)
        finish()
    }

    override fun onBackPressed() = saveAndFinish()

    // ---------- список ----------

    private lateinit var adapter: RowAdapter
    private var filteredIdx: List<Int> = rows.indices.toList()
    private var compactMode = false

    private fun applyFilter() {
        val q = b.searchInput.text.toString().trim().lowercase()
        filteredIdx = if (q.isEmpty()) rows.indices.toList()
        else rows.indices.filter { dev.readtracker.lite.data.Fmt.num(rows[it].num).contains(q) || rows[it].words.toString().contains(q) }
        adapter.notifyDataSetChanged()
        b.emptyView.visibility = if (filteredIdx.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun sortByNum() {
        val s = rows.sortedBy { it.num }
        rows.clear(); rows.addAll(s); applyFilter()
    }

    private fun sortByWords() {
        val s = rows.sortedByDescending { it.words }
        rows.clear(); rows.addAll(s); applyFilter()
    }

    private fun addOne() {
        val next = (rows.maxOfOrNull { it.num } ?: 0.0) + 1.0
        rows.add(NumRow(next, 0))
        applyFilter(); refreshSummary()
    }

    private fun showFillDialog() {
        val from = EditTextDlg(this, getString(R.string.fill_from), "")
        val to = EditTextDlg(this, getString(R.string.fill_to), "")
        val words = EditTextDlg(this, getString(R.string.words_label), "")
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.auto_fill)
            .setView(from.wrapVertical(to.wrapVertical(words)))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val a = from.value.toDoubleOrNull() ?: return@setPositiveButton
                val z = to.value.toDoubleOrNull() ?: return@setPositiveButton
                val w = words.value.toLongOrNull() ?: 0L
                fillRange(a, z, w)
            }
            .show()
    }

    /** Автозаполнение диапазона номеров; строки в диапазоне получают слова (если заданы). */
    private fun fillRange(from: Double, to: Double, wordsVal: Long) {
        var x = from
        while (x <= to + 1e-9) {
            val existing = rows.firstOrNull { it.num == x }
            if (existing == null) rows.add(NumRow(x, wordsVal))
            else if (wordsVal > 0) existing.words = wordsVal
            x += 1.0
        }
        sortByNum(); refreshSummary()
    }

    private fun showShiftDialog() {
        val e = EditTextDlg(this, getString(R.string.shift_by), "")
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.mass_shift)
            .setMessage(R.string.mass_shift_hint)
            .setView(e.wrap())
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val d = e.value.replace(',', '.').toDoubleOrNull() ?: return@setPositiveButton
                for (r in rows) r.num += d
                val dup = rows.groupBy { it.num }.values.firstOrNull { it.size > 1 }
                if (dup != null) {
                    rows.removeAll(dup.drop(1))
                    Ui.alert(this, getString(R.string.mass_shift), getString(R.string.exists_error, dev.readtracker.lite.data.Fmt.num(dup.first().num)))
                }
                sortByNum(); refreshSummary()
            }
            .show()
    }

    private fun refreshSummary() {
        val tt = t ?: return
        val total = rows.sumOf { it.words }
        val avg = if (rows.isNotEmpty()) total / rows.size else 0
        b.summary.text = getString(R.string.total_rows_summary,
            FmtWrap.long(total, st), FmtWrap.long(avg, st), getString(R.string.per_chapter_word))
        b.countInfo.text = getString(R.string.chapters_count_info, rows.size) +
                (if (tt.totalChapters != null) " · ${getString(R.string.of_total)} ${tt.totalChapters}" else "")
    }

    inner class RowAdapter : RecyclerView.Adapter<RowAdapter.VH>() {
        inner class VH(val v: TextView, val numEdit: android.widget.EditText, val wordsEdit: android.widget.EditText) : RecyclerView.ViewHolder(v)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val row = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chapter_row, parent, false) as android.widget.LinearLayout
            return VH(row, row.findViewById(R.id.rowNum), row.findViewById(R.id.rowWords))
        }

        override fun getItemCount(): Int = filteredIdx.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val idx = filteredIdx[pos]
            val r = rows[idx]
            h.numEdit.setText(dev.readtracker.lite.data.Fmt.num(r.num))
            h.wordsEdit.setText(if (r.words > 0) r.words.toString() else "")
            h.itemView.setBackgroundColor(
                if (pos % 2 == 0) Ui.withAlpha(Ui.themeColor(h.itemView.context, R.attr.rtlCard), 140)
                else android.graphics.Color.TRANSPARENT)
            val w = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, i: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, c: Int, i: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val nv = h.numEdit.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (nv > 0 && rows.any { x -> x !== r && x.num == nv }) {
                        h.numEdit.setText(dev.readtracker.lite.data.Fmt.num(r.num))
                        h.numEdit.setSelection(h.numEdit.text.length)
                        MaterialAlertDialogBuilder(this@ChaptersActivity)
                            .setMessage(getString(R.string.exists_error, dev.readtracker.lite.data.Fmt.num(nv)))
                            .setPositiveButton(R.string.ok, null).show()
                    } else r.num = nv
                    r.words = h.wordsEdit.text.toString().toLongOrNull() ?: 0L
                    refreshSummary()
                }
            }
            h.numEdit.addTextChangedListener(w)
            h.wordsEdit.addTextChangedListener(w)
            h.itemView.findViewById<TextView>(R.id.rowDel).setOnClickListener {
                val real = rows.indexOf(r)
                if (real >= 0) { rows.removeAt(real); applyFilter(); refreshSummary() }
            }
        }

        override fun onViewRecycled(h: VH) {
            super.onViewRecycled(h)
        }
    }
}

/** Мини-помощник для полей ввода в диалогах. */
class EditTextDlg(ctx: android.content.Context, hint: String, text: String) {
    val edit = android.widget.EditText(ctx).apply {
        this.hint = hint
        setText(text)
        setTextColor(android.graphics.Color.WHITE)
        setBackgroundResource(R.drawable.bg_field)
        val p = (10 * ctx.resources.displayMetrics.density).toInt()
        setPadding(p, p, p, p)
    }
    val value: String get() = edit.text.toString()
    fun wrap(): View {
        val pad = (20 * edit.resources.displayMetrics.density).toInt()
        val box = android.widget.FrameLayout(edit.context).apply { setPadding(pad, pad / 2, pad, 0); addView(edit) }
        return box
    }
    fun wrapVertical(vararg more: EditTextDlg): View {
        val pad = (20 * edit.resources.displayMetrics.density).toInt()
        val ll = android.widget.LinearLayout(edit.context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        ll.addView(edit)
        for (m in more) ll.addView(m.edit)
        return ll
    }
}
