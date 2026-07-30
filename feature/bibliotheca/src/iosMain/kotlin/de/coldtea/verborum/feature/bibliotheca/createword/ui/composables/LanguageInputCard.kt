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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.Gender
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordFormInput
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordGrammar
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordType

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
    modifier: Modifier = Modifier,
) {
    val genders = if (wordType == WordType.NOUN) {
        WordGrammar.genderOptions(languageCode)
    } else {
        emptyList()
    }
    val fields = WordGrammar.fieldsFor(languageCode, wordType)
    val auxiliaryOptions = WordGrammar.auxiliaryOptions(languageCode)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.medium)) {
            Text(
                text = SupportedLanguage.displayNameOf(languageCode).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            OutlinedTextField(
                value = input.text,
                onValueChange = onTextChanged,
                label = { Text("Word") },
                singleLine = true,
                shape = Shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            if (genders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.small))

                ChoiceRow(
                    options = genders.map { gender ->
                        WordGrammar.genderLabel(languageCode, gender) to gender
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
                        text = key.label,
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
                    OutlinedTextField(
                        value = input.field(key),
                        onValueChange = { value -> onFieldChanged(key, value) },
                        label = { Text(key.label) },
                        singleLine = true,
                        shape = Shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
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

private const val SelectedAlpha = 0.12f
