package de.coldtea.verborum.feature.bibliotheca.createword.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.coldtea.verborum.core.designsystem.component.WebChip
import de.coldtea.verborum.core.designsystem.component.WebFieldLabel
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.KeyboardButton
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.keyboardLayoutFor
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.KeyboardTextField
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.LanguageKeyboardPopup
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.LocalKeyboardController
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalUiLanguage
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.Gender
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordFormInput
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordGrammar
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordType
import de.coldtea.verborum.core.localization.strings

/**
 * One language side of one meaning, as a card with a coloured bar down its leading edge — crimson
 * for the language being learned from, gold for the one being learned into, so the pair is readable
 * without reading the headings.
 *
 * Which fields appear is [WordGrammar]'s decision, exactly as on iOS: a German verb offers
 * past/participle/auxiliary, a Japanese noun offers a counter and no gender.
 *
 * The key in the corner opens this language's on-screen keyboard, for the letters a learner's own
 * keyboard does not have. It anchors to this card, so the popup knows where to sit without anyone
 * measuring anything.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WebLanguageCard(
    languageCode: String,
    wordType: WordType,
    input: WordFormInput,
    accentColor: Color,
    cardId: String,
    fieldOrder: Int,
    onTextChanged: (String) -> Unit,
    onGenderChanged: (Gender?) -> Unit,
    onFieldChanged: (FieldKey, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = LocalKeyboardController.current
    val uiLanguage = LocalUiLanguage.current

    // A reading is a pronunciation note, not the word itself, so it is written in the **learner's**
    // script — the one they demonstrably read. Never the word's own: writing にほんご helps only
    // someone who already reads kana, which is not who a reading is for.
    val isReadingFocused = controller.focusedField()
        ?.let { it.cardId == cardId && it.fieldKey == FieldKey.READING } == true

    // The one exception, and it is not really one: where a language has a *reading* keyboard of its
    // own it is already a romanisation — pinyin, with the tone marks a plain Latin keyboard lacks.
    // Japanese has no such thing, so it gets no second tab rather than being offered kana.
    val readingKeyboard = languageCode.takeIf {
        isReadingFocused && keyboardLayoutFor(it, FieldKey.READING) != keyboardLayoutFor(it)
    }
    val genders = if (wordType == WordType.NOUN) {
        WordGrammar.genderOptions(languageCode)
    } else {
        emptyList()
    }
    val fields = WordGrammar.fieldsFor(languageCode, wordType)
    val auxiliaryOptions = WordGrammar.auxiliaryOptions(languageCode)

    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(Dimens.accentBar)
                    .fillMaxHeight()
                    .background(accentColor),
            )

            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.large)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    WebFieldLabel(SupportedLanguage.displayNameOf(languageCode))

                    KeyboardButton(group = cardId, languageCode = languageCode)
                }

                if (genders.isNotEmpty()) {
                    WebFieldLabel("Gender")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        genders.forEach { gender ->
                            WebChip(
                                label = WordGrammar.genderLabel(languageCode, gender),
                                isSelected = input.gender == gender,
                                // Tapping the selected chip clears it: a word may have no gender.
                                onClick = {
                                    onGenderChanged(gender.takeIf { it != input.gender })
                                },
                            )
                        }
                    }
                    WebPageSpacer(Spacing.medium)
                }

                KeyboardTextField(
                    id = "$cardId-word",
                    order = fieldOrder,
                    cardId = cardId,
                    languageCode = languageCode,
                    value = input.text,
                    onValueChange = onTextChanged,
                    placeholder = strings.word,
                )

                fields.forEach { key ->
                    WebPageSpacer(Spacing.small)

                    if (key == FieldKey.AUXILIARY && auxiliaryOptions.isNotEmpty()) {
                        WebFieldLabel(key.label)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                            auxiliaryOptions.forEach { option ->
                                WebChip(
                                    label = option,
                                    isSelected = input.field(key) == option,
                                    onClick = {
                                        onFieldChanged(
                                            key,
                                            if (input.field(key) == option) "" else option,
                                        )
                                    },
                                )
                            }
                        }
                    } else {
                        KeyboardTextField(
                            id = "$cardId-${key.metaKey}",
                            // Its place in the run of fields the form walks with Enter: after the
                            // word itself, in the order the grammar asks for them.
                            order = fieldOrder + 1 + fields.indexOf(key),
                            cardId = cardId,
                            languageCode = languageCode,
                            value = input.field(key),
                            onValueChange = { value -> onFieldChanged(key, value) },
                            fieldKey = key,
                            placeholder = key.label,
                            // A reading is not a word surface — "nihongo" is a perfectly good note
                            // on 日本語 — so the per-language character rule does not apply to it.
                            restrictToKeyboard = key != FieldKey.READING,
                        )
                    }
                }
            }
        }
    }

    if (controller.isOpenFor(cardId)) {
        LanguageKeyboardPopup(
            // One card, one language — except on a reading, which may be romanised.
            language1 = if (isReadingFocused) uiLanguage.code else languageCode,
            controller = controller,
            language2 = readingKeyboard,
            isExtendedKeyboard = isReadingFocused,
        )
    }
}
