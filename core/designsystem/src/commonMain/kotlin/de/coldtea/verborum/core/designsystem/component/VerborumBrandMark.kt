package de.coldtea.verborum.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The app's mark: the book glyph on a gold tile. Lives here rather than in the screen that shows it
 * so its dimensions stay out of a feature module — and so a second entry point can reuse it.
 */
@Composable
fun VerborumBrandMark(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(MarkCornerRadius),
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.size(MarkSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = VerborumIcons.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(MarkIconSize),
            )
        }
    }
}

private val MarkSize = 88.dp
private val MarkIconSize = 44.dp
private val MarkCornerRadius = 24.dp
