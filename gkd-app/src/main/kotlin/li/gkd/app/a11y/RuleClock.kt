package li.gkd.app.a11y

import android.os.SystemClock

/** In-process matching intervals only; persisted dates keep wall-clock time. */
internal fun ruleNow(): Long = SystemClock.elapsedRealtime()
