package mg.sitraka.fihirana

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch

/** Vrai seulement pour la page affichée (les pages voisines pré-chargées sont « inactives »). */
val LocalPageActive = compositionLocalOf { true }

/** Permet aux boutons (précédent / suivant) de lancer la même animation que le balayage. */
class SwipeCtl {
    var prev: () -> Unit = {}
    var next: () -> Unit = {}
}

val LocalSwipe = staticCompositionLocalOf { SwipeCtl() }

/** Mis à true pendant la projection pour désactiver le balayage. */
object SwipeLock {
    var locked by mutableStateOf(false)
}

private fun chapterStep(book: Int, chapter: Int, d: Int): Pair<Int, Int>? {
    val idx = Repo.books.indexOfFirst { it.book == book }
    var b = book
    var c = chapter + d
    if (c < 1) {
        if (idx <= 0) return null
        b = Repo.books[idx - 1].book
        c = Repo.chapterCount(b)
    } else if (c > Repo.chapterCount(book)) {
        if (idx < 0 || idx >= Repo.books.size - 1) return null
        b = Repo.books[idx + 1].book
        c = 1
    }
    return b to c
}

fun neighbors(s: Screen): Pair<Screen?, Screen?> = when (s) {
    is Screen.HymnView -> Pair(
        if (Repo.hymn(s.lang, s.num - 1) != null) Screen.HymnView(s.lang, s.num - 1) else null,
        if (Repo.hymn(s.lang, s.num + 1) != null) Screen.HymnView(s.lang, s.num + 1) else null
    )
    is Screen.BibleRead -> Pair(
        chapterStep(s.book, s.chapter, -1)?.let { Screen.BibleRead(it.first, it.second) },
        chapterStep(s.book, s.chapter, 1)?.let { Screen.BibleRead(it.first, it.second) }
    )
    is Screen.Compare -> Pair(
        chapterStep(s.book, s.chapter, -1)?.let { Screen.Compare(it.first, it.second) },
        chapterStep(s.book, s.chapter, 1)?.let { Screen.Compare(it.first, it.second) }
    )
    else -> Pair(null, null)
}

/** Affiche la page courante + voisines ; la page suit le doigt (sortie d'une page, entrée de l'autre). */
@Composable
fun SwipeNav(
    current: Screen,
    prev: Screen?,
    next: Screen?,
    onCommit: (Screen) -> Unit,
    content: @Composable (Screen) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val off = remember { Animatable(0f) }
    var width by remember { mutableStateOf(1f) }
    val ctl = remember { SwipeCtl() }

    suspend fun settle() {
        val o = off.value
        val th = width * 0.22f
        if (o > th && prev != null) {
            off.animateTo(width, tween(240, easing = FastOutSlowInEasing))
            onCommit(prev)
            off.snapTo(0f)
        } else if (o < -th && next != null) {
            off.animateTo(-width, tween(240, easing = FastOutSlowInEasing))
            onCommit(next)
            off.snapTo(0f)
        } else {
            off.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        }
    }

    ctl.prev = {
        if (prev != null) scope.launch {
            off.animateTo(width, tween(260, easing = FastOutSlowInEasing))
            onCommit(prev)
            off.snapTo(0f)
        }
    }
    ctl.next = {
        if (next != null) scope.launch {
            off.animateTo(-width, tween(260, easing = FastOutSlowInEasing))
            onCommit(next)
            off.snapTo(0f)
        }
    }

    val locked = SwipeLock.locked
    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { width = it.width.toFloat() }
            .pointerInput(current, prev, next, locked) {
                if (locked) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { scope.launch { off.stop() } },
                    onDragEnd = { scope.launch { settle() } },
                    onDragCancel = { scope.launch { settle() } }
                ) { change, dx ->
                    change.consume()
                    scope.launch {
                        val lo = if (next != null) -width else 0f
                        val hi = if (prev != null) width else 0f
                        off.snapTo((off.value + dx).coerceIn(lo, hi))
                    }
                }
            }
    ) {
        val pages = listOfNotNull(prev, current, next)
        val ci = pages.indexOf(current)
        CompositionLocalProvider(LocalSwipe provides ctl) {
            for (p in pages) {
                key(p) {
                    val rel = pages.indexOf(p) - ci
                    Box(Modifier.fillMaxSize().graphicsLayer { translationX = rel * width + off.value }) {
                        CompositionLocalProvider(LocalPageActive provides (p == current)) { content(p) }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenHost(s: Screen) {
    val nav = LocalNav.current
    val nb = remember(s) { neighbors(s) }
    if (nb.first == null && nb.second == null) {
        ScreenPage(s)
    } else {
        SwipeNav(s, nb.first, nb.second, { nav.replace(it) }) { ScreenPage(it) }
    }
}
