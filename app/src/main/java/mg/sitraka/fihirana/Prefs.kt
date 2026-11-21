package mg.sitraka.fihirana

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Réglages de l'utilisateur (observables par Compose) + dernières positions. */
object Prefs {
    private lateinit var sp: SharedPreferences

    var themeMode by mutableIntStateOf(0)      // 0 = système, 1 = clair, 2 = sombre
        private set
    var accent by mutableIntStateOf(0)
        private set
    var fontScale by mutableFloatStateOf(1f)
        private set
    var justify by mutableStateOf(true)
        private set
    var keepOn by mutableStateOf(false)
        private set
    var headings by mutableStateOf(true)
        private set
    var bibleTr by mutableIntStateOf(1)
        private set
    var compareTrs by mutableStateOf(listOf(1, 2, 3, 4))
        private set

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        themeMode = sp.getInt("theme", 0)
        accent = sp.getInt("accent", 0).coerceIn(0, Accents.size - 1)
        fontScale = sp.getFloat("font", 1f).coerceIn(0.8f, 1.8f)
        justify = sp.getBoolean("justify", true)
        keepOn = sp.getBoolean("keepOn", false)
        headings = sp.getBoolean("headings", true)
        bibleTr = sp.getInt("tr", 1)
        compareTrs = (sp.getString("cmp", "1,2,3,4") ?: "1,2,3,4").split(',').mapNotNull { it.toIntOrNull() }.ifEmpty { listOf(1, 2) }
    }

    fun putThemeMode(v: Int) { themeMode = v; sp.edit().putInt("theme", v).apply() }
    fun putAccent(v: Int) { accent = v; sp.edit().putInt("accent", v).apply() }
    fun putFontScale(v: Float) { fontScale = v; sp.edit().putFloat("font", v).apply() }
    fun putJustify(v: Boolean) { justify = v; sp.edit().putBoolean("justify", v).apply() }
    fun putKeepOn(v: Boolean) { keepOn = v; sp.edit().putBoolean("keepOn", v).apply() }
    fun putHeadings(v: Boolean) { headings = v; sp.edit().putBoolean("headings", v).apply() }
    fun putBibleTr(v: Int) { bibleTr = v; sp.edit().putInt("tr", v).apply() }
    fun putCompareTrs(v: List<Int>) { compareTrs = v; sp.edit().putString("cmp", v.joinToString(",")).apply() }

    // ---- dernières positions
    fun rememberHymn(lang: String, num: Int) {
        sp.edit().putString("lastHymnLang", lang).putInt("lastHymnNum", num).apply()
    }

    fun lastHymn(): Pair<String, Int>? {
        val l = sp.getString("lastHymnLang", null) ?: return null
        val n = sp.getInt("lastHymnNum", 0)
        return if (n > 0) l to n else null
    }

    fun rememberBible(book: Int, chapter: Int) {
        sp.edit().putInt("lastBook", book).putInt("lastChapter", chapter).apply()
    }

    fun lastBible(): Pair<Int, Int> = sp.getInt("lastBook", 400) to sp.getInt("lastChapter", 1)
}
