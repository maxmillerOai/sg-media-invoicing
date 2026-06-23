package org.example.project.core

import kotlinx.datetime.LocalDateTime
import java.time.LocalDateTime as JavaLocalDateTime

actual fun currentDateTime(): LocalDateTime {
    val n = JavaLocalDateTime.now()
    return LocalDateTime(n.year, n.monthValue, n.dayOfMonth, n.hour, n.minute, n.second)
}
