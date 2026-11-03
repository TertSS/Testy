package dev.readtracker.lite.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.readtracker.lite.R
import dev.readtracker.lite.data.AppTheme
import dev.readtracker.lite.data.SettingsState
import dev.readtracker.lite.data.Store

/** Общая логика активностей: применение темы, навигация, мелкие хелперы. */
object Ui {

    /** Применяет выбранную тему до setContentView (изменения — через перезапуск активности). */
    fun applyTheme(ctx: Context, theme: AppTheme) {
        val mode = when (theme) {
            AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        val res = when (theme) {
            AppTheme.DARK -> R.style.Theme_RTL_Dark
            AppTheme.AMOLED -> R.style.Theme_RTL_Amoled
            AppTheme.LIGHT -> R.style.Theme_RTL_Light
        }
        ctx.theme.applyStyle(res, true)
    }

    fun store(ctx: Context): Store = Store.get(ctx)
    fun settings(ctx: Context): SettingsState = Store.get(ctx).settings

    fun openTitle(ctx: Context, id: String) {
        ctx.startActivity(Intent(ctx, TitleActivity::class.java).putExtra(EXTRA_ID, id))
    }

    /** Простой способ — сразу форма; обычный — экран тайтла (если включён). */
    fun openCard(ctx: Context, id: String) {
        if (settings(ctx).titleScreenEnabled) openTitle(ctx, id)
        else edit(ctx, id)
    }

    fun edit(ctx: Context, id: String?) {
        ctx.startActivity(Intent(ctx, EditActivity::class.java).let {
            if (id != null) it.putExtra(EXTRA_ID, id)
            it
        })
    }

    fun chapters(ctx: Context, activity: Activity, req: Int) {
        activity.startActivityForResult(
            Intent(ctx, ChaptersActivity::class.java), req
        )
    }

    fun settings(ctx: Context) {
        ctx.startActivity(Intent(ctx, SettingsActivity::class.java))
    }

    const val EXTRA_ID = "title_id"

    // ---------- Диалоги ----------

    fun alert(ctx: Context, title: String, body: String, onOk: (() -> Unit)? = null) {
        MaterialAlertDialogBuilder(ctx)
            .setTitle(title).setMessage(body)
            .setPositiveButton(R.string.ok) { d, _ -> d.dismiss(); onOk?.invoke() }
            .show()
    }

    fun confirm(
        ctx: Context, title: String, body: String,
        okText: Int = R.string.delete, danger: Boolean = false,
        onOk: () -> Unit,
    ) {
        MaterialAlertDialogBuilder(ctx)
            .setTitle(title).setMessage(body)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(okText) { d, _ -> d.dismiss(); onOk() }
            .show()
    }

    fun <T> choose(
        ctx: Context, title: String, items: List<T>, label: (T) -> String,
        current: T? = null, onPick: (T) -> Unit,
    ) {
        val arr = items.map { label(it) }.toTypedArray()
        MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setItems(arr) { d, i -> d.dismiss(); onPick(items[i]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Однострочный ввод с подтверждением. */
    fun input(
        ctx: Context, title: String, initial: String, hint: String = "",
        numeric: Boolean = false, onOk: (String) -> Unit,
    ) {
        val input = EditText(ctx).apply {
            setText(initial); setSelection(initial.length)
            this.hint = hint
            if (numeric) {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            }
        }
        val pad = (16 * ctx.resources.displayMetrics.density).toInt()
        val box = android.widget.FrameLayout(ctx).apply {
            setPadding(pad, pad / 2, pad, 0); addView(input)
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setView(box)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { d, _ -> d.dismiss(); onOk(input.text.toString()) }
            .show()
    }

    /** Копирует выбранный файл обложки во внутреннюю область; возвращает путь. */
    fun persistCover(ctx: Context, uri: Uri): String? = runCatching {
        val name = "cover_" + uri.toString().hashCode().toString(16) + ".img"
        val dst = java.io.File(ctx.filesDir, name)
        ctx.contentResolver.openInputStream(uri)?.use { ins ->
            dst.outputStream().use { out -> ins.copyTo(out) }
        } ?: return null
        dst.absolutePath
    }.getOrNull()

    fun dp(ctx: Context, v: Float): Int = (v * ctx.resources.displayMetrics.density).toInt()

    /** Значение атрибута темы (например «rtlCard») как Int-цвет. */
    fun themeColor(ctx: Context, attr: Int): Int {
        val tv = android.util.TypedValue()
        ctx.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    /** Прозрачность цвета: alpha := a (0..255). */
    fun withAlpha(color: Int, a: Int): Int =
        (color and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

    /** Смешивание двух цветов (для скруглённых фонов-подложек). */
    fun blend(base: Int, overlay: Int, frac: Float): Int {
        val f = frac.coerceIn(0f, 1f)
        fun ch(sh: Int) = (((base shr sh) and 0xFF) * (1 - f) + ((overlay shr sh) and 0xFF) * f).toInt()
        return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
