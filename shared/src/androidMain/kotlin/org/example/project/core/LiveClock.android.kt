package org.example.project.core

import kotlinx.datetime.LocalDateTime
import java.util.Calendar

actual fun currentDateTime(): LocalDateTime {
    // Calendar avoids java.time desugaring concerns on older Android API levels.
    val c = Calendar.getInstance()
    return LocalDateTime(
        year = c.get(Calendar.YEAR),
        monthNumber = c.get(Calendar.MONTH) + 1,
        dayOfMonth = c.get(Calendar.DAY_OF_MONTH),
        hour = c.get(Calendar.HOUR_OF_DAY),
        minute = c.get(Calendar.MINUTE),
        second = c.get(Calendar.SECOND),
    )
}
