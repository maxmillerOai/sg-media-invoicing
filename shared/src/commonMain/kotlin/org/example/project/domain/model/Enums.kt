package org.example.project.domain.model

/** How the document is/was paid. Stamp tax (droit de timbre) applies only to [CASH]. */
enum class PaymentMode { CASH, BANK_TRANSFER, CHEQUE, CARD }

/** French "mode de règlement" label, including the cheque number/bank when relevant. */
fun PaymentMode.label(chequeNumber: String? = null, chequeBank: String? = null): String = when (this) {
    PaymentMode.CASH -> "Espèces"
    PaymentMode.BANK_TRANSFER -> "Virement bancaire"
    PaymentMode.CARD -> "Carte bancaire"
    PaymentMode.CHEQUE -> buildString {
        append("Chèque")
        chequeNumber?.takeIf { it.isNotBlank() }?.let { append(" n° $it") }
        chequeBank?.takeIf { it.isNotBlank() }?.let { append(" — $it") }
    }
}

/** The kind of commercial document — all share the same editor and template. */
enum class DocumentType { INVOICE, PROFORMA, CREDIT_NOTE, RECEIPT }

/** Lifecycle status of a document. */
enum class DocumentStatus { DRAFT, ISSUED, SENT, PAID, PARTIAL, OVERDUE, CANCELLED }
