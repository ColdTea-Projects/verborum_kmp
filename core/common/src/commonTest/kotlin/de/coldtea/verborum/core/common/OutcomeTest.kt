package de.coldtea.verborum.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OutcomeTest {

    @Test
    fun `map transforms success payloads`() {
        val mapped = Outcome.Success(2).map { it * 3 }

        assertEquals(Outcome.Success(6), mapped)
    }

    @Test
    fun `map leaves failures untouched`() {
        val failure = Outcome.Failure(VerborumError.Unauthorized)

        assertEquals(failure, failure.map { it })
        assertNull(failure.getOrNull())
    }

    @Test
    fun `envelope with an error unwraps to a http failure`() {
        val envelope = Envelope<String>(error = ErrorDto(code = "not_found", message = "missing"))

        assertEquals(
            Outcome.Failure(VerborumError.Http(status = 404, code = "not_found", message = "missing")),
            envelope.toOutcome(status = 404),
        )
    }
}
