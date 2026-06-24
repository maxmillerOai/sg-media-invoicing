package org.example.project.export

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.core.Money
import org.example.project.core.amountInWordsDZD
import org.example.project.domain.addressLine
import org.example.project.domain.legalIdsLine
import org.example.project.domain.legalName
import org.example.project.domain.model.LineItem
import org.example.project.domain.model.PaymentMode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Android document generation + printing using PdfBox-Android (Apache-2.0). Draws a true vector
 * A4 invoice directly (no WebView), then routes Print (Android Print Framework), Share, Save to
 * Drive and Email through that one PDF file.
 */
class AndroidInvoiceExporter(private val activity: Activity) : InvoiceExporter {

    override suspend fun export(format: ExportFormat, data: InvoiceExportData): ExportResult {
        if (format != ExportFormat.PDF) return ExportResult.Unsupported
        return try {
            val file = makePdf(data)
            share(file, "Partager le PDF")
            ExportResult.Saved(file.absolutePath)
        } catch (t: Throwable) {
            ExportResult.Failed(t.message ?: t.toString())
        }
    }

    override suspend fun email(data: InvoiceExportData): ExportResult {
        return try {
            val file = makePdf(data)
            share(file, "Envoyer par e-mail")
            ExportResult.Saved(file.absolutePath)
        } catch (t: Throwable) {
            ExportResult.Failed(t.message ?: t.toString())
        }
    }

    override suspend fun print(data: InvoiceExportData): ExportResult {
        return try {
            val file = makePdf(data)
            withContext(Dispatchers.Main) {
                val pm = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val attrs = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .build()
                pm.print("Facture", PdfFilePrintAdapter(file, data.suggestedFileName), attrs)
            }
            ExportResult.Saved("Boîte d'impression ouverte")
        } catch (t: Throwable) {
            ExportResult.Failed(t.message ?: t.toString())
        }
    }

    private suspend fun makePdf(data: InvoiceExportData): File = withContext(Dispatchers.Default) {
        PDFBoxResourceLoader.init(activity.applicationContext)
        val file = pdfFile(data)
        generatePdf(data, file)
        file
    }

    private fun pdfFile(data: InvoiceExportData): File {
        val dir = File(activity.getExternalFilesDir(null), "invoices").apply { mkdirs() }
        return File(dir, data.suggestedFileName + ".pdf")
    }

    private fun share(file: File, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, "Veuillez trouver ci-joint le document : ${file.nameWithoutExtension}.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(send, chooserTitle))
    }

    /** Streams a pre-rendered PDF to the platform print dialog (printer or "Save as PDF"). */
    private class PdfFilePrintAdapter(private val file: File, private val jobName: String) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
            val info = PrintDocumentInfo.Builder("$jobName.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback,
        ) {
            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (t: Throwable) {
                callback.onWriteFailed(t.message)
            }
        }
    }

    // ── Logo: key out the light/checkerboard background to transparent ────────

    private fun cleanLogoBitmap(png: ByteArray): Bitmap? = try {
        val src = BitmapFactory.decodeByteArray(png, 0, png.size)
        if (src == null) null else {
            val size = 256
            val scaled = Bitmap.createScaledBitmap(src, size, size, true)
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (y in 0 until size) for (x in 0 until size) {
                val p = scaled.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val lum = r * 0.299 + g * 0.587 + b * 0.114
                out.setPixel(x, y, if (lum > 200) 0x00000000 else (0xFF000000.toInt() or (p and 0xFFFFFF)))
            }
            out
        }
    } catch (_: Throwable) {
        null
    }

    // ── PDF layout (PdfBox, exact A4, vector) ─────────────────────────────────

    private val ink = intArrayOf(0x12, 0x13, 0x2A)
    private val muted = intArrayOf(0x6B, 0x72, 0x80)
    private val line = intArrayOf(0xD8, 0xDE, 0xE9)
    private val boxBg = intArrayOf(0xF1, 0xF3, 0xF8)
    private val ttcBg = intArrayOf(0xDC, 0xEA, 0xF7)

    private fun san(s: String): String = buildString {
        for (ch in s) append(
            when (ch) {
                '•', '·' -> '-'
                '–', '—' -> '-'
                ' ', ' ', ' ' -> ' '
                '’', '‘' -> '\''
                '“', '”' -> '"'
                else -> when {
                    ch.code in 0x80..0x9F -> ' '
                    ch.code <= 0x24F -> ch // Latin + Latin-1 + Latin Extended-A/B (covered by Roboto)
                    else -> '?'
                }
            },
        )
    }

    private fun generatePdf(d: InvoiceExportData, outFile: File) {
        val doc = PDDocument()
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)
        val cs = PDPageContentStream(doc, page)

        // Match the desktop output: built-in Helvetica / Helvetica-Bold (PDFBox supports the
        // WinAnsi/French accented glyphs for the standard Latin fonts).
        val regular: PDFont = PDType1Font.HELVETICA
        val bold: PDFont = PDType1Font.HELVETICA_BOLD

        val pw = PDRectangle.A4.width
        val ph = PDRectangle.A4.height
        val margin = 40f
        val left = margin
        val right = pw - margin

        fun widthOf(s: String, f: PDFont, size: Float) = f.getStringWidth(san(s)) / 1000f * size
        fun draw(x: Float, baseline: Float, s: String, f: PDFont, size: Float, c: IntArray) {
            cs.beginText(); cs.setFont(f, size); cs.setNonStrokingColor(c[0], c[1], c[2])
            cs.newLineAtOffset(x, baseline); cs.showText(san(s)); cs.endText()
        }
        fun drawR(xRight: Float, baseline: Float, s: String, f: PDFont, size: Float, c: IntArray) =
            draw(xRight - widthOf(s, f, size), baseline, s, f, size, c)
        fun drawC(xMid: Float, baseline: Float, s: String, f: PDFont, size: Float, c: IntArray) =
            draw(xMid - widthOf(s, f, size) / 2f, baseline, s, f, size, c)
        fun rectFill(x: Float, yB: Float, w: Float, h: Float, c: IntArray) {
            cs.setNonStrokingColor(c[0], c[1], c[2]); cs.addRect(x, yB, w, h); cs.fill()
        }
        fun rectStroke(x: Float, yB: Float, w: Float, h: Float, c: IntArray) {
            cs.setStrokingColor(c[0], c[1], c[2]); cs.setLineWidth(0.7f); cs.addRect(x, yB, w, h); cs.stroke()
        }
        fun hLine(x1: Float, x2: Float, yy: Float, c: IntArray) {
            cs.setStrokingColor(c[0], c[1], c[2]); cs.setLineWidth(0.7f); cs.moveTo(x1, yy); cs.lineTo(x2, yy); cs.stroke()
        }
        fun vLine(xx: Float, y1: Float, y2: Float, c: IntArray) {
            cs.setStrokingColor(c[0], c[1], c[2]); cs.setLineWidth(0.7f); cs.moveTo(xx, y1); cs.lineTo(xx, y2); cs.stroke()
        }
        fun wrap(s: String, maxW: Float, f: PDFont, size: Float): List<String> {
            val out = mutableListOf<String>()
            var cur = ""
            for (w in s.split(" ")) {
                val test = if (cur.isEmpty()) w else "$cur $w"
                if (widthOf(test, f, size) <= maxW || cur.isEmpty()) cur = test
                else { out.add(cur); cur = w }
            }
            if (cur.isNotEmpty()) out.add(cur)
            if (out.isEmpty()) out.add("")
            return out
        }
        fun pct(v: Double) = (if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()) + "%"
        fun qty(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
        fun lineNet(li: LineItem): Money { val g = li.unitPriceHT * li.qty; return g - g.percent(li.discountPct) }

        var y = ph - margin
        val headerTop = y

        // Header — company info (left) + logo (right)
        draw(left, y - 13, d.company.legalName, bold, 13f, ink); y -= 18
        draw(left, y - 9, d.company.addressLine, regular, 9f, muted); y -= 12
        draw(left, y - 9, d.company.legalIdsLine, regular, 9f, muted); y -= 12
        d.company.phone?.let { draw(left, y - 9, "Tél : $it", regular, 9f, muted); y -= 12 }
        d.company.email?.let { draw(left, y - 9, "Email : $it", regular, 9f, muted); y -= 12 }
        d.logoPng?.let { cleanLogoBitmap(it) }?.let { bmp ->
            val img = LosslessFactory.createFromImage(doc, bmp)
            val s = 84f
            cs.drawImage(img, right - s, headerTop - s, s, s)
        }
        y = minOf(y, headerTop - 88f) - 6f

        hLine(left, right, y, line); y -= 16

        // Document meta (left) + Adressé à (right)
        val metaTop = y
        draw(left, y - 18, d.docTitle, bold, 18f, ink)
        var ly = y - 18 - 14
        draw(left, ly - 10, "N° : ${d.number}", regular, 10f, ink); ly -= 14
        draw(left, ly - 10, "Date : ${d.dateText}", regular, 10f, muted); ly -= 14
        draw(left, ly - 10, "Mode : ${paymentMethodText(d)}", regular, 10f, muted); ly -= 14

        val boxX = left + (right - left) / 2f + 10f
        val boxW = right - boxX
        draw(boxX, metaTop - 9, "ADRESSÉ À", bold, 8.5f, muted)
        val boxTop = metaTop - 14
        var by = boxTop - 14
        draw(boxX + 8, by, d.clientName, bold, 11f, ink); by -= 13
        if (d.clientAddress.isNotBlank()) { wrap(d.clientAddress, boxW - 16, regular, 9f).forEach { draw(boxX + 8, by, it, regular, 9f, muted); by -= 11 } }
        d.clientRc?.takeIf { it.isNotBlank() }?.let { draw(boxX + 8, by, "RC : $it", regular, 9f, muted); by -= 11 }
        d.clientNif?.takeIf { it.isNotBlank() }?.let { draw(boxX + 8, by, "NIF : $it", regular, 9f, muted); by -= 11 }
        d.clientNis?.takeIf { it.isNotBlank() }?.let { draw(boxX + 8, by, "NIS : $it", regular, 9f, muted); by -= 11 }
        d.clientArticle?.takeIf { it.isNotBlank() }?.let { draw(boxX + 8, by, "Art. imp. : $it", regular, 9f, muted); by -= 11 }
        val boxBottom = by - 4
        rectStroke(boxX, boxBottom, boxW, boxTop - boxBottom, line)

        y = minOf(ly, boxBottom) - 14

        drawR(right, y - 8, "Montants exprimés en Dinar Algérien", regular, 8f, muted); y -= 12

        // Line-items table
        val tW = right - left
        val sum = 4.2f + 0.9f + 1.4f + 0.8f + 1.4f
        val cDes = tW * 4.2f / sum
        val cTva = tW * 0.9f / sum
        val cPu = tW * 1.4f / sum
        val cQte = tW * 0.8f / sum
        val x0 = left
        val x1 = x0 + cDes
        val x2 = x1 + cTva
        val x3 = x2 + cPu
        val x4 = x3 + cQte
        val x5 = right
        val tableTop = y
        val headH = 18f
        rectFill(x0, y - headH, tW, headH, boxBg)
        val hb = y - headH + 6
        draw(x0 + 6, hb, "Désignation", bold, 8.5f, muted)
        drawR(x2 - 6, hb, "TVA", bold, 8.5f, muted)
        drawR(x3 - 6, hb, "P.U. HT", bold, 8.5f, muted)
        drawR(x4 - 6, hb, "Qté", bold, 8.5f, muted)
        drawR(x5 - 6, hb, "Total HT", bold, 8.5f, muted)
        y -= headH
        val boundaries = mutableListOf(y)
        for (li in d.lines) {
            val desLines = wrap(li.designation, cDes - 12, bold, 9.5f)
            val sub = li.unit + (if (li.discountPct > 0) "  •  remise ${pct(li.discountPct)}" else "")
            val rowH = maxOf(26f, 10f + desLines.size * 11f + 11f)
            var ry = y - 12
            for (dl in desLines) { draw(x0 + 6, ry, dl, bold, 9.5f, ink); ry -= 11 }
            draw(x0 + 6, ry, sub, regular, 8f, muted)
            val cb = y - 13
            drawR(x2 - 6, cb, pct(li.vatPct), regular, 9.5f, ink)
            drawR(x3 - 6, cb, li.unitPriceHT.format(withSymbol = false), regular, 9.5f, ink)
            drawR(x4 - 6, cb, qty(li.qty), regular, 9.5f, ink)
            drawR(x5 - 6, cb, lineNet(li).format(withSymbol = false), bold, 9.5f, ink)
            y -= rowH
            boundaries.add(y)
        }
        val tableBottom = y
        rectStroke(x0, tableBottom, tW, tableTop - tableBottom, line)
        for (i in 0 until boundaries.size - 1) hLine(x0, x5, boundaries[i], line)
        for (vx in listOf(x1, x2, x3, x4)) vLine(vx, tableBottom, tableTop, line)
        y = tableBottom - 18

        // Mode de règlement (left) + totals (right)
        val totW = 250f
        val totX = right - totW
        val condTop = y
        draw(left, condTop - 11, "Mode de règlement :", bold, 9.5f, ink)
        var cy = condTop - 11 - 13
        draw(left, cy, paymentMethodText(d), regular, 9f, muted); cy -= 13
        d.company.bankName?.let { draw(left, cy, "Banque : $it", regular, 9f, muted); cy -= 12 }
        d.company.iban?.let { draw(left, cy, "RIB / IBAN : $it", regular, 9f, muted); cy -= 12 }

        var ty = condTop
        fun totalRow(label: String, value: String) {
            draw(totX, ty - 11, label, regular, 10f, muted)
            drawR(right, ty - 11, value, regular, 10f, ink)
            ty -= 16
        }
        totalRow("Total HT", d.breakdown.totalHT.format())
        if (d.breakdown.totalDiscount.amountMinor > 0) totalRow("Remise", "-" + d.breakdown.totalDiscount.format())
        totalRow(if (d.vatExempt) "TVA (exonéré)" else "TVA", d.breakdown.vatAmount.format())
        if (d.cash) totalRow("Droit de timbre", d.breakdown.stampTax.format())
        ty -= 4
        val ttcH = 24f
        rectFill(totX, ty - ttcH, totW, ttcH, ttcBg)
        draw(totX + 12, ty - ttcH + 8, "Total TTC", bold, 11f, ink)
        drawR(right - 12, ty - ttcH + 8, d.breakdown.totalTTC.format(), bold, 12f, ink)
        ty -= ttcH

        y = minOf(cy, ty) - 16

        draw(left, y - 10, "Arrêté la présente facture à la somme de : ${amountInWordsDZD(d.breakdown.totalTTC)}.", bold, 9.5f, ink)
        y -= 24

        draw(left, y - 8, "Cachet, Date, Signature", regular, 8f, muted); y -= 12
        rectStroke(left, y - 55, right - left, 55f, line); y -= 64

        hLine(left, right, y, line); y -= 12
        drawC((left + right) / 2f, y - 9, d.company.legalName, bold, 9f, ink); y -= 12
        drawC((left + right) / 2f, y - 8, "Siège social : ${d.company.addressLine}", regular, 8f, muted); y -= 11
        drawC((left + right) / 2f, y - 8, d.company.legalIdsLine + (d.company.phone?.let { "   •   Tél : $it" } ?: ""), regular, 8f, muted)

        cs.close()
        FileOutputStream(outFile).use { doc.save(it) }
        doc.close()
    }

    private fun paymentMethodText(d: InvoiceExportData): String = when (d.paymentMethod) {
        PaymentMode.CASH -> "Espèces"
        PaymentMode.BANK_TRANSFER -> "Virement bancaire"
        PaymentMode.CARD -> "Carte bancaire"
        PaymentMode.CHEQUE -> buildString {
            append("Chèque")
            d.chequeNumber?.takeIf { it.isNotBlank() }?.let { append(" n° $it") }
            d.chequeBank?.takeIf { it.isNotBlank() }?.let { append(" — $it") }
        }
    }
}
