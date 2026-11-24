package mg.sitraka.fihirana

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream

/** Accès en lecture seule à la base de contenu (cantiques + Bibles) livrée dans les assets. */
object Repo {
    private const val ASSET = "fihirana_baiboly.db"
    private const val CONTENT_VERSION = 2

    lateinit var db: SQLiteDatabase
        private set

    var langs: List<HymnLang> = emptyList(); private set
    private var hymnsByLang: Map<String, List<Hymn>> = emptyMap()
    private var hymnIndex: Map<String, Hymn> = emptyMap()
    private var themesByLang: Map<String, List<Theme>> = emptyMap()
    var trs: List<BibleTr> = emptyList(); private set
    var books: List<BibleBook> = emptyList(); private set
    private var names: Map<Int, Map<Int, String>> = emptyMap()
    private var bookIndex: List<Pair<String, Int>> = emptyList()

    /** Bloquant : à appeler hors du thread principal. */
    fun prepare(ctx: Context) {
        if (this::db.isInitialized && db.isOpen) return
        val f = ctx.getDatabasePath("content.db")
        val sp = ctx.getSharedPreferences("repo", Context.MODE_PRIVATE)
        if (!f.exists() || sp.getInt("ver", 0) != CONTENT_VERSION) {
            f.parentFile?.mkdirs()
            val tmp = File(f.parentFile, "content.tmp")
            ctx.assets.open(ASSET).use { input ->
                FileOutputStream(tmp).use { out -> input.copyTo(out, 64 * 1024) }
            }
            if (f.exists()) f.delete()
            if (!tmp.renameTo(f)) throw IllegalStateException("Tsy afaka namorona ny angona")
            sp.edit().putInt("ver", CONTENT_VERSION).commit()
        }
        db = SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY)
        loadCaches()
    }

    private inline fun <T> query(sql: String, args: Array<String> = emptyArray(), f: (Cursor) -> T): List<T> {
        val out = ArrayList<T>()
        db.rawQuery(sql, args).use { c -> while (c.moveToNext()) out.add(f(c)) }
        return out
    }

    private fun Cursor.str(i: Int): String = getString(i) ?: ""

    private fun loadCaches() {
        langs = query("SELECT code,name,short FROM hymn_langs ORDER BY ord") { HymnLang(it.str(0), it.str(1), it.str(2)) }
        val all = query("SELECT lang,num,title,tnorm,key,author,theme,sub,url,dl FROM hymns ORDER BY lang,num") {
            Hymn(it.str(0), it.getInt(1), it.str(2), it.str(3), it.str(4), it.str(5), it.getInt(6), it.getInt(7), it.str(8), it.str(9))
        }
        hymnsByLang = all.groupBy { it.lang }
        hymnIndex = all.associateBy { it.id }
        themesByLang = query("SELECT lang,id,name,parent FROM hymn_themes ORDER BY lang,id") {
            it.str(0) to Theme(it.getInt(1), it.str(2), it.getString(3))
        }.groupBy({ it.first }, { it.second })

        trs = query("SELECT id,code,name,short,lang,rtl FROM bible_tr ORDER BY ord") {
            BibleTr(it.getInt(0), it.str(1), it.str(2), it.str(3), it.str(4), it.getInt(5) == 1)
        }
        books = query("SELECT book,abbr,color,chapters,ord FROM bible_books ORDER BY ord") {
            BibleBook(it.getInt(0), it.str(1), it.str(2), it.getInt(3), it.getInt(4))
        }
        val nm = HashMap<Int, HashMap<Int, String>>()
        query("SELECT tr,book,name FROM bible_book_names") { Triple(it.getInt(0), it.getInt(1), it.str(2)) }
            .forEach { (tr, b, n) -> nm.getOrPut(tr) { HashMap() }[b] = n }
        names = nm
        val idx = ArrayList<Pair<String, Int>>()
        for (b in books) idx.add(Norm.of(b.abbr) to b.book)
        for (tr in trs) for ((b, n) in names[tr.id].orEmpty()) idx.add(Norm.of(n) to b)
        bookIndex = idx
    }

    // ------------------------------------------------------------------ Fihirana
    fun hymnList(lang: String): List<Hymn> = hymnsByLang[lang].orEmpty()
    fun hymn(lang: String, num: Int): Hymn? = hymnIndex["$lang:$num"]
    fun count(lang: String): Int = hymnsByLang[lang]?.size ?: 0

    fun themeGroups(lang: String): List<ThemeGroup> {
        val themes = themesByLang[lang].orEmpty()
        val groups = LinkedHashMap<String, MutableList<Theme>>()
        val roots = HashMap<String, Theme>()
        for (t in themes) {
            if (t.parent != null) groups.getOrPut(t.parent) { ArrayList() }.add(t)
            else {
                groups.getOrPut(t.name) { ArrayList() }
                roots[t.name] = t
            }
        }
        return groups.map { (title, items) ->
            if (items.isEmpty()) ThemeGroup(title, listOf(roots.getValue(title))) else ThemeGroup(title, items)
        }
    }

    fun stanzas(lang: String, num: Int): List<Stanza> =
        query("SELECT idx,kind,text FROM stanzas WHERE lang=? AND num=? ORDER BY idx", arrayOf(lang, num.toString())) {
            Stanza(it.getInt(0), it.str(1), it.str(2))
        }

    fun sameMelody(num: Int): List<Int> =
        query("SELECT DISTINCT other FROM melodies WHERE lang='mg' AND num=? ORDER BY other", arrayOf(num.toString())) { it.getInt(0) }

    fun lyricsSearch(lang: String, q: String, limit: Int = 200): List<LyricHit> {
        val terms = Norm.terms(q)
        if (terms.isEmpty()) return emptyList()
        val where = terms.joinToString(" AND ") { "norm LIKE ?" }
        val args = ArrayList<String>()
        args.add(lang)
        terms.forEach { args.add("%$it%") }
        val rows = query("SELECT num,text FROM stanzas WHERE lang=? AND $where ORDER BY num,idx LIMIT 600", args.toTypedArray()) {
            it.getInt(0) to it.str(1)
        }
        val seen = HashSet<Int>()
        val out = ArrayList<LyricHit>()
        val phrase = Norm.of(q)
        for ((num, text) in rows) {
            if (!seen.add(num)) continue
            val h = hymn(lang, num) ?: continue
            val lines = text.split('\n')
            val line = lines.firstOrNull { l -> val n = Norm.of(l); terms.any { n.contains(it) } } ?: lines.first()
            out.add(LyricHit(h, line))
            if (out.size >= limit) break
        }
        return if (terms.size > 1) out.sortedBy { if (Norm.of(it.snippet).contains(phrase)) 0 else 1 } else out
    }

    // ------------------------------------------------------------------ Baiboly
    fun tr(id: Int): BibleTr = trs.firstOrNull { it.id == id } ?: trs.first()
    fun bookOf(b: Int): BibleBook = books.firstOrNull { it.book == b } ?: books.first()

    fun bookName(tr: Int, book: Int): String = names[tr]?.get(book) ?: names[1]?.get(book) ?: book.toString()

    fun chapter(tr: Int, book: Int, chapter: Int): List<Verse> =
        query(
            "SELECT book,chapter,verse,text,heading,note FROM bible_verses WHERE tr=? AND book=? AND chapter=? ORDER BY verse",
            arrayOf(tr.toString(), book.toString(), chapter.toString())
        ) { Verse(it.getInt(0), it.getInt(1), it.getInt(2), it.str(3), it.getString(4), it.getString(5)) }

    fun verse(tr: Int, book: Int, chapter: Int, verse: Int): Verse? =
        query(
            "SELECT book,chapter,verse,text,heading,note FROM bible_verses WHERE tr=? AND book=? AND chapter=? AND verse=?",
            arrayOf(tr.toString(), book.toString(), chapter.toString(), verse.toString())
        ) { Verse(it.getInt(0), it.getInt(1), it.getInt(2), it.str(3), it.getString(4), it.getString(5)) }.firstOrNull()

    /** Pour la comparaison : tr -> (verset -> texte). */
    fun compare(trIds: List<Int>, book: Int, chapter: Int): Map<Int, Map<Int, Verse>> {
        val out = LinkedHashMap<Int, Map<Int, Verse>>()
        for (t in trIds) out[t] = chapter(t, book, chapter).associateBy { it.verse }
        return out
    }

    fun chapterCount(book: Int): Int = bookOf(book).chapters

    class AudioRow(val book: Int, val chapter: Int, val urls: List<String>)

    private val audioCache = HashMap<Int, List<AudioRow>>()

    fun audioTrs(): List<Int> = query("SELECT DISTINCT tr FROM bible_audio ORDER BY tr") { it.getInt(0) }

    fun audioRows(tr: Int): List<AudioRow> = audioCache.getOrPut(tr) {
        query("SELECT book,chapter,url,url2 FROM bible_audio WHERE tr=? ORDER BY book,chapter", arrayOf(tr.toString())) {
            AudioRow(it.getInt(0), it.getInt(1), listOf(it.str(2), it.str(3)).filter { u -> u.isNotBlank() })
        }
    }

    fun verseCount(tr: Int, book: Int, chapter: Int): Int =
        query("SELECT MAX(verse) FROM bible_verses WHERE tr=? AND book=? AND chapter=?",
            arrayOf(tr.toString(), book.toString(), chapter.toString())) { it.getInt(0) }.firstOrNull() ?: 0

    fun audioUrls(tr: Int, book: Int, chapter: Int): List<String> =
        query("SELECT url,url2 FROM bible_audio WHERE tr=? AND book=? AND chapter=?", arrayOf(tr.toString(), book.toString(), chapter.toString())) {
            listOf(it.str(0), it.str(1)).filter { u -> u.isNotBlank() }
        }.firstOrNull().orEmpty()

    fun searchBible(tr: Int, q: String, fromBook: Int, toBook: Int, limit: Int = 300): SearchResult {
        val terms = Norm.terms(q)
        if (terms.isEmpty()) return SearchResult(emptyList(), 0)
        val phrase = Norm.of(q)
        val hits = ArrayList<Verse>()
        var total = 0
        db.rawQuery(
            "SELECT book,chapter,verse,text,heading,note FROM bible_verses WHERE tr=? AND book BETWEEN ? AND ? ORDER BY book,chapter,verse",
            arrayOf(tr.toString(), fromBook.toString(), toBook.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val text = c.getString(3) ?: ""
                val n = Norm.of(text)
                var ok = true
                for (t in terms) if (!n.contains(t)) { ok = false; break }
                if (ok) {
                    total++
                    if (hits.size < limit) hits.add(Verse(c.getInt(0), c.getInt(1), c.getInt(2), text, c.getString(4), c.getString(5)))
                }
            }
        }
        val sorted = if (terms.size > 1) hits.sortedBy { if (Norm.of(it.text).contains(phrase)) 0 else 1 } else hits
        return SearchResult(sorted, total)
    }

    private val refRegex = Regex("""^\s*(.*?\D)\s*(\d{1,3})(?:\s*[:.,]\s*(\d{1,3})(?:\s*-\s*(\d{1,3}))?)?\s*$""")

    /** "Josoa 24", "Jaona 3:16", "1 Jaona 4:8-10" -> cible. */
    fun resolveRef(tr: Int, input: String): RefTarget? {
        val m = refRegex.find(input) ?: return null
        val nb = Norm.of(m.groupValues[1]).replace(" ", "")
        if (nb.length < 2) return null
        val ch = m.groupValues[2].toInt()
        val v1 = m.groupValues[3].toIntOrNull() ?: 0
        val v2 = m.groupValues[4].toIntOrNull() ?: 0
        val own = ArrayList<Pair<String, Int>>()
        for ((b, n) in names[tr].orEmpty()) own.add(Norm.of(n).replace(" ", "") to b)
        val pool = own + bookIndex.map { it.first.replace(" ", "") to it.second }
        val book = pool.firstOrNull { it.first == nb }?.second
            ?: pool.firstOrNull { it.first.startsWith(nb) }?.second
            ?: return null
        if (ch < 1 || ch > chapterCount(book)) return null
        return RefTarget(book, ch, v1, if (v2 > v1) v2 else 0)
    }
}
