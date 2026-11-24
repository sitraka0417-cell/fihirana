package mg.sitraka.fihirana

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

// ======================================================================= Liste
@Composable
fun HymnRow(h: Hymn, snippet: String? = null, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumberBadge(h.num.toString())
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(h.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val sub = snippet ?: listOf(h.author, h.key).filter { it.isNotBlank() }.joinToString("  ·  ")
            if (sub.isNotBlank()) {
                Text(sub, color = ex.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontStyle = if (snippet != null) FontStyle.Italic else FontStyle.Normal)
            }
        }
        if (UserStore.isFavHymn(h)) Icon(Icons.Default.Favorite, null, tint = cs.primary, modifier = Modifier.size(18.dp))
        if (h.hasAudio) {
            val got = AudioStore.has("h_${h.lang}_${h.num}.mp3")
            Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Ico.Music, null, tint = if (got) cs.primary else ex.muted, modifier = Modifier.size(17.dp))
                if (got) Icon(Ico.Check, null, tint = cs.primary, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun GroupHeader(title: String, count: Int, open: Boolean, level: Int, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = (16 + level * 12).dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (level == 0) cs.surface else cs.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, modifier = Modifier.weight(1f), fontWeight = if (level == 0) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (level == 0) 14.sp else 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        Text("$count", color = LocalExtra.current.muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
        Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = cs.primary)
    }
}

@Composable
fun HymnListScreen(lang: String) {
    val nav = LocalNav.current
    val ex = LocalExtra.current
    val hymns = remember(lang) { Repo.hymnList(lang) }
    val info = Repo.langs.firstOrNull { it.code == lang } ?: Repo.langs.first()
    var tab by remember(lang) { mutableStateOf(0) }
    var query by remember(lang) { mutableStateOf("") }
    var lyric by remember(lang) { mutableStateOf<List<LyricHit>>(emptyList()) }
    var pad by remember { mutableStateOf(false) }
    var focusTick by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    val q = query.trim()

    LaunchedEffect(q, lang) {
        if (q.length >= 3 && q.toIntOrNull() == null) {
            delay(300)
            lyric = withContext(Dispatchers.IO) { Repo.lyricsSearch(lang, q) }
        } else lyric = emptyList()
    }
    LaunchedEffect(focusTick) {
        if (focusTick > 0) {
            listState.animateScrollToItem(0)
            delay(250)
            try { focus.requestFocus() } catch (_: Exception) {}
        }
    }
    val titleHits = remember(q, hymns) {
        if (q.isEmpty()) emptyList()
        else {
            val num = q.toIntOrNull()
            if (num != null) hymns.filter { it.num.toString().startsWith(q) }.take(120)
            else {
                val ts = Norm.terms(q)
                hymns.filter { h -> ts.all { h.tnorm.contains(it) } }.take(120)
            }
        }
    }
    val groups = remember(lang) { Repo.themeGroups(lang) }
    val bySub = remember(lang) { hymns.groupBy { if (lang == "mg") it.sub else it.theme } }
    var openG by remember(lang) { mutableStateOf(setOf<String>()) }
    var openT by remember(lang) { mutableStateOf(setOf<Int>()) }

    HeroScreen(
        title = info.name,
        heroHeight = 270.dp,
        image = Bg.hymn(lang, 0),
        seed = lang.hashCode(),
        base = heroColorFor(lang),
        listState = listState,
        actions = { GlassButton(Icons.Default.Search, "Hikaroka") { focusTick++ } },
        heroContent = {
            Column(Modifier.align(Alignment.BottomStart)) {
                HeroTitle(info.name, "${hymns.size} hira")
                SearchPill(
                    value = query, onValue = { query = it },
                    hint = "Laharana, lohateny na teny ao anatiny…",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                    focusRequester = focus
                )
            }
        },
        overlay = {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(18.dp)
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { pad = true },
                contentAlignment = Alignment.Center
            ) { Icon(Ico.Dialpad, "Laharana", tint = MaterialTheme.colorScheme.onPrimary) }
        },
        body = {
            item(key = "langs") {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (l in Repo.langs) Chip(l.short, l.code == lang, { if (l.code != lang) nav.replace(Screen.HymnList(l.code)) })
                    Spacer(Modifier.width(8.dp))
                    if (q.isEmpty()) {
                        Chip("Laharana", tab == 0, { tab = 0 })
                        Chip("Lohahevitra", tab == 1, { tab = 1 })
                    }
                }
            }
            if (q.isNotEmpty()) {
                if (titleHits.isEmpty() && lyric.isEmpty()) item(key = "none") { EmptyNote("Tsy nahita zavatra") }
                if (titleHits.isNotEmpty()) {
                    item(key = "st1") { SectionTitle("Lohateny / laharana", Modifier.padding(horizontal = 20.dp)) }
                    items(titleHits, key = { "t:" + it.id }) { h -> HymnRow(h) { nav.go(Screen.HymnView(h.lang, h.num)) } }
                }
                val shown = titleHits.map { it.num }.toSet()
                val lh = lyric.filter { it.hymn.num !in shown }
                if (lh.isNotEmpty()) {
                    item(key = "st2") { SectionTitle("Ao anatin'ny hira", Modifier.padding(horizontal = 20.dp)) }
                    items(lh, key = { "l:" + it.hymn.id }) { r -> HymnRow(r.hymn, r.snippet) { nav.go(Screen.HymnView(r.hymn.lang, r.hymn.num)) } }
                }
            } else if (tab == 0) {
                items(hymns, key = { it.id }) { h -> HymnRow(h) { nav.go(Screen.HymnView(h.lang, h.num)) } }
            } else {
                for (g in groups) {
                    val total = g.items.sumOf { bySub[it.id]?.size ?: 0 }
                    val gOpen = g.title in openG
                    item(key = "g:" + g.title) {
                        GroupHeader(g.title, total, gOpen, 0) { openG = if (gOpen) openG - g.title else openG + g.title }
                    }
                    if (gOpen) {
                        for (th in g.items) {
                            val hs = bySub[th.id].orEmpty()
                            val single = g.items.size == 1
                            val tOpen = th.id in openT
                            if (!single) item(key = "t:" + th.id) {
                                GroupHeader(th.name, hs.size, tOpen, 1) { openT = if (tOpen) openT - th.id else openT + th.id }
                            }
                            if (single || tOpen) items(hs, key = { "h:${th.id}:${it.num}" }) { h ->
                                HymnRow(h) { nav.go(Screen.HymnView(h.lang, h.num)) }
                            }
                        }
                    }
                }
            }
        }
    )
    if (pad) {
        NumberPadDialog(
            title = info.name,
            max = hymns.maxOfOrNull { it.num } ?: 1,
            exists = { Repo.hymn(lang, it) != null },
            onDismiss = { pad = false },
            onGo = { pad = false; nav.go(Screen.HymnView(lang, it)) }
        )
    }
}

// ======================================================================= Détail
private fun refrainLabel(lang: String) = when (lang) {
    "mg", "ak" -> "Isan'andininy"
    "fr" -> "Refrain"
    else -> "Chorus"
}

private fun lyricsText(h: Hymn, st: List<Stanza>): String {
    val sb = StringBuilder()
    sb.append("${h.num} - ${h.title}\n")
    if (h.key.isNotBlank()) sb.append(h.key).append('\n')
    var n = 0
    for (s in st) {
        sb.append('\n')
        if (s.isRefrain) sb.append("[${refrainLabel(h.lang)}]\n") else { n++; sb.append("$n.\n") }
        sb.append(s.text).append('\n')
    }
    sb.append("\n— Fihirana & Baiboly")
    return sb.toString()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HymnScreen(lang: String, num: Int) {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    val h = Repo.hymn(lang, num)
    if (h == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tsy hita ny hira") }
        return
    }
    val stanzas by produceState(emptyList<Stanza>(), lang, num) {
        value = withContext(Dispatchers.IO) { Repo.stanzas(lang, num) }
    }
    val melodies by produceState(emptyList<Int>(), lang, num) {
        value = if (lang == "mg") withContext(Dispatchers.IO) { Repo.sameMelody(num) } else emptyList()
    }
    val active = LocalPageActive.current
    val swipe = LocalSwipe.current
    LaunchedEffect(lang, num) { if (active) Prefs.rememberHymn(lang, num) }
    var pad by remember { mutableStateOf(false) }
    var present by remember { mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(present, active) {
        if (active) SwipeLock.locked = present
        onDispose { if (active) SwipeLock.locked = false }
    }
    var fontDlg by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scale = Prefs.fontScale
    val fav = UserStore.isFavHymn(h)
    val audioItem = remember(h) { AudioItems.hymn(h) }
    val heroDp = 330.dp
    val density = LocalDensity.current
    val heroPx = with(density) { heroDp.toPx() }
    val btnTop = with(density) { (heroDp - 44.dp).toPx() }
    val maxNum = remember(lang) { Repo.hymnList(lang).maxOfOrNull { it.num } ?: 1 }

    HeroScreen(
        title = "${h.num} - ${h.title}",
        heroHeight = heroDp,
        image = Bg.hymn(lang, num),
        seed = h.num * 31 + lang.hashCode(),
        base = heroColorFor(lang),
        listState = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 140.dp),
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(Icons.Default.Share, "Zarao") {
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "text/plain"
                    i.putExtra(Intent.EXTRA_TEXT, lyricsText(h, stanzas))
                    ctx.startActivity(Intent.createChooser(i, "Zarao ny hira"))
                }
            }
        },
        heroContent = {
            Column(Modifier.align(Alignment.BottomStart).padding(end = 76.dp)) {
                HeroTitle("${h.num} - ${h.title}")
                Row(
                    Modifier.padding(start = 22.dp, end = 12.dp, bottom = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (h.key.isNotBlank()) {
                        Text(
                            h.key, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x66FFFFFF))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (h.author.isNotBlank()) {
                        Text(h.author, color = Color(0xE6FFFFFF), fontSize = 14.sp, fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        overlay = {
            if (present && stanzas.isNotEmpty()) {
                PresentOverlay(h, stanzas) { present = false }
            } else {
            val off = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else Int.MAX_VALUE
            if (off < heroPx) {
                Column(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(0, (btnTop - off).toInt()) }
                        .padding(end = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoundAction(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Tiana", active = fav) { UserStore.toggleHymn(h) }
                    RoundAction(Ico.Present, "Fampisehoana") { if (stanzas.isNotEmpty()) present = true }
                    RoundAction(
                        if (Audio.key == audioItem.key && Audio.playing) Ico.Pause else Icons.Default.PlayArrow,
                        "Henoy", active = Audio.key == audioItem.key
                    ) { AudioUi.request(ctx, audioItem) }
                    if (audioItem.urls.isNotEmpty()) RoundAction(Ico.Download, "Rohy audio") { AudioUi.showLinks(audioItem) }
                }
            }
            Row(
                Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallRound(Icons.Default.KeyboardArrowLeft, "Teo aloha") { swipe.prev() }
                SmallRound(Icons.Default.KeyboardArrowRight, "Manaraka") { swipe.next() }
                SmallRound(null, "Aa", text = "Aa") { fontDlg = true }
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(18.dp)
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(cs.primary)
                    .clickable { pad = true },
                contentAlignment = Alignment.Center
            ) { Icon(Ico.Dialpad, "Laharana", tint = cs.onPrimary) }
            }
        },
        body = {
            item(key = "gap") { Spacer(Modifier.height(24.dp)) }
            var vn = 0
            items(stanzas, key = { it.idx }) { s ->
                val label = if (s.isRefrain) "R" else { "" }
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 76.dp, bottom = 26.dp)) {
                    val shownLabel = if (s.isRefrain) label else stanzas.filter { !it.isRefrain && it.idx <= s.idx }.size.toString()
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(cs.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { Text(shownLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ex.muted) }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        s.text,
                        fontSize = (21 * scale).sp,
                        lineHeight = (33 * scale).sp,
                        fontWeight = if (s.isRefrain) FontWeight.Medium else FontWeight.Normal,
                        fontStyle = if (s.isRefrain) FontStyle.Italic else FontStyle.Normal,
                        color = if (s.isRefrain) cs.primary else cs.onBackground
                    )
                }
            }
            if (melodies.isNotEmpty()) item(key = "mel") {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    SectionTitle("Mitovy feo")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (n in melodies) Chip("#$n", false, { nav.go(Screen.HymnView("mg", n)) })
                    }
                }
            }
        }
    )

    if (pad) {
        NumberPadDialog(
            title = Repo.langs.firstOrNull { it.code == lang }?.name ?: "",
            max = maxNum,
            exists = { Repo.hymn(lang, it) != null },
            onDismiss = { pad = false },
            onGo = { pad = false; nav.replace(Screen.HymnView(lang, it)) }
        )
    }
    androidx.activity.compose.BackHandler(present) { present = false }
    if (fontDlg) FontDialog { fontDlg = false }
}

@Composable
private fun SmallRound(icon: androidx.compose.ui.graphics.vector.ImageVector?, desc: String, text: String? = null, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(cs.surface)
            .border(1.dp, cs.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) Icon(icon, desc, tint = cs.onSurface)
        if (text != null) Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun FontDialog(onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(24.dp)).background(cs.surface).padding(22.dp)
        ) {
            Text("Haben'ny soratra", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Efa ananao ve ny fiadanan'ny fo?",
                fontSize = (20 * Prefs.fontScale).sp, lineHeight = (30 * Prefs.fontScale).sp
            )
            Spacer(Modifier.height(10.dp))
            Slider(
                value = Prefs.fontScale, onValueChange = { Prefs.putFontScale(it) }, valueRange = 0.8f..1.8f
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("A", fontSize = 13.sp, color = LocalExtra.current.muted)
                Text("${(Prefs.fontScale * 100).toInt()}%", fontSize = 13.sp, color = LocalExtra.current.muted)
                Text("A", fontSize = 24.sp, color = LocalExtra.current.muted)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresentOverlay(h: Hymn, stanzas: List<Stanza>, onClose: () -> Unit) {
    val pager = rememberPagerState(pageCount = { stanzas.size })
    val scope = rememberCoroutineScope()
    var showRef by remember { mutableStateOf(false) }
    var refIdx by remember { mutableStateOf(-1) }
    val refrain = if (refIdx in stanzas.indices) stanzas[refIdx] else stanzas.firstOrNull { it.isRefrain }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    run {
        Box(Modifier.fillMaxSize().background(Color(0xFF05070A)).pointerInput(Unit) { detectTapGestures { } }) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                val s = stanzas[page]
                val lines = s.text.count { it == '\n' } + 1
                val sp = when {
                    lines <= 4 -> 40
                    lines <= 6 -> 33
                    lines <= 8 -> 28
                    else -> 23
                }
                val label = if (s.isRefrain) refrainLabel(h.lang) else "${stanzas.filter { !it.isRefrain && it.idx <= s.idx }.size}"
                Column(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 70.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        label, color = if (s.isRefrain) Color(0xFF2DD4BF) else Color(0xFFFFB300),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        s.text, color = Color.White, textAlign = TextAlign.Center,
                        fontSize = sp.sp, lineHeight = (sp * 1.4f).sp,
                        fontStyle = if (s.isRefrain) FontStyle.Italic else FontStyle.Normal
                    )
                }
            }
            if (showRef && refrain != null) {
                val rl = refrain.text.count { it == '\n' } + 1
                val rsp = when {
                    rl <= 4 -> 40
                    rl <= 6 -> 33
                    rl <= 8 -> 28
                    else -> 23
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF05070A))
                        .pointerInput(Unit) { detectTapGestures { } }
                        .statusBarsPadding().navigationBarsPadding()
                        .padding(horizontal = 28.dp, vertical = 70.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(refrainLabel(h.lang), color = Color(0xFF2DD4BF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        refrain.text, color = Color.White, textAlign = TextAlign.Center,
                        fontSize = rsp.sp, lineHeight = (rsp * 1.4f).sp, fontStyle = FontStyle.Italic
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${h.num} · ${h.title}", color = Color(0xB3FFFFFF), fontSize = 14.sp,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text("${pager.currentPage + 1}/${stanzas.size}", color = Color(0xB3FFFFFF), fontSize = 14.sp)
                Spacer(Modifier.size(10.dp))
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color(0x33FFFFFF)).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Close, "Akatony", tint = Color.White) }
            }
            val aItem = remember(h) { AudioItems.hymn(h) }
            val aCtx = LocalContext.current
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 22.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (Audio.key == aItem.key) Color(0xFF2DD4BF) else Color(0x33FFFFFF))
                    .clickable { AudioUi.request(aCtx, aItem) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (Audio.key == aItem.key && Audio.playing) Ico.Pause else Icons.Default.PlayArrow, "Henoy",
                    tint = if (Audio.key == aItem.key) Color(0xFF05070A) else Color.White
                )
            }
            if (refrain != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, bottom = 22.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (showRef) Color(0xFF2DD4BF) else Color(0x33FFFFFF))
                        .clickable {
                            if (showRef) {
                                showRef = false
                                val cur = pager.currentPage
                                var nxt = -1
                                for (i in cur + 1 until stanzas.size) {
                                    if (!stanzas[i].isRefrain) { nxt = i; break }
                                }
                                if (nxt >= 0) scope.launch { pager.scrollToPage(nxt) }
                            } else {
                                // refrain qui correspond à la page courante :
                                // 1) la page elle-même, 2) le refrain juste après, 3) le dernier avant, 4) le premier après
                                val cur = pager.currentPage
                                var pick = -1
                                if (stanzas[cur].isRefrain) pick = cur
                                else if (cur + 1 < stanzas.size && stanzas[cur + 1].isRefrain) pick = cur + 1
                                if (pick < 0) {
                                    for (i in cur downTo 0) {
                                        if (stanzas[i].isRefrain) { pick = i; break }
                                    }
                                }
                                if (pick < 0) {
                                    for (i in cur until stanzas.size) {
                                        if (stanzas[i].isRefrain) { pick = i; break }
                                    }
                                }
                                refIdx = pick
                                showRef = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("R", color = if (showRef) Color(0xFF05070A) else Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
        }
    }
}
