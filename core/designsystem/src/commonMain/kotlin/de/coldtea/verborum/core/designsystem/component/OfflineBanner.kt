package de.coldtea.verborum.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.common.observeConnectivity
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.strings

/**
 * Tracks whether the app currently has usable internet, for the offline banner. Starts out `true`
 * so a first frame never accuses the user of being offline before the platform has answered.
 */
@Composable
fun rememberIsOnline(): Boolean {
    val isOnline by observeConnectivity().collectAsStateWithLifecycle(initialValue = true)
    return isOnline
}

/**
 * Standing "you are offline" notice, shown directly beneath the shared top bar. Pinned there rather
 * than floating over the content: being offline is a standing condition, so it should stay visible
 * instead of timing out the way a snackbar would.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large, vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.offline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
