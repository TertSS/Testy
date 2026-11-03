package dev.readtracker.lite.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Варианты вида карточек (II-6). */
enum class CardStyle { NUMBERS, TILE, BAR }

/** Расположение закладки в карточке. */
enum class BookmarkPos { INLINE, BOTTOM }

/** Роль цвета для палитры (Персонализация, II-11). */
object ColorRoles {
    const val ACCENT = "accent"
    const val ST_PLANNED = "stPlanned"
    const val ST_READING = "stReading"
    const val ST_PAUSED = "stPaused"
    const val ST_COMPLETED = "stCompleted"
    const val ST_DROPPED = "stDropped"
    const val TAG_SERIES = "tagSeries"
    const val TAG_WEB = "tagWeb"
    const val TAG_SINGLE = "tagSingle"
    const val TAG_ONGOING = "tagOngoing"

    val ALL = listOf(ACCENT, ST_PLANNED, ST_READING, ST_PAUSED, ST_COMPLETED, ST_DROPPED,
        TAG_SERIES, TAG_WEB, TAG_SINGLE, TAG_ONGOING)

    fun defaults(): Map<String, Int> = mapOf(
        ACCENT to 0xFFFF9F0A.toInt(),
        ST_PLANNED to 0xFF60A5FA.toInt(),
        ST_READING to 0xFF34D399.toInt(),
        ST_PAUSED to 0xFFFBBF24.toInt(),
        ST_COMPLETED to 0xFFA78BFA.toInt(),
        ST_DROPPED to 0xFFF87171.toInt(),
        TAG_SERIES to 0xFFA78BFA.toInt(),
        TAG_WEB to 0xFFFBBF24.toInt(),
        TAG_SINGLE to 0xFFFF9F0A.toInt(),
        TAG_ONGOING to 0xFF34D399.toInt(),
    )
}

/** Темы: AMOLED / Тёмная (по умолчанию) / Светлая. */
enum class AppTheme { DARK, AMOLED, LIGHT }

/**
 * Настройки приложения (II-11). Хранятся отдельно от библиотеки (SharedPreferences JSON-строкой).
 * Значения по умолчанию соответствуют оригиналу.
 */
data class SettingsState(
    var theme: AppTheme = AppTheme.DARK,
    var cardStyle: CardStyle = CardStyle.NUMBERS,
    // Вид карточек
    var titleOneLine: Boolean = false,               // «Название в одну строку» (выкл)
    // Интерфейс
    var centeredHeaders: Boolean = false,
    var alignFilters: Boolean = true,
    var cardArrow: Boolean = true,
    var statusTextInList: Boolean = true,
    var dropReasonInCards: Boolean = true,
    var hideOngoingTag: Boolean = false,
    var shortenNumbers: Boolean = false,
    var disableAnimations: Boolean = false,
    var fabGlow: Boolean = false,
    var hideBottomBar: Boolean = true,               // в оригинале включена по умолчанию
    // Функции
    var titleScreenEnabled: Boolean = false,         // «Экран тайтла по тапу» (выкл)
    var titleScreenHeader: Boolean = true,           // название и статус на экране тайтла
    var titleScreenProgress: Boolean = false,        // блок прогресса на экране тайтла
    var simpleAdd: Boolean = false,                  // упрощённое добавление
    var goalsEnabled: Boolean = false,
    var bookmarksEnabled: Boolean = true,
    var bookmarkPosition: BookmarkPos = BookmarkPos.INLINE,
    var totalWordsEnabled: Boolean = false,          // «Всего слов в тайтле»
    var ratingEnabled: Boolean = true,
    var ratingScale: Int = 10,                       // 5 или 10
    var searchEnabled: Boolean = false,              // «Поиск и фильтр»
    var genresEnabled: Boolean = false,
    var onlyChartGenres: Boolean = false,            // «Только жанры в диаграмме»
    var genreChartOrder: MutableList<String> = mutableListOf(), // редактор диаграммы жанров
    // Аналитика
    var wideMetricCards: Boolean = false,
    var webChaptersOnCards: Boolean = true,          // «Главы веб-романов»
    var sortAllByStatus: Boolean = true,             // сортировка «Все» по статусу
    var analyticsSections: MutableList<String> =
        mutableListOf("top", "byStatus", "genres", "goals"), // порядок/включение разделов
    // Цвета
    var colors: MutableMap<String, Int> = ColorRoles.defaults().toMutableMap(),
    // Цели (пустое/0 — цели нет)
    var goalWords: Long = 0,
    var goalVolumes: Long = 0,
    var goalWebDone: Long = 0,
    var goalSeriesDone: Long = 0,
    // Прочее
    var lastStatusTab: String = "all",               // последняя вкладка запоминается
    var knownGenres: MutableList<String> = mutableListOf(), // словарь жанров (не уменьшается)
) {
    fun color(role: String): Int = colors[role] ?: ColorRoles.defaults()[role]!!

    fun statusColor(s: TitleStatus): Int = when (s) {
        TitleStatus.PLANNED -> color(ColorRoles.ST_PLANNED)
        TitleStatus.READING -> color(ColorRoles.ST_READING)
        TitleStatus.PAUSED -> color(ColorRoles.ST_PAUSED)
        TitleStatus.COMPLETED -> color(ColorRoles.ST_COMPLETED)
        TitleStatus.DROPPED -> color(ColorRoles.ST_DROPPED)
    }

    fun formatColor(f: FormatType): Int = when (f) {
        FormatType.SERIES -> color(ColorRoles.TAG_SERIES)
        FormatType.WEB -> color(ColorRoles.TAG_WEB)
        FormatType.SINGLE -> color(ColorRoles.TAG_SINGLE)
    }

    fun isSectionOn(key: String) = analyticsSections.contains(key)

    // ---------- JSON ----------

    fun toJson(): JSONObject = JSONObject().apply {
        put("theme", theme.name)
        put("cardStyle", cardStyle.name)
        put("titleOneLine", titleOneLine)
        put("centeredHeaders", centeredHeaders)
        put("alignFilters", alignFilters)
        put("cardArrow", cardArrow)
        put("statusTextInList", statusTextInList)
        put("dropReasonInCards", dropReasonInCards)
        put("hideOngoingTag", hideOngoingTag)
        put("shortenNumbers", shortenNumbers)
        put("disableAnimations", disableAnimations)
        put("fabGlow", fabGlow)
        put("hideBottomBar", hideBottomBar)
        put("titleScreenEnabled", titleScreenEnabled)
        put("titleScreenHeader", titleScreenHeader)
        put("titleScreenProgress", titleScreenProgress)
        put("simpleAdd", simpleAdd)
        put("goalsEnabled", goalsEnabled)
        put("bookmarksEnabled", bookmarksEnabled)
        put("bookmarkPosition", bookmarkPosition.name)
        put("totalWordsEnabled", totalWordsEnabled)
        put("ratingEnabled", ratingEnabled)
        put("ratingScale", ratingScale)
        put("searchEnabled", searchEnabled)
        put("genresEnabled", genresEnabled)
        put("onlyChartGenres", onlyChartGenres)
        put("genreChartOrder", JSONArray(genreChartOrder))
        put("wideMetricCards", wideMetricCards)
        put("webChaptersOnCards", webChaptersOnCards)
        put("sortAllByStatus", sortAllByStatus)
        put("analyticsSections", JSONArray(analyticsSections))
        put("goalWords", goalWords)
        put("goalVolumes", goalVolumes)
        put("goalWebDone", goalWebDone)
        put("goalSeriesDone", goalSeriesDone)
        put("lastStatusTab", lastStatusTab)
        put("knownGenres", JSONArray(knownGenres))
        val c = JSONObject()
        for ((k, v) in colors) c.put(k, v)
        put("colors", c)
    }

    companion object {
        fun fromJson(o: JSONObject): SettingsState {
            val base = SettingsState()
            val colors = base.colors
            o.optJSONObject("colors")?.let { co ->
                for (role in ColorRoles.ALL) {
                    if (co.has(role)) colors[role] = co.getInt(role)
                }
            }
            return SettingsState(
                theme = runCatching { AppTheme.valueOf(o.optString("theme", AppTheme.DARK.name)) }.getOrDefault(AppTheme.DARK),
                cardStyle = runCatching { CardStyle.valueOf(o.optString("cardStyle", CardStyle.NUMBERS.name)) }.getOrDefault(CardStyle.NUMBERS),
                titleOneLine = o.optBoolean("titleOneLine", base.titleOneLine),
                centeredHeaders = o.optBoolean("centeredHeaders", base.centeredHeaders),
                alignFilters = o.optBoolean("alignFilters", base.alignFilters),
                cardArrow = o.optBoolean("cardArrow", base.cardArrow),
                statusTextInList = o.optBoolean("statusTextInList", base.statusTextInList),
                dropReasonInCards = o.optBoolean("dropReasonInCards", base.dropReasonInCards),
                hideOngoingTag = o.optBoolean("hideOngoingTag", base.hideOngoingTag),
                shortenNumbers = o.optBoolean("shortenNumbers", base.shortenNumbers),
                disableAnimations = o.optBoolean("disableAnimations", base.disableAnimations),
                fabGlow = o.optBoolean("fabGlow", base.fabGlow),
                hideBottomBar = o.optBoolean("hideBottomBar", base.hideBottomBar),
                titleScreenEnabled = o.optBoolean("titleScreenEnabled", base.titleScreenEnabled),
                titleScreenHeader = o.optBoolean("titleScreenHeader", base.titleScreenHeader),
                titleScreenProgress = o.optBoolean("titleScreenProgress", base.titleScreenProgress),
                simpleAdd = o.optBoolean("simpleAdd", base.simpleAdd),
                goalsEnabled = o.optBoolean("goalsEnabled", base.goalsEnabled),
                bookmarksEnabled = o.optBoolean("bookmarksEnabled", base.bookmarksEnabled),
                bookmarkPosition = runCatching { BookmarkPos.valueOf(o.optString("bookmarkPosition", "INLINE")) }.getOrDefault(BookmarkPos.INLINE),
                totalWordsEnabled = o.optBoolean("totalWordsEnabled", base.totalWordsEnabled),
                ratingEnabled = o.optBoolean("ratingEnabled", base.ratingEnabled),
                ratingScale = if (o.optInt("ratingScale", 10) == 5) 5 else 10,
                searchEnabled = o.optBoolean("searchEnabled", base.searchEnabled),
                genresEnabled = o.optBoolean("genresEnabled", base.genresEnabled),
                onlyChartGenres = o.optBoolean("onlyChartGenres", base.onlyChartGenres),
                genreChartOrder = o.optJSONArray("genreChartOrder").toStringList(),
                wideMetricCards = o.optBoolean("wideMetricCards", base.wideMetricCards),
                webChaptersOnCards = o.optBoolean("webChaptersOnCards", base.webChaptersOnCards),
                sortAllByStatus = o.optBoolean("sortAllByStatus", base.sortAllByStatus),
                analyticsSections = o.optJSONArray("analyticsSections").toStringList()
                    .ifEmpty { base.analyticsSections },
                goalWords = o.optLong("goalWords", 0L),
                goalVolumes = o.optLong("goalVolumes", 0L),
                goalWebDone = o.optLong("goalWebDone", 0L),
                goalSeriesDone = o.optLong("goalSeriesDone", 0L),
                lastStatusTab = o.optString("lastStatusTab", "all"),
                knownGenres = o.optJSONArray("knownGenres").toStringList(),
                colors = colors,
            )
        }

        private fun JSONArray?.toStringList(): MutableList<String> =
            (0 until (this?.length() ?: 0)).map { get(it).toString() }.toMutableList()
    }
}

/**
 * Единое локальное хранилище: библиотека и настройки хранятся раздельно.
 * Сохранение при каждом изменении и при сворачивании приложения (см. App.onPause).
 */
class Store private constructor(private val ctx: Context) {

    private val prefs = ctx.getSharedPreferences("rtl_store", Context.MODE_PRIVATE)

    var titles: MutableList<NovelTitle> = loadTitles()
        private set
    var settings: SettingsState = loadSettings()
        private set

    private fun loadTitles(): MutableList<NovelTitle> {
        val raw = prefs.getString(KEY_TITLES, null) ?: return mutableListOf()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { NovelTitle.fromJson(arr.getJSONObject(it)) }.toMutableList()
        }.getOrDefault(mutableListOf())
    }

    private fun loadSettings(): SettingsState {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return SettingsState()
        return runCatching { SettingsState.fromJson(JSONObject(raw)) }.getOrDefault(SettingsState())
    }

    fun saveTitles() {
        prefs.edit().putString(KEY_TITLES, JSONArray(titles.map { it.toJson() }).toString()).apply()
    }

    fun saveSettings() {
        prefs.edit().putString(KEY_SETTINGS, settings.toJson().toString()).apply()
    }

    /** Новый тайтл вставляется в начало списка. */
    fun addTitle(t: NovelTitle) {
        titles.add(0, t); mergeGenres(t); saveTitles()
    }

    fun updateTitle(t: NovelTitle) {
        val i = titles.indexOfFirst { it.id == t.id }
        if (i >= 0) titles[i] = t else titles.add(0, t)
        mergeGenres(t); saveTitles()
    }

    fun removeTitle(id: String) {
        titles.removeAll { it.id == id }; saveTitles()
    }

    fun find(id: String?): NovelTitle? = titles.firstOrNull { it.id == id }

    /** Словарь известных жанров пополняется при сохранении любого тайтла (без дублей без учёта регистра). */
    private fun mergeGenres(t: NovelTitle) {
        for (g in t.genres) {
            val gg = g.trim()
            if (gg.isNotEmpty() && settings.knownGenres.none { it.equals(gg, true) }) {
                settings.knownGenres.add(gg)
            }
        }
        saveSettings()
    }

    // ---------- Общие итоги (II-2): считаются по ВСЕМ тайтлам независимо от статуса ----------

    data class Totals(val words: Long, val volumes: Long, val chapters: Long, val count: Int,
                      val seriesDone: Int, val webDone: Int)

    fun totals(): Totals {
        var w = 0L; var v = 0L; var ch = 0L
        var sd = 0; var wd = 0
        for (t in titles) {
            w += t.effWords()
            if (t.hasVolumes()) v += t.effVolumes()
            if (t.format == FormatType.WEB) {
                ch += if (t.byChapterMode) t.chapterRows.size.toLong() else t.chaptersRead.toLong()
                if (t.status == TitleStatus.COMPLETED) wd++
            }
            if (t.format == FormatType.SERIES && t.status == TitleStatus.COMPLETED) sd++
        }
        return Totals(w, v, ch, titles.size, sd, wd)
    }

    // ---------- Экспорт / импорт (II-11) ----------

    fun libraryJson(): String = JSONArray(titles.map { it.toJson() }).toString(2)

    fun settingsJson(): String = settings.toJson().toString(2)

    sealed class ImportResult {
        data class Ok(val added: Int, val replaced: Boolean) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    /** Импорт библиотеки: merge — слияние по id, replace — полная замена. */
    fun importLibrary(json: String, replace: Boolean): ImportResult {
        return try {
            val parsed = JSONArray(json).let { arr ->
                (0 until arr.length()).map { NovelTitle.fromJson(arr.getJSONObject(it)) }
            }
            if (replace) {
                titles = parsed.toMutableList()
            } else {
                for (t in parsed) {
                    val i = titles.indexOfFirst { it.id == t.id }
                    if (i >= 0) titles[i] = t else titles.add(0, t)
                }
            }
            for (t in parsed) mergeGenres(t)
            saveTitles()
            ImportResult.Ok(parsed.size, replace)
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "некорректный JSON")
        }
    }

    fun importSettings(json: String): ImportResult {
        return try {
            settings = SettingsState.fromJson(JSONObject(json))
            saveSettings()
            ImportResult.Ok(1, false)
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "некорректный JSON")
        }
    }

    fun resetSettingsToDefaults() {
        settings = SettingsState()
        saveSettings()
    }

    fun clearAllData() {
        titles = mutableListOf()
        saveTitles()
    }

    companion object {
        private const val KEY_TITLES = "library_json"
        private const val KEY_SETTINGS = "settings_json"

        @Volatile private var instance: Store? = null

        fun get(ctx: Context): Store =
            instance ?: synchronized(this) {
                instance ?: Store(ctx.applicationContext).also { instance = it }
            }
    }
}
