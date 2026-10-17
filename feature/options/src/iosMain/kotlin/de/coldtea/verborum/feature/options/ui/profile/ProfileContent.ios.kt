package de.coldtea.verborum.feature.options.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.feature.options.ui.profile.composables.LeaveForumDialog
import de.coldtea.verborum.feature.options.ui.profile.composables.MarketplaceTermsSection
import de.coldtea.verborum.feature.options.ui.profile.model.ProfileForm

/**
 * The profile form as a phone screen, matching the Android app: everything above the Update button
 * scrolls, and the button itself never does. A primary action that can be scrolled out of reach is
 * the one thing a form this short must not do — the terms text expands to several paragraphs, which
 * is exactly when the user wants the button.
 */
@Composable
internal actual fun ProfileContent(
    form: ProfileForm,
    onDisplayNameChange: (String) -> Unit,
    onAgreementChange: (Boolean) -> Unit,
    onUpdate: () -> Unit,
    onConfirmLeave: () -> Unit,
    onDismissLeave: () -> Unit,
    modifier: Modifier,
) {
    val strings = LocalStrings.current

    if (form.isLeaveConfirmationShown) {
        LeaveForumDialog(onConfirm = onConfirmLeave, onDismiss = onDismissLeave)
    }

    ContentColumn(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(Spacing.medium))

            OutlinedTextField(
                value = form.email,
                onValueChange = {},
                // Read-only rather than disabled: the address has to stay fully legible, and a
                // disabled field greys it to the point of being hard to read.
                readOnly = true,
                label = { Text(strings.profileEmailLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.medium))

            OutlinedTextField(
                value = form.displayName,
                onValueChange = onDisplayNameChange,
                enabled = form.isEditable,
                label = { Text(strings.profileDisplayNameLabel) },
                singleLine = true,
                isError = form.isDisplayNameMissing,
                supportingText = {
                    Text(
                        if (form.isDisplayNameMissing) {
                            strings.profileDisplayNameRequired
                        } else {
                            strings.profileDisplayNameSupporting
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.medium))

            MarketplaceTermsSection(
                accepted = form.agreementAccepted,
                onAcceptedChange = onAgreementChange,
                isEnabled = form.isEditable,
            )

            Spacer(Modifier.height(Spacing.medium))
        }

        Spacer(Modifier.height(Spacing.medium))

        Button(
            onClick = onUpdate,
            enabled = form.canUpdate,
            shape = Shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.buttonHeight),
        ) {
            if (form.isSaving) {
                CircularProgressIndicator(
                    strokeWidth = SAVING_STROKE,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Dimens.iconLarge),
                )
            } else {
                Text(text = strings.profileUpdate, style = MaterialTheme.typography.titleSmall)
            }
        }

        Spacer(Modifier.height(Spacing.large))
    }
}

/** Thin enough to read as a spinner inside a button rather than as a second filled shape. */
private val SAVING_STROKE = 2.dp
