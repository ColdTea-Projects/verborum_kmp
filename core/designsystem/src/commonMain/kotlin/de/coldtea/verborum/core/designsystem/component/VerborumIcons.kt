package de.coldtea.verborum.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The app's icons, drawn here rather than pulled from `material-icons-extended`: the set is small,
 * and a dependency that ships every Material glyph is a large addition to a Wasm bundle.
 *
 * Every shape uses Compose's own [path] builder and states its `fill` — the same shape the
 * SVG-to-Compose converters emit, so their output can be pasted in as it comes. Do not add a local
 * helper called `path` again: an explicit import outranks a same-file declaration, so the import
 * would silently capture every call in the file, and `fill` defaults to null — which paints nothing.
 *
 * The fill colour itself is arbitrary. `Icon` tints with a colour filter, so it replaces whatever is
 * declared here; black is used throughout only so the file reads consistently.
 */
object VerborumIcons {

    val Search: ImageVector by lazy {
        materialIcon {
            // A magnifier: ring plus handle.
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.5f, 3f)
                curveTo(6.9f, 3f, 4f, 5.9f, 4f, 9.5f)
                curveTo(4f, 13.1f, 6.9f, 16f, 10.5f, 16f)
                curveTo(11.9f, 16f, 13.2f, 15.6f, 14.2f, 14.8f)
                lineTo(19.1f, 19.7f)
                lineTo(20.5f, 18.3f)
                lineTo(15.6f, 13.4f)
                curveTo(16.4f, 12.3f, 17f, 11f, 17f, 9.5f)
                curveTo(17f, 5.9f, 14.1f, 3f, 10.5f, 3f)
                close()
                moveTo(10.5f, 5f)
                curveTo(13f, 5f, 15f, 7f, 15f, 9.5f)
                curveTo(15f, 12f, 13f, 14f, 10.5f, 14f)
                curveTo(8f, 14f, 6f, 12f, 6f, 9.5f)
                curveTo(6f, 7f, 8f, 5f, 10.5f, 5f)
                close()
            }
        }
    }

    val MoreVertical: ImageVector by lazy {
        materialIcon {
            // Three stacked dots — the row overflow affordance.
            path(fill = SolidColor(Color.Black)) {
                listOf(5f, 12f, 19f).forEach { centerY ->
                    moveTo(12f, centerY - 2f)
                    curveTo(13.1f, centerY - 2f, 14f, centerY - 1.1f, 14f, centerY)
                    curveTo(14f, centerY + 1.1f, 13.1f, centerY + 2f, 12f, centerY + 2f)
                    curveTo(10.9f, centerY + 2f, 10f, centerY + 1.1f, 10f, centerY)
                    curveTo(10f, centerY - 1.1f, 10.9f, centerY - 2f, 12f, centerY - 2f)
                    close()
                }
            }
        }
    }

    val Add: ImageVector by lazy {
        materialIcon {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 4f)
                horizontalLineTo(13f)
                verticalLineTo(11f)
                horizontalLineTo(20f)
                verticalLineTo(13f)
                horizontalLineTo(13f)
                verticalLineTo(20f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                horizontalLineTo(4f)
                verticalLineTo(11f)
                horizontalLineTo(11f)
                close()
            }
        }
    }

    val Close: ImageVector by lazy {
        materialIcon {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.3f, 7.1f)
                lineTo(16.9f, 5.7f)
                lineTo(12f, 10.6f)
                lineTo(7.1f, 5.7f)
                lineTo(5.7f, 7.1f)
                lineTo(10.6f, 12f)
                lineTo(5.7f, 16.9f)
                lineTo(7.1f, 18.3f)
                lineTo(12f, 13.4f)
                lineTo(16.9f, 18.3f)
                lineTo(18.3f, 16.9f)
                lineTo(13.4f, 12f)
                close()
            }
        }
    }

    val Check: ImageVector by lazy {
        materialIcon {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9.6f, 16.2f)
                lineTo(5.4f, 12f)
                lineTo(4f, 13.4f)
                lineTo(9.6f, 19f)
                lineTo(20f, 8.6f)
                lineTo(18.6f, 7.2f)
                close()
            }
        }
    }

    val Edit: ImageVector by lazy {
        materialIcon {
            // A pencil pointing at the lower left.
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 17.2f)
                verticalLineTo(21f)
                horizontalLineTo(6.8f)
                lineTo(17.8f, 10f)
                lineTo(14f, 6.2f)
                close()
                moveTo(20.7f, 7.0f)
                curveTo(21.1f, 6.6f, 21.1f, 6f, 20.7f, 5.6f)
                lineTo(18.4f, 3.3f)
                curveTo(18f, 2.9f, 17.4f, 2.9f, 17f, 3.3f)
                lineTo(15.2f, 5.1f)
                lineTo(19f, 8.9f)
                close()
            }
        }
    }

    val Delete: ImageVector by lazy {
        materialIcon {
            // A bin with a lid.
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 3f)
                horizontalLineTo(15f)
                verticalLineTo(5f)
                horizontalLineTo(20f)
                verticalLineTo(7f)
                horizontalLineTo(4f)
                verticalLineTo(5f)
                horizontalLineTo(9f)
                close()
                moveTo(6f, 8f)
                horizontalLineTo(18f)
                verticalLineTo(20f)
                curveTo(18f, 20.6f, 17.6f, 21f, 17f, 21f)
                horizontalLineTo(7f)
                curveTo(6.4f, 21f, 6f, 20.6f, 6f, 20f)
                close()
            }
        }
    }

    val Settings: ImageVector by lazy {
        materialIcon {
            // Sliders rather than a gear: a gear's centre hole needs opposite path winding to punch
            // out, and two adjustable rows read as "settings" just as clearly.
            path(fill = SolidColor(Color.Black)) {
                // Upper slider: track, then its knob.
                moveTo(3f, 7f)
                horizontalLineTo(13f)
                verticalLineTo(9f)
                horizontalLineTo(3f)
                close()
                moveTo(19f, 7f)
                horizontalLineTo(21f)
                verticalLineTo(9f)
                horizontalLineTo(19f)
                close()
                moveTo(16f, 4.5f)
                horizontalLineTo(18f)
                verticalLineTo(11.5f)
                horizontalLineTo(16f)
                close()

                // Lower slider.
                moveTo(3f, 15f)
                horizontalLineTo(7f)
                verticalLineTo(17f)
                horizontalLineTo(3f)
                close()
                moveTo(13f, 15f)
                horizontalLineTo(21f)
                verticalLineTo(17f)
                horizontalLineTo(13f)
                close()
                moveTo(10f, 12.5f)
                horizontalLineTo(12f)
                verticalLineTo(19.5f)
                horizontalLineTo(10f)
                close()
            }
        }
    }

    val Sort: ImageVector by lazy {
        materialIcon {
            // Three lines of decreasing width.
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 6f)
                horizontalLineTo(20f)
                verticalLineTo(8f)
                horizontalLineTo(4f)
                close()
                moveTo(4f, 11f)
                horizontalLineTo(15f)
                verticalLineTo(13f)
                horizontalLineTo(4f)
                close()
                moveTo(4f, 16f)
                horizontalLineTo(10f)
                verticalLineTo(18f)
                horizontalLineTo(4f)
                close()
            }
        }
    }

    val ChevronDown: ImageVector by lazy {
        materialIcon {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7.4f, 9f)
                lineTo(6f, 10.4f)
                lineTo(12f, 16.4f)
                lineTo(18f, 10.4f)
                lineTo(16.6f, 9f)
                lineTo(12f, 13.6f)
                close()
            }
        }
    }

    val Book: ImageVector by lazy {
        materialIcon {
            // An open book: two facing pages meeting at the spine.
            path(fill = SolidColor(Color.Black)) {
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

    val Backspace: ImageVector by lazy {
        materialIcon {
            // A leftwards-pointing key with a cross in it.
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 5f)
                horizontalLineTo(21f)
                verticalLineTo(19f)
                horizontalLineTo(9f)
                lineTo(2f, 12f)
                close()
            }
        }
    }

    val ShiftUp: ImageVector by lazy {
        materialIcon {
            // A hollow upwards arrow: the shift key.
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 4f)
                lineTo(20f, 12f)
                horizontalLineTo(15.5f)
                verticalLineTo(19f)
                horizontalLineTo(8.5f)
                verticalLineTo(12f)
                horizontalLineTo(4f)
                close()
            }
        }
    }

    val EnterKey: ImageVector by lazy {
        materialIcon {
            // The return arrow: down the right-hand side, then left into a chevron head.
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 5f)
                horizontalLineTo(21f)
                verticalLineTo(15f)
                horizontalLineTo(8f)
                verticalLineTo(19f)
                lineTo(2f, 14f)
                lineTo(8f, 9f)
                verticalLineTo(13f)
                horizontalLineTo(19f)
                close()
            }
        }
    }

    val Keyboard: ImageVector by lazy {
        materialIcon {
            path(
                fill = SolidColor(Color(0xFF1C274C))
            ) {
                moveTo(7f, 9f)
                curveTo(7f, 9.55228f, 6.55228f, 10f, 6f, 10f)
                curveTo(5.44772f, 10f, 5f, 9.55228f, 5f, 9f)
                curveTo(5f, 8.44772f, 5.44772f, 8f, 6f, 8f)
                curveTo(6.55228f, 8f, 7f, 8.44772f, 7f, 9f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF1C274C))
            ) {
                moveTo(7f, 12f)
                curveTo(7f, 12.5523f, 6.55228f, 13f, 6f, 13f)
                curveTo(5.44772f, 13f, 5f, 12.5523f, 5f, 12f)
                curveTo(5f, 11.4477f, 5.44772f, 11f, 6f, 11f)
                curveTo(6.55228f, 11f, 7f, 11.4477f, 7f, 12f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF1C274C))
            ) {
                moveTo(10f, 12f)
                curveTo(10f, 12.5523f, 9.55228f, 13f, 9f, 13f)
                curveTo(8.44772f, 13f, 8f, 12.5523f, 8f, 12f)
                curveTo(8f, 11.4477f, 8.44772f, 11f, 9f, 11f)
                curveTo(9.55228f, 11f, 10f, 11.4477f, 10f, 12f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF1C274C))
            ) {
                moveTo(10f, 9f)
                curveTo(10f, 9.55228f, 9.55228f, 10f, 9f, 10f)
                curveTo(8.44772f, 10f, 8f, 9.55228f, 8f, 9f)
                curveTo(8f, 8.44772f, 8.44772f, 8f, 9f, 8f)
                curveTo(9.55228f, 8f, 10f, 8.44772f, 10f, 9f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF1C274C))
            ) {
                moveTo(13f, 9f)
                curveTo(13f, 9.55228f, 12.5523f, 10f, 12f, 10f)
                curveTo(11.4477f, 10f, 11f, 9.55228f, 11f, 9f)
                curveTo(11f, 8.44772f, 11.4477f, 8f, 12f, 8f)
                curveTo(12.5523f, 8f, 13f, 8.44772f, 13f, 9f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF1C274C))
            ) {
                moveTo(13f, 12f)
                curveTo(13f, 12.5523f, 12.5523f, 13f, 12f, 13f)
                curveTo(11.4477f, 13f, 11f, 12.5523f, 11f, 12f)
                curveTo(11f, 11.4477f, 11.4477f, 11f, 12f, 11f)
                curveTo(12.5523f, 11f, 13f, 11.4477f, 13f, 12f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF1C274C))
            ) {
                moveTo(16f, 9f)
                curveTo(16f, 9.55228f, 15.5523f, 10f, 15f, 10f)
                curveTo(14.4477f, 10f, 14f, 9.55228f, 14f, 9f)
                curveTo(14f, 8.44772f, 14.4477f, 8f, 15f, 8f)
                curveTo(15.5523f, 8f, 16f, 8.44772f, 16f, 9f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF1C274C))
            ) {
                moveTo(16f, 12f)
                curveTo(16f, 12.5523f, 15.5523f, 13f, 15f, 13f)
                curveTo(14.4477f, 13f, 14f, 12.5523f, 14f, 12f)
                curveTo(14f, 11.4477f, 14.4477f, 11f, 15f, 11f)
                curveTo(15.5523f, 11f, 16f, 11.4477f, 16f, 12f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF1C274C))
            ) {
                moveTo(19f, 9f)
                curveTo(19f, 9.55228f, 18.5523f, 10f, 18f, 10f)
                curveTo(17.4477f, 10f, 17f, 9.55228f, 17f, 9f)
                curveTo(17f, 8.44772f, 17.4477f, 8f, 18f, 8f)
                curveTo(18.5523f, 8f, 19f, 8.44772f, 19f, 9f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF1C274C))
            ) {
                moveTo(19f, 12f)
                curveTo(19f, 12.5523f, 18.5523f, 13f, 18f, 13f)
                curveTo(17.4477f, 13f, 17f, 12.5523f, 17f, 12f)
                curveTo(17f, 11.4477f, 17.4477f, 11f, 18f, 11f)
                curveTo(18.5523f, 11f, 19f, 11.4477f, 19f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF1C274C)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16f, 5f)
                curveTo(18.8284f, 5f, 20.2426f, 5f, 21.1213f, 5.87868f)
                curveTo(22f, 6.75736f, 22f, 8.17157f, 22f, 11f)
                verticalLineTo(13f)
                curveTo(22f, 15.8284f, 22f, 17.2426f, 21.1213f, 18.1213f)
                curveTo(20.2426f, 19f, 18.8284f, 19f, 16f, 19f)
                horizontalLineTo(8f)
                curveTo(5.17157f, 19f, 3.75736f, 19f, 2.87868f, 18.1213f)
                curveTo(2f, 17.2426f, 2f, 15.8284f, 2f, 13f)
                verticalLineTo(11f)
                curveTo(2f, 8.17157f, 2f, 6.75736f, 2.87868f, 5.87868f)
                curveTo(3.75736f, 5f, 5.17157f, 5f, 8f, 5f)
                horizontalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF1C274C)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7f, 16f)
                horizontalLineTo(17f)
            }
        }
    }

    val Play: ImageVector by lazy {
        materialIcon {
            // A solid rightwards triangle: "start practising".
            path(fill = SolidColor(Color.Black)) {
                moveTo(8f, 5f)
                lineTo(19f, 12f)
                lineTo(8f, 19f)
                close()
            }
        }
    }

    val ArrowBack: ImageVector by lazy {
        materialIcon {
            // A leftwards arrow: shaft plus chevron head, for the shared top bar's back button.
            path(fill = SolidColor(Color.Black)) {
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

    val Logout: ImageVector by lazy {
        materialIcon {
            // An arrow leaving through a doorway.
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 7f)
                lineTo(9.6f, 8.4f)
                lineTo(12.2f, 11f)
                horizontalLineTo(4f)
                verticalLineTo(13f)
                horizontalLineTo(12.2f)
                lineTo(9.6f, 15.6f)
                lineTo(11f, 17f)
                lineTo(16f, 12f)
                close()
                moveTo(14f, 3f)
                horizontalLineTo(20f)
                verticalLineTo(21f)
                horizontalLineTo(14f)
                verticalLineTo(19f)
                horizontalLineTo(18f)
                verticalLineTo(5f)
                horizontalLineTo(14f)
                close()
            }
        }
    }

    val Storefront: ImageVector by lazy {
        materialIcon {
            // An awning over a shopfront.
            path(fill = SolidColor(Color.Black)) {
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

/**
 * The frame every icon is drawn in. [viewport] matches the source SVG's `viewBox`, so an icon drawn
 * on a grid other than 24 keeps its proportions.
 */
private fun materialIcon(
    viewport: Float = 24f,
    block: ImageVector.Builder.() -> ImageVector.Builder,
): ImageVector = ImageVector.Builder(
    defaultWidth = IconSize,
    defaultHeight = IconSize,
    viewportWidth = viewport,
    viewportHeight = viewport,
).block().build()

private val IconSize = 24.dp
