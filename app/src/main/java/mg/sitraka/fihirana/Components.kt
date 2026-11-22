package mg.sitraka.fihirana

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Random
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Couleur d'en-tête propre à chaque langue de cantiques. */
fun heroColorFor(lang: String): Color = when (lang) {
    "mg" -> Color(0xFF0F766E)
    "fr" -> Color(0xFF3949AB)
    else -> Color(0xFF9D2B6B)
}

fun parseColor(hex: String, fallback: Color = Color(0xFF3949AB)): Color =
    try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { fallback }

private fun DrawScope.drawArt(seed: Int, s: Size) {
    val r = Random(seed.toLong() * 7919L + 13L)
    repeat(5) {
        val cx = r.nextFloat() * s.width
        val cy = r.nextFloat() * s.height * 0.8f
        val rad = (0.16f + r.nextFloat() * 0.34f) * s.width
        drawCircle(Color.White.copy(alpha = 0.04f + r.nextFloat() * 0.06f), rad, Offset(cx, cy))
    }
    val c = Offset(s.width * (0.72f + r.nextFloat() * 0.2f), s.height * 0.18f)
    for (i in 1..5) {
        drawCircle(
            Color.White.copy(alpha = 0.09f - i * 0.012f),
            s.width * 0.11f * i,
            c,
            style = Stroke(width = 2f + i)
        )
    }
}

/** En-tête « hero » : image de fond (ou dégradé + motifs) + voile sombre. */
@Composable
fun HeroBox(
    height: Dp,
    seed: Int,
    base: Color? = null,
    image: Int? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val ex = LocalExtra.current
    val ctx = LocalContext.current
    val a = base ?: ex.heroA
    val b = if (base != null) lerp(base, Color(0xFF070B12), 0.72f) else ex.heroB
    val bmp by produceState<ImageBitmap?>(null, image) {
        value = if (image == null) null else withContext(Dispatchers.IO) { Bg.load(ctx, image) }
    }
    val pic = bmp
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(listOf(a, b)))
    ) {
        if (pic != null) {
            Image(bitmap = pic, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0x80000000), 0.3f to Color(0x14000000),
                        0.55f to Color(0x33000000), 1f to Color(0xD9000000)
                    )
                )
            )
        } else {
            Canvas(Modifier.fillMaxSize()) { drawArt(seed, size) }
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xB3000000)))
            )
        }
        content()
    }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.then(Modifier.fillMaxSize())

@Composable
fun GlassButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x66000000),
        modifier = Modifier.size(44.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, desc, tint = Color.White)
        }
    }
}

@Composable
fun RoundAction(
    icon: ImageVector,
    desc: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = if (active) MaterialTheme.colorScheme.primary else Color(0x59000000)
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else Color.White
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, Color(0x66FFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = fg, modifier = Modifier.size(24.dp))
    }
}

/**
 * Écran à en-tête qui défile : le hero disparaît puis une barre compacte reste en haut.
 * [heroContent] est dessiné dans le hero (titre, boutons…), [actions] à droite de la barre du haut.
 */
@Composable
fun HeroScreen(
    title: String,
    heroHeight: Dp,
    seed: Int,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    base: Color? = null,
    image: Int? = null,
    contentPadding: PaddingValues = PaddingValues(bottom = 96.dp),
    actions: @Composable RowScope.() -> Unit = {},
    heroContent: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
    body: LazyListScope.() -> Unit,
) {
    val nav = LocalNav.current
    val ex = LocalExtra.current
    val density = LocalDensity.current
    val limit = with(density) { (heroHeight * 0.55f).toPx() }
    val scrolled by remember(limit) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > limit }
    }
    val barBase = base?.let { lerp(it, Color(0xFF070B12), 0.72f) } ?: ex.heroB
    val barBg by animateColorAsState(if (scrolled) barBase else Color.Transparent, label = "bar")
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            item(key = "hero") { HeroBox(heroHeight, seed, base, image, content = heroContent) }
            body()
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(barBg)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassButton(Icons.Default.Menu, "Menu") { nav.openDrawer() }
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .alpha(if (scrolled) 1f else 0f)
            )
            actions()
        }
        overlay()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(vertical = 10.dp)
    )
}

@Composable
fun NumberBadge(text: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = if (text.length > 3) 12.sp else 15.sp,
            maxLines = 1
        )
    }
}

@Composable
fun EmptyNote(text: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = LocalExtra.current.muted, fontSize = 15.sp)
    }
}

@Composable
fun BottomBarSpace() {
    Spacer(Modifier.navigationBarsPadding().height(8.dp))
}

@Composable
fun HeroTitle(
    big: String,
    small: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            big,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 33.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (small != null) Text(small, color = Color(0xCCFFFFFF), fontSize = 14.sp)
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) cs.primary else cs.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) cs.onPrimary else cs.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
fun SearchPill(
    value: String,
    onValue: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    keyboardNumbers: Boolean = false,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (keyboardNumbers) androidx.compose.ui.text.input.KeyboardType.Number
            else androidx.compose.ui.text.input.KeyboardType.Text,
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        ),
        modifier = modifier.let { m -> if (focusRequester != null) m.androidxFocus(focusRequester) else m },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x66000000))
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.Search, null, tint = Color(0xCCFFFFFF))
                Box(Modifier.weight(1f).padding(start = 10.dp)) {
                    if (value.isEmpty()) Text(hint, color = Color(0x99FFFFFF), fontSize = 15.sp, maxLines = 1)
                    inner()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Close, "Fafana",
                        tint = Color(0xCCFFFFFF),
                        modifier = Modifier.size(20.dp).clickable { onValue("") }
                    )
                }
            }
        }
    )
}

private fun Modifier.androidxFocus(fr: androidx.compose.ui.focus.FocusRequester): Modifier =
    this.then(Modifier.focusRequester(fr))


/** Balayage horizontal : vers la droite = précédent, vers la gauche = suivant. */
fun Modifier.swipeNav(key: Any?, onPrev: () -> Unit, onNext: () -> Unit): Modifier =
    this.pointerInput(key) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragCancel = { total = 0f },
            onDragEnd = {
                if (total > 140f) onPrev() else if (total < -140f) onNext()
                total = 0f
            }
        ) { _, dx -> total += dx }
    }
