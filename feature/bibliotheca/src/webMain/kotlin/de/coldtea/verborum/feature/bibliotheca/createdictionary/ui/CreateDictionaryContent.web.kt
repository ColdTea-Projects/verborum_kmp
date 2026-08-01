package de.coldtea.verborum.feature.bibliotheca.createdictionary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.component.ContentPane
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.WebChip
import de.coldtea.verborum.core.designsystem.component.WebFieldLabel
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.component.WebSelect
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.KeyboardButton
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.KeyboardController
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.KeyboardTextField
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.LanguageKeyboardPopup
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.LocalKeyboardController
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.model.ALL_TAGS
import de.coldtea.verborum.core.localization.strings

/**
 * The dictionary form as a desktop page: a name, the language pair side by side, and the tag
 * catalogue as chips.
 *
 * Editing locks the two language selects. A dictionary's words are written *in* its language pair,
 * so changing the pair afterwards would leave every one of them mislabelled — the same rule the
 * Android app applies.
 */
@Composable
internal actual fun CreateDictionaryContent(
    state: CreateDictionaryUiState,
    onNameChanged: (String) -> Unit,
    onFromLanguageChanged: (SupportedLanguage) -> Unit,
    onToLanguageChanged: (SupportedLanguage) -> Unit,
    onTagToggled: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier,
) {
    // Registers the chrome as well as the way back: an unregistered screen reads to the shell as one
    // that wants no chrome at all, and would lose the sidebar with it.
    RegisterTopBar(
        title = if (state.isEditing) strings.editDictionaryTitle else strings.newDictionary,
        showBackButton = true,
        backLabel = strings.backToDictionaries,
    )

    val keyboardController = remember { KeyboardController() }

    CompositionLocalProvider(LocalKeyboardController provides keyboardController) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ContentPane(maxWidth = ContentWidth.Web.dictionaryForm) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                WebPageSpacer(Spacing.extraLarge)

                WebPageTitle(
                    title = if (state.isEditing) strings.editDictionaryTitle else strings.createDictionary,
                    subtitle = strings.createDictionarySubtitle,
                )

                WebPageSpacer()

                IdentityFields(
                    state = state,
                    onNameChanged = onNameChanged,
                    onFromLanguageChanged = onFromLanguageChanged,
                    onToLanguageChanged = onToLanguageChanged,
                )

                WebPageSpacer()

                WebFieldLabel("Tags")
                TagChips(selectedCodes = state.tags, onToggle = onTagToggled)

                WebPageSpacer()

                WebPrimaryButton(
                    label = when {
                        state.isSaving -> "Saving…"
                        state.isEditing -> "Save Changes"
                        else -> "Create Dictionary"
                    },
                    onClick = onSave,
                    // A dictionary without a name or a direction is not something the app can store.
                    isEnabled = state.canSave,
                )

                WebPageSpacer(Spacing.extraLarge)
            }
        }
    }
    }
}

/**
 * The language pair and the name — abreast where the window allows it, stacked where it does not.
 *
 * The pair leads in both arrangements. It is the choice that cannot be changed later, and the one
 * that decides what the name will be *about*, so it is asked first — and the keyboard the name field
 * offers depends on having answered it.
 *
 * Three controls in a row need real width before any of them is still readable, so the layout is
 * chosen from the space actually available rather than from a screen-size guess: all three abreast,
 * then the pair over the name, then one per line. Every arrangement fills the same width, which is
 * what lets the tags below line up with whichever one is showing.
 */
@Composable
private fun IdentityFields(
    state: CreateDictionaryUiState,
    onNameChanged: (String) -> Unit,
    onFromLanguageChanged: (SupportedLanguage) -> Unit,
    onToLanguageChanged: (SupportedLanguage) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val name: @Composable (Modifier) -> Unit = { fieldModifier ->
            // A dictionary's name might be written in either half of its pair — "Deutsch für
            // Anfänger" or "German Basics" — so the keyboard offers both and lets the user say.
            // Whichever languages are chosen so far are what it can offer.
            val languages = listOfNotNull(state.fromLanguage?.code, state.toLanguage?.code)

            NameField(
                name = state.name,
                keyboardLanguage = languages.firstOrNull().orEmpty(),
                alternativeKeyboardLanguage = languages.getOrNull(1),
                onNameChanged = onNameChanged,
                modifier = fieldModifier,
            )
        }
        val from: @Composable (Modifier) -> Unit = { fieldModifier ->
            LanguageField(
                label = strings.fromLanguage,
                selected = state.fromLanguage,
                // A dictionary's words are written in its pair, so changing it later would leave
                // every one of them mislabelled.
                isEnabled = !state.isEditing,
                onSelect = onFromLanguageChanged,
                modifier = fieldModifier,
            )
        }
        val to: @Composable (Modifier) -> Unit = { fieldModifier ->
            LanguageField(
                label = strings.toLanguage,
                selected = state.toLanguage,
                isEnabled = !state.isEditing,
                onSelect = onToLanguageChanged,
                modifier = fieldModifier,
            )
        }

        when {
            maxWidth >= ThreeAcross -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                from(Modifier.weight(1f))
                to(Modifier.weight(1f))
                name(Modifier.weight(NameWeight))
            }

            maxWidth >= PairAcross -> Column(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    from(Modifier.weight(1f))
                    to(Modifier.weight(1f))
                }

                WebPageSpacer(Spacing.medium)

                name(Modifier.fillMaxWidth())
            }

            else -> Column(modifier = Modifier.fillMaxWidth()) {
                from(Modifier.fillMaxWidth())
                WebPageSpacer(Spacing.medium)
                to(Modifier.fillMaxWidth())
                WebPageSpacer(Spacing.medium)
                name(Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * The dictionary's name, with its keyboard button opposite the label — title at the top-start of the
 * field, keyboard at the top-end.
 *
 * The name is deliberately *not* restricted to that keyboard's characters: it is a label, not a word
 * in the language, and someone naming a Japanese dictionary "Kanji, week 1" must be able to.
 */
@Composable
private fun NameField(
    name: String,
    keyboardLanguage: String,
    alternativeKeyboardLanguage: String?,
    onNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = LocalKeyboardController.current
    // Only a starting point: once anything is typed the field reads its own script off the text, so
    // a Latin name stays Latin even with the Arabic keyboard open.
    val activeLanguage = controller.keyboardLanguage ?: keyboardLanguage
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WebFieldLabel("Dictionary name")
            KeyboardButton(group = NameFieldGroup, languageCode = keyboardLanguage)
        }

        KeyboardTextField(
            id = NameFieldGroup,
            order = 0,
            cardId = NameFieldGroup,
            languageCode = activeLanguage,
            value = name,
            onValueChange = onNameChanged,
            placeholder = strings.dictionaryNameHint,
            restrictToKeyboard = false,
        )

        if (controller.isOpenFor(NameFieldGroup)) {
            LanguageKeyboardPopup(
                language1 = keyboardLanguage,
                controller = controller,
                language2 = alternativeKeyboardLanguage,
                // A name is a label — "German Basics 2" — so it gets digits and punctuation. It can
                // afford them because the field is not restricted to the keyboard's characters.
                isExtendedKeyboard = true,
            )
        }
    }
}

@Composable
private fun LanguageField(
    label: String,
    selected: SupportedLanguage?,
    isEnabled: Boolean,
    onSelect: (SupportedLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        WebFieldLabel(label)

        WebSelect(label = selected?.displayName ?: strings.select, isEnabled = isEnabled) { dismiss ->
            SupportedLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onSelect(language)
                        dismiss()
                    },
                )
            }
        }
    }
}

/** The whole tag catalogue, wrapping across the full width of the fields above it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChips(selectedCodes: List<String>, onToggle: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        ALL_TAGS.forEach { tag ->
            WebChip(
                label = tag.label,
                isSelected = tag.code in selectedCodes,
                onClick = { onToggle(tag.code) },
            )
        }
    }
}

/** A name is free text and needs more room than a select, which only ever shows one word. */
private const val NameWeight = 1.4f

private const val NameFieldGroup = "dictionary-name"

/** Below these the row would squeeze its controls past readability, so it breaks instead. */
private val ThreeAcross = 720.dp
private val PairAcross = 420.dp
