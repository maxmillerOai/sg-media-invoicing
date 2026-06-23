package org.example.project.core

import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Monetary amount stored as **minor units** (centimes) to avoid floating-point error.
 * The app is single-currency (Algerian Dinar, DZD): 1 DA = 100 centimes.
 *
 * All arithmetic is exact on the [amountMinor] Long. Operations that take a [Double]
 * (quantities, percentages) round half-up to the nearest centime.
 */
@JvmInline
value class Money(val amountMinor: Long) : Comparable<Money> {

    val isZero: Boolean get() = amountMinor == 0L
    val isNegative: Boolean get() = amountMinor < 0L

    /** Whole-dinar part (truncated toward zero). */
    val dinars: Long get() = amountMinor / 100
    /** Centime part (0..99, sign follows the amount). */
    val centimes: Int get() = (amountMinor % 100).toInt()

    operator fun plus(other: Money) = Money(amountMinor + other.amountMinor)
    operator fun minus(other: Money) = Money(amountMinor - other.amountMinor)
    operator fun unaryMinus() = Money(-amountMinor)

    /** Multiply by a quantity (e.g. unit price × qty), rounding to the nearest centime. */
    operator fun times(factor: Double) = Money((amountMinor * factor).roundToLong())

    /** Take a percentage of this amount (e.g. 19.0 → 19%), rounding to the nearest centime. */
    fun percent(pct: Double) = Money((amountMinor * pct / 100.0).roundToLong())

    override fun compareTo(other: Money) = amountMinor.compareTo(other.amountMinor)

    /** Formats as Algerian convention: space thousands separator, comma decimal, e.g. `1 234,56 DA`. */
    fun format(withSymbol: Boolean = true): String {
        val negative = amountMinor < 0
        val cents = abs(amountMinor)
        val whole = cents / 100
        val frac = (cents % 100).toInt()

        val digits = whole.toString()
        val grouped = StringBuilder()
        for ((i, c) in digits.withIndex()) {
            if (i > 0 && (digits.length - i) % 3 == 0) grouped.append(' ')
            grouped.append(c)
        }
        val sign = if (negative) "-" else ""
        val fracStr = frac.toString().padStart(2, '0')
        val symbol = if (withSymbol) " DA" else ""
        return "$sign$grouped,$fracStr$symbol"
    }

    companion object {
        val ZERO = Money(0)

        /** From whole dinars. */
        fun ofDinars(da: Long) = Money(da * 100)

        /** From a major-unit decimal (e.g. 1234.56 DA), rounding to the nearest centime. */
        fun ofMajor(amount: Double) = Money((amount * 100).roundToLong())

        /**
         * Parses a user-entered amount. Strips the DA symbol and any whitespace or apostrophe
         * thousands separators, and accepts either comma or dot as the decimal separator.
         * Returns null if the input cannot be parsed.
         */
        fun parse(input: String): Money? {
            val cleaned = buildString {
                for (ch in input.trim()) {
                    when {
                        ch.isDigit() -> append(ch)
                        ch == '-' || ch == '.' -> append(ch)
                        ch == ',' -> append('.')
                        else -> { /* drop spaces, NBSP, apostrophes, the DA symbol, etc. */ }
                    }
                }
            }
            if (cleaned.isEmpty() || cleaned == "-") return null
            val value = cleaned.toDoubleOrNull() ?: return null
            return ofMajor(value)
        }
    }
}
