package mg.sitraka.fihirana

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Rohy mivantana hampidinana ny application (APK avy amin'ny GitHub Releases). */
object AppLinks {
    const val APK = "https://github.com/sitraka0417-cell/fihirana/releases/download/apk/Fihirana-Baiboly.apk"
    const val PAGE = "https://github.com/sitraka0417-cell/fihirana/releases/tag/apk"
    const val TEXT = "Fihirana & Baiboly: Baiboly 6 dikan-teny sy fihirana (malagasy, frantsay, anglisy, ankizy). Alaivo eto: "
}

fun shareApp(ctx: Context) {
    val i = Intent(Intent.ACTION_SEND)
    i.type = "text/plain"
    i.putExtra(Intent.EXTRA_TEXT, AppLinks.TEXT + AppLinks.APK)
    ctx.startActivity(Intent.createChooser(i, "Zarao ny application"))
}

@Composable
fun AppLinkCard() {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtra.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cs.surface)
            .padding(18.dp)
    ) {
        Text("Rohy hampidinana ny application", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = cs.primary)
        Spacer(Modifier.height(8.dp))
        Text("Zarao amin'ny namanao ity rohy ity mba hampidinany ny application (APK).", fontSize = 13.sp, color = ex.muted)
        Spacer(Modifier.height(8.dp))
        Text(AppLinks.APK, fontSize = 12.sp)
        Row {
            TextButton(onClick = { shareApp(ctx) }) { Text("Zarao") }
            TextButton(onClick = {
                try {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.PAGE)))
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Tsy misy application hamoha ny rohy", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Pejy") }
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(AppLinks.APK))
                Toast.makeText(ctx, "Voadika ny rohy", Toast.LENGTH_SHORT).show()
            }) { Text("Adika") }
        }
    }
}
