package de.coldtea.verborum.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp

/**
 * The app's icons, drawn here rather than pulled from `material-icons-extended`: the set is small,
 * and a dependency that ships every Material glyph is a large addition to a Wasm bundle.
 */
object VerborumIcons {

    val Search: ImageVector by lazy {
        materialIcon {
            // A magnifier: ring plus handle.
            path {
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
            path {
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
            path {
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
            path {
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
            path {
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
            path {
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
            path {
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

    val Sort: ImageVector by lazy {
        materialIcon {
            // Three lines of decreasing width.
            path {
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
            path {
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

    val Logout: ImageVector by lazy {
        materialIcon {
            // An arrow leaving through a doorway.
            path {
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
