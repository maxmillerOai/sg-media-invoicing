package org.example.project.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.example.project.domain.model.DocumentStatus
import org.example.project.presentation.i18n.AppStrings
import org.example.project.presentation.i18n.LocalStrings
import org.example.project.presentation.theme.AgencyPalette

/** Brand colour assigned to each document status (used for chips, dots, accents). */
fun statusColor(status: DocumentStatus): Color = when (status) {
    DocumentStatus.DRAFT -> AgencyPalette.Muted
    DocumentStatus.ISSUED -> AgencyPalette.Cyan
    DocumentStatus.SENT -> AgencyPalette.Violet
    DocumentStatus.PARTIAL -> AgencyPalette.Amber
    DocumentStatus.PAID -> AgencyPalette.Mint
    DocumentStatus.OVERDUE -> AgencyPalette.Coral
    DocumentStatus.CANCELLED -> AgencyPalette.Muted
}

/** Localized human label for a document status. */
fun statusLabel(status: DocumentStatus, s: AppStrings): String = when (status) {
    DocumentStatus.DRAFT -> s.statusDraft
    DocumentStatus.ISSUED -> s.statusIssued
    DocumentStatus.SENT -> s.statusSent
    DocumentStatus.PARTIAL -> s.statusPartial
    DocumentStatus.PAID -> s.statusPaid
    DocumentStatus.OVERDUE -> s.statusOverdue
    DocumentStatus.CANCELLED -> s.statusCancelled
}

/** A coloured status chip with the localized label. */
@Composable
fun StatusPill(status: DocumentStatus, modifier: Modifier = Modifier) {
    StatusChip(statusLabel(status, LocalStrings.current), statusColor(status), modifier)
}
