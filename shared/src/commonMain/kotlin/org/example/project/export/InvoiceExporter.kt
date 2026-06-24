package org.example.project.export

import androidx.compose.runtime.staticCompositionLocalOf
import org.example.project.domain.model.CompanyProfile
import org.example.project.domain.model.LineItem
import org.example.project.domain.model.PaymentMode
import org.example.project.domain.model.TaxBreakdown

enum class ExportFormat(val label: String, val extension: String) {
    PDF("PDF", "pdf"),
    XLSX("XLSX", "xlsx"),
    DOCX("DOCX", "docx"),
}

/** Everything needed to render a document to any format. Platform-agnostic. */
data class InvoiceExportData(
    val company: CompanyProfile,
    val docTitle: String,
    val number: String,
    val dateText: String,
    val clientName: String,
    val clientAddress: String,
    val clientNif: String?,
    val clientRc: String? = null,
    val clientNis: String? = null,
    val clientArticle: String? = null,
    val lines: List<LineItem>,
    val breakdown: TaxBreakdown,
    val cash: Boolean,
    val vatExempt: Boolean,
    val paymentMethod: PaymentMode = PaymentMode.CASH,
    val chequeNumber: String? = null,
    val chequeBank: String? = null,
    val logoPng: ByteArray? = null,
) {
    /** A safe default file name without extension, e.g. "Facture_PRO-2026-001". */
    val suggestedFileName: String
        get() = ("Facture_" + number).replace(Regex("[^A-Za-z0-9_\\-]"), "_")
}

sealed interface ExportResult {
    data class Saved(val path: String) : ExportResult
    data object Cancelled : ExportResult
    data class Failed(val message: String) : ExportResult
    data object Unsupported : ExportResult
}

/** Generates documents and prints. Implemented per platform; desktop is the first target. */
interface InvoiceExporter {
    suspend fun export(format: ExportFormat, data: InvoiceExportData): ExportResult
    suspend fun print(data: InvoiceExportData): ExportResult

    /** Generates a PDF and hands it to the platform's email/share flow. Default = unsupported. */
    suspend fun email(data: InvoiceExportData): ExportResult = ExportResult.Unsupported
}

/** Fallback used on platforms without an export implementation yet (iOS). */
object NoopInvoiceExporter : InvoiceExporter {
    override suspend fun export(format: ExportFormat, data: InvoiceExportData) = ExportResult.Unsupported
    override suspend fun print(data: InvoiceExportData) = ExportResult.Unsupported
}

val LocalInvoiceExporter = staticCompositionLocalOf<InvoiceExporter> { NoopInvoiceExporter }
