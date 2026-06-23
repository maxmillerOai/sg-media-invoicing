package org.example.project.core

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// On iOS the kotlinx-datetime Clock is available (the desktop-only classpath issue does not apply).
actual fun currentDateTime(): LocalDateTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
