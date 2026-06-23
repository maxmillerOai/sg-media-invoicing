package org.example.project.core

import kotlinx.datetime.LocalDateTime

/**
 * Current wall-clock date **and time** in the device's local zone.
 *
 * Implemented per-platform with native APIs on purpose: `kotlinx.datetime.Clock` throws
 * `NoClassDefFoundError` on this project's desktop classpath, and `kotlin.time.Clock` gives
 * only a UTC instant with no local-zone conversion. See [currentLocalDate].
 */
expect fun currentDateTime(): LocalDateTime
