package mg.sitraka.fihirana

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Clavier numérique pour aller directement à un cantique (comme le bouton en bas à droite de la maquette). */
@Composable
fun NumberPadDialog(
    title: String,
    max: Int,
    exists: (Int) -> Boolean,
    onDismiss: () -> Unit,
    onGo: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var text by remember { mutableStateOf("") }
    val n = text.toIntOrNull()
    val valid = n != null && n in 1..max && exists(n)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(cs.surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = LocalExtra.current.muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                if (text.isEmpty()) "—" else text,
                fontSize = 44.sp, fontWeight = FontWeight.Bold,
                color = if (text.isEmpty() || valid) cs.onSurface else cs.error,
                textAlign = TextAlign.Center
            )
            Text(
                if (text.isNotEmpty() && !valid) "Tsy misy io laharana io (1–$max)" else "Laharana 1–$max",
                color = LocalExtra.current.muted, fontSize = 12.sp
            )
            Spacer(Modifier.height(14.dp))
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
            for (r in rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    for (k in r) PadKey(k) { if (text.length < 4) text += k }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PadKey(null, icon = Ico.Backspace) { if (text.isNotEmpty()) text = text.dropLast(1) }
                PadKey("0") { if (text.length < 4 && text.isNotEmpty()) text += "0" }
                PadKey(null, icon = Ico.Check, primary = true, enabled = valid) { n?.let { onGo(it) } }
            }
        }
    }
}

@Composable
private fun PadKey(
    label: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val bg = when {
        primary && enabled -> cs.primary
        primary -> cs.surfaceVariant
        else -> cs.surfaceVariant
    }
    Box(
        Modifier
            .size(width = 72.dp, height = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) Text(label, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        if (icon != null) Icon(
            icon, null, modifier = Modifier.size(24.dp),
            tint = if (primary && enabled) cs.onPrimary else cs.onSurfaceVariant
        )
    }
}
