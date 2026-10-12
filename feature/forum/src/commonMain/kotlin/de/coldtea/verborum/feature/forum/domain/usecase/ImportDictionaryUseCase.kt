package de.coldtea.verborum.feature.forum.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.forum.data.MarketplaceApi
import de.coldtea.verborum.feature.forum.domain.ImportResult

/**
 * Imports a listing into the caller's vault.
 *
 * The 500 retry is not optimism about server faults in general: two imports of the same dictionary
 * racing each other lose a unique-constraint check, and the backend documents the second one as a
 * 500 that succeeds when repeated. Nothing is corrupted either way, so retrying once turns a race
 * the user caused into the success they asked for.
 *
 * The import is also **idempotent** — a repeat answers 201, does not raise the import count, and
 * re-sends the vault update — which makes it the repair action for a vault entry that went missing.
 */
internal class ImportDictionaryUseCase(private val api: MarketplaceApi) {

    suspend operator fun invoke(dictionaryId: String): ImportResult {
        val first = api.import(dictionaryId)
        if (first is Outcome.Success) return ImportResult.IMPORTED

        val error = (first as? Outcome.Failure)?.error

        if (error.isServerFault()) {
            return if (api.import(dictionaryId) is Outcome.Success) {
                ImportResult.IMPORTED
            } else {
                ImportResult.FAILED
            }
        }

        return when {
            error.isForbidden() -> ImportResult.NOT_MEMBER

            // Unknown, made private, deleted, or its publisher left since the list was loaded.
            error is VerborumError.Http && error.status == 404 -> ImportResult.UNAVAILABLE

            else -> ImportResult.FAILED
        }
    }
}

private fun VerborumError?.isServerFault(): Boolean =
    this is VerborumError.Http && status >= 500
