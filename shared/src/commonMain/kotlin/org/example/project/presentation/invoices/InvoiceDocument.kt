package org.example.project.presentation.invoices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.core.amountInWordsDZD
import org.example.project.domain.addressLine
import org.example.project.domain.legalIdsLine
import org.example.project.domain.legalName
import org.example.project.domain.model.CompanyProfile
import org.example.project.domain.model.LineItem
import org.example.project.domain.model.TaxBreakdown
import org.jetbrains.compose.resources.painterResource
import sgmediaprod.shared.generated.resources.Res
import sgmediaprod.shared.generated.resources.sg_logo_vector

// Print-page palette — always dark-on-white regardless of app theme.
private val Ink = Color(0xFF12132A)
private val Muted = Color(0xFF6B7280)
private val Line = Color(0xFFD8DEE9)
private val BoxBg = Color(0xFFF1F3F8)
private val TtcBg = Color(0xFFDCEAF7)

private const val W_DESIGNATION = 4.2f
private const val W_TVA = 0.9f
private const val W_PU = 1.4f
private const val W_QTY = 0.8f
private const val W_TOTAL = 1.4f

private fun qtyLabel(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

private fun pctLabel(value: Double): String =
    (if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()) + "%"

/** The printable invoice page (A4), styled after a standard Algerian proforma. */
@Composable
fun InvoiceDocument(
    company: CompanyProfile,
    docTitle: String,
    number: String,
    dateText: String,
    clientName: String,
    clientAddress: String,
    clientNif: String?,
    clientRc: String? = null,
    clientNis: String? = null,
    clientArticle: String? = null,
    lines: List<LineItem>,
    breakdown: TaxBreakdown,
    cash: Boolean,
    vatExempt: Boolean,
    paymentMethodText: String = if (cash) "Espèces" else "Virement bancaire",
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(Color.White).padding(32.dp)) {

        // ── Company core info (left) + logo (right), same line ────────────────
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(company.legalName, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(company.addressLine, color = Muted, fontSize = 11.sp)
                Text(company.legalIdsLine, color = Muted, fontSize = 11.sp)
                company.phone?.let { Text("Tél : $it", color = Muted, fontSize = 11.sp) }
                company.email?.let { Text("Email : $it", color = Muted, fontSize = 11.sp) }
            }
            Image(
                painter = painterResource(Res.drawable.sg_logo_vector),
                contentDescription = "SG Media",
                modifier = Modifier.size(110.dp),
            )
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Line)
        Spacer(Modifier.height(14.dp))

        // ── Document info (left) + Adressé à (right) ──────────────────────────
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(docTitle, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text("N° : $number", color = Ink, fontSize = 12.sp)
                Text("Date : $dateText", color = Muted, fontSize = 12.sp)
                Text("Mode : $paymentMethodText", color = Muted, fontSize = 12.sp)
            }
            PartyBox(
                title = "ADRESSÉ À",
                modifier = Modifier.weight(1f),
                background = Color.White,
                border = true,
            ) {
                Text(clientName, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (clientAddress.isNotBlank()) Text(clientAddress, color = Muted, fontSize = 11.sp)
                clientRc?.takeIf { it.isNotBlank() }?.let { Text("RC : $it", color = Muted, fontSize = 11.sp) }
                clientNif?.takeIf { it.isNotBlank() }?.let { Text("NIF : $it", color = Muted, fontSize = 11.sp) }
                clientNis?.takeIf { it.isNotBlank() }?.let { Text("NIS : $it", color = Muted, fontSize = 11.sp) }
                clientArticle?.takeIf { it.isNotBlank() }?.let { Text("Art. imp. : $it", color = Muted, fontSize = 11.sp) }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Montants exprimés en Dinar Algérien",
            modifier = Modifier.fillMaxWidth(),
            color = Muted,
            fontSize = 10.sp,
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.height(4.dp))

        // ── Line-items table ─────────────────────────────────────────────────
        LineItemsTable(lines)

        Spacer(Modifier.height(16.dp))

        // ── Mode de règlement (left) + totals (right) ─────────────────────────
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Mode de règlement :", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Text(paymentMethodText, color = Muted, fontSize = 11.sp)
                if (!company.bankName.isNullOrBlank() || !company.iban.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    company.bankName?.let { Text("Banque : $it", color = Muted, fontSize = 11.sp) }
                    company.iban?.let { Text("RIB / IBAN : $it", color = Muted, fontSize = 11.sp) }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.width(280.dp)) {
                TotalRow("Total HT", breakdown.totalHT.format())
                if (breakdown.totalDiscount.amountMinor > 0) TotalRow("Remise", "-" + breakdown.totalDiscount.format())
                TotalRow(if (vatExempt) "TVA (exonéré)" else "TVA", breakdown.vatAmount.format())
                if (cash) TotalRow("Droit de timbre", breakdown.stampTax.format())
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TtcBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Total TTC", color = Ink, fontWeight = FontWeight.Bold)
                    Text(breakdown.totalTTC.format(), color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        // ── Amount in words ──────────────────────────────────────────────────
        Text(
            "Arrêté la présente facture à la somme de : ${amountInWordsDZD(breakdown.totalTTC)}.",
            color = Ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // ── Signature box ─────────────────────────────────────────────────────
        Text("Cachet, Date, Signature", color = Muted, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp)
                .border(BorderStroke(1.dp, Line), RoundedCornerShape(6.dp)),
        ) {}

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = Line)
        Spacer(Modifier.height(8.dp))
        // ── Legal footer: company name, then registered office (siège social), then IDs ──
        Text(
            company.legalName,
            modifier = Modifier.fillMaxWidth(),
            color = Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            "Siège social : ${company.addressLine}",
            modifier = Modifier.fillMaxWidth(),
            color = Muted,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            company.legalIdsLine + (company.phone?.let { "   •   Tél : $it" } ?: ""),
            modifier = Modifier.fillMaxWidth(),
            color = Muted,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PartyBox(
    title: String,
    modifier: Modifier,
    background: Color,
    border: Boolean,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        Text(title, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 92.dp)
                .then(if (border) Modifier.border(BorderStroke(1.dp, Line), RoundedCornerShape(8.dp)) else Modifier)
                .background(background, RoundedCornerShape(8.dp))
                .padding(12.dp),
        ) { content() }
    }
}

@Composable
private fun LineItemsTable(lines: List<LineItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, Line), RoundedCornerShape(8.dp)),
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().background(BoxBg).height(IntrinsicSize.Min)) {
            HeadCell("Désignation", W_DESIGNATION, TextAlign.Start)
            VLine(); HeadCell("TVA", W_TVA, TextAlign.End)
            VLine(); HeadCell("P.U. HT", W_PU, TextAlign.End)
            VLine(); HeadCell("Qté", W_QTY, TextAlign.End)
            VLine(); HeadCell("Total HT", W_TOTAL, TextAlign.End)
        }
        lines.forEach { line ->
            HorizontalDivider(color = Line)
            val gross = line.unitPriceHT * line.qty
            val net = gross - gross.percent(line.discountPct)
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(W_DESIGNATION).padding(horizontal = 10.dp, vertical = 9.dp)) {
                    Text(line.designation, color = Ink, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    Text(line.unit + (if (line.discountPct > 0) "  •  remise ${pctLabel(line.discountPct)}" else ""), color = Muted, fontSize = 10.sp)
                }
                VLine(); BodyCell(pctLabel(line.vatPct), W_TVA, TextAlign.End)
                VLine(); BodyCell(line.unitPriceHT.format(withSymbol = false), W_PU, TextAlign.End)
                VLine(); BodyCell(qtyLabel(line.qty), W_QTY, TextAlign.End)
                VLine(); BodyCell(net.format(withSymbol = false), W_TOTAL, TextAlign.End, strong = true)
            }
        }
    }
}

@Composable
private fun RowScope.VLine() {
    VerticalDivider(modifier = Modifier.fillMaxHeight(), color = Line)
}

@Composable
private fun RowScope.HeadCell(text: String, weight: Float, align: TextAlign) {
    Text(
        text,
        modifier = Modifier.weight(weight).padding(horizontal = 10.dp, vertical = 9.dp),
        color = Muted,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = align,
    )
}

@Composable
private fun RowScope.BodyCell(text: String, weight: Float, align: TextAlign, strong: Boolean = false) {
    Text(
        text,
        modifier = Modifier.weight(weight).padding(horizontal = 10.dp, vertical = 9.dp),
        color = if (strong) Ink else Muted,
        fontSize = 12.sp,
        fontWeight = if (strong) FontWeight.Medium else FontWeight.Normal,
        textAlign = align,
    )
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, color = Ink, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}
