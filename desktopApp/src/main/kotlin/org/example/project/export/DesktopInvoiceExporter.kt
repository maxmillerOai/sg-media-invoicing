package org.example.project.export

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.Image
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.apache.poi.util.Units
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.example.project.core.Money
import org.example.project.core.amountInWordsDZD
import org.example.project.domain.addressLine
import org.example.project.domain.legalIdsLine
import org.example.project.domain.legalName
import org.example.project.domain.model.LineItem
import org.example.project.domain.model.PaymentMode
import java.awt.Color
import java.awt.Desktop
import java.awt.FileDialog
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** Human "Mode de règlement" label, including cheque number/bank when relevant. */
private fun paymentMethodText(data: InvoiceExportData): String = when (data.paymentMethod) {
    PaymentMode.CASH -> "Espèces"
    PaymentMode.BANK_TRANSFER -> "Virement bancaire"
    PaymentMode.CARD -> "Carte bancaire"
    PaymentMode.CHEQUE -> buildString {
        append("Chèque")
        data.chequeNumber?.takeIf { it.isNotBlank() }?.let { append(" n° $it") }
        data.chequeBank?.takeIf { it.isNotBlank() }?.let { append(" — $it") }
    }
}

private val INK = Color(0x12, 0x13, 0x2A)
private val MUTED = Color(0x6B, 0x72, 0x80)
private val LINE = Color(0xD8, 0xDE, 0xE9)
private val BOX_BG = Color(0xF1, 0xF3, 0xF8)
private val TTC_BG = Color(0xDC, 0xEA, 0xF7)

/** Desktop (JVM) document generation + printing. */
class DesktopInvoiceExporter : InvoiceExporter {

    override suspend fun export(format: ExportFormat, data: InvoiceExportData): ExportResult {
        // The save dialog must run on the AWT event thread; a native FileDialog avoids the
        // re-entrant dispatcher corruption that a Swing JFileChooser triggers under Compose.
        val file = withContext(Dispatchers.Swing) { chooseSaveFile(data.suggestedFileName, format) }
            ?: return ExportResult.Cancelled
        return try {
            val bytes = withContext(Dispatchers.IO) {
                when (format) {
                    ExportFormat.PDF -> generatePdf(data)
                    ExportFormat.XLSX -> generateXlsx(data)
                    ExportFormat.DOCX -> generateDocx(data)
                }
            }
            withContext(Dispatchers.IO) { file.writeBytes(bytes) }
            ExportResult.Saved(file.absolutePath)
        } catch (t: Throwable) {
            ExportResult.Failed(t.message ?: t.toString())
        }
    }

    override suspend fun print(data: InvoiceExportData): ExportResult {
        return try {
            val bytes = withContext(Dispatchers.IO) { generatePdf(data) }
            val tmp = withContext(Dispatchers.IO) {
                File.createTempFile(data.suggestedFileName, ".pdf").also { it.writeBytes(bytes); it.deleteOnExit() }
            }
            if (!Desktop.isDesktopSupported()) return ExportResult.Failed("Impression non supportée sur ce système")
            val d = Desktop.getDesktop()
            when {
                d.isSupported(Desktop.Action.PRINT) -> d.print(tmp)
                d.isSupported(Desktop.Action.OPEN) -> d.open(tmp)
                else -> return ExportResult.Failed("Aucune action d'impression disponible")
            }
            ExportResult.Saved(tmp.absolutePath)
        } catch (t: Throwable) {
            ExportResult.Failed(t.message ?: t.toString())
        }
    }

    private fun chooseSaveFile(baseName: String, format: ExportFormat): File? {
        val dialog = FileDialog(null as java.awt.Frame?, "Enregistrer ${format.label}", FileDialog.SAVE).apply {
            file = "$baseName.${format.extension}"
            isVisible = true
        }
        val name = dialog.file ?: return null // null when the user cancels
        var file = File(dialog.directory ?: "", name)
        if (!file.name.lowercase().endsWith(".${format.extension}")) {
            file = File(file.absolutePath + "." + format.extension)
        }
        return file
    }

    // ---- helpers -------------------------------------------------------------

    private fun lineNet(line: LineItem): Money {
        val gross = line.unitPriceHT * line.qty
        return gross - gross.percent(line.discountPct)
    }

    private fun pct(value: Double): String =
        (if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()) + "%"

    private fun qty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private fun Money.toMajor(): Double = amountMinor / 100.0

    /**
     * Prepares the logo for the PDF: downscales it and "keys out" its light grey/checkerboard
     * background to pure white (so it disappears against the white page), keeping the dark mark.
     * The source PNG is RGB (no alpha), so a plain composite wouldn't remove the backdrop.
     * Returns an OpenPDF [Image] built directly from the processed AWT image, or null on failure.
     */
    private fun cleanLogoBytes(png: ByteArray): ByteArray? = try {
        val src = javax.imageio.ImageIO.read(ByteArrayInputStream(png)) ?: return null
        val size = 360
        val buf = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB)
        buf.createGraphics().apply {
            setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            color = Color.WHITE
            fillRect(0, 0, size, size)
            drawImage(src, 0, 0, size, size, null)
            dispose()
        }
        for (y in 0 until size) for (x in 0 until size) {
            val rgb = buf.getRGB(x, y)
            val r = (rgb shr 16) and 0xFF; val g = (rgb shr 8) and 0xFF; val b = rgb and 0xFF
            val lum = r * 0.299 + g * 0.587 + b * 0.114
            if (lum > 200) buf.setRGB(x, y, 0xFFFFFF) // background → white
        }
        ByteArrayOutputStream().use { bos -> javax.imageio.ImageIO.write(buf, "png", bos); bos.toByteArray() }
    } catch (_: Throwable) {
        null
    }

    private fun cleanLogo(png: ByteArray): Image? = cleanLogoBytes(png)?.let { Image.getInstance(it) }

    // ---- PDF (OpenPDF) -------------------------------------------------------

    private fun generatePdf(data: InvoiceExportData): ByteArray {
        val out = ByteArrayOutputStream()
        val doc = Document(PageSize.A4, 40f, 40f, 40f, 40f)
        PdfWriter.getInstance(doc, out)
        doc.open()

        fun font(size: Float, bold: Boolean = false, color: Color = INK) =
            Font(Font.HELVETICA, size, if (bold) Font.BOLD else Font.NORMAL, color)

        fun noBorder(block: PdfPCell.() -> Unit) = PdfPCell().apply { border = 0; block() }

        // A paragraph with line spacing proportional to its font size (clean, no overlap/cramming).
        fun line(text: String, f: Font) = Paragraph(text, f).apply { setLeading(0f, 1.3f) }

        // Header: company info (left) + logo (right), same level
        val header = PdfPTable(floatArrayOf(1.7f, 1f)).apply { widthPercentage = 100f }
        header.addCell(noBorder {
            setPaddingRight(12f)
            verticalAlignment = Element.ALIGN_TOP
            addElement(line(data.company.legalName, font(13f, bold = true)))
            addElement(line(data.company.addressLine, font(9f, color = MUTED)))
            addElement(line(data.company.legalIdsLine, font(9f, color = MUTED)))
            data.company.phone?.let { addElement(line("Tél : $it", font(9f, color = MUTED))) }
            data.company.email?.let { addElement(line("Email : $it", font(9f, color = MUTED))) }
        })
        header.addCell(noBorder {
            horizontalAlignment = Element.ALIGN_RIGHT
            verticalAlignment = Element.ALIGN_TOP
            data.logoPng?.let { png ->
                cleanLogo(png)?.let { img ->
                    img.scaleToFit(86f, 86f)
                    img.alignment = Image.RIGHT
                    addElement(img)
                }
            }
        })
        doc.add(header)
        doc.add(Paragraph(" ", font(8f)))

        // Document title/meta (left) + Adressé à (right)
        val parties = PdfPTable(floatArrayOf(1f, 1f)).apply { widthPercentage = 100f }
        parties.addCell(noBorder {
            verticalAlignment = Element.ALIGN_TOP
            addElement(Paragraph(data.docTitle, font(18f, bold = true)).apply { setLeading(0f, 1.1f) })
            addElement(line("N° : ${data.number}", font(10f)))
            addElement(line("Date : ${data.dateText}", font(10f, color = MUTED)))
        })
        parties.addCell(PdfPCell().apply {
            borderColor = LINE; setPadding(10f)
            addElement(line("ADRESSÉ À", font(8.5f, color = MUTED)))
            addElement(line(data.clientName, font(11f, bold = true)))
            if (data.clientAddress.isNotBlank()) addElement(line(data.clientAddress, font(9f, color = MUTED)))
            data.clientRc?.takeIf { it.isNotBlank() }?.let { addElement(line("RC : $it", font(9f, color = MUTED))) }
            data.clientNif?.takeIf { it.isNotBlank() }?.let { addElement(line("NIF : $it", font(9f, color = MUTED))) }
            data.clientNis?.takeIf { it.isNotBlank() }?.let { addElement(line("NIS : $it", font(9f, color = MUTED))) }
            data.clientArticle?.takeIf { it.isNotBlank() }?.let { addElement(line("Art. imp. : $it", font(9f, color = MUTED))) }
        })
        doc.add(parties)

        doc.add(Paragraph("Montants exprimés en Dinar Algérien", font(8f, color = MUTED)).apply {
            alignment = Element.ALIGN_RIGHT; spacingBefore = 10f; spacingAfter = 4f
        })

        // Items table
        val table = PdfPTable(floatArrayOf(4.2f, 0.9f, 1.4f, 0.8f, 1.4f)).apply { widthPercentage = 100f }
        fun headCell(text: String, align: Int) = PdfPCell(Phrase(text, font(8.5f, bold = true, color = MUTED))).apply {
            backgroundColor = BOX_BG; horizontalAlignment = align; setPadding(6f); borderColor = LINE
        }
        table.addCell(headCell("Désignation", Element.ALIGN_LEFT))
        table.addCell(headCell("TVA", Element.ALIGN_RIGHT))
        table.addCell(headCell("P.U. HT", Element.ALIGN_RIGHT))
        table.addCell(headCell("Qté", Element.ALIGN_RIGHT))
        table.addCell(headCell("Total HT", Element.ALIGN_RIGHT))
        fun bodyCell(phrase: Phrase, align: Int) = PdfPCell(phrase).apply { horizontalAlignment = align; setPadding(6f); borderColor = LINE }
        data.lines.forEach { line ->
            val desig = Paragraph().apply {
                add(Phrase(line.designation + "\n", font(9.5f, bold = true)))
                val sub = line.unit + (if (line.discountPct > 0) "  •  remise ${pct(line.discountPct)}" else "")
                add(Phrase(sub, font(8f, color = MUTED)))
            }
            table.addCell(PdfPCell().apply { setPadding(6f); borderColor = LINE; addElement(desig) })
            table.addCell(bodyCell(Phrase(pct(line.vatPct), font(9.5f)), Element.ALIGN_RIGHT))
            table.addCell(bodyCell(Phrase(line.unitPriceHT.format(withSymbol = false), font(9.5f)), Element.ALIGN_RIGHT))
            table.addCell(bodyCell(Phrase(qty(line.qty), font(9.5f)), Element.ALIGN_RIGHT))
            table.addCell(bodyCell(Phrase(lineNet(line).format(withSymbol = false), font(9.5f, bold = true)), Element.ALIGN_RIGHT))
        }
        doc.add(table)
        doc.add(Paragraph(" ", font(6f)))

        // Conditions (left) + totals (right)
        val bottom = PdfPTable(floatArrayOf(1.1f, 1f)).apply { widthPercentage = 100f }
        bottom.addCell(noBorder {
            addElement(Paragraph("Mode de règlement :", font(9.5f, bold = true)))
            addElement(line(paymentMethodText(data), font(9f, color = MUTED)))
            data.company.bankName?.let { addElement(Paragraph("Banque : $it", font(9f, color = MUTED))) }
            data.company.iban?.let { addElement(Paragraph("RIB / IBAN : $it", font(9f, color = MUTED))) }
        })
        val totals = PdfPTable(floatArrayOf(1f, 1f)).apply { widthPercentage = 100f }
        fun totalRow(label: String, value: String) {
            totals.addCell(noBorder { addElement(Paragraph(label, font(9.5f, color = MUTED))) })
            totals.addCell(noBorder { horizontalAlignment = Element.ALIGN_RIGHT; addElement(Paragraph(value, font(9.5f)).apply { alignment = Element.ALIGN_RIGHT }) })
        }
        totalRow("Total HT", data.breakdown.totalHT.format())
        if (data.breakdown.totalDiscount.amountMinor > 0) totalRow("Remise", "-" + data.breakdown.totalDiscount.format())
        totalRow(if (data.vatExempt) "TVA (exonéré)" else "TVA", data.breakdown.vatAmount.format())
        if (data.cash) totalRow("Droit de timbre", data.breakdown.stampTax.format())
        totals.addCell(PdfPCell(Phrase("Total TTC", font(11f, bold = true))).apply { backgroundColor = TTC_BG; border = 0; setPadding(8f) })
        totals.addCell(PdfPCell(Phrase(data.breakdown.totalTTC.format(), font(11f, bold = true))).apply { backgroundColor = TTC_BG; border = 0; setPadding(8f); horizontalAlignment = Element.ALIGN_RIGHT })
        bottom.addCell(noBorder { addElement(totals) })
        doc.add(bottom)

        // Amount in words
        doc.add(Paragraph("Arrêté la présente facture à la somme de : ${amountInWordsDZD(data.breakdown.totalTTC)}.", font(9.5f, bold = true)).apply { spacingBefore = 12f })

        // Signature box
        doc.add(Paragraph("Cachet, Date, Signature", font(8f, color = MUTED)).apply { spacingBefore = 16f; spacingAfter = 4f })
        val sign = PdfPTable(1).apply { widthPercentage = 100f }
        sign.addCell(PdfPCell().apply { minimumHeight = 55f; borderColor = LINE })
        doc.add(sign)

        // Footer — mirrors the header: company name, then registered office (siège social), then IDs.
        doc.add(Paragraph("\n${data.company.legalName}", font(9f, bold = true)).apply { alignment = Element.ALIGN_CENTER; setLeading(0f, 1.3f) })
        doc.add(Paragraph("Siège social : ${data.company.addressLine}", font(8f, color = MUTED)).apply { alignment = Element.ALIGN_CENTER; setLeading(0f, 1.3f) })
        doc.add(Paragraph(data.company.legalIdsLine + (data.company.phone?.let { "   •   Tél : $it" } ?: ""), font(8f, color = MUTED)).apply { alignment = Element.ALIGN_CENTER; setLeading(0f, 1.3f) })

        doc.close()
        return out.toByteArray()
    }

    // ---- XLSX (Apache POI) ---------------------------------------------------

    private fun generateXlsx(data: InvoiceExportData): ByteArray {
        XSSFWorkbook().use { wb ->
            val sheet = wb.createSheet("Facture")
            val money = wb.createDataFormat().getFormat("#,##0.00")

            val boldFont = wb.createFont().apply { bold = true }
            val titleFont = wb.createFont().apply { bold = true; fontHeightInPoints = 14.toShort() }
            val titleStyle = wb.createCellStyle().apply { setFont(titleFont) }
            val headStyle = wb.createCellStyle().apply { setFont(boldFont) }
            val moneyStyle = wb.createCellStyle().apply { dataFormat = money }
            val moneyBold = wb.createCellStyle().apply { setFont(boldFont); dataFormat = money }

            var r = 0
            fun row() = sheet.createRow(r++)
            fun text(row: org.apache.poi.ss.usermodel.Row, col: Int, value: String, style: org.apache.poi.ss.usermodel.CellStyle? = null) {
                row.createCell(col).apply { setCellValue(value); if (style != null) cellStyle = style }
            }
            fun num(row: org.apache.poi.ss.usermodel.Row, col: Int, value: Double, style: org.apache.poi.ss.usermodel.CellStyle) {
                row.createCell(col).apply { setCellValue(value); cellStyle = style }
            }

            text(row(), 0, data.company.legalName, titleStyle)
            text(row(), 0, data.company.addressLine)
            text(row(), 0, data.company.legalIdsLine)
            data.company.phone?.let { text(row(), 0, "Tél : $it") }
            row()
            text(row(), 0, "${data.docTitle}  N° ${data.number}", headStyle)
            text(row(), 0, "Date : ${data.dateText}")
            text(row(), 0, "Mode : ${paymentMethodText(data)}")
            row()
            text(row(), 0, "Adressé à : ${data.clientName}", headStyle)
            if (data.clientAddress.isNotBlank()) text(row(), 0, data.clientAddress)
            data.clientRc?.takeIf { it.isNotBlank() }?.let { text(row(), 0, "RC : $it") }
            data.clientNif?.takeIf { it.isNotBlank() }?.let { text(row(), 0, "NIF : $it") }
            data.clientNis?.takeIf { it.isNotBlank() }?.let { text(row(), 0, "NIS : $it") }
            data.clientArticle?.takeIf { it.isNotBlank() }?.let { text(row(), 0, "Art. imp. : $it") }
            row()

            val header = row()
            listOf("Désignation", "Unité", "TVA %", "P.U. HT", "Qté", "Remise %", "Total HT").forEachIndexed { i, h ->
                text(header, i, h, headStyle)
            }
            data.lines.forEach { line ->
                val row = row()
                text(row, 0, line.designation)
                text(row, 1, line.unit)
                num(row, 2, line.vatPct, wb.createCellStyle())
                num(row, 3, line.unitPriceHT.toMajor(), moneyStyle)
                num(row, 4, line.qty, wb.createCellStyle())
                num(row, 5, line.discountPct, wb.createCellStyle())
                num(row, 6, lineNet(line).toMajor(), moneyStyle)
            }
            row()
            fun totalRow(label: String, value: Money, strong: Boolean = false) {
                val row = row()
                text(row, 5, label, if (strong) headStyle else null)
                num(row, 6, value.toMajor(), if (strong) moneyBold else moneyStyle)
            }
            totalRow("Total HT", data.breakdown.totalHT)
            if (data.breakdown.totalDiscount.amountMinor > 0) totalRow("Remise", data.breakdown.totalDiscount)
            totalRow(if (data.vatExempt) "TVA (exonéré)" else "TVA", data.breakdown.vatAmount)
            if (data.cash) totalRow("Droit de timbre", data.breakdown.stampTax)
            totalRow("Total TTC", data.breakdown.totalTTC, strong = true)
            row()
            text(row(), 0, "Arrêté la présente facture à la somme de : ${amountInWordsDZD(data.breakdown.totalTTC)}.")

            (0..6).forEach { sheet.setColumnWidth(it, if (it == 0) 12000 else 3800) }

            val out = ByteArrayOutputStream()
            wb.write(out)
            return out.toByteArray()
        }
    }

    // ---- DOCX (Apache POI) ---------------------------------------------------

    private fun generateDocx(data: InvoiceExportData): ByteArray {
        XWPFDocument().use { doc ->
            (data.logoPng?.let { cleanLogoBytes(it) } ?: data.logoPng)?.let { png ->
                val p = doc.createParagraph()
                val run = p.createRun()
                ByteArrayInputStream(png).use { stream ->
                    run.addPicture(stream, XWPFDocument.PICTURE_TYPE_PNG, "logo.png", Units.toEMU(70.0), Units.toEMU(70.0))
                }
            }
            fun para(text: String, bold: Boolean = false, size: Int = 11) {
                val p = doc.createParagraph()
                p.createRun().apply { isBold = bold; fontSize = size; setText(text) }
            }
            para(data.docTitle, bold = true, size = 16)
            para("N° ${data.number}     Date : ${data.dateText}     Mode : ${paymentMethodText(data)}", size = 10)
            para("")
            para("Émetteur : ${data.company.legalName}", bold = true)
            para(data.company.addressLine, size = 9)
            para(data.company.legalIdsLine, size = 9)
            para("")
            para("Adressé à : ${data.clientName}", bold = true)
            if (data.clientAddress.isNotBlank()) para(data.clientAddress, size = 9)
            data.clientRc?.takeIf { it.isNotBlank() }?.let { para("RC : $it", size = 9) }
            data.clientNif?.takeIf { it.isNotBlank() }?.let { para("NIF : $it", size = 9) }
            data.clientNis?.takeIf { it.isNotBlank() }?.let { para("NIS : $it", size = 9) }
            data.clientArticle?.takeIf { it.isNotBlank() }?.let { para("Art. imp. : $it", size = 9) }
            para("")

            val headers = listOf("Désignation", "TVA", "P.U. HT", "Qté", "Total HT")
            val table = doc.createTable(1, headers.size)
            table.getRow(0).tableCells.forEachIndexed { i, cell ->
                cell.paragraphs.firstOrNull()?.createRun()?.apply { isBold = true; setText(headers[i]) }
            }
            data.lines.forEach { line ->
                val cells = table.createRow().tableCells
                cells[0].setText("${line.designation} (${line.unit})${if (line.discountPct > 0) " — remise ${pct(line.discountPct)}" else ""}")
                cells[1].setText(pct(line.vatPct))
                cells[2].setText(line.unitPriceHT.format(withSymbol = false))
                cells[3].setText(qty(line.qty))
                cells[4].setText(lineNet(line).format(withSymbol = false))
            }
            para("")
            fun totalLine(label: String, value: Money, bold: Boolean = false) = para("$label : ${value.format()}", bold = bold, size = if (bold) 12 else 10)
            totalLine("Total HT", data.breakdown.totalHT)
            if (data.breakdown.totalDiscount.amountMinor > 0) totalLine("Remise", data.breakdown.totalDiscount)
            totalLine(if (data.vatExempt) "TVA (exonéré)" else "TVA", data.breakdown.vatAmount)
            if (data.cash) totalLine("Droit de timbre", data.breakdown.stampTax)
            totalLine("Total TTC", data.breakdown.totalTTC, bold = true)

            para("")
            para("Arrêté la présente facture à la somme de : ${amountInWordsDZD(data.breakdown.totalTTC)}.", bold = true, size = 10)
            para("")
            para("Cachet, Date, Signature", size = 9)
            para("")
            para(data.company.legalName, bold = true, size = 9)
            para("Siège social : ${data.company.addressLine}", size = 8)
            para("${data.company.legalIdsLine}${data.company.phone?.let { " — Tél : $it" } ?: ""}", size = 8)

            val out = ByteArrayOutputStream()
            doc.write(out)
            return out.toByteArray()
        }
    }
}
