package de.coldtea.verborum.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class RefreshThrottleTest {

    /** A clock the test moves by hand, so no test waits for real time. */
    private class ManualTimeSource : TimeSource {
        var now: Duration = Duration.ZERO

        override fun markNow(): TimeMark {
            val taken = now
            return object : TimeMark {
                override fun elapsedNow(): Duration = now - taken
            }
        }
    }

    @Test
    fun `the first refresh is let through`() {
        val throttle = RefreshThrottle(10.seconds, ManualTimeSource())

        assertTrue(throttle.tryAcquire())
    }

    @Test
    fun `a second refresh inside the gap is refused`() {
        val clock = ManualTimeSource()
        val throttle = RefreshThrottle(10.seconds, clock)
        throttle.tryAcquire()

        clock.now += 9.seconds

        // Focus, a visibility change and the timer can fire within moments of each other
        assertFalse(throttle.tryAcquire())
    }

    @Test
    fun `a refresh after the gap is let through again`() {
        val clock = ManualTimeSource()
        val throttle = RefreshThrottle(10.seconds, clock)
        throttle.tryAcquire()

        clock.now += 10.seconds

        assertTrue(throttle.tryAcquire())
    }

    @Test
    fun `a refused attempt does not push the next one further out`() {
        val clock = ManualTimeSource()
        val throttle = RefreshThrottle(10.seconds, clock)
        throttle.tryAcquire()

        clock.now += 6.seconds
        assertFalse(throttle.tryAcquire())
        clock.now += 5.seconds

        // 11 s after the allowed one: the refused call at 6 s did not restart the gap
        assertTrue(throttle.tryAcquire())
    }
}
