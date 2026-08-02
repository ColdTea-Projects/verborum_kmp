package de.coldtea.verborum.feature.bibliotheca.createword.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.isRightToLeft
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.keyboardLayoutFor
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.Gender
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordFormInput
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordGrammar
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordType
import de.coldtea.verborum.core.localization.strings

/**
 * One language side of one meaning: the word itself, its gender where the language marks one, and
 * whichever grammatical forms that language asks for at this word type.
 *
 * Which fields appear is [WordGrammar]'s decision, so a German verb offers past/participle/auxiliary
 * while a Japanese noun offers none of them.
 */
@Composable
internal fun LanguageInputCard(
    languageCode: String,
    wordType: WordType,
    input: WordFormInput,
    onTextChanged: (String) -> Unit,
    onGenderChanged: (Gender?) -> Unit,
    onFieldChanged: (FieldKey, String) -> Unit,
    focusFor: (FieldKey?) -> WordFieldFocus,
    modifier: Modifier = Modifier,
) {
    val genders = if (wordType == WordType.NOUN) {
        WordGrammar.genderOptions(languageCode)
    } else {
        emptyList()
    }
    val fields = WordGrammar.fieldsFor(languageCode, wordType)
    val auxiliaryOptions = WordGrammar.auxiliaryOptions(languageCode)
    // The characters this language's words may be written with, shared with the web app rather
    // than copied: a Greek field takes Greek because those are the keys the web keyboard draws. Null
    // for a language the app has no layout for, which then has no contract to enforce.
    val charset = keyboardLayoutFor(languageCode)
    // Arabic and Persian are written right to left: the caret starts on the right and the text grows
    // leftwards from it. Only the fields flip — the card's own header and chips stay in the app's
    // direction, which is the reading order of the surrounding screen rather than of the word.
    val direction = if (isRightToLeft(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.medium)) {
            Text(
                text = strings.languageName(languageCode).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            WordTextField(
                value = input.text,
                onValueChange = { value -> onTextChanged(charset?.filter(value) ?: value) },
                label = strings.word,
                direction = direction,
                focus = focusFor(null),
                keyboardOptions = VocabularyKeyboard,
            )

            if (genders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.small))

                ChoiceRow(
                    options = genders.map { gender ->
                        WordGrammar.genderLabel(languageCode, gender, strings) to gender
                    },
                    selected = input.gender,
                    // Tapping the selected chip clears it: a word may simply have no gender set.
                    onSelect = onGenderChanged,
                )
            }

            fields.forEach { key ->
                Spacer(modifier = Modifier.height(Spacing.small))

                if (key == FieldKey.AUXILIARY && auxiliaryOptions.isNotEmpty()) {
                    Text(
                        text = key.label(strings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.extraSmall))
                    ChoiceRow(
                        options = auxiliaryOptions.map { option -> option to option },
                        selected = input.field(key).takeIf { it.isNotEmpty() },
                        onSelect = { value -> onFieldChanged(key, value.orEmpty()) },
                    )
                } else {
                    // A reading is a romanisation — pinyin, romaji — so it is the one field the
                    // language's own charset must not govern.
                    val isReading = key == FieldKey.READING

                    WordTextField(
                        value = input.field(key),
                        onValueChange = { value ->
                            onFieldChanged(key, if (isReading) value else charset?.filter(value) ?: value)
                        },
                        label = key.label(strings),
                        // A reading is Latin whatever the word's script, so it stays left to right.
                        direction = if (isReading) LayoutDirection.Ltr else direction,
                        focus = focusFor(key),
                        keyboardOptions = if (isReading) RomanisationKeyboard else VocabularyKeyboard,
                    )
                }
            }
        }
    }
}

/**
 * Where the return key goes from one field.
 *
 * The next *empty* field rather than simply the next one: a user filling in a word they already
 * half-know tabs past what they have written, and stopping them on a field they just typed into
 * would make the key useless exactly when the form is nearly done. With nothing left to fill the
 * key reads Done and puts the keyboard away.
 */
internal class WordFieldFocus(
    val requester: FocusRequester,
    val hasNext: Boolean,
    val onNext: () -> Unit,
)

/** One field of a word, in the script's own direction and wired to the return key. */
@Composable
private fun WordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    direction: LayoutDirection,
    focus: WordFieldFocus,
    keyboardOptions: KeyboardOptions,
) {
    val focusManager = LocalFocusManager.current

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            shape = Shapes.medium,
            keyboardOptions = keyboardOptions.copy(
                imeAction = if (focus.hasNext) ImeAction.Next else ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focus.onNext() },
                onDone = { focusManager.clearFocus() },
            ),
            modifier = Modifier.fillMaxWidth().focusRequester(focus.requester),
        )
    }
}

/** A row of single-choice chips; selecting the current one clears the choice. */
@Composable
private fun <T> ChoiceRow(
    options: List<Pair<String, T>>,
    selected: T?,
    onSelect: (T?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        options.forEach { (label, value) ->
            val isSelected = value == selected
            val accent = MaterialTheme.colorScheme.primary

            Surface(
                onClick = { onSelect(if (isSelected) null else value) },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                shape = Shapes.pill,
                color = if (isSelected) {
                    accent.copy(alpha = SelectedAlpha)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                border = BorderStroke(
                    width = Dimens.border,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        horizontal = Spacing.medium,
                        vertical = Spacing.small,
                    ),
                )
            }
        }
    }
}

/**
 * Vocabulary is not prose: autocorrect in the reader's own language turns *kaufen* into *kaufer* and
 * sentence capitalisation puts a capital on a Spanish verb. Both are off for every field the user
 * types a word into.
 *
 * iOS gives an app no supported way to choose the keyboard's *language*, and Compose Multiplatform
 * exposes only `UIKeyboardType` to begin with — so the word and grammar fields take whatever
 * keyboard the user last picked, and this is as close as the platform allows.
 */
private val VocabularyKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
)

/**
 * A reading is a romanisation of the word above it — *chē* for 车 — so it wants a Latin keyboard
 * even though the word itself is Chinese or Japanese. `Ascii` is the one keyboard iOS will actually
 * switch to on request, via `UIKeyboardTypeASCIICapable`.
 */
private val RomanisationKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = KeyboardType.Ascii,
)

private const val SelectedAlpha = 0.12f
