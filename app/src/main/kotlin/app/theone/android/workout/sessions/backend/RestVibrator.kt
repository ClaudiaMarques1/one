package app.theone.android.workout.sessions.backend

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Seconds left at which the rest bar gives a countdown tick: 3, 2, 1. */
const val REST_COUNTDOWN_SECONDS = 3

fun isRestCountdownTick(remainingSeconds: Int): Boolean = remainingSeconds in 1..REST_COUNTDOWN_SECONDS

/**
 * The rest timer's vibrations, on the device vibrator rather than Compose haptic feedback - touch
 * haptics are faint and many phones (EMUI) mute them with the system touch-vibration setting.
 * Needs the VIBRATE permission (normal, granted at install).
 */
class RestVibrator(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    /** A quick tick for each of the last seconds. */
    fun tick() = vibrate(TICK_MILLIS)

    /** A longer buzz when the rest reaches zero. */
    fun end() = vibrate(END_MILLIS)

    private fun vibrate(millis: Long) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(millis)
        }
    }

    private companion object {
        const val TICK_MILLIS = 60L
        const val END_MILLIS = 400L
    }
}
