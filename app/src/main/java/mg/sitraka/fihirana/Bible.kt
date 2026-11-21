@file:OptIn(ExperimentalMaterial3Api::class)

package mg.sitraka.fihirana

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ------------------------------------------------------------------ utilitaires

/** "Josoa 24:1-3,5" */
fun refString(trId: Int, book: Int, chapter: Int, verses: List<Int>): String {
    val name = Repo.bookName(trId, book)
    if (verses.isEmpty()) return "$name $chapter"
    val sorted = verses.sorted()
    val parts = ArrayList<String>()
    var start = sorted[0]
    var prev = start
    for (i in 1..sorted.size) {
        val cur = if (i < sorted.size) sorted[i] else -1
        if (cur != prev + 1) {
            parts.add(if (start == prev) "$start" else "$start-$prev")
            start = cur
        }
        prev = cur
    }
    return "$name $chapter:${parts.joinToString(",")}"
}

private val TrColors = listOf(
    Color(0xFF0F766E), Color(0xFF3949AB), Color(0xFFB45309), Color(0xFFBE185D), Color(0xFF15803D), Color(0xFF6D28D9)
)

fun highlighted(text: String, terms: List<String>, color: Color): AnnotatedString = buildAnnotatedString {
    val words = text.split(' ')
    words.forEachIndexed { i, w ->
        if (i > 0) append(' ')
        val n = Norm.of(w)
        if (n.isNotEmpty() && terms.any { n.contains(it) }) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color, background = color.copy(alpha = 0.15f))) { append(w) }
        } else append(w)
    }
}

// ------------------------------------------------------------------ Lecture

@Composable
private fun VerseRow(
    v: Verse,
    rtl: Boolean,
    selected: Boolean,
    colorIdx: Int,
    bookmarked: Boolean,
    hasNote: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    val scale = Prefs.fontScale
    CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Column(Modifier.fillMaxWidth()) {
            if (Prefs.headings && !v.heading.isNullOrBlank()) {
                Text(
                    v.heading,
                    color = ex.heading,
                    fontWeight = FontWeight.Medium,
                    fontSize = (18 * scale).sp,
                    lineHeight = (26 * scale).sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
                )
            }
            val bg = when {
                selected -> cs.primary.copy(alpha = 0.16f)
                colorIdx in 1..HighlightColors.size -> HighlightColors[colorIdx - 1]
                else -> Color.Transparent
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .then(if (selected) Modifier.border(1.5.dp, cs.primary, RoundedCornerShape(12.dp)) else Modifier)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    v.verse.toString(),
                    color = ex.verseNum,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = (15 * scale).sp,
                    modifier = Modifier.width(30.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        v.text,
                        fontSize = (18 * scale).sp,
                        lineHeight = (29 * scale).sp,
                        textAlign = if (Prefs.justify) TextAlign.Justify else TextAlign.Start,
                    )
                    if (!v.note.isNullOrBlank()) {
                        Text(v.note, color = ex.muted, fontSize = (13 * scale).sp, fontStyle = FontStyle.Italic)
                    }
                }
                if (bookmarked) Icon(Ico.BookmarkFilled, null, tint = cs.primary, modifier = Modifier.padding(start = 6.dp).size(16.dp))
                if (hasNote) Icon(Ico.Note, null, tint = ex.muted, modifier = Modifier.padding(start = 6.dp).size(16.dp))
            }
        }
    }
}

@Composable
fun BibleReadScreen(book0: Int, chapter0: Int, verse0: Int) {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    val scope = rememberCoroutineScope()
    var book by remember { mutableStateOf(book0) }
    var chapter by remember { mutableStateOf(chapter0) }
    val trId = Prefs.bibleTr
    val tr = Repo.tr(trId)
    val verses by produceState(emptyList<Verse>(), trId, book, chapter) {
        value = withContext(Dispatchers.IO) { Repo.chapter(trId, book, chapter) }
    }
    val audio by produceState(emptyList<String>(), trId, book, chapter) {
        value = withContext(Dispatchers.IO) { Repo.audioUrls(trId, book, chapter) }
    }
    var selected by remember(book, chapter, trId) { mutableStateOf(setOf<Int>()) }
    var picker by remember { mutableStateOf(false) }
    var showColors by remember { mutableStateOf(false) }
    var noteFor by remember { mutableStateOf<Long?>(null) }
    var jump by remember { mutableStateOf(verse0) }
    val listState = rememberLazyListState()
    LaunchedEffect(book, chapter) { Prefs.rememberBible(book, chapter) }
    LaunchedEffect(verses) {
        if (jump > 0 && verses.isNotEmpty()) {
            val i = verses.indexOfFirst { it.verse == jump }
            if (i >= 0) listState.scrollToItem(i + 2)
            jump = 0
        }
    }

    fun go(b: Int, c: Int) {
        book = b; chapter = c; jump = 0; showColors = false
        scope.launch { listState.scrollToItem(0) }
    }

    fun step(d: Int) {
        val idx = Repo.books.indexOfFirst { it.book == book }
        var b = book
        var c = chapter + d
        if (c < 1) {
            if (idx <= 0) return
            b = Repo.books[idx - 1].book
            c = Repo.chapterCount(b)
        } else if (c > Repo.chapterCount(book)) {
            if (idx >= Repo.books.size - 1) return
            b = Repo.books[idx + 1].book
            c = 1
        }
        go(b, c)
    }

    val bookName = Repo.bookName(trId, book)
    val base = lerp(parseColor(Repo.bookOf(book).color), Color(0xFF0B1220), 0.5f)
    val audioKey = "b:$trId:$book:$chapter"
    val selList = verses.filter { it.verse in selected }
    val selKeys = selList.map { it.key }

    HeroScreen(
        title = "$bookName $chapter",
        heroHeight = 270.dp,
        seed = book / 10,
        base = base,
        listState = listState,
        modifier = Modifier.swipeNav("$book:$chapter", { step(-1) }, { step(1) }),
        contentPadding = PaddingValues(bottom = 150.dp),
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (audio.isNotEmpty()) {
                    val on = Audio.key == audioKey && Audio.playing
                    GlassButton(if (on) Ico.Pause else Icons.Default.PlayArrow, "Henoy") {
                        Audio.play(audioKey, "$bookName $chapter · ${tr.short}", audio)
                    }
                }
                GlassButton(Ico.Compare, "Fampitahana") { nav.go(Screen.Compare(book, chapter)) }
                GlassButton(Icons.Default.Search, "Hikaroka") { nav.go(Screen.BibleSearch) }
            }
        },
        heroContent = {
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 18.dp, vertical = 18.dp)) {
                Row(Modifier.clickable { picker = true }, verticalAlignment = Alignment.CenterVertically) {
                    Text(bookName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 34.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("$chapter", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 34.sp)
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Text(tr.name, color = Color(0xCCFFFFFF), fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
            }
        },
        overlay = {
            AnimatedVisibility(
                visible = selected.isNotEmpty(),
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Column {
                    if (showColors) {
                        Row(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(cs.surface)
                                .border(1.dp, cs.outlineVariant, RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HighlightColors.forEachIndexed { i, c ->
                                Box(
                                    Modifier.size(34.dp).clip(CircleShape).background(c).border(1.dp, cs.outline, CircleShape)
                                        .clickable { UserStore.setColor(selKeys, i + 1); showColors = false; selected = emptySet() }
                                )
                            }
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).border(1.dp, cs.outline, CircleShape)
                                    .clickable { UserStore.setColor(selKeys, 0); showColors = false; selected = emptySet() },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Close, "Fafana", modifier = Modifier.size(18.dp)) }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = cs.surface,
                        shadowElevation = 18.dp,
                        tonalElevation = 3.dp
                    ) {
                        val allFav = selKeys.isNotEmpty() && selKeys.all { UserStore.isFavVerse(it) }
                        val refText = refString(trId, book, chapter, selected.toList())
                        fun textOut() = refText + " (" + tr.short + ")\n" +
                            selList.joinToString("\n") { "${it.verse}. ${it.text}" }
                        Row(
                            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            SelAction(Ico.Copy, "Tahaka") {
                                clipboard.setText(AnnotatedString(textOut()))
                                Toast.makeText(ctx, "Voasoratra: $refText", Toast.LENGTH_SHORT).show()
                                selected = emptySet()
                            }
                            SelAction(Icons.Default.Share, "Zarao") {
                                val i = Intent(Intent.ACTION_SEND)
                                i.type = "text/plain"
                                i.putExtra(Intent.EXTRA_TEXT, textOut() + "\n— Fihirana & Baiboly")
                                ctx.startActivity(Intent.createChooser(i, "Zarao"))
                            }
                            SelAction(if (allFav) Ico.BookmarkFilled else Ico.Bookmark, "Tiana", active = allFav) {
                                UserStore.toggleVerses(selKeys)
                                selected = emptySet()
                            }
                            SelAction(Ico.Highlight, "Loko", active = showColors) { showColors = !showColors }
                            if (selKeys.size == 1) SelAction(Ico.Note, "Naoty") { noteFor = selKeys[0] }
                            SelAction(Ico.Compare, "Hampitaha") {
                                val mn = selected.minOrNull() ?: 0
                                val mx = selected.maxOrNull() ?: 0
                                nav.go(Screen.Compare(book, chapter, mn, mx))
                            }
                            SelAction(Icons.Default.Close, "Akatony") { selected = emptySet(); showColors = false }
                        }
                    }
                }
            }
        },
        body = {
            item(key = "trs") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Repo.trs, key = { it.id }) { t ->
                        Chip(t.short, t.id == trId, { Prefs.putBibleTr(t.id) })
                    }
                }
            }
            if (verses.isEmpty()) {
                item(key = "empty") { EmptyNote("Tsy misy andininy amin'ity dikan-teny ity ho an'ity toko ity") }
            }
            items(verses, key = { it.verse }) { v ->
                VerseRow(
                    v = v,
                    rtl = tr.rtl,
                    selected = v.verse in selected,
                    colorIdx = UserStore.colors[v.key] ?: 0,
                    bookmarked = UserStore.isFavVerse(v.key),
                    hasNote = UserStore.notes.containsKey(v.key),
                    onClick = {
                        selected = if (v.verse in selected) selected - v.verse else selected + v.verse
                        if (selected.isEmpty()) showColors = false
                    }
                )
            }
            item(key = "nav") {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NavButton("Toko teo aloha", Icons.Default.KeyboardArrowLeft, true, Modifier.weight(1f)) { step(-1) }
                    NavButton("Toko manaraka", Icons.Default.KeyboardArrowRight, false, Modifier.weight(1f)) { step(1) }
                }
                Text(
                    "Tsindrio ny andininy iray na maromaro mba hahafahana mandrafitra, mamaritra loko, manampy naoty na mampitaha.",
                    color = ex.muted, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
                )
            }
        }
    )

    if (picker) {
        BiblePickerSheet(trId, book, chapter, onPick = { b, c -> picker = false; go(b, c) }, onDismiss = { picker = false })
    }
    noteFor?.let { k ->
        var text by remember(k) { mutableStateOf(UserStore.notes[k] ?: "") }
        AlertDialog(
            onDismissRequest = { noteFor = null },
            title = { Text("Naoty · ${refString(trId, vkBook(k), vkChapter(k), listOf(vkVerse(k)))}") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    placeholder = { Text("Soraty eto ny naotinao…") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { UserStore.setNote(k, text); noteFor = null; selected = emptySet() }) { Text("Tehirizina") }
            },
            dismissButton = { TextButton(onClick = { noteFor = null }) { Text("Aoka ihany") } }
        )
    }
}

@Composable
private fun SelAction(icon: ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (active) cs.primary else cs.onSurface, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp, color = if (active) cs.primary else cs.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun NavButton(text: String, icon: ImageVector, left: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (left) Icon(icon, null, tint = cs.primary)
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        if (!left) Icon(icon, null, tint = cs.primary)
    }
}

// ------------------------------------------------------------------ Sélecteur livre / chapitre

@Composable
fun BiblePickerSheet(trId: Int, curBook: Int, curChapter: Int, onPick: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableStateOf(0) }
    var pb by remember { mutableStateOf(curBook) }
    val h = (LocalConfiguration.current.screenHeightDp * 0.78f).dp
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = cs.surface) {
        if (step == 0) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(h)
            ) {
                val old = Repo.books.filter { it.isOld }
                val new = Repo.books.filter { !it.isOld }
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Testamenta Taloha") }
                gridItems(old, key = { it.book }) { b -> BookCell(b, trId, b.book == curBook) { pb = b.book; step = 1 } }
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Testamenta Vaovao") }
                gridItems(new, key = { it.book }) { b -> BookCell(b, trId, b.book == curBook) { pb = b.book; step = 1 } }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
            }
        } else {
            val n = Repo.chapterCount(pb)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(cs.surfaceVariant).clickable { step = 0 },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.KeyboardArrowLeft, "Miverina") }
                Text(
                    Repo.bookName(trId, pb), fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(62.dp),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(h - 48.dp)
            ) {
                gridItems((1..n).toList(), key = { it }) { c ->
                    val cur = pb == curBook && c == curChapter
                    Box(
                        Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (cur) cs.primary else cs.surfaceVariant)
                            .clickable { onPick(pb, c) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$c", fontWeight = FontWeight.SemiBold,
                            color = if (cur) cs.onPrimary else cs.onSurface
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun BookCell(b: BibleBook, trId: Int, current: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (current) cs.primaryContainer else cs.surfaceVariant)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(7.dp).height(52.dp).background(parseColor(b.color)))
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(Repo.bookName(trId, b.book), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${b.abbr} · ${b.chapters}", fontSize = 11.sp, color = LocalExtra.current.muted, maxLines = 1)
        }
    }
}

// ------------------------------------------------------------------ Comparaison

@Composable
fun CompareScreen(book0: Int, chapter0: Int, from0: Int, to0: Int) {
    val ex = LocalExtra.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var book by remember { mutableStateOf(book0) }
    var chapter by remember { mutableStateOf(chapter0) }
    var from by remember { mutableStateOf(from0) }
    var to by remember { mutableStateOf(to0) }
    var picker by remember { mutableStateOf(false) }
    val chosen = Prefs.compareTrs.filter { id -> Repo.trs.any { it.id == id } }.sorted()
    val data by produceState(emptyMap<Int, Map<Int, Verse>>(), chosen, book, chapter) {
        value = withContext(Dispatchers.IO) { Repo.compare(chosen, book, chapter) }
    }
    val listState = rememberLazyListState()
    val allVerses = data.values.flatMap { it.keys }.toSortedSet().toList()
    val range = if (from > 0) from..(if (to > 0) to else from) else null
    val shown = if (range != null) allVerses.filter { it in range } else allVerses
    val trId = Prefs.bibleTr

    fun step(d: Int) {
        val idx = Repo.books.indexOfFirst { it.book == book }
        var b = book
        var c = chapter + d
        if (c < 1) {
            if (idx <= 0) return
            b = Repo.books[idx - 1].book; c = Repo.chapterCount(b)
        } else if (c > Repo.chapterCount(book)) {
            if (idx >= Repo.books.size - 1) return
            b = Repo.books[idx + 1].book; c = 1
        }
        book = b; chapter = c; from = 0; to = 0
        scope.launch { listState.scrollToItem(0) }
    }

    HeroScreen(
        title = "Fampitahana · ${refString(trId, book, chapter, emptyList())}",
        heroHeight = 230.dp,
        seed = 91,
        listState = listState,
        modifier = Modifier.swipeNav("$book:$chapter", { step(-1) }, { step(1) }),
        heroContent = {
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                Text("Fampitahana Baiboly", color = Color(0xCCFFFFFF), fontSize = 14.sp)
                Row(Modifier.clickable { picker = true }, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (range != null) refString(trId, book, chapter, range.toList())
                        else refString(trId, book, chapter, emptyList()),
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        },
        body = {
            item(key = "ctrl") {
                Column(Modifier.padding(vertical = 12.dp)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(Repo.trs, key = { it.id }) { t ->
                            val on = t.id in chosen
                            Chip(t.short, on, {
                                val l = if (on) chosen - t.id else chosen + t.id
                                if (l.isNotEmpty()) Prefs.putCompareTrs(l.sorted())
                            })
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip("‹ Toko teo aloha", false, { step(-1) })
                        Chip("Toko manaraka ›", false, { step(1) })
                        if (range != null) Chip("Toko manontolo", false, { from = 0; to = 0 })
                    }
                }
            }
            if (shown.isEmpty()) item(key = "none") { EmptyNote("Tsy misy andininy") }
            items(shown, key = { it }) { vn ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(cs.surface)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Andininy $vn", color = ex.verseNum, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    for (t in chosen) {
                        val tr = Repo.tr(t)
                        val v = data[t]?.get(vn)
                        val color = TrColors[(t - 1).coerceIn(0, TrColors.size - 1)]
                        CompositionLocalProvider(
                            LocalLayoutDirection provides if (tr.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    tr.short, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(top = 4.dp, end = 10.dp, start = 0.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(color)
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                                Text(
                                    v?.text ?: "—",
                                    fontSize = (16 * Prefs.fontScale).sp,
                                    lineHeight = (25 * Prefs.fontScale).sp,
                                    color = if (v == null) ex.muted else cs.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
    if (picker) {
        BiblePickerSheet(trId, book, chapter, onPick = { b, c -> picker = false; book = b; chapter = c; from = 0; to = 0 }, onDismiss = { picker = false })
    }
}

// ------------------------------------------------------------------ Recherche

@Composable
fun BibleSearchScreen() {
    val nav = LocalNav.current
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    var query by remember { mutableStateOf("") }
    var trId by remember { mutableStateOf(Prefs.bibleTr) }
    var scopeIdx by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf(SearchResult(emptyList(), 0)) }
    var loading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    val q = query.trim()
    val terms = remember(q) { Norm.terms(q) }
    val ref = remember(q, trId) { if (q.any { it.isDigit() }) Repo.resolveRef(trId, q) else null }

    LaunchedEffect(Unit) { delay(300); try { focus.requestFocus() } catch (_: Exception) {} }
    LaunchedEffect(q, trId, scopeIdx) {
        if (q.length < 2) { result = SearchResult(emptyList(), 0); loading = false; return@LaunchedEffect }
        delay(350)
        loading = true
        val (a, b) = when (scopeIdx) { 1 -> 10 to 390; 2 -> 400 to 660; else -> 10 to 660 }
        result = withContext(Dispatchers.IO) { Repo.searchBible(trId, q, a, b) }
        loading = false
    }

    HeroScreen(
        title = "Fikarohana",
        heroHeight = 220.dp,
        seed = 12,
        listState = listState,
        heroContent = {
            Column(Modifier.align(Alignment.BottomStart)) {
                HeroTitle("Fikarohana ao amin'ny Baiboly", Repo.tr(trId).name)
                SearchPill(
                    value = query, onValue = { query = it },
                    hint = "Teny, andian-teny na toerana (Jaona 3:16)",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                    focusRequester = focus
                )
            }
        },
        body = {
            item(key = "filters") {
                Column(Modifier.padding(vertical = 12.dp)) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Repo.trs, key = { it.id }) { t -> Chip(t.short, t.id == trId, { trId = t.id }) }
                    }
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip("Rehetra", scopeIdx == 0, { scopeIdx = 0 })
                        Chip("Testamenta Taloha", scopeIdx == 1, { scopeIdx = 1 })
                        Chip("Testamenta Vaovao", scopeIdx == 2, { scopeIdx = 2 })
                    }
                }
            }
            if (ref != null) item(key = "ref") {
                val label = refString(
                    trId, ref.book, ref.chapter,
                    if (ref.verse > 0) (if (ref.verseEnd > 0) (ref.verse..ref.verseEnd).toList() else listOf(ref.verse)) else emptyList()
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.primaryContainer)
                        .clickable {
                            Prefs.putBibleTr(trId)
                            nav.go(Screen.BibleRead(ref.book, ref.chapter, ref.verse))
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Ico.Book, null, tint = cs.primary)
                    Text("Hamaky: $label", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = cs.primary)
                }
            }
            if (loading) item(key = "load") {
                Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (q.length >= 2) {
                item(key = "count") {
                    Text(
                        if (result.total == 0) "Tsy nahita valiny" else
                            "${result.total} valiny" + if (result.total > result.hits.size) " (aseho ny ${result.hits.size} voalohany)" else "",
                        color = ex.muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                items(result.hits, key = { it.key }) { v ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                Prefs.putBibleTr(trId)
                                nav.go(Screen.BibleRead(v.book, v.chapter, v.verse))
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            refString(trId, v.book, v.chapter, listOf(v.verse)),
                            color = cs.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp
                        )
                        Spacer(Modifier.height(3.dp))
                        CompositionLocalProvider(
                            LocalLayoutDirection provides if (Repo.tr(trId).rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                        ) {
                            Text(
                                highlighted(v.text, terms, cs.primary),
                                fontSize = (15 * Prefs.fontScale).sp, lineHeight = (23 * Prefs.fontScale).sp,
                                maxLines = 5, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                item(key = "hint") {
                    EmptyNote("Soraty ny teny na andian-teny tadiavina.\nHo an'ny toerana: « Josoa 24 » na « Jaona 3:16 ».")
                }
            }
        }
    )
}
