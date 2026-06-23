package org.example.project.domain.tax

import org.example.project.core.Money
import org.example.project.domain.model.PaymentMode

/**
 * Algerian tax rules: VAT (TVA) and the stamp tax (droit de timbre).
 *
 * **Stamp tax** applies only to cash-paid documents and is computed on the total amount
 * (TTC, VAT included) by 100-DA slices, rounded *up*:
 *  - 300 – 30 000 DA  → 1%
 *  - 30 001 – 100 000 DA → 1.5%
 *  - > 100 000 DA     → 2%
 * Below 300 DA there is no stamp tax.
 *
 * The computation is exact in centimes: with a slice of 100 DA (= 10 000 centimes) and a
 * rate in basis points (per 10 000), the tax in centimes is simply `slices × rateBp`.
 */
object AlgerianTaxCalculator {

    const val DEFAULT_VAT_PCT = 19.0

    private const val CENTIMES_PER_DA = 100L
    private const val SLICE_CENTIMES = 100L * CENTIMES_PER_DA   // 100 DA slice = 10 000 centimes
    private const val STAMP_MIN_CENTIMES = 300L * CENTIMES_PER_DA // 300 DA threshold

    private const val BRACKET_1_MAX_CENTIMES = 30_000L * CENTIMES_PER_DA
    private const val BRACKET_2_MAX_CENTIMES = 100_000L * CENTIMES_PER_DA

    private const val RATE_BP_LOW = 100L   // 1.0%
    private const val RATE_BP_MID = 150L   // 1.5%
    private const val RATE_BP_HIGH = 200L  // 2.0%

    /** VAT on a (post-discount) base. Returns [Money.ZERO] for a non-positive rate. */
    fun vat(base: Money, vatPct: Double): Money =
        if (vatPct <= 0.0) Money.ZERO else base.percent(vatPct)

    /**
     * Stamp tax (droit de timbre) on a cash document total.
     * Returns [Money.ZERO] when [paymentMode] is not [PaymentMode.CASH] or the amount is
     * below the 300 DA threshold.
     */
    fun stampTax(total: Money, paymentMode: PaymentMode): Money {
        if (paymentMode != PaymentMode.CASH) return Money.ZERO
        val cents = total.amountMinor
        if (cents < STAMP_MIN_CENTIMES) return Money.ZERO

        val rateBp = when {
            cents <= BRACKET_1_MAX_CENTIMES -> RATE_BP_LOW
            cents <= BRACKET_2_MAX_CENTIMES -> RATE_BP_MID
            else -> RATE_BP_HIGH
        }
        // Number of 100-DA slices, rounded up.
        val slices = (cents + SLICE_CENTIMES - 1) / SLICE_CENTIMES
        return Money(slices * rateBp)
    }
}
