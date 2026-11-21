package mg.sitraka.fihirana

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ======================================================================= Tiana
@Composable
fun FavoritesScreen() {
    val nav = LocalNav.current
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    var tab by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val trId = Prefs.bibleTr
    val favH = UserStore.favHymns.mapNotNull { id ->
        val i = id.indexOf(':')
        if (i < 0) null else Repo.hymn(id.substring(0, i), id.substring(i + 1).toIntOrNull() ?: 0)
    }
    val markKeys = (UserStore.colors.keys + UserStore.notes.keys).sorted()
    val favV by produceState(emptyList<Pair<Long, String>>(), UserStore.favVerses, trId) {
        value = withContext(Dispatchers.IO) {
            UserStore.favVerses.take(300).map { k -> k to (Repo.verse(trId, vkBook(k), vkChapter(k), vkVerse(k))?.text ?: "") }
        }
    }
    val marks by produceState(emptyList<Pair<Long, String>>(), markKeys, trId) {
        value = withContext(Dispatchers.IO) {
            markKeys.take(300).map { k -> k to (Repo.verse(trId, vkBook(k), vkChapter(k), vkVerse(k))?.text ?: "") }
        }
    }

    HeroScreen(
        title = "Tiana sy naoty",
        heroHeight = 200.dp,
        seed = 33,
        listState = listState,
        heroContent = { Column(Modifier.align(Alignment.BottomStart)) { HeroTitle("Tiana sy naoty", "Hira, andininy, loko ary naoty") } },
        body = {
            item(key = "tabs") {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Chip("Hira (${favH.size})", tab == 0, { tab = 0 })
                    Chip("Andininy (${UserStore.favVerses.size})", tab == 1, { tab = 1 })
                    Chip("Loko/Naoty (${markKeys.size})", tab == 2, { tab = 2 })
                }
            }
            when (tab) {
                0 -> {
                    if (favH.isEmpty()) item(key = "e0") { EmptyNote("Tsy mbola misy hira tiana.\nTsindrio ny fo ao amin'ny hira iray.") }
                    items(favH, key = { it.id }) { h ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                HymnRow(h, listOf(Repo.langs.firstOrNull { it.code == h.lang }?.name ?: "").joinToString()) {
                                    nav.go(Screen.HymnView(h.lang, h.num))
                                }
                            }
                            Icon(
                                Icons.Default.Close, "Esory", tint = ex.muted,
                                modifier = Modifier.padding(end = 16.dp).size(20.dp).clickable { UserStore.toggleHymn(h) }
                            )
                        }
                    }
                }
                1 -> {
                    if (favV.isEmpty()) item(key = "e1") { EmptyNote("Tsy mbola misy andininy tiana.\nTsindrio ny andininy ao amin'ny Baiboly dia safidio ny « Tiana ».") }
                    items(favV, key = { "f" + it.first }) { (k, text) -> VerseLine(k, text, trId) { UserStore.toggleVerses(listOf(k)) } }
                }
                else -> {
                    if (marks.isEmpty()) item(key = "e2") { EmptyNote("Tsy mbola misy andininy voaloko na misy naoty.") }
                    items(marks, key = { "m" + it.first }) { (k, text) -> VerseLine(k, text, trId, null) }
                }
            }
        }
    )
}

@Composable
private fun VerseLine(k: Long, text: String, trId: Int, onRemove: (() -> Unit)? = null) {
    val nav = LocalNav.current
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    val ci = UserStore.colors[k] ?: 0
    val note = UserStore.notes[k]
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { nav.go(Screen.BibleRead(vkBook(k), vkChapter(k), vkVerse(k))) }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (ci in 1..HighlightColors.size) {
            Box(Modifier.padding(top = 4.dp, end = 10.dp).size(12.dp).clip(CircleShape).background(HighlightColors[ci - 1]))
        }
        Column(Modifier.weight(1f)) {
            Text(
                refString(trId, vkBook(k), vkChapter(k), listOf(vkVerse(k))),
                color = cs.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp
            )
            if (text.isNotBlank()) Text(text, fontSize = 15.sp, lineHeight = 22.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            if (!note.isNullOrBlank()) Text("✎ $note", color = ex.muted, fontSize = 13.sp, fontStyle = FontStyle.Italic, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (onRemove != null) Icon(
            Icons.Default.Close, "Esory", tint = ex.muted,
            modifier = Modifier.padding(start = 10.dp).size(20.dp).clickable(onClick = onRemove)
        )
    }
}

// ======================================================================= Fanovana
@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cs.surface)
            .padding(18.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = cs.primary)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ToggleRow(label: String, desc: String?, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (desc != null) Text(desc, fontSize = 12.sp, color = LocalExtra.current.muted)
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen() {
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    val listState = rememberLazyListState()
    HeroScreen(
        title = "Fanovana",
        heroHeight = 190.dp,
        seed = 8,
        listState = listState,
        heroContent = { Column(Modifier.align(Alignment.BottomStart)) { HeroTitle("Fanovana", "Endrika, soratra ary Baiboly") } },
        body = {
            item(key = "theme") {
                SettingCard("Endrika (thème)") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Araka ny finday", Prefs.themeMode == 0, { Prefs.putThemeMode(0) })
                        Chip("Mazava", Prefs.themeMode == 1, { Prefs.putThemeMode(1) })
                        Chip("Maizina", Prefs.themeMode == 2, { Prefs.putThemeMode(2) })
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Loko lehibe", fontSize = 13.sp, color = ex.muted)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Accents.forEachIndexed { i, a ->
                            val c = if (ex.dark) a.dark else a.light
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .then(if (Prefs.accent == i) Modifier.border(3.dp, cs.onSurface, CircleShape) else Modifier)
                                    .clickable { Prefs.putAccent(i) },
                                contentAlignment = Alignment.Center
                            ) { if (Prefs.accent == i) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
            item(key = "font") {
                SettingCard("Soratra") {
                    Text(
                        "Efa anananao ve ny fiadanan'ny fo?",
                        fontSize = (20 * Prefs.fontScale).sp, lineHeight = (30 * Prefs.fontScale).sp
                    )
                    Slider(value = Prefs.fontScale, onValueChange = { Prefs.putFontScale(it) }, valueRange = 0.8f..1.8f)
                    Text("${(Prefs.fontScale * 100).toInt()}%", fontSize = 12.sp, color = ex.muted)
                    Spacer(Modifier.height(8.dp))
                    ToggleRow("Soratra voalamina (justifié)", "Ho an'ny Baiboly", Prefs.justify) { Prefs.putJustify(it) }
                    ToggleRow("Aseho ny lohateny ao amin'ny Baiboly", "Ny lohatenin'ny fizarana (manga)", Prefs.headings) { Prefs.putHeadings(it) }
                }
            }
            item(key = "bible") {
                SettingCard("Baiboly fototra") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (t in Repo.trs.take(3)) Chip(t.short, Prefs.bibleTr == t.id, { Prefs.putBibleTr(t.id) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (t in Repo.trs.drop(3)) Chip(t.short, Prefs.bibleTr == t.id, { Prefs.putBibleTr(t.id) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(Repo.tr(Prefs.bibleTr).name, fontSize = 13.sp, color = ex.muted)
                }
            }
            item(key = "cmp") {
                SettingCard("Dikan-teny ampitahaina") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (t in Repo.trs.take(3)) {
                            val on = t.id in Prefs.compareTrs
                            Chip(t.short, on, {
                                val l = if (on) Prefs.compareTrs - t.id else Prefs.compareTrs + t.id
                                if (l.isNotEmpty()) Prefs.putCompareTrs(l.sorted())
                            })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (t in Repo.trs.drop(3)) {
                            val on = t.id in Prefs.compareTrs
                            Chip(t.short, on, {
                                val l = if (on) Prefs.compareTrs - t.id else Prefs.compareTrs + t.id
                                if (l.isNotEmpty()) Prefs.putCompareTrs(l.sorted())
                            })
                        }
                    }
                }
            }
            item(key = "misc") {
                SettingCard("Hafa") {
                    ToggleRow("Aza avela hatory ny efijery", "Mandritra ny famakiana", Prefs.keepOn) { Prefs.putKeepOn(it) }
                }
            }
        }
    )
}

// ======================================================================= Momba
@Composable
fun AboutScreen() {
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    val listState = rememberLazyListState()
    HeroScreen(
        title = "Momba ny application",
        heroHeight = 250.dp,
        seed = 3,
        listState = listState,
        heroContent = {
            Column(Modifier.align(Alignment.BottomStart)) {
                Icon(Ico.Book, null, tint = Color.White, modifier = Modifier.padding(start = 22.dp).size(44.dp))
                HeroTitle("Fihirana & Baiboly", "Version 1.0.0")
            }
        },
        body = {
            item(key = "by") {
                SettingCard("Mpamorona") {
                    Text("Sitraka Nambinintsoa", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Namboarina tamim-pitiavana ho fanompoana sy fanomezam-boninahitra an'Andriamanitra.", color = ex.muted, fontSize = 14.sp)
                }
            }
            item(key = "content") {
                SettingCard("Votoatiny") {
                    for (l in Repo.langs) Text("• ${l.name}: ${Repo.count(l.code)} hira", fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
                    Spacer(Modifier.height(8.dp))
                    for (t in Repo.trs) Text("• ${t.name}", fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            item(key = "tips") {
                SettingCard("Torohevitra") {
                    Text("• Mivantana mankany amin'ny hira: tsindrio ny bokotra laharana.", fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
                    Text("• Hira na toko manaraka/teo aloha: hetezo mankavia na mankavanana.", fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
                    Text("• Baiboly: tsindrio ny andininy hisafidy, dia mandrafitra, mamaritra loko, manampy naoty na mampitaha.", fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
                    Text("• Fikarohana: afaka soratana koa ny toerana, ohatra « Jaona 3:16 ».", fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            item(key = "copy") {
                Text(
                    "© Sitraka Nambinintsoa",
                    color = ex.muted, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    )
}
