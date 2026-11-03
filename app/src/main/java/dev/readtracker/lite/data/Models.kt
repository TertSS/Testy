package dev.readtracker.lite.data

import org.json.JSONArray
import org.json.JSONObject

/** Статусы тайтла (5 значений, порядок = канонический). */
enum class TitleStatus(val key: String) {
    PLANNED("planned"), READING("reading"), PAUSED("paused"), COMPLETED("completed"), DROPPED("dropped");

    companion object {
        /** Неизвестный статус при загрузке -> «В планах» (II-12). */
        fun fromKey(k: String?): TitleStatus = entries.firstOrNull { it.key == k } ?: PLANNED
    }
}

/** Формат издания: Серия / Веб / Одиночка. */
enum class FormatType(val key: String) {
    SERIES("series"), WEB("web"), SINGLE("single");

    companion object {
        fun fromKey(k: String?): FormatType = entries.firstOrNull { it.key == k } ?: SERIES
    }
}

/** Строка детальной таблицы: номер тома/главы (дробный, 1.5) -> слов. */
data class NumRow(var num: Double, var words: Long) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("num", num); put("words", words)
    }

    companion object {
        fun fromJson(o: JSONObject): NumRow = NumRow(
            o.optDouble("num", 0.0), o.optLong("words", 0L)
        )
    }
}

/**
 * Модель тайтла (II-1). id — строка времени создания в мс; новые тайтлы вставляются в начало.
 */
data class NovelTitle(
    val id: String,
    var name: String = "",
    var author: String = "",
    var description: String = "",
    // обложка: url либо локальный файл-Uri (выбор файла стирает url и наоборот)
    var coverUrl: String = "",
    var coverFile: String = "",
    var coverColor: Int = DEFAULT_COVER_COLOR,
    var status: TitleStatus = TitleStatus.PLANNED,
    var format: FormatType = FormatType.SERIES,
    var dropReason: String = "",
    var bookmark: String = "",
    var rating: Int? = null,               // 0..10, null = не задана
    var genres: MutableList<String> = mutableListOf(),
    var ongoing: Boolean = false,
    var countVolumes: Boolean = true,      // «считать тома»: вкл у Серии и Одиночки, выкл у Веба
    var totalVolumesInSeries: Int? = null, // «Всего томов в серии», null = не задано
    var chaptersRead: Int = 0,             // прогресс глав веба (не зависит от таблицы)
    var totalChapters: Int? = null,        // «всего глав», null/0 = не задано
    var words: Long = 0,                   // суммарное поле слов
    var volumes: Int = 0,                  // суммарное поле томов
    var totalWordsInTitle: Long? = null,   // «Всего слов в тайтле»
    var byVolumeMode: Boolean = false,
    var volumeRows: MutableList<NumRow> = mutableListOf(),
    var byChapterMode: Boolean = false,
    var chapterRows: MutableList<NumRow> = mutableListOf(),
) {
    // ---------- Формулы (II-2) ----------

    /** Эффективные слова: сумма строк в детальном режиме, иначе введённое число. */
    fun effWords(): Long = when {
        format == FormatType.WEB && byChapterMode -> chapterRows.sumOf { it.words }
        format != FormatType.WEB && byVolumeMode -> volumeRows.sumOf { it.words }
        else -> words
    }

    /** Эффективные тома: в режиме по томам — число строк, иначе введённое число. */
    fun effVolumes(): Int =
        if (format != FormatType.WEB && byVolumeMode) volumeRows.size else volumes

    /** Есть ли у формата тома (Веб — нет). */
    fun hasVolumes(): Boolean = countVolumes && format != FormatType.WEB

    /** Подпись томов: "" | "5/? т." | "5/22 т." | "5 т.". */
    fun volumesCaption(): String {
        if (!hasVolumes()) return ""
        val v = effVolumes()
        return when {
            ongoing -> "$v/?"
            totalVolumesInSeries != null && totalVolumesInSeries!! > 0 -> "$v/${totalVolumesInSeries}"
            else -> "$v"
        }
    }

    /** Подпись глав (Веб): "120/340 гл." или "120 гл."; 0 в «всего» = не задано. */
    fun chaptersCaption(): String {
        if (format != FormatType.WEB) return ""
        val t = totalChapters
        return if (t != null && t > 0) "$chaptersRead/$t" else "$chaptersRead"
    }

    /** Прогресс (значение 0..1, подпись, цвет-роль). Первый подходящий вариант из II-2. */
    class Progress(val value: Float, val caption: String, val colorRole: String)

    fun progress(settings: SettingsState): Progress? {
        // 1. всего слов
        if (settings.totalWordsEnabled) {
            val tw = totalWordsInTitle
            if (tw != null && tw > 0) {
                val w = effWords()
                return Progress(
                    clamp01(w.toDouble() / tw),
                    "Слова: ${Fmt.thousands(w)} / ${Fmt.thousands(tw)}",
                    "accent"
                )
            }
        }
        // 2. тома
        val tv = totalVolumesInSeries
        if (hasVolumes() && tv != null && tv > 0) {
            val v = effVolumes()
            return Progress(
                clamp01(v.toDouble() / tv),
                "Тома: $v/$tv т.",
                "status"
            )
        }
        // 3. главы (веб)
        val tc = totalChapters
        if (format == FormatType.WEB && tc != null && tc > 0) {
            return Progress(
                clamp01(chaptersRead.toDouble() / tc),
                "Главы: $chaptersRead/$tc гл.",
                "tagWeb"
            )
        }
        return null
    }

    private fun clamp01(x: Double) = (x.coerceIn(0.0, 1.0)).toFloat()

    // ---------- JSON ----------

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("author", author)
        put("description", description)
        put("coverUrl", coverUrl)
        put("coverFile", coverFile)
        put("coverColor", coverColor)
        put("status", status.key)
        put("format", format.key)
        put("dropReason", dropReason)
        put("bookmark", bookmark)
        put("ongoing", ongoing)
        put("countVolumes", countVolumes)
        put("words", words)
        put("volumes", volumes)
        put("chaptersRead", chaptersRead)
        put("byVolumeMode", byVolumeMode)
        put("byChapterMode", byChapterMode)
        put("genres", JSONArray(genres))
        put("volumeRows", JSONArray(volumeRows.map { it.toJson() }))
        put("chapterRows", JSONArray(chapterRows.map { it.toJson() }))
        rating?.let { put("rating", it) } ?: putNull("rating")
        totalVolumesInSeries?.let { put("totalVolumesInSeries", it) } ?: putNull("totalVolumesInSeries")
        totalChapters?.let { put("totalChapters", it) } ?: putNull("totalChapters")
        totalWordsInTitle?.let { put("totalWordsInTitle", it) } ?: putNull("totalWordsInTitle")
    }

    private fun JSONObject.putNull(key: String) = put(key, JSONObject.NULL)

    companion object {
        const val DEFAULT_COVER_COLOR = 0xFF30336B.toInt() // тёмно-индиго

        /** Битые/неполные записи не должны ломать приложение: неизвестный статус → «В планах», числа → 0. */
        fun fromJson(o: JSONObject): NovelTitle {
            val fmt = FormatType.fromKey(o.optString("format"))
            return NovelTitle(
                id = o.optString("id").ifBlank { System.currentTimeMillis().toString() },
                name = o.optString("name"),
                author = o.optString("author"),
                description = o.optString("description"),
                coverUrl = o.optString("coverUrl"),
                coverFile = o.optString("coverFile"),
                coverColor = o.optInt("coverColor", DEFAULT_COVER_COLOR),
                status = TitleStatus.fromKey(o.optString("status")),
                format = fmt,
                dropReason = o.optString("dropReason"),
                bookmark = o.optString("bookmark"),
                rating = if (o.isNull("rating")) null else o.optInt("rating", -1).takeIf { it in 0..10 },
                genres = o.optJSONArray("genres").let { a ->
                    (0 until (a?.length() ?: 0)).map { a!!.getString(it) }.toMutableList()
                },
                ongoing = o.optBoolean("ongoing"),
                countVolumes = o.optBoolean("countVolumes", fmt != FormatType.WEB),
                totalVolumesInSeries = if (o.isNull("totalVolumesInSeries")) null
                else o.optInt("totalVolumesInSeries", 0).takeIf { it > 0 },
                chaptersRead = o.optInt("chaptersRead", 0),
                totalChapters = if (o.isNull("totalChapters")) null
                else o.optInt("totalChapters", 0).takeIf { it > 0 },
                words = o.optLong("words", 0L),
                volumes = o.optInt("volumes", 0),
                totalWordsInTitle = if (o.isNull("totalWordsInTitle")) null
                else o.optLong("totalWordsInTitle", 0L).takeIf { it > 0 },
                byVolumeMode = o.optBoolean("byVolumeMode"),
                volumeRows = o.optJSONArray("volumeRows").let { a ->
                    (0 until (a?.length() ?: 0)).map { NumRow.fromJson(a!!.getJSONObject(it)) }.toMutableList()
                },
                byChapterMode = o.optBoolean("byChapterMode"),
                chapterRows = o.optJSONArray("chapterRows").let { a ->
                    (0 until (a?.length() ?: 0)).map { NumRow.fromJson(a!!.getJSONObject(it)) }.toMutableList()
                },
            )
        }
    }
}

/** Утилиты форматирования чисел (II-2). */
object Fmt {
    /** Разделитель тысяч — пробел: «1 250 000». */
    fun thousands(n: Long): String {
        val s = n.toString()
        val sb = StringBuilder()
        for ((i, c) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(' ')
            sb.append(c)
        }
        return sb.toString()
    }

    /** Число с учётом настройки «сокращать числа»: >=1M -> «1.2M», >=1K -> «150K». */
    fun smart(n: Long, shorten: Boolean): String {
        if (!shorten) return thousands(n)
        return when {
            n >= 1_000_000 -> trimZero("%.1f".format(n / 1_000_000.0)) + "M"
            n >= 1_000 -> (n / 1000).toString() + "K"
            else -> n.toString()
        }
    }

    /** Дробный номер без «.0», если целое. */
    fun num(d: Double): String =
        if (d == Math.floor(d) && !d.isInfinite()) d.toLong().toString()
        else d.toString().trimEnd('0').trimEnd('.')

    private fun trimZero(s: String) = if (s.endsWith(".0")) s.dropLast(2) else s
}
