package mg.sitraka.fihirana

data class HymnLang(val code: String, val name: String, val short: String)

data class Hymn(
    val lang: String,
    val num: Int,
    val title: String,
    val tnorm: String,
    val key: String,
    val author: String,
    val theme: Int,
    val sub: Int,
    val url: String,
    val dl: String,
) {
    val id: String get() = "$lang:$num"
    val hasAudio: Boolean get() = url.isNotBlank() || dl.isNotBlank()
}

data class Stanza(val idx: Int, val kind: String, val text: String) {
    val isRefrain: Boolean get() = kind == "r"
}

data class Theme(val id: Int, val name: String, val parent: String?)

data class ThemeGroup(val title: String, val items: List<Theme>)

data class LyricHit(val hymn: Hymn, val snippet: String)

data class BibleTr(
    val id: Int,
    val code: String,
    val name: String,
    val short: String,
    val lang: String,
    val rtl: Boolean,
)

data class BibleBook(val book: Int, val abbr: String, val color: String, val chapters: Int, val ord: Int) {
    val isOld: Boolean get() = book < 400
}

data class Verse(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val heading: String?,
    val note: String?,
) {
    val key: Long get() = vkey(book, chapter, verse)
}

data class SearchResult(val hits: List<Verse>, val total: Int)

data class RefTarget(val book: Int, val chapter: Int, val verse: Int, val verseEnd: Int)

fun vkey(book: Int, chapter: Int, verse: Int): Long =
    book.toLong() * 1_000_000L + chapter.toLong() * 1000L + verse.toLong()

fun vkBook(k: Long): Int = (k / 1_000_000L).toInt()
fun vkChapter(k: Long): Int = ((k / 1000L) % 1000L).toInt()
fun vkVerse(k: Long): Int = (k % 1000L).toInt()
