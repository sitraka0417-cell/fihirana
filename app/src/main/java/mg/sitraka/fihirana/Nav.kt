package mg.sitraka.fihirana

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf

sealed interface Screen {
    data object Home : Screen
    data class HymnList(val lang: String) : Screen
    data class HymnView(val lang: String, val num: Int) : Screen
    data class BibleRead(val book: Int, val chapter: Int, val verse: Int = 0) : Screen
    data class Compare(val book: Int, val chapter: Int, val verses: List<Int> = emptyList()) : Screen
    data object BibleSearch : Screen
    data object Favorites : Screen
    data object AudioFiles : Screen
    data object SettingsScreen : Screen
    data object About : Screen
}

@Stable
class Nav {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    var openDrawer: () -> Unit = {}

    val current: Screen get() = stack.last()

    fun go(s: Screen) {
        if (stack.isNotEmpty() && stack.last() == s) return
        stack.add(s)
    }

    /** Remplace la pile (navigation depuis le tiroir). */
    fun root(s: Screen) {
        stack.clear()
        stack.add(Screen.Home)
        if (s != Screen.Home) stack.add(s)
    }

    /** Remplace l'écran courant (ex. changer de langue / de chapitre). */
    fun replace(s: Screen) {
        if (stack.size > 1) stack.removeAt(stack.size - 1)
        stack.add(s)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.size - 1)
    }
}

val LocalNav = staticCompositionLocalOf<Nav> { error("Nav non fourni") }
