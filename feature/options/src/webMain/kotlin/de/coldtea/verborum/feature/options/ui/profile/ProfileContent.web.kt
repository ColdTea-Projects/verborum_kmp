package de.coldtea.verborum.feature.options.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.component.ContentPane
import de.coldtea.verborum.core.designsystem.component.WebFieldLabel
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.component.WebPanel
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.component.WebTextField
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.feature.options.ui.profile.composables.LeaveForumDialog
import de.coldtea.verborum.feature.options.ui.profile.composables.WebReadOnlyField
import de.coldtea.verborum.feature.options.ui.profile.composables.WebTermsPanel
import de.coldtea.verborum.feature.options.ui.profile.model.ProfileForm

/**
 * The profile form as a desktop page: one titled pane, the fields in a single column, the terms in a
 * panel of their own, and Update at the end of the flow.
 *
 * Two deliberate departures from the phone layout. The button is **not** pinned to the window's
 * bottom edge — a browser window is tall enough that the whole form fits without scrolling, and a
 * bar fixed across the foot of a desktop page reads as app chrome rather than as this form's submit.
 * And the controls are capped at form width instead of spanning the pane, because a text field as
 * wide as a monitor gives no clue how much is expected in it.
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

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ContentPane(maxWidth = ContentWidth.Web.detail) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                WebPageSpacer(Spacing.extraLarge)

                WebPageTitle(
                    title = strings.optionsProfile,
                    subtitle = strings.profileDisplayNameSupporting,
                )

                WebPageSpacer()

                WebPanel {
                    Column(modifier = Modifier.widthIn(max = ContentWidth.reading)) {
                        WebFieldLabel(strings.profileEmailLabel)
                        WebReadOnlyField(value = form.email)

                        WebPageSpacer(Spacing.large)

                        WebFieldLabel(strings.profileDisplayNameLabel)
                        WebTextField(
                            value = form.displayName,
                            onValueChange = onDisplayNameChange,
                            isEnabled = !form.isSaving,
                        )
                        Text(
                            text = if (form.isDisplayNameMissing) {
                                strings.profileDisplayNameRequired
                            } else {
                                strings.profileDisplayNameSupporting
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (form.isDisplayNameMissing) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        WebPageSpacer(Spacing.large)

                        WebTermsPanel(
                            accepted = form.agreementAccepted,
                            onAcceptedChange = onAgreementChange,
                            isEnabled = !form.isSaving,
                        )

                        WebPageSpacer(Spacing.large)

                        WebPrimaryButton(
                            label = if (form.isSaving) "${strings.profileUpdate}…" else strings.profileUpdate,
                            onClick = onUpdate,
                            isEnabled = form.canUpdate,
                            modifier = Modifier.widthIn(max = ContentWidth.form),
                        )
                    }
                }

                WebPageSpacer(Spacing.extraLarge)
            }
        }
    }
}
