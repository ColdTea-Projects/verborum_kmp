package de.coldtea.verborum.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp

/**
 * The icons the shell needs, drawn here rather than pulled from
 * `material-icons-extended` — two glyphs do not justify that dependency.
 */
object VerborumIcons {

    val Book: ImageVector by lazy {
        materialIcon {
            // An open book: two facing pages meeting at the spine.
            path {
                moveTo(12f, 6.5f)
                curveTo(10.3f, 5.2f, 8.1f, 4.5f, 5.5f, 4.5f)
                curveTo(4.4f, 4.5f, 3.4f, 4.6f, 2.5f, 4.9f)
                verticalLineTo(18.4f)
                curveTo(3.4f, 18.1f, 4.4f, 18f, 5.5f, 18f)
                curveTo(8.1f, 18f, 10.3f, 18.7f, 12f, 20f)
                curveTo(13.7f, 18.7f, 15.9f, 18f, 18.5f, 18f)
                curveTo(19.6f, 18f, 20.6f, 18.1f, 21.5f, 18.4f)
                verticalLineTo(4.9f)
                curveTo(20.6f, 4.6f, 19.6f, 4.5f, 18.5f, 4.5f)
                curveTo(15.9f, 4.5f, 13.7f, 5.2f, 12f, 6.5f)
                close()
            }
        }
    }

    val ArrowBack: ImageVector by lazy {
        materialIcon {
            // A leftwards arrow: shaft plus chevron head, for the shared top bar's back button.
            path {
                moveTo(20f, 11f)
                horizontalLineTo(7.8f)
                lineTo(13.4f, 5.4f)
                lineTo(12f, 4f)
                lineTo(4f, 12f)
                lineTo(12f, 20f)
                lineTo(13.4f, 18.6f)
                lineTo(7.8f, 13f)
                horizontalLineTo(20f)
                close()
            }
        }
    }

    val Storefront: ImageVector by lazy {
        materialIcon {
            // An awning over a shopfront.
            path {
                moveTo(4f, 4f)
                horizontalLineTo(20f)
                verticalLineTo(6f)
                horizontalLineTo(4f)
                close()
                moveTo(2f, 8f)
                horizontalLineTo(22f)
                lineTo(20.5f, 12f)
                horizontalLineTo(3.5f)
                close()
                moveTo(4f, 13f)
                horizontalLineTo(20f)
                verticalLineTo(20f)
                horizontalLineTo(14f)
                verticalLineTo(15.5f)
                horizontalLineTo(10f)
                verticalLineTo(20f)
                horizontalLineTo(4f)
                close()
            }
        }
    }
}

private fun materialIcon(block: ImageVector.Builder.() -> ImageVector.Builder): ImageVector =
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).block().build()

private fun ImageVector.Builder.path(block: PathBuilder.() -> Unit): ImageVector.Builder =
    addPath(pathData = PathData(block), fill = SolidColor(Color.Black))
