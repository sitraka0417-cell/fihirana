package mg.sitraka.fihirana

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Petites icônes vectorielles maison (le jeu « core » de Material n'a pas tout). */
object Ico {
    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, true, false, cx + r, cy)
        arcTo(r, r, 0f, true, false, cx - r, cy)
        close()
    }

    private fun outline(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block
            )
        }.build()

    private fun solid(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black), pathBuilder = block)
        }.build()

    val Book: ImageVector by lazy {
        outline("book") {
            moveTo(12f, 6.5f)
            curveTo(10f, 5f, 7f, 4.6f, 3.5f, 5f)
            lineTo(3.5f, 18.5f)
            curveTo(7f, 18.1f, 10f, 18.6f, 12f, 20f)
            curveTo(14f, 18.6f, 17f, 18.1f, 20.5f, 18.5f)
            lineTo(20.5f, 5f)
            curveTo(17f, 4.6f, 14f, 5f, 12f, 6.5f)
            close()
            moveTo(12f, 6.5f)
            lineTo(12f, 20f)
        }
    }

    val Music: ImageVector by lazy {
        outline("music") {
            moveTo(9f, 17f)
            lineTo(9f, 5f)
            lineTo(19f, 3f)
            lineTo(19f, 15f)
            circle(6.5f, 17f, 2.5f)
            circle(16.5f, 15f, 2.5f)
        }
    }

    val Copy: ImageVector by lazy {
        outline("copy") {
            moveTo(9f, 9f)
            lineTo(19.5f, 9f)
            lineTo(19.5f, 19.5f)
            lineTo(9f, 19.5f)
            close()
            moveTo(5f, 15f)
            lineTo(4.5f, 15f)
            lineTo(4.5f, 4.5f)
            lineTo(15f, 4.5f)
            lineTo(15f, 5f)
        }
    }

    val Bookmark: ImageVector by lazy {
        outline("bookmark") {
            moveTo(7f, 4f)
            lineTo(17f, 4f)
            lineTo(17f, 20.5f)
            lineTo(12f, 16.5f)
            lineTo(7f, 20.5f)
            close()
        }
    }

    val BookmarkFilled: ImageVector by lazy {
        solid("bookmark_filled") {
            moveTo(7f, 4f)
            lineTo(17f, 4f)
            lineTo(17f, 20.5f)
            lineTo(12f, 16.5f)
            lineTo(7f, 20.5f)
            close()
        }
    }

    val Highlight: ImageVector by lazy {
        outline("highlight") {
            moveTo(4f, 20f)
            lineTo(5.2f, 15f)
            lineTo(15.5f, 4.7f)
            lineTo(19.3f, 8.5f)
            lineTo(9f, 18.8f)
            close()
            moveTo(13.5f, 6.7f)
            lineTo(17.3f, 10.5f)
            moveTo(14f, 20f)
            lineTo(20f, 20f)
        }
    }

    val Moon: ImageVector by lazy {
        outline("moon") {
            moveTo(20f, 14.2f)
            arcTo(8.5f, 8.5f, 0f, true, true, 9.8f, 4f)
            arcTo(6.8f, 6.8f, 0f, false, false, 20f, 14.2f)
            close()
        }
    }

    val Download: ImageVector by lazy {
        outline("download") {
            moveTo(12f, 4f)
            lineTo(12f, 15f)
            moveTo(7f, 10.5f)
            lineTo(12f, 15.5f)
            lineTo(17f, 10.5f)
            moveTo(5f, 20f)
            lineTo(19f, 20f)
        }
    }

    val Check: ImageVector by lazy {
        outline("check") {
            moveTo(4.5f, 12.5f)
            lineTo(10f, 18f)
            lineTo(19.5f, 6.5f)
        }
    }

    val Present: ImageVector by lazy {
        outline("present") {
            moveTo(4f, 4f)
            lineTo(20f, 4f)
            lineTo(20f, 15f)
            lineTo(4f, 15f)
            close()
            moveTo(12f, 15f)
            lineTo(12f, 20f)
            moveTo(8f, 20f)
            lineTo(16f, 20f)
        }
    }

    val Dialpad: ImageVector by lazy {
        solid("dialpad") {
            val xs = floatArrayOf(6f, 12f, 18f)
            val ys = floatArrayOf(5f, 10.7f, 16.4f, 21.6f)
            for (x in xs) for (i in 0..2) circle(x, ys[i], 1.7f)
            circle(12f, ys[3], 1.7f)
        }
    }

    val Compare: ImageVector by lazy {
        outline("compare") {
            moveTo(3.5f, 5f)
            lineTo(10.5f, 5f)
            lineTo(10.5f, 19f)
            lineTo(3.5f, 19f)
            close()
            moveTo(13.5f, 5f)
            lineTo(20.5f, 5f)
            lineTo(20.5f, 19f)
            lineTo(13.5f, 19f)
            close()
        }
    }

    val Pause: ImageVector by lazy {
        solid("pause") {
            moveTo(6.5f, 4.5f)
            lineTo(10f, 4.5f)
            lineTo(10f, 19.5f)
            lineTo(6.5f, 19.5f)
            close()
            moveTo(14f, 4.5f)
            lineTo(17.5f, 4.5f)
            lineTo(17.5f, 19.5f)
            lineTo(14f, 19.5f)
            close()
        }
    }

    val Backspace: ImageVector by lazy {
        outline("backspace") {
            moveTo(9f, 5f)
            lineTo(21f, 5f)
            lineTo(21f, 19f)
            lineTo(9f, 19f)
            lineTo(2.5f, 12f)
            close()
            moveTo(12.5f, 9f)
            lineTo(17f, 15f)
            moveTo(17f, 9f)
            lineTo(12.5f, 15f)
        }
    }

    val Note: ImageVector by lazy {
        outline("note") {
            moveTo(5f, 4f)
            lineTo(19f, 4f)
            lineTo(19f, 20f)
            lineTo(5f, 20f)
            close()
            moveTo(8.5f, 9f)
            lineTo(15.5f, 9f)
            moveTo(8.5f, 13f)
            lineTo(15.5f, 13f)
        }
    }
}
