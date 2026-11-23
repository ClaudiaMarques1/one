package app.theone.android.notifications

import android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.app.NotificationManager.INTERRUPTION_FILTER_NONE
import android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutDoNotDisturbTest {
    @Test
    fun `a workout turns DND on as Priority only when it is off`() {
        assertEquals(INTERRUPTION_FILTER_PRIORITY, dndFilterOnStart(INTERRUPTION_FILTER_ALL))
    }

    @Test
    fun `a workout leaves DND alone when it is already on`() {
        assertNull(dndFilterOnStart(INTERRUPTION_FILTER_PRIORITY))
        assertNull(dndFilterOnStart(INTERRUPTION_FILTER_ALARMS))
        assertNull(dndFilterOnStart(INTERRUPTION_FILTER_NONE))
    }

    @Test
    fun `restore puts back the earlier filter only when the workout turned DND on`() {
        assertEquals(INTERRUPTION_FILTER_ALL, dndFilterOnRestore(INTERRUPTION_FILTER_ALL, INTERRUPTION_FILTER_PRIORITY))
        assertNull(dndFilterOnRestore(null, INTERRUPTION_FILTER_PRIORITY))
    }

    @Test
    fun `restore leaves a filter the user changed by hand`() {
        assertNull(dndFilterOnRestore(INTERRUPTION_FILTER_ALL, INTERRUPTION_FILTER_ALL))
        assertNull(dndFilterOnRestore(INTERRUPTION_FILTER_ALL, INTERRUPTION_FILTER_NONE))
    }
}
