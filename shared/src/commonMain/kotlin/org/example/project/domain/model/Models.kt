package org.example.project.domain.model

import kotlinx.datetime.LocalDate
import org.example.project.core.Money

/**
 * A client in the portfolio. Algerian legal identifiers (RC/NIF/NIS/Article) are optional
 * and hidden in personal mode ([isPersonal] = true).
 */
data class Client(
    val id: Long = 0,
    val displayName: String,
    val legalForm: String? = null,      // SARL, EURL, SPA, Particulier...
    val rc: String? = null,             // Registre de Commerce
    val nif: String? = null,            // 15 digits
    val nis: String? = null,
    val articleImposition: String? = null,
    val address: String = "",
    val city: String = "",
    val wilaya: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val iban: String? = null,
    val bankName: String? = null,
    val isPersonal: Boolean = false,
    val notes: String? = null,
)

/** A reusable service/product in the catalog, used to prefill invoice lines. */
data class CatalogItem(
    val id: Long = 0,
    val name: String,
    val unit: String = "u",
    val defaultPriceHT: Money,
    val defaultVatPct: Double = 19.0,
    val description: String? = null,
)

/** A single line on a document. [vatPct] defaults to the standard Algerian rate. */
data class LineItem(
    val designation: String,
    val qty: Double,
    val unit: String = "u",
    val unitPriceHT: Money,
    val discountPct: Double = 0.0,
    val vatPct: Double = 19.0,
)

/** A document (invoice / proforma / credit note / receipt). */
data class Invoice(
    val id: Long = 0,
    val number: String,
    val type: DocumentType,
    val clientId: Long,
    val issueDate: LocalDate,
    val dueDate: LocalDate? = null,
    val paymentMode: PaymentMode,
    val lines: List<LineItem>,
    val status: DocumentStatus = DocumentStatus.DRAFT,
    val notes: String? = null,
)

/** A payment recorded against an invoice. */
data class Payment(
    val id: Long = 0,
    val invoiceId: Long,
    val date: LocalDate,
    val amount: Money,
    val method: PaymentMode,
)

/** The issuer (company) profile shown on every document and used for tax decisions. */
data class CompanyProfile(
    val id: Long = 0,
    val name: String,
    val legalForm: String? = null,
    val rc: String? = null,
    val nif: String? = null,
    val nis: String? = null,
    val articleImposition: String? = null,
    val capitalSocial: Money? = null,
    val address: String = "",
    val city: String = "",
    val wilaya: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val iban: String? = null,
    val bankName: String? = null,
    val footerNote: String? = null,
    val defaultVatPct: Double = 19.0,
    /** When true the issuer is VAT-exempt: no VAT is charged on any line. */
    val vatExempt: Boolean = false,
)

/** The computed monetary breakdown of a document. All values are [Money]. */
data class TaxBreakdown(
    val totalHT: Money,        // sum of line totals before discount
    val totalDiscount: Money,  // sum of line discounts
    val vatBase: Money,        // totalHT - totalDiscount (net taxable base)
    val vatAmount: Money,      // total VAT (0 if exempt)
    val stampTax: Money,       // droit de timbre (0 unless CASH)
    val totalTTC: Money,       // vatBase + vatAmount + stampTax
)
