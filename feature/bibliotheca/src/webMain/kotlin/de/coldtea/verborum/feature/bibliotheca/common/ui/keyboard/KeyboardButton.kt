package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.core.localization.strings

/**
 * Opens the on-screen keyboard for a group of fields.
 *
 * It stays in place when there is no keyboard to offer — because no language has been chosen yet —
 * and greys out instead of vanishing: a control that disappears leaves the row jumping about and
 * gives no hint that anything is missing. Its colour carries the whole state: grey where there is
 * nothing to open, the accent lightened where a keyboard is available, and the full accent while
 * that keyboard is showing.
 *
 * [group] is what the keyboard treats as "here": pressing this stays on whichever of the group's
 * fields was last being typed in, and only moves to its first field when the cursor was elsewhere.
 */
@Composable
internal fun KeyboardButton(
    group: String,
    languageCode: String,
    modifier: Modifier = Modifier,
) {
    val controller = LocalKeyboardController.current
    val isEnabled = keyboardLayoutFor(languageCode) != null
    val isOpen = isEnabled && controller.isOpenFor(group)

    val tint = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
        isOpen -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = AvailableAlpha)
    }

    Icon(
        imageVector = VerborumIcons.Keyboard,
        contentDescription = when {
            !isEnabled -> strings.keyboardUnavailable
            isOpen -> strings.hideKeyboard(strings.languageName(languageCode))
            else -> strings.showKeyboard(strings.languageName(languageCode))
        },
        tint = tint,
        modifier = modifier
            .pointerHoverIcon(if (isEnabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(enabled = isEnabled) { controller.toggleFor(group) }
            .size(Dimens.iconLarge),
    )
}

/** Material's disabled opacity. */
private const val DisabledAlpha = 0.38f

/** Lightened accent: available, but not currently showing. */
private const val AvailableAlpha = 0.5f
