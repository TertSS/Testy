package dev.readtracker.lite.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import dev.readtracker.lite.R
import dev.readtracker.lite.data.CardStyle
import dev.readtracker.lite.data.FormatType
import dev.readtracker.lite.data.NovelTitle
import dev.readtracker.lite.data.SettingsState
import dev.readtracker.lite.data.TitleStatus
import dev.readtracker.lite.databinding.ItemTitleBinding

/** Адаптер списка тайтлов с тремя стилями карточек (II-6). */
class TitleAdapter(
    private val st: SettingsState,
    private val onClick: (NovelTitle) -> Unit,
    private val onLongClick: (NovelTitle) -> Unit,
) : RecyclerView.Adapter<TitleAdapter.VH>() {

    inner class VH(val b: ItemTitleBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(h: VH, pos: Int) = bind(h.b, items[pos], h.itemView.context)

    var items: List<NovelTitle> = emptyList()
        private set

    fun submit(list: List<NovelTitle>) {
        items = list
        notifyDataSetChanged()
    }

    private fun bind(b: ItemTitleBinding, t: NovelTitle, ctx: Context) {
        val root = b.root
        // обложка: локальный файл > url > цветная заглушка с инициалами
        when {
            t.coverFile.isNotBlank() -> {
                b.cover.scaleType = ImageView.ScaleType.CENTER_CROP
                b.cover.load(Uri.parse("file://" + t.coverFile)) { placeholder(t.coverColor) }
                b.coverText.visibility = View.GONE
            }
            t.coverUrl.isNotBlank() -> {
                b.cover.scaleType = ImageView.ScaleType.CENTER_CROP
                b.cover.load(t.coverUrl) { placeholder(t.coverColor); error(t.coverColor) }
                b.coverText.visibility = View.GONE
            }
            else -> {
                b.cover.setImageDrawable(null)
                b.cover.setBackgroundColor(t.coverColor)
                b.coverText.visibility = View.VISIBLE
                b.coverText.text = initials(t.name)
            }
        }
        round(b.cover, 8f, ctx)

        b.name.text = t.name.ifBlank { ctx.getString(R.string.untitled) }
        b.name.setSingleLine(st.titleOneLine)
        b.author.visibility = if (t.author.isBlank()) View.GONE else View.VISIBLE
        b.author.text = t.author

        // теги: формат (+ онгоинг) и статус в списке
        b.tags.removeAllViews()
        addTag(b.tags, ctx, formatLabel(ctx, t.format), st.formatColor(t.format))
        if (t.ongoing && !st.hideOngoingTag)
            addTag(b.tags, ctx, ctx.getString(R.string.ongoing_tag), st.color(dev.readtracker.lite.data.ColorRoles.TAG_ONGOING))
        if (st.statusTextInList)
            addTag(b.tags, ctx, statusLabel(ctx, t.status), st.statusColor(t.status))

        // закладка в ряд
        val bmInline = st.bookmarksEnabled && t.bookmark.isNotBlank() && st.bookmarkPosition == dev.readtracker.lite.data.BookmarkPos.INLINE
        b.bookmark.visibility = if (bmInline) View.VISIBLE else View.GONE
        if (bmInline) b.bookmark.text = "🔖 ${t.bookmark}"

        // причина дропа в карточках
        b.dropReason.visibility =
            if (st.dropReasonInCards && t.status == TitleStatus.DROPPED && t.dropReason.isNotBlank()) View.VISIBLE else View.GONE
        if (b.dropReason.visibility == View.VISIBLE) b.dropReason.text = "✕ ${t.dropReason}"

        // оценка
        if (st.ratingEnabled && t.rating != null) {
            b.ratingBar.visibility = View.VISIBLE
            b.ratingText.visibility = View.VISIBLE
            b.ratingBar.max = st.ratingScale
            b.ratingBar.progress = t.rating ?: 0
            b.ratingText.text = "${t.rating}/${st.ratingScale}"
        } else {
            b.ratingBar.visibility = View.GONE
            b.ratingText.visibility = View.GONE
        }

        // правый блок по стилю карточки
        configureRightBlock(b, t, ctx, st)

        // полоса прогресса для стиля «Цифры» (II-6: как «Цифры» плюс полоса снизу)
        if (st.cardStyle == CardStyle.BAR) {
            val p = t.progress(st)
            if (p != null) {
                b.cardProgress.visibility = View.VISIBLE
                b.cardProgress.max = 1000
                b.cardProgress.progress = (p.value * 1000).toInt()
                tintProgress(b.cardProgress, progressColor(ctx, st, p.colorRole))
            } else b.cardProgress.visibility = View.GONE
        } else b.cardProgress.visibility = View.GONE

        // закладка снизу
        val bmBottom = st.bookmarksEnabled && t.bookmark.isNotBlank() && st.bookmarkPosition == dev.readtracker.lite.data.BookmarkPos.BOTTOM
        b.bookmarkBottom.visibility = if (bmBottom) View.VISIBLE else View.GONE
        if (bmBottom) b.bookmarkBottom.text = "🔖 ${t.bookmark}"

        b.arrow.visibility = if (st.cardArrow) View.VISIBLE else View.GONE

        root.setOnClickListener { onClick(t) }
        root.setOnLongClickListener { onLongClick(t); true }
    }

    private fun configureRightBlock(b: ItemTitleBinding, t: NovelTitle, ctx: Context, st: SettingsState) {
        b.blockNumbers.visibility = View.GONE
        b.blockTile.visibility = View.GONE
        b.blockBar.visibility = View.GONE
        when (st.cardStyle) {
            CardStyle.NUMBERS -> {
                b.blockNumbers.visibility = View.VISIBLE
                setMainNumber(b.numBig, b.numCaption, t, ctx, st)
            }
            CardStyle.TILE -> {
                b.blockTile.visibility = View.VISIBLE
                val acc = st.color(dev.readtracker.lite.data.ColorRoles.ACCENT)
                val tileBg = GradientDrawable().apply {
                    cornerRadius = Ui.dp(ctx, 10f).toFloat()
                    setColor(Ui.withAlpha(acc, 42))
                }
                b.tile.background = tileBg
                setMainNumber(b.tileBig, b.tileCap, t, ctx, st)
                b.tileBig.setTextColor(acc)
                // вторичные метрики
                b.tileSub.text = secondaryLine(t, ctx, st)
            }
            CardStyle.BAR -> {
                b.blockBar.visibility = View.VISIBLE
                setMainNumber(b.barBig, b.barCap, t, ctx, st)
                val p = t.progress(st)
                if (p != null) {
                    b.barProgress.visibility = View.VISIBLE
                    b.barProgress.progress = (p.value * 1000).toInt()
                    tintProgress(b.barProgress, progressColor(ctx, st, p.colorRole))
                    b.barCaption.visibility = View.VISIBLE
                    b.barCaption.text = p.caption
                } else {
                    b.barProgress.visibility = View.GONE
                    b.barCaption.visibility = View.GONE
                }
            }
        }
    }

    /** Главное число карточки: слова/тома/главы — по формату и настройкам. */
    private fun setMainNumber(big: TextView, cap: TextView, t: NovelTitle, ctx: Context, st: SettingsState) {
        if (t.format == FormatType.WEB) {
            val ch = if (t.byChapterMode) t.chapterRows.size else t.chaptersRead
            big.text = FmtWrap.int(ch, st)
            cap.text = if (st.webChaptersOnCards) {
                val tot = t.totalChapters
                if (tot != null && tot > 0) "$ch/$tot ${ctx.getString(R.string.chapters_suffix)}"
                else ctx.getString(R.string.chapters_suffix)
            } else ctx.getString(R.string.chapters_suffix)
        } else {
            big.text = FmtWrap.long(t.effWords(), st)
            val vc = t.volumesCaption()
            cap.text = if (vc.isNotEmpty()) "$vc ${ctx.getString(R.string.volumes_suffix)} · ${ctx.getString(R.string.words_suffix)}"
            else ctx.getString(R.string.words_suffix)
        }
    }

    private fun secondaryLine(t: NovelTitle, ctx: Context, st: SettingsState): String {
        return if (t.format == FormatType.WEB) {
            FmtWrap.long(t.effWords(), st) + " " + ctx.getString(R.string.words_suffix)
        } else {
            val v = t.volumesCaption()
            if (v.isNotEmpty()) "$v ${ctx.getString(R.string.volumes_suffix)}"
            else ctx.getString(R.string.volumes_label)
        }
    }

    private fun progressColor(ctx: Context, st: SettingsState, role: String): Int = when (role) {
        "accent" -> st.color(dev.readtracker.lite.data.ColorRoles.ACCENT)
        "status" -> st.colors[dev.readtracker.lite.data.ColorRoles.ST_READING] ?: 0xFF34D399.toInt()
        else -> st.color(dev.readtracker.lite.data.ColorRoles.TAG_WEB)
    }

    private fun tintProgress(pb: ProgressBar, color: Int) {
        val d = pb.progressDrawable?.mutate()
        (d as? android.graphics.drawable.LayerDrawable)?.let { ld ->
            ld.findDrawableByLayerId(android.R.id.progress)?.setTint(color)
        }
    }

    private fun addTag(container: LinearLayout, ctx: Context, text: String, color: Int) {
        val tv = TextView(ctx).apply {
            this.text = text
            setTextColor(color)
            textSize = 11f
            setBackgroundResource(R.drawable.bg_tag)
            background.setTint(Ui.withAlpha(color, 38))
            val p = Ui.dp(ctx, 5f)
            setPadding(p, Ui.dp(ctx, 1.5f), p, Ui.dp(ctx, 1.5f))
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.marginEnd = Ui.dp(ctx, 4f)
        container.addView(tv, lp)
    }

    private fun round(v: View, radiusDp: Float, ctx: Context) {
        val outline = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, o: android.graphics.Outline) {
                o.setRoundRect(0, 0, view.width, view.height, Ui.dp(ctx, radiusDp).toFloat())
            }
        }
        v.clipToOutline = true
        v.outlineProvider = outline
    }

    companion object {
        fun initials(name: String): String =
            name.trim().split("\\s+".toRegex()).take(2).mapNotNull { it.firstOrNull() }
                .joinToString("").uppercase().ifEmpty { "?" }

        fun statusLabel(ctx: Context, s: TitleStatus): String = ctx.getString(
            when (s) {
                TitleStatus.PLANNED -> R.string.status_planned
                TitleStatus.READING -> R.string.status_reading
                TitleStatus.PAUSED -> R.string.status_paused
                TitleStatus.COMPLETED -> R.string.status_completed
                TitleStatus.DROPPED -> R.string.status_dropped
            }
        )

        fun formatLabel(ctx: Context, f: FormatType): String = ctx.getString(
            when (f) {
                FormatType.SERIES -> R.string.format_series
                FormatType.WEB -> R.string.format_web
                FormatType.SINGLE -> R.string.format_single_card
            }
        )
    }
}

/** Форматирование с учётом настройки «сокращать числа». */
object FmtWrap {
    fun long(n: Long, st: SettingsState): String = dev.readtracker.lite.data.Fmt.smart(n, st.shortenNumbers)
    fun int(n: Int, st: SettingsState): String = dev.readtracker.lite.data.Fmt.smart(n.toLong(), st.shortenNumbers)
}
