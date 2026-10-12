package de.coldtea.verborum.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The app's icons, drawn here rather than pulled from `material-icons-extended`: the set is small,
 * and a dependency that ships every Material glyph is a large addition to a Wasm bundle.
 *
 * Most of them are the **Android app's own vector drawables**, their path data copied across
 * verbatim and parsed at runtime by [PathParser]. That is the point: an icon redrawn by hand is an
 * icon that looks subtly different on each platform, and the three clients are meant to be one
 * product. When a drawable changes in the Android project, replace the string here — do not adjust
 * it to taste.
 *
 * A few have no Android counterpart, because the screen that needs them does not exist there: the
 * on-screen keyboard is a web affordance for scripts a physical keyboard cannot type. Those are
 * built with the Compose path DSL below.
 *
 * The fill colour is arbitrary throughout. `Icon` tints with a colour filter, so it replaces
 * whatever is declared here; black is used only so the file reads consistently.
 */
object VerborumIcons {

    /** `ic_search_24` in the Android app. */
    val Search: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59,16,11.11,16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 " +
                    "5.91,16 9.5,16c1.61,0 3.09,-0.59 " +
                    "4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 " +
                    "14,7.01 14,9.5 11.99,14 9.5,14z",
            )
        }
    }

    /** `ic_more_vert_24` in the Android app. */
    val MoreVertical: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 " +
                    "2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z",
            )
        }
    }

    /** `ic_plus_24` in the Android app. */
    val Add: ImageVector by lazy {
        androidIcon(viewportWidth = 960f) {
            androidPath(
                pathData = "M440,520L200,520L200,440L440,440L440,200L520,200L520,440L760,440L760,520L520,520L520,760L440,760L440,520Z",
            )
        }
    }

    /** `ic_close_24` in the Android app. */
    val Close: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 " +
                    "13.41,12z",
            )
        }
    }

    /** `ic_check_24` in the Android app. */
    val Check: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData = "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z",
            )
        }
    }

    /** `ic_check_square_24` in the Android app. */
    val CheckSquare: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M2,12C2,7.286 2,4.929 3.464,3.464C4.929,2 7.286,2 12,2C16.714,2 19.071,2 20.535,3.464C22,4.929 " +
                    "22,7.286 22,12C22,16.714 22,19.071 20.535,20.535C19.071,22 16.714,22 12,22C7.286,22 4.929,22 " +
                    "3.464,20.535C2,19.071 2,16.714 2,12Z",
                isStroked = true,
                strokeWidth = 1.5f,
                isFilled = false,
            )
            androidPath(
                pathData = "M8.5,12.5L10.5,14.5L15.5,9.5",
                isStroked = true,
                strokeWidth = 1.5f,
                isFilled = false,
            )
        }
    }

    /** `ic_edit_24` in the Android app. */
    val Edit: ImageVector by lazy {
        androidIcon(viewportWidth = 960f) {
            androidPath(
                pathData =
                    "M200,760L257,760L648,369L591,312L200,703L200,760ZM120,840L120,669L648,142Q660,130 " +
                    "675,124Q690,118 706,118Q722,118 737,124Q752,130 764,142L818,198Q830,210 835.5,225Q841,240 " +
                    "841,255Q841,271 835.5,286Q830,301 " +
                    "818,313L291,840L120,840ZM760,256L704,200L760,256ZM620,341L591,312L648,369L620,341Z",
            )
        }
    }

    /** `ic_delete_24` in the Android app. */
    val Delete: ImageVector by lazy {
        androidIcon(viewportWidth = 40f) {
            androidPath(
                pathData = "M8,0L32,0A8,8 0,0 1,40 8L40,32A8,8 0,0 1,32 40L8,40A8,8 0,0 1,0 32L0,8A8,8 0,0 1,8 0z",
                fillAlpha = 0.4f,
            )
            androidPath(
                pathData =
                    "M28.62,12.834C28.461,12.661 28.241,12.555 28.007,12.536L27.937,12.533C27.701,12.532 " +
                    "27.474,12.62 27.301,12.78C27.128,12.939 27.021,13.159 " +
                    "27.003,13.393H12.997L12.989,13.324C12.951,13.086 12.823,12.871 12.631,12.725C12.439,12.579 " +
                    "12.198,12.513 11.959,12.541C11.719,12.569 11.5,12.689 11.346,12.875C11.193,13.061 11.118,13.3 " +
                    "11.136,13.54L12.468,30.414L12.478,30.507C12.536,30.957 12.756,31.37 " +
                    "13.097,31.669C13.438,31.968 13.876,32.133 14.329,32.133H25.671L25.765,32.131C26.218,32.108 " +
                    "26.647,31.921 26.972,31.605C27.297,31.289 27.496,30.866 " +
                    "27.531,30.414L28.864,13.54L28.867,13.47C28.868,13.235 28.779,13.008 28.62,12.834Z",
            )
            androidPath(
                pathData =
                    "M30.267,11.6C30.509,11.599 30.742,11.692 30.917,11.86C31.092,12.027 31.194,12.256 " +
                    "31.204,12.498C31.213,12.74 31.127,12.976 30.965,13.157C30.804,13.337 30.578,13.447 " +
                    "30.337,13.464L30.267,13.467H9.733C9.491,13.468 9.258,13.375 9.083,13.207C8.908,13.039 " +
                    "8.806,12.81 8.796,12.568C8.787,12.326 8.873,12.09 9.034,11.91C9.196,11.73 9.422,11.619 " +
                    "9.663,11.602L9.733,11.6H30.267Z",
            )
            androidPath(
                pathData =
                    "M22.333,7.867C23.058,7.867 23.754,8.147 24.276,8.65C24.798,9.153 25.104,9.838 " +
                    "25.131,10.562L25.133,10.667V12.533C25.134,12.776 25.041,13.009 24.874,13.184C24.706,13.358 " +
                    "24.477,13.461 24.235,13.47C23.993,13.479 23.757,13.394 23.577,13.232C23.396,13.07 " +
                    "23.286,12.845 23.269,12.603L23.267,12.533V10.667C23.267,10.431 23.178,10.204 " +
                    "23.018,10.032C22.858,9.859 22.638,9.753 22.403,9.736L22.333,9.733H17.667C17.431,9.733 " +
                    "17.204,9.822 17.032,9.982C16.859,10.142 16.753,10.362 " +
                    "16.736,10.597L16.733,10.667V12.533C16.734,12.776 16.641,13.009 16.474,13.184C16.306,13.358 " +
                    "16.077,13.461 15.835,13.47C15.593,13.479 15.357,13.394 15.177,13.232C14.996,13.07 " +
                    "14.886,12.845 14.869,12.603L14.867,12.533V10.667C14.867,9.942 15.147,9.246 " +
                    "15.65,8.724C16.153,8.202 16.838,7.896 17.562,7.869L17.667,7.867H22.333Z",
            )
            androidPath(
                pathData =
                    "M23.733,26.533C24.249,26.533 24.667,26.951 24.667,27.467V27.467C24.667,27.589 24.642,27.711 " +
                    "24.596,27.824C24.549,27.937 24.48,28.04 24.393,28.127C24.307,28.213 24.204,28.282 " +
                    "24.09,28.329C23.977,28.376 23.856,28.4 23.733,28.4H16.267C16.144,28.4 16.023,28.376 " +
                    "15.91,28.329C15.796,28.282 15.693,28.213 15.607,28.127C15.52,28.04 15.451,27.937 " +
                    "15.404,27.824C15.358,27.711 15.333,27.589 15.333,27.467V27.467C15.333,26.951 15.751,26.533 " +
                    "16.267,26.533H23.733Z",
            )
        }
    }

    /** `ic_sort_24` in the Android app. */
    val Sort: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData = "M3,18h6v-2L3,16v2zM3,6v2h18L21,6L3,6zM3,13h12v-2L3,11v2z",
            )
        }
    }

    /** `ic_chevron_right_24` in the Android app. */
    val ChevronRight: ImageVector by lazy {
        androidIcon(viewportWidth = 960f) {
            androidPath(pathData = ChevronRightPath)
        }
    }

    /**
     * The same glyph a quarter-turn clockwise. The Android app ships one chevron and rotates it at
     * the call site; rotating it here instead keeps the dozen places that want a downward caret
     * free of rotation arithmetic, and guarantees they all get the identical shape.
     */
    val ChevronDown: ImageVector by lazy {
        androidIcon(viewportWidth = 960f) {
            group(rotate = 90f, pivotX = ChevronPivot, pivotY = ChevronPivot) {
                androidPath(pathData = ChevronRightPath)
            }
        }
    }

    /** `ic_book_24_black` in the Android app. */
    val Book: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M4,19V6.2C4,5.08 4,4.52 4.218,4.092C4.41,3.716 4.716,3.41 5.092,3.218C5.52,3 6.08,3 " +
                    "7.2,3H16.8C17.92,3 18.48,3 18.908,3.218C19.284,3.41 19.59,3.716 19.782,4.092C20,4.52 20,5.08 " +
                    "20,6.2V17H6C4.895,17 4,17.895 4,19ZM4,19C4,20.105 4.895,21 6,21H20M9,7H15M9,11H15M19,17V21",
                isStroked = true,
                strokeWidth = 2f,
                isFilled = false,
            )
        }
    }

    /** `baseline_book_24` in the Android app, where it marks the Bibliotheca tab. */
    val BookFilled: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M18,2H6c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 " +
                    "2,-2V4c0,-1.1 -0.9,-2 -2,-2zM6,4h5v8l-2.5,-1.5L6,12V4z",
            )
        }
    }

    /**
     * `baseline_account_balance_24` in the Android app, where it marks the Forum tab — a portico,
     * for the place the name comes from.
     */
    val AccountBalance: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(pathData = "M4,10h3v7h-3z")
            androidPath(pathData = "M10.5,10h3v7h-3z")
            androidPath(pathData = "M2,19h20v3h-20z")
            androidPath(pathData = "M17,10h3v7h-3z")
            androidPath(pathData = "M12,1l-10,5l0,2l20,0l0,-2z")
        }
    }

    /** `ic_play_24` in the Android app. */
    val Play: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M16.658,9.286C18.098,10.186 18.818,10.636 19.065,11.212C19.28,11.715 19.28,12.285 " +
                    "19.065,12.788C18.818,13.364 18.098,13.814 16.658,14.714L9.896,18.94C8.298,19.939 7.499,20.438 " +
                    "6.84,20.385C6.265,20.339 5.738,20.047 5.394,19.584C5,19.053 5,18.111 5,16.226V7.774C5,5.889 " +
                    "5,4.947 5.394,4.416C5.738,3.953 6.265,3.661 6.84,3.615C7.499,3.562 8.298,4.061 " +
                    "9.896,5.06L16.658,9.286Z",
                isStroked = true,
                strokeWidth = 2f,
                isFilled = false,
            )
        }
    }

    /** `ic_arrow_back_24` in the Android app. */
    val ArrowBack: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData = "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z",
            )
        }
    }

    /** `ic_logout_24` in the Android app. */
    val Logout: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M17,7l-1.41,1.41L18.17,11H8v2h10.17l-2.58,2.58L17,17l5,-5zM4,5h8V3H4c-1.1,0 -2,0.9 " +
                    "-2,2v14c0,1.1 0.9,2 2,2h8v-2H4V5z",
            )
        }
    }

    /** `ic_person_24` in the Android app. */
    val Person: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M12,6c1.1,0 2,0.9 2,2s-0.9,2 -2,2 -2,-0.9 -2,-2 0.9,-2 2,-2m0,10c2.7,0 5.8,1.29 " +
                    "6,2L6,18c0.23,-0.72 3.31,-2 6,-2m0,-12C9.79,4 8,5.79 8,8s1.79,4 4,4 4,-1.79 4,-4 -1.79,-4 " +
                    "-4,-4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z",
            )
        }
    }

    /** `ic_language_24` in the Android app. */
    val Language: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 " +
                    "11.99,2zM18.92,8h-2.95c-0.32,-1.25 -0.78,-2.45 -1.38,-3.56 1.84,0.63 3.37,1.91 " +
                    "4.33,3.56zM12,4.04c0.83,1.2 1.48,2.53 1.91,3.96h-3.82c0.43,-1.43 1.08,-2.76 " +
                    "1.91,-3.96zM4.26,14C4.1,13.36 4,12.69 4,12s0.1,-1.36 0.26,-2h3.38c-0.08,0.66 -0.14,1.32 " +
                    "-0.14,2s0.06,1.34 0.14,2L4.26,14zM5.08,16h2.95c0.32,1.25 0.78,2.45 1.38,3.56 -1.84,-0.63 " +
                    "-3.37,-1.9 -4.33,-3.56zM8.03,8L5.08,8c0.96,-1.66 2.49,-2.93 4.33,-3.56C8.81,5.55 8.35,6.75 " +
                    "8.03,8zM12,19.96c-0.83,-1.2 -1.48,-2.53 -1.91,-3.96h3.82c-0.43,1.43 -1.08,2.76 " +
                    "-1.91,3.96zM14.34,14L9.66,14c-0.09,-0.66 -0.16,-1.32 -0.16,-2s0.07,-1.35 " +
                    "0.16,-2h4.68c0.09,0.65 0.16,1.32 0.16,2s-0.07,1.34 -0.16,2zM14.59,19.56c0.6,-1.11 1.06,-2.31 " +
                    "1.38,-3.56h2.95c-0.96,1.65 -2.49,2.93 -4.33,3.56zM16.36,14c0.08,-0.66 0.14,-1.32 " +
                    "0.14,-2s-0.06,-1.34 -0.14,-2h3.38c0.16,0.64 0.26,1.31 0.26,2s-0.1,1.36 -0.26,2h-3.38z",
            )
        }
    }

    /** `ic_star_24` in the Android app. */
    val Star: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData = "M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z",
            )
        }
    }

    /** `ic_star_outline_24` in the Android app. */
    val StarOutline: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M22,9.24l-7.19,-0.62L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21 12,17.27 " +
                    "18.18,21l-1.63,-7.03L22,9.24zM12,15.4l-3.76,2.27 1,-4.28 -3.32,-2.88 " +
                    "4.38,-0.38L12,6.1l1.71,4.04 4.38,0.38 -3.32,2.88 1,4.28L12,15.4z",
            )
        }
    }

    /** `ic_star_half_24` in the Android app. */
    val StarHalf: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M22,9.24l-7.19,-0.62L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21 12,17.27 " +
                    "18.18,21l-1.63,-7.03L22,9.24zM12,15.4V6.1l1.71,4.04 4.38,0.38 -3.32,2.88 1,4.28L12,15.4z",
            )
        }
    }

    /** `ic_download_24` in the Android app. */
    val Download: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData = "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z",
            )
        }
    }

    /** `ic_switch_48` in the Android app. */
    val SwapHorizontal: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            androidPath(
                pathData =
                    "M18.715,1.716C18.324,1.324 18.324,0.687 18.715,0.294C19.107,-0.098 19.741,-0.098 " +
                    "20.132,0.294L22.404,2.574L22.417,2.587C23.194,3.372 23.192,4.641 " +
                    "22.412,5.423L20.137,7.706C19.746,8.098 19.112,8.098 18.721,7.706C18.33,7.313 18.33,6.677 " +
                    "18.721,6.285L20.002,5L5,5C4.448,5 4,5.448 4,6V13C4,13.552 3.552,14 3,14C2.448,14 2,13.552 " +
                    "2,13V6C2,4.343 3.343,3 5,3H19.995L18.715,1.716Z",
            )
            androidPath(
                pathData =
                    "M22,11C22,10.448 21.552,10 21,10C20.448,10 20,10.448 20,11V18C20,18.552 19.552,19 " +
                    "19,19L4.003,19L5.282,17.716C5.673,17.323 5.673,16.687 5.282,16.294C4.891,15.902 4.257,15.902 " +
                    "3.865,16.294L1.594,18.574L1.581,18.587C0.804,19.372 0.806,20.641 " +
                    "1.586,21.423L3.86,23.706C4.251,24.098 4.885,24.098 5.276,23.706C5.667,23.313 5.667,22.677 " +
                    "5.276,22.285L3.996,21L19,21C20.657,21 22,19.657 22,18L22,11Z",
            )
        }
    }


    // ---- no Android counterpart ----------------------------------------------------------
    //
    // The on-screen keyboard is a web affordance: it types scripts a physical keyboard cannot,
    // and the Android app has the system keyboard instead. `Settings` likewise belongs to a row
    // the Android Options screen does not have.

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

    /** `baseline_settings_24` in the Android app, where it marks the Options tab. */
    val Settings: ImageVector by lazy {
        androidIcon(viewportWidth = 24f) {
            // The gear's centre punches out by winding, which is why the inner circle has to stay
            // part of this one path rather than become a second one.
            androidPath(
                pathData =
                    "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 " +
                    "-0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 " +
                    "-0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 " +
                    "-1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 " +
                    "-0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 " +
                    "-0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 " +
                    "4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 " +
                    "-0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 " +
                    "1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 " +
                    "0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 " +
                    "0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 " +
                    "-0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 " +
                    "3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z",
            )
        }
    }
}

/**
 * One path of an Android drawable, by its `pathData`.
 *
 * Parsed rather than transcribed into the Compose DSL. The SVG grammar has implicit repeated
 * commands and relative variants that a careful hand-conversion still gets wrong — and a wrong
 * conversion does not fail, it just draws something slightly else.
 */
private fun ImageVector.Builder.androidPath(
    pathData: String,
    isFilled: Boolean = true,
    isStroked: Boolean = false,
    strokeWidth: Float = 0f,
    isEvenOdd: Boolean = false,
    /**
     * The drawable's own `fillAlpha`. It has to be carried across: `Icon` replaces the hue but
     * keeps the alpha, which is how a drawable that layers a faint plate under a solid glyph still
     * reads as two tones once tinted. Flattening it to 1 paints the plate over the glyph.
     */
    fillAlpha: Float = 1f,
): ImageVector.Builder = addPath(
    pathData = PathParser().parsePathString(pathData).toNodes(),
    pathFillType = if (isEvenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
    fill = if (isFilled) SolidColor(Color.Black) else null,
    fillAlpha = fillAlpha,
    stroke = if (isStroked) SolidColor(Color.Black) else null,
    strokeLineWidth = strokeWidth,
    // The drawables that stroke their outline all round their ends; a mitre on a 2dp line at icon
    // size shows up as a spur.
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
)

/** An icon ported from an Android drawable, which may declare a viewport other than 24. */
private fun androidIcon(
    viewportWidth: Float = 24f,
    viewportHeight: Float = viewportWidth,
    block: ImageVector.Builder.() -> ImageVector.Builder,
): ImageVector = ImageVector.Builder(
    defaultWidth = IconSize,
    defaultHeight = IconSize,
    viewportWidth = viewportWidth,
    viewportHeight = viewportHeight,
).block().build()

/** An icon with no Android counterpart, drawn with the Compose path DSL. */
private fun materialIcon(
    viewport: Float = 24f,
    block: ImageVector.Builder.() -> ImageVector.Builder,
): ImageVector = ImageVector.Builder(
    defaultWidth = IconSize,
    defaultHeight = IconSize,
    viewportWidth = viewport,
    viewportHeight = viewport,
).block().build()

private const val ChevronRightPath = "M321,880L250,809L579,480L250,151L321,80L721,480L321,880Z"

/** The centre of the chevron's 960 viewport, which the glyph is drawn around. */
private const val ChevronPivot = 480f

private val IconSize = 24.dp

