package org.example.project.core

private val UNITS = listOf(
    "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
    "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
)

private fun word10to19(m: Int): String = if (m <= 16) UNITS[m] else "dix-${UNITS[m - 10]}"

private fun below100(n: Int): String {
    if (n < 17) return UNITS[n]
    if (n < 20) return word10to19(n)
    val t = n / 10
    val u = n % 10
    return when (t) {
        2, 3, 4, 5 -> {
            val base = listOf("vingt", "trente", "quarante", "cinquante")[t - 2]
            when (u) {
                0 -> base
                1 -> "$base et un"
                else -> "$base-${UNITS[u]}"
            }
        }
        6 -> when (u) {
            0 -> "soixante"
            1 -> "soixante et un"
            else -> "soixante-${UNITS[u]}"
        }
        7 -> if (n == 71) "soixante et onze" else "soixante-${word10to19(n - 60)}"
        8 -> if (u == 0) "quatre-vingts" else "quatre-vingt-${UNITS[u]}"
        else -> "quatre-vingt-${word10to19(n - 80)}"
    }
}

private fun below1000(n: Int): String {
    if (n < 100) return below100(n)
    val h = n / 100
    val r = n % 100
    val hundreds = if (h == 1) "cent" else "${UNITS[h]} cent"
    return when {
        r == 0 -> if (h > 1) hundreds + "s" else hundreds
        else -> "$hundreds ${below100(r)}"
    }
}

/** Spells a non-negative integer in French (handles up to hundreds of billions). */
fun spellFrench(n: Long): String {
    if (n == 0L) return "zéro"
    val milliards = (n / 1_000_000_000_000L).toInt() // groups beyond billions are rare; cap safely
    val rest = n % 1_000_000_000_000L
    val billions = (rest / 1_000_000_000L).toInt()
    val millions = ((rest / 1_000_000L) % 1000L).toInt()
    val mille = ((rest / 1000L) % 1000L).toInt()
    val unites = (rest % 1000L).toInt()

    val parts = mutableListOf<String>()
    if (milliards > 0) parts += "${below1000(milliards)} billions"
    if (billions > 0) parts += if (billions == 1) "un milliard" else "${below1000(billions)} milliards"
    if (millions > 0) parts += if (millions == 1) "un million" else "${below1000(millions)} millions"
    if (mille > 0) parts += if (mille == 1) "mille" else "${below1000(mille)} mille"
    if (unites > 0) parts += below1000(unites)
    return parts.joinToString(" ")
}

/** "Cent vingt-huit mille soixante-six dinars algériens" — for the legal amount-in-words line. */
fun amountInWordsDZD(money: Money): String {
    val dinars = (money.amountMinor / 100).let { if (it < 0) -it else it }
    val centimes = ((money.amountMinor % 100).toInt()).let { if (it < 0) -it else it }
    val dinarUnit = if (dinars > 1L) "dinars algériens" else "dinar algérien"
    var result = "${spellFrench(dinars)} $dinarUnit"
    if (centimes > 0) {
        val centUnit = if (centimes > 1) "centimes" else "centime"
        result += " et ${spellFrench(centimes.toLong())} $centUnit"
    }
    return result.replaceFirstChar { it.uppercase() }
}
