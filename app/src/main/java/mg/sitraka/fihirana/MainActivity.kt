package mg.sitraka.fihirana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Root() }
    }

    override fun onDestroy() {
        if (isFinishing) Audio.stop()
        super.onDestroy()
    }
}

@Composable
fun Root() {
    val ctx = LocalContext.current
    val dark = isDarkMode()
    DisposableEffect(dark) {
        (ctx as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        onDispose { }
    }
    var ready by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) { Repo.prepare(ctx.applicationContext) }
            ready = true
        } catch (e: Throwable) {
            error = e.message ?: e.toString()
        }
    }
    AppTheme {
        if (ready) MainShell() else Splash(error)
    }
}

@Composable
fun Splash(error: String?) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F766E), Color(0xFF0A1220)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Ico.Book, null, tint = Color.White, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(18.dp))
            Text("Fihirana & Baiboly", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Sitraka Nambinintsoa", color = Color(0xCCFFFFFF), fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))
            if (error == null) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                Spacer(Modifier.height(14.dp))
                Text("Mamboatra ny angona… (indray mandeha ihany)", color = Color(0xCCFFFFFF), fontSize = 13.sp)
            } else {
                Text("Nisy olana: $error", color = Color(0xFFFFB4AB), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun MainShell() {
    val nav = remember { Nav() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    nav.openDrawer = { scope.launch { drawerState.open() } }
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Prefs.keepOn) {
        view.keepScreenOn = Prefs.keepOn
        onDispose { }
    }

    CompositionLocalProvider(LocalNav provides nav) {
        BackHandler(nav.stack.size > 1) { nav.back() }
        BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen ||
                !(nav.current is Screen.HymnView || nav.current is Screen.BibleRead || nav.current is Screen.Compare),
            drawerContent = { DrawerContent(nav) { scope.launch { drawerState.close() } } }
        ) {
            val playerOn = Audio.key.isNotEmpty()
            val dlOn = Dl.running
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Box(
                    Modifier
                        .weight(1f)
                        .then(if (playerOn || dlOn) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier)
                ) {
                    AnimatedContent(
                        targetState = nav.current,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 }) togetherWith fadeOut(tween(140))
                        },
                        label = "screens",
                        contentKey = { s ->
                            when (s) {
                                is Screen.HymnView -> "hymn:" + s.lang
                                is Screen.BibleRead -> "bible"
                                is Screen.Compare -> "compare"
                                else -> s
                            }
                        }
                    ) { s -> ScreenHost(s) }
                }
                if (dlOn) DownloadBar(!playerOn)
                if (playerOn) PlayerBar()
            }
        }
        AudioPromptHost()
        AudioLinkHost()
    }
}

@Composable
fun ScreenPage(s: Screen) {
    when (s) {
        Screen.Home -> HomeScreen()
        is Screen.HymnList -> HymnListScreen(s.lang)
        is Screen.HymnView -> HymnScreen(s.lang, s.num)
        is Screen.BibleRead -> BibleReadScreen(s.book, s.chapter, s.verse)
        is Screen.Compare -> CompareScreen(s.book, s.chapter, s.verses)
        Screen.BibleSearch -> BibleSearchScreen()
        Screen.Favorites -> FavoritesScreen()
        Screen.AudioFiles -> AudioFilesScreen()
        Screen.SettingsScreen -> SettingsScreen()
        Screen.About -> AboutScreen()
    }
}

@Composable
fun DrawerContent(nav: Nav, close: () -> Unit) {
    val ex = LocalExtra.current
    val cur = nav.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            HeroBox(height = 190.dp, seed = 77) {
                Column(
                    Modifier.align(Alignment.BottomStart).padding(20.dp).statusBarsPadding(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Ico.Book, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    Text("Fihirana & Baiboly", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Mpamorona: Sitraka Nambinintsoa", color = Color(0xD9FFFFFF), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            val pad = NavigationDrawerItemDefaults.ItemPadding
            fun go(s: Screen) { nav.root(s); close() }
            NavigationDrawerItem(
                label = { Text("Fandraisana") }, selected = cur == Screen.Home,
                icon = { Icon(Icons.Default.Home, null) }, onClick = { go(Screen.Home) }, modifier = Modifier.padding(pad)
            )
            SectionTitle("Fihirana", Modifier.padding(horizontal = 28.dp))
            for (l in Repo.langs) {
                NavigationDrawerItem(
                    label = { Text("${l.name}  ·  ${Repo.count(l.code)}") },
                    selected = (cur as? Screen.HymnList)?.lang == l.code || (cur as? Screen.HymnView)?.lang == l.code,
                    icon = { Icon(Ico.Music, null) },
                    onClick = { go(Screen.HymnList(l.code)) },
                    modifier = Modifier.padding(pad)
                )
            }
            SectionTitle("Baiboly", Modifier.padding(horizontal = 28.dp))
            NavigationDrawerItem(
                label = { Text("Mamaky Baiboly") },
                selected = cur is Screen.BibleRead,
                icon = { Icon(Ico.Book, null) },
                onClick = { val (b, c) = Prefs.lastBible(); go(Screen.BibleRead(b, c)) },
                modifier = Modifier.padding(pad)
            )
            NavigationDrawerItem(
                label = { Text("Fikarohana ao amin'ny Baiboly") },
                selected = cur == Screen.BibleSearch,
                icon = { Icon(Icons.Default.Search, null) },
                onClick = { go(Screen.BibleSearch) },
                modifier = Modifier.padding(pad)
            )
            NavigationDrawerItem(
                label = { Text("Fampitahana Baiboly") },
                selected = cur is Screen.Compare,
                icon = { Icon(Ico.Compare, null) },
                onClick = { val (b, c) = Prefs.lastBible(); go(Screen.Compare(b, c)) },
                modifier = Modifier.padding(pad)
            )
            SectionTitle("Ahy", Modifier.padding(horizontal = 28.dp))
            NavigationDrawerItem(
                label = { Text("Rakitra audio") }, selected = cur == Screen.AudioFiles,
                icon = { Icon(Ico.Download, null) }, onClick = { go(Screen.AudioFiles) }, modifier = Modifier.padding(pad)
            )
            NavigationDrawerItem(
                label = { Text("Tiana sy naoty") }, selected = cur == Screen.Favorites,
                icon = { Icon(Icons.Default.Favorite, null) }, onClick = { go(Screen.Favorites) }, modifier = Modifier.padding(pad)
            )
            NavigationDrawerItem(
                label = { Text("Fanovana (Paramètres)") }, selected = cur == Screen.SettingsScreen,
                icon = { Icon(Icons.Default.Settings, null) }, onClick = { go(Screen.SettingsScreen) }, modifier = Modifier.padding(pad)
            )
            NavigationDrawerItem(
                label = { Text("Zarao ny application") }, selected = false,
                icon = { Icon(Icons.Default.Share, null) }, onClick = { close(); shareApp(ctx) }, modifier = Modifier.padding(pad)
            )
            NavigationDrawerItem(
                label = { Text("Momba ny application") }, selected = cur == Screen.About,
                icon = { Icon(Icons.Default.Info, null) }, onClick = { go(Screen.About) }, modifier = Modifier.padding(pad)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "© Sitraka Nambinintsoa",
                color = ex.muted, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 28.dp).navigationBarsPadding()
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun PlayerBar() {
    val cs = MaterialTheme.colorScheme
    LaunchedEffect(Audio.key) {
        while (Audio.key.isNotEmpty()) {
            Audio.tick()
            delay(400)
        }
    }
    var drag by remember { mutableStateOf<Float?>(null) }
    Surface(color = cs.surface, tonalElevation = 6.dp, shadowElevation = 10.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (Audio.loading) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                }
            } else {
                IconButton(onClick = { Audio.toggle() }) {
                    Icon(if (Audio.playing) Ico.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = cs.primary)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(Audio.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                val max = if (Audio.duration > 0) Audio.duration.toFloat() else 1f
                Slider(
                    value = (drag ?: Audio.position.toFloat()).coerceIn(0f, max),
                    onValueChange = { drag = it },
                    onValueChangeFinished = { drag?.let { Audio.seekTo(it.toInt()) }; drag = null },
                    valueRange = 0f..max,
                    colors = SliderDefaults.colors(),
                    modifier = Modifier.height(24.dp)
                )
            }
            IconButton(onClick = { Audio.stop() }) { Icon(Icons.Default.Close, "Akatony") }
        }
    }
}
