package mg.sitraka.fihirana

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

private val DailyRefs = listOf(
    Triple(430, 3, 16), Triple(190, 23, 1), Triple(500, 4, 13), Triple(240, 29, 11), Triple(450, 8, 28),
    Triple(230, 41, 10), Triple(200, 3, 5), Triple(400, 11, 28), Triple(190, 46, 1), Triple(60, 1, 9),
    Triple(450, 12, 2), Triple(550, 1, 7), Triple(190, 121, 1), Triple(480, 2, 20), Triple(580, 11, 1),
    Triple(460, 13, 4), Triple(230, 40, 31), Triple(190, 27, 1), Triple(400, 6, 33), Triple(620, 4, 8),
    Triple(490, 2, 8), Triple(190, 119, 105), Triple(250, 3, 22), Triple(500, 4, 6), Triple(510, 3, 23),
    Triple(590, 1, 5), Triple(600, 5, 7), Triple(660, 21, 4), Triple(330, 6, 8), Triple(190, 34, 8),
    Triple(400, 28, 20),
)

@Composable
fun HomeScreen() {
    val nav = LocalNav.current
    val ex = LocalExtra.current
    val listState = rememberLazyListState()
    val trId = Prefs.bibleTr
    val ref = remember {
        val d = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        DailyRefs[d % DailyRefs.size]
    }
    val vod by produceState<Verse?>(null, trId) {
        value = withContext(Dispatchers.IO) { Repo.verse(trId, ref.first, ref.second, ref.third) }
    }
    var pad by remember { mutableStateOf(false) }
    var padLang by remember { mutableStateOf("mg") }

    HeroScreen(
        title = "Fihirana & Baiboly",
        heroHeight = 270.dp,
        seed = 5,
        listState = listState,
        heroContent = {
            Column(Modifier.align(Alignment.BottomStart)) {
                Icon(
                    Ico.Book, null, tint = Color.White,
                    modifier = Modifier.padding(start = 22.dp).size(40.dp)
                )
                HeroTitle("Fihirana & Baiboly", "Mpamorona: Sitraka Nambinintsoa")
            }
        },
        actions = { GlassButton(Icons.Default.Search, "Hikaroka") { nav.go(Screen.BibleSearch) } },
        body = {
            item(key = "vod") {
                val v = vod
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)
                            )
                        )
                        .clickable { nav.go(Screen.BibleRead(ref.first, ref.second, ref.third)) }
                        .padding(20.dp)
                ) {
                    Text(
                        "ANDININY ANDRO", color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        v?.text ?: "…",
                        fontSize = (18 * Prefs.fontScale).sp,
                        lineHeight = (27 * Prefs.fontScale).sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${Repo.bookName(trId, ref.first)} ${ref.second}:${ref.third}",
                        color = ex.muted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                }
            }
            item(key = "resume") {
                val lh = Prefs.lastHymn()
                val lb = Prefs.lastBible()
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionTitle("Tohizo", Modifier.padding(horizontal = 0.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ResumeCard(
                            Ico.Book, "Baiboly",
                            "${Repo.bookName(trId, lb.first)} ${lb.second}",
                            Modifier.weight(1f)
                        ) { nav.go(Screen.BibleRead(lb.first, lb.second)) }
                        val h = lh?.let { Repo.hymn(it.first, it.second) }
                        ResumeCard(
                            Ico.Music, "Fihirana",
                            if (h != null) "${h.num} · ${h.title}" else "Mbola tsy nisy",
                            Modifier.weight(1f)
                        ) { if (h != null) nav.go(Screen.HymnView(h.lang, h.num)) else nav.go(Screen.HymnList("mg")) }
                    }
                }
            }
            item(key = "hymns") {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionTitle("Fihirana", Modifier.padding(horizontal = 0.dp))
                    for (l in Repo.langs) {
                        LangRow(l, Modifier.padding(bottom = 10.dp), { nav.go(Screen.HymnList(l.code)) }) {
                            padLang = l.code; pad = true
                        }
                    }
                }
            }
            item(key = "bible") {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionTitle("Baiboly", Modifier.padding(horizontal = 0.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Tile(Ico.Book, "Mamaky", Modifier.weight(1f)) { nav.go(Screen.BibleRead(lb0().first, lb0().second)) }
                        Tile(Icons.Default.Search, "Hikaroka", Modifier.weight(1f)) { nav.go(Screen.BibleSearch) }
                        Tile(Ico.Compare, "Hampitaha", Modifier.weight(1f)) { nav.go(Screen.Compare(lb0().first, lb0().second)) }
                        Tile(Icons.Default.Favorite, "Tiana", Modifier.weight(1f)) { nav.go(Screen.Favorites) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        Repo.trs.joinToString("  ·  ") { it.short },
                        color = ex.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, start = 4.dp)
                    )
                }
            }
        }
    )
    if (pad) {
        val max = Repo.hymnList(padLang).maxOfOrNull { it.num } ?: 1
        NumberPadDialog(
            title = Repo.langs.firstOrNull { it.code == padLang }?.name ?: "",
            max = max,
            exists = { Repo.hymn(padLang, it) != null },
            onDismiss = { pad = false },
            onGo = { pad = false; nav.go(Screen.HymnView(padLang, it)) }
        )
    }
}

private fun lb0(): Pair<Int, Int> = Prefs.lastBible()

@Composable
private fun ResumeCard(icon: ImageVector, label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cs.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = cs.primary)
        Text(label, color = LocalExtra.current.muted, fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
    }
}

@Composable
private fun LangRow(l: HymnLang, modifier: Modifier, onOpen: () -> Unit, onPad: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cs.surface)
            .clickable(onClick = onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(heroColorFor(l.code)),
            contentAlignment = Alignment.Center
        ) { Text(l.short, color = Color.White, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(l.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("${Repo.count(l.code)} hira", color = LocalExtra.current.muted, fontSize = 13.sp)
        }
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(cs.primaryContainer).clickable(onClick = onPad),
            contentAlignment = Alignment.Center
        ) { Icon(Ico.Dialpad, "Laharana", tint = cs.primary, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun Tile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(18.dp))
            .background(cs.surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = cs.primary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
