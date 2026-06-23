package org.example.project.domain.usecase

import org.example.project.core.Money
import org.example.project.domain.model.CompanyProfile
import org.example.project.domain.model.Invoice
import org.example.project.domain.model.LineItem
import org.example.project.domain.model.PaymentMode
import org.example.project.domain.model.TaxBreakdown
import org.example.project.domain.tax.AlgerianTaxCalculator

/**
 * Computes the full [TaxBreakdown] for a document.
 *
 * Per line: gross = unitPriceHT × qty; discount = gross × discountPct; net = gross − discount;
 * VAT = net × vatPct (unless the issuer is VAT-exempt). Stamp tax is applied once on the
 * VAT-inclusive total when the payment mode is cash.
 */
class ComputeTotals {

    operator fun invoke(invoice: Invoice, company: CompanyProfile? = null): TaxBreakdown =
        compute(invoice.lines, invoice.paymentMode, vatExempt = company?.vatExempt == true)

    fun compute(
        lines: List<LineItem>,
        paymentMode: PaymentMode,
        vatExempt: Boolean = false,
    ): TaxBreakdown {
        var totalHT = Money.ZERO
        var totalDiscount = Money.ZERO
        var vatAmount = Money.ZERO

        for (line in lines) {
            val gross = line.unitPriceHT * line.qty
            val discount = gross.percent(line.discountPct)
            val net = gross - discount
            totalHT += gross
            totalDiscount += discount
            if (!vatExempt) {
                vatAmount += AlgerianTaxCalculator.vat(net, line.vatPct)
            }
        }

        val vatBase = totalHT - totalDiscount
        val totalBeforeStamp = vatBase + vatAmount
        val stampTax = AlgerianTaxCalculator.stampTax(totalBeforeStamp, paymentMode)

        return TaxBreakdown(
            totalHT = totalHT,
            totalDiscount = totalDiscount,
            vatBase = vatBase,
            vatAmount = vatAmount,
            stampTax = stampTax,
            totalTTC = totalBeforeStamp + stampTax,
        )
    }
}
