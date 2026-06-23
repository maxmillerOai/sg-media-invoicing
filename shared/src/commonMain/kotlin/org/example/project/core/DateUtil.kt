package org.example.project.core

import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Today's date, computed via the stdlib [kotlin.time.Clock] (always on the classpath) rather
 * than `kotlinx.datetime.Clock`, which throws NoClassDefFoundError at runtime in this project.
 */
@OptIn(ExperimentalTime::class)
fun currentLocalDate(): LocalDate {
    val days = (Clock.System.now().toEpochMilliseconds() / 86_400_000L).toInt()
    return LocalDate.fromEpochDays(days)
}

/** French short date: `DD/MM/YYYY` (e.g. 23/06/2026). Storage stays ISO; this is display-only. */
fun LocalDate.frShort(): String =
    dayOfMonth.toString().padStart(2, '0') + "/" +
        monthNumber.toString().padStart(2, '0') + "/" + year
