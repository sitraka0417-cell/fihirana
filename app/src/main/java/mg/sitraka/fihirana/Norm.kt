package mg.sitraka.fihirana

import java.text.Normalizer

/** Normalisation de recherche : minuscules, sans accents, apostrophes/ponctuation -> espace.
 *  Doit rester identique à `norm()` de tools/build_db.py. */
object Norm {
    fun of(src: String): String {
        if (src.isEmpty()) return ""
        var ascii = true
        for (c in src) if (c.code > 127) { ascii = false; break }
        val low = src.lowercase()
        val s = if (ascii) low else Normalizer.normalize(low, Normalizer.Form.NFD)
        val sb = StringBuilder(s.length)
        var pendingSpace = false
        for (ch in s) {
            if (Character.getType(ch) == Character.NON_SPACING_MARK.toInt()) continue
            if (Character.isLetterOrDigit(ch) || ch == '_') {
                if (pendingSpace && sb.isNotEmpty()) sb.append(' ')
                pendingSpace = false
                sb.append(ch)
            } else {
                pendingSpace = true
            }
        }
        return sb.toString()
    }

    fun terms(q: String): List<String> = of(q).split(' ').filter { it.isNotEmpty() }
}
