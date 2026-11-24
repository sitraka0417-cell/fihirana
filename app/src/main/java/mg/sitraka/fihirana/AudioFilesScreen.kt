package mg.sitraka.fihirana

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class AudioCat(
    val id: String,
    val title: String,
    val subtitle: String,
    val items: List<AudioItem>,
    val byBook: Map<Int, List<AudioItem>>,
    val prefix: String,
    val tr: Int,
)

private fun chapterOf(item: AudioItem): String = item.key.substringAfterLast('_').removeSuffix(".mp3")

@Composable
fun AudioFilesScreen() {
    val ex = LocalExtra.current
    val listState = rememberLazyListState()
    val bibleCats = remember {
        Repo.audioTrs().map { tr ->
            val rows = Repo.audioRows(tr).filter { it.urls.isNotEmpty() }
            val items = rows.map { AudioItems.bible(tr, it.book, it.chapter, it.urls) }
            val byBook = LinkedHashMap<Int, MutableList<AudioItem>>()
            for ((i, r) in rows.withIndex()) byBook.getOrPut(r.book) { ArrayList() }.add(items[i])
            AudioCat("b$tr", Repo.tr(tr).name, "Baiboly · ${Repo.tr(tr).short}", items, byBook, "b${tr}_", tr)
        }
    }
    val hymnCats = remember {
        Repo.langs.mapNotNull { l ->
            val hs = Repo.hymnList(l.code).filter { it.hasAudio }
            if (hs.isEmpty()) null
            else AudioCat("h_${l.code}", l.name, "Fihirana", hs.map { AudioItems.hymn(it) }, emptyMap(), "h_${l.code}_", 0)
        }
    }
    val noAudio = remember {
        Repo.langs.filter { l -> Repo.hymnList(l.code).none { it.hasAudio } }.map { it.name }
    }
    var confirm by remember { mutableStateOf<AudioCat?>(null) }
    val used = remember(AudioStore.done) { AudioStore.usedBytes("") }

    HeroScreen(
        title = "Rakitra audio",
        heroHeight = 200.dp,
        image = Bg.hymns[3],
        seed = 55,
        listState = listState,
        heroContent = {
            Column(Modifier.align(Alignment.BottomStart)) {
                HeroTitle("Rakitra audio", "Voatahiry ao amin'ny finday: ${fmtSize(used)}")
            }
        },
        body = {
            item(key = "dl") { DownloadCard() }
            item(key = "h-bible") { SectionTitle("Baiboly", Modifier.padding(horizontal = 20.dp)) }
            for (c in bibleCats) {
                item(key = c.id) { CatCard(c, true) { confirm = c } }
            }
            item(key = "h-hymn") { SectionTitle("Fihirana", Modifier.padding(horizontal = 20.dp)) }
            for (c in hymnCats) {
                item(key = c.id) { CatCard(c, false) { confirm = c } }
            }
            if (noAudio.isNotEmpty()) item(key = "noaudio") {
                Text(
                    "Tsy misy rakitra audio ho an'ny: " + noAudio.joinToString(", "),
                    color = ex.muted, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            item(key = "tip") {
                Text(
                    "Fanamarihana: ny habe aseho dia tombana (≈) araka ny santionany. Mila aterineto ny fampidinana, ary aza akatona ny application mandritra ny fampidinana be.",
                    color = ex.muted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    )

    confirm?.let { c ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Hamafa ve?") },
            text = { Text("Hofafana ny rakitra audio voatahiry rehetra ao amin'ny « ${c.title} ».") },
            confirmButton = {
                TextButton(onClick = {
                    val keys = c.items.filter { it.key in AudioStore.done }.map { it.key }
                    AudioStore.delete(keys)
                    confirm = null
                }) { Text("Fafana") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Aoka ihany") } }
        )
    }
}

@Composable
private fun DownloadCard() {
    if (Dl.total == 0) return
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    val done = Dl.finished + Dl.failed
    val pct = ((Dl.finished + Dl.failed + Dl.fileFraction) / Dl.total.coerceAtLeast(1) * 100f).toInt().coerceIn(0, 100)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cs.primaryContainer)
            .padding(16.dp)
    ) {
        Text(
            if (Dl.running) "Mampidina… $done/${Dl.total} · $pct%" else "Vita ny fampidinana · ${Dl.finished}/${Dl.total}",
            fontWeight = FontWeight.Bold, fontSize = 15.sp
        )
        Spacer(Modifier.height(8.dp))
        if (Dl.running && Dl.fileFraction == 0f && done == 0) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(
                progress = { ((Dl.finished + Dl.failed + Dl.fileFraction) / Dl.total.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (Dl.running && Dl.current.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                Dl.current + (if (Dl.fileBytes > 0L) " · " + fmtSize(Dl.fileBytes) else ""),
                fontSize = 12.sp, color = ex.muted, maxLines = 1
            )
        }
        if (Dl.failed > 0) {
            Spacer(Modifier.height(6.dp))
            Text("Tsy lasa: ${Dl.failed} (jereo ny aterineto na tsy misy ilay rakitra)", fontSize = 12.sp, color = cs.error)
        }
        Spacer(Modifier.height(8.dp))
        if (Dl.running) OutlinedButton(onClick = { Dl.cancel() }) { Text("Foano ny fampidinana") }
        else TextButton(onClick = { Dl.clearSummary() }) { Text("Hanafina") }
    }
}

@Composable
private fun CatCard(c: AudioCat, bible: Boolean, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    val done = AudioStore.done
    val pending = c.items.filter { it.key !in done }
    val have = c.items.size - pending.size
    val avg by produceState(-1L, c.id) {
        value = AudioNet.averageSize(c.items.map { it.urls.first() })
    }
    var showBooks by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cs.surface)
            .padding(16.dp)
    ) {
        Text(c.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            "${c.subtitle} · $have/${c.items.size} voadina" +
                if (have > 0) " (${fmtSize(AudioStore.usedBytes(c.prefix))})" else "",
            color = ex.muted, fontSize = 13.sp
        )
        Spacer(Modifier.height(10.dp))
        if (pending.isNotEmpty()) {
            val label = when {
                avg < 0L -> "mikajy ny habe…"
                avg == 0L -> "habe tsy fantatra"
                else -> "≈ " + fmtSize(avg * pending.size)
            }
            Button(onClick = { AudioUi.download(ctx, pending) }, modifier = Modifier.fillMaxWidth()) {
                Text("Ampidino rehetra (${pending.size}) · $label")
            }
        } else {
            Text("Voadina daholo ✓ (azo henoina offline)", color = cs.primary, fontWeight = FontWeight.Medium)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (bible) {
                TextButton(onClick = { showBooks = !showBooks }) {
                    Text(if (showBooks) "Afeno ny boky" else "Boky sy toko")
                    Icon(if (showBooks) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                }
            }
            Spacer(Modifier.weight(1f))
            if (have > 0) TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text("Fafana")
            }
        }
        if (bible && showBooks) BookList(c, avg)
    }
}

@Composable
private fun BookList(c: AudioCat, avg: Long) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    var open by remember { mutableStateOf(setOf<Int>()) }
    val done = AudioStore.done
    val trId = c.tr
    for (b in Repo.books) {
        val items = c.byBook[b.book] ?: continue
        val pending = items.filter { it.key !in done }
        val isOpen = b.book in open
        Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceVariant)
                    .clickable { open = if (isOpen) open - b.book else open + b.book }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(Repo.bookName(trId, b.book), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                    Text("${items.size - pending.size}/${items.size} toko", fontSize = 12.sp, color = ex.muted)
                }
                if (pending.isNotEmpty()) {
                    TextButton(onClick = { AudioUi.download(ctx, pending) }) {
                        Text(if (avg > 0L) "Ampidino ≈ " + fmtSize(avg * pending.size) else "Ampidino")
                    }
                } else {
                    Text("✓", color = cs.primary, fontWeight = FontWeight.Bold)
                }
                Icon(if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
            }
            if (isOpen) {
                Spacer(Modifier.height(6.dp))
                for (row in items.chunked(6)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (ai in row) {
                            val has = ai.key in done
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (has) cs.primaryContainer else cs.surfaceVariant)
                                    .clickable {
                                        if (has) AudioUi.request(ctx, ai) else AudioUi.download(ctx, listOf(ai))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    chapterOf(ai), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                    color = if (has) cs.primary else cs.onSurface
                                )
                            }
                        }
                        for (k in row.size until 6) Spacer(Modifier.weight(1f))
                    }
                }
                Text("Tsindrio ny laharan'ny toko mba hampidina azy (voasary = efa voatahiry, tsindrio hihaino).", fontSize = 11.sp, color = ex.muted)
            }
        }
    }
}

/** Bandeau de progression visible sur tous les écrans pendant un téléchargement. */
@Composable
fun DownloadBar(applyInset: Boolean) {
    val nav = LocalNav.current
    val cs = MaterialTheme.colorScheme
    val done = Dl.finished + Dl.failed
    val pct = ((Dl.finished + Dl.failed + Dl.fileFraction) / Dl.total.coerceAtLeast(1) * 100f).toInt().coerceIn(0, 100)
    Surface(
        color = cs.surface, tonalElevation = 6.dp, shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().clickable { nav.go(Screen.AudioFiles) }
    ) {
        Column(
            Modifier
                .then(if (applyInset) Modifier.navigationBarsPadding() else Modifier)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Ico.Download, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                Text(
                    "Mampidina $done/${Dl.total} · $pct% · " + Dl.current +
                        (if (Dl.fileBytes > 0L) " (" + fmtSize(Dl.fileBytes) + ")" else ""),
                    fontSize = 13.sp, maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                )
                TextButton(onClick = { Dl.cancel() }) { Text("Foano") }
            }
            if (Dl.fileFraction == 0f && done == 0) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { ((Dl.finished + Dl.failed + Dl.fileFraction) / Dl.total.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Petit cercle qui tourne (ou se remplit) pendant le téléchargement du fichier. */
@Composable
fun PlaySpinner(key: String, tint: Color) {
    if (Dl.activeKey == key && Dl.fileFraction > 0f) {
        CircularProgressIndicator(
            progress = { Dl.fileFraction },
            modifier = Modifier.size(26.dp),
            color = tint,
            strokeWidth = 2.5.dp,
            trackColor = tint.copy(alpha = 0.25f)
        )
    } else {
        CircularProgressIndicator(modifier = Modifier.size(26.dp), color = tint, strokeWidth = 2.5.dp)
    }
}

@Composable
private fun DoneBadge(modifier: Modifier) {
    Box(
        modifier
            .offset(x = 3.dp, y = (-3).dp)
            .size(18.dp)
            .clip(CircleShape)
            .background(Color(0xFF2DD4BF))
            .border(1.5.dp, Color(0xFF05070A), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Ico.Check, null, tint = Color(0xFF05070A), modifier = Modifier.size(11.dp))
    }
}

/** Bouton play rond (page d'un cantique) : spinner pendant le téléchargement, badge ✓ si déjà hors ligne. */
@Composable
fun PlayRound(item: AudioItem, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val active = Audio.key == item.key
    val busy = Dl.isBusy(item.key)
    Box(Modifier.size(52.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (active) cs.primary else Color(0x59000000))
                .border(1.dp, Color(0x66FFFFFF), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            val fg = if (active) cs.onPrimary else Color.White
            if (busy) PlaySpinner(item.key, fg)
            else Icon(
                if (active && Audio.playing) Ico.Pause else Icons.Default.PlayArrow, "Henoy",
                tint = fg, modifier = Modifier.size(24.dp)
            )
        }
        if (AudioStore.has(item.key)) DoneBadge(Modifier.align(Alignment.TopEnd))
    }
}

/** Bouton play carré translucide (barre du haut de la Bible). */
@Composable
fun PlayGlass(item: AudioItem, onClick: () -> Unit) {
    val active = Audio.key == item.key
    val busy = Dl.isBusy(item.key)
    Box(Modifier.size(44.dp)) {
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0x66000000), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
                if (busy) PlaySpinner(item.key, Color.White)
                else Icon(if (active && Audio.playing) Ico.Pause else Icons.Default.PlayArrow, "Henoy", tint = Color.White)
            }
        }
        if (AudioStore.has(item.key)) DoneBadge(Modifier.align(Alignment.TopEnd))
    }
}
