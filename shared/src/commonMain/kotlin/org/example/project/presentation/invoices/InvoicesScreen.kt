package org.example.project.presentation.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.example.project.core.Money
import org.example.project.core.currentLocalDate
import org.example.project.core.frShort
import org.example.project.data.CatalogRepository
import org.example.project.data.ClientRepository
import org.example.project.data.CompanyRepository
import org.example.project.data.InvoiceRepository
import org.example.project.data.PaymentRecord
import org.example.project.data.PaymentRepository
import org.example.project.data.SavedInvoice
import org.example.project.domain.SgMediaCompany
import org.example.project.domain.model.CatalogItem
import org.example.project.domain.model.Client
import org.example.project.domain.model.DocumentStatus
import org.example.project.domain.model.LineItem
import org.example.project.domain.model.PaymentMode
import org.example.project.domain.model.label
import org.example.project.domain.usecase.ComputeTotals
import org.example.project.domain.usecase.effectiveStatus
import org.example.project.domain.usecase.outstanding
import org.example.project.export.ExportFormat
import org.example.project.export.ExportResult
import org.example.project.export.InvoiceExportData
import org.example.project.export.LocalInvoiceExporter
import org.example.project.presentation.components.AppIcon
import org.example.project.presentation.components.GradientButton
import org.example.project.presentation.components.OutlineAction
import org.example.project.presentation.components.ScrollableColumn
import org.example.project.presentation.components.SectionHeader
import org.example.project.presentation.components.StatusChip
import org.example.project.presentation.components.StatusPill
import org.example.project.presentation.components.VScrollbar
import org.example.project.presentation.components.statusLabel
import org.example.project.presentation.i18n.AppStrings
import org.example.project.presentation.i18n.LocalStrings
import org.example.project.presentation.theme.AgencyPalette
import org.example.project.presentation.theme.Gradients
import org.koin.compose.koinInject
import sgmediaprod.shared.generated.resources.Res

private enum class Mode { LIST, EDITOR, VIEW }

private data class DocType(val title: String, val prefix: String)

private val docTypes = listOf(
    DocType("FACTURE", "FAC"),
    DocType("FACTURE PROFORMA", "PRO"),
    DocType("BON DE COMMANDE", "BC"),
    DocType("OFFRE", "OFF"),
)

private fun effectiveLines(inv: SavedInvoice): List<LineItem> =
    if (inv.applyRemise) inv.lines else inv.lines.map { it.copy(discountPct = 0.0) }

@Composable
fun InvoicesScreen(modifier: Modifier = Modifier) {
    val repo: InvoiceRepository = koinInject()
    val paymentRepo: PaymentRepository = koinInject()
    val compute: ComputeTotals = koinInject()
    var mode by remember { mutableStateOf(Mode.LIST) }
    var selected by remember { mutableStateOf<SavedInvoice?>(null) }
    var editing by remember { mutableStateOf<SavedInvoice?>(null) }

    when (mode) {
        Mode.LIST -> InvoiceListView(
            modifier = modifier,
            repo = repo,
            paymentRepo = paymentRepo,
            compute = compute,
            onNew = { editing = null; mode = Mode.EDITOR },
            onOpen = { selected = it; mode = Mode.VIEW },
        )
        Mode.EDITOR -> InvoiceEditorView(
            modifier = modifier,
            repo = repo,
            compute = compute,
            onDone = { editing = null; mode = Mode.LIST },
            existing = editing,
        )
        Mode.VIEW -> selected?.let { inv ->
            InvoiceDetailView(
                modifier = modifier,
                invoice = inv,
                repo = repo,
                paymentRepo = paymentRepo,
                compute = compute,
                onBack = { mode = Mode.LIST },
                onEdit = { editing = inv; mode = Mode.EDITOR },
            )
        } ?: run { mode = Mode.LIST }
    }
}

// ── List ─────────────────────────────────────────────────────────────────────

@Composable
private fun InvoiceListView(
    modifier: Modifier,
    repo: InvoiceRepository,
    paymentRepo: PaymentRepository,
    compute: ComputeTotals,
    onNew: () -> Unit,
    onOpen: (SavedInvoice) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val today = remember { currentLocalDate() }
    var invoices by remember { mutableStateOf<List<SavedInvoice>>(emptyList()) }
    var paidMap by remember { mutableStateOf<Map<Long, Money>>(emptyMap()) }
    var loaded by remember { mutableStateOf(false) }

    suspend fun reload() { invoices = repo.list(); paidMap = paymentRepo.paidByInvoice(); loaded = true }
    LaunchedEffect(Unit) { reload() }

    Column(modifier = modifier.fillMaxSize().padding(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Factures", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            GradientButton("Nouvelle facture", Gradients.brand, leadingIcon = AppIcon.ADD, onClick = onNew)
        }
        Spacer(Modifier.height(20.dp))

        if (loaded && invoices.isEmpty()) {
            Text("Aucune facture pour le moment. Créez-en une avec « Nouvelle facture ».", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(Modifier.verticalScroll(rememberScrollState())) {
            invoices.forEach { inv ->
                val ttc = compute.compute(effectiveLines(inv), if (inv.cash) PaymentMode.CASH else PaymentMode.BANK_TRANSFER, inv.vatExempt).totalTTC
                val paid = paidMap[inv.id] ?: Money.ZERO
                val status = effectiveStatus(inv.status, ttc, paid, inv.dueDate, today)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(inv.clientName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("${inv.number}  •  ${inv.issueDate.frShort()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(ttc.format(), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(12.dp))
                        StatusChip(inv.docType.substringBefore(" ").ifBlank { "DOC" }, AgencyPalette.Violet)
                        Spacer(Modifier.width(8.dp))
                        StatusPill(status)
                        Spacer(Modifier.width(12.dp))
                        OutlineAction("Ouvrir", onClick = { onOpen(inv) })
                        Spacer(Modifier.width(8.dp))
                        OutlineAction("Suppr.", onClick = { scope.launch { repo.delete(inv.id); reload() } })
                    }
                }
            }
        }
    }
}

// ── Editor ───────────────────────────────────────────────────────────────────

private data class LineDraft(
    val designation: String = "",
    val unit: String = "u",
    val qty: String = "1",
    val price: String = "",
    val vat: String = "19",
    val discount: String = "0",
)

private fun LineDraft.toLineItem() = LineItem(
    designation = designation,
    qty = qty.replace(',', '.').toDoubleOrNull() ?: 0.0,
    unit = unit.ifBlank { "u" },
    unitPriceHT = Money.parse(price) ?: Money.ZERO,
    discountPct = discount.replace(',', '.').toDoubleOrNull() ?: 0.0,
    vatPct = vat.replace(',', '.').toDoubleOrNull() ?: 19.0,
)

private fun numStr(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

/** Maps a stored line item back into an editable draft (for the edit flow). */
private fun LineItem.toDraft() = LineDraft(
    designation = designation,
    unit = unit,
    qty = numStr(qty),
    price = unitPriceHT.format(withSymbol = false),
    vat = numStr(vatPct),
    discount = numStr(discountPct),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceEditorView(
    modifier: Modifier,
    repo: InvoiceRepository,
    compute: ComputeTotals,
    onDone: () -> Unit,
    existing: SavedInvoice? = null,
) {
    val scope = rememberCoroutineScope()
    val today = remember { currentLocalDate() }
    val editing = existing != null

    var docType by remember { mutableStateOf(existing?.let { e -> docTypes.firstOrNull { it.title == e.docType } } ?: docTypes[0]) }
    var issueDate by remember { mutableStateOf(existing?.issueDate ?: today) }
    var dueDate by remember { mutableStateOf(existing?.dueDate) }
    var number by remember { mutableStateOf(existing?.number ?: "…") }
    var clientName by remember { mutableStateOf(existing?.clientName ?: "") }
    var clientAddress by remember { mutableStateOf(existing?.clientAddress ?: "") }
    var clientNif by remember { mutableStateOf(existing?.clientNif ?: "") }
    var clientRc by remember { mutableStateOf(existing?.clientRc ?: "") }
    var clientNis by remember { mutableStateOf(existing?.clientNis ?: "") }
    var clientArticle by remember { mutableStateOf(existing?.clientArticle ?: "") }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: PaymentMode.CASH) }
    var chequeNumber by remember { mutableStateOf(existing?.chequeNumber ?: "") }
    var chequeBank by remember { mutableStateOf(existing?.chequeBank ?: "") }
    val cash = paymentMethod == PaymentMode.CASH // droit de timbre applies only to cash
    var vatExempt by remember { mutableStateOf(existing?.vatExempt ?: false) }
    var applyRemise by remember { mutableStateOf(existing?.applyRemise ?: true) }
    val drafts = remember {
        if (existing != null) existing.lines.map { it.toDraft() }.toMutableStateList()
        else mutableStateListOf(
            LineDraft("Identité visuelle (logo + charte)", "forfait", "1", "45000", "19", "0"),
            LineDraft("Community management", "mois", "3", "18000", "19", "10"),
        )
    }
    var saving by remember { mutableStateOf(false) }

    val clientRepo: ClientRepository = koinInject()
    val catalogRepo: CatalogRepository = koinInject()
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var catalog by remember { mutableStateOf<List<CatalogItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        clients = clientRepo.list()
        catalog = catalogRepo.list()
    }
    LaunchedEffect(docType, issueDate) {
        // Keep the existing number when editing; only auto-generate for brand-new documents.
        if (!editing) number = repo.nextNumber(issueDate.year, docType.prefix)
    }

    val lineItems = drafts.map { it.toLineItem() }
    val effective = if (applyRemise) lineItems else lineItems.map { it.copy(discountPct = 0.0) }
    val breakdown = compute.compute(effective, paymentMethod, vatExempt)
    val canSave = clientName.isNotBlank() && drafts.any { it.designation.isNotBlank() }

    ScrollableColumn(modifier = modifier, contentPadding = PaddingValues(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (editing) "Modifier le document" else "Nouveau document", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            Text(number, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                SectionHeader("Document")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Type :", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    var typeMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlineAction(docType.title, onClick = { typeMenu = true })
                        DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                            docTypes.forEach { t ->
                                DropdownMenuItem(text = { Text(t.title) }, onClick = { docType = t; typeMenu = false })
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    var dateDialog by remember { mutableStateOf(false) }
                    OutlineAction("Date : ${issueDate.frShort()}", onClick = { dateDialog = true })
                    if (dateDialog) {
                        val state = rememberDatePickerState(initialSelectedDateMillis = issueDate.toEpochDays().toLong() * 86_400_000L)
                        DatePickerDialog(
                            onDismissRequest = { dateDialog = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    state.selectedDateMillis?.let { issueDate = LocalDate.fromEpochDays((it / 86_400_000L).toInt()) }
                                    dateDialog = false
                                }) { Text("OK") }
                            },
                            dismissButton = { TextButton(onClick = { dateDialog = false }) { Text("Annuler") } },
                        ) { DatePicker(state = state) }
                    }
                    Spacer(Modifier.width(8.dp))
                    var dueDialog by remember { mutableStateOf(false) }
                    OutlineAction(dueDate?.let { "Échéance : ${it.frShort()}" } ?: "Échéance", onClick = { dueDialog = true })
                    if (dueDate != null) {
                        Spacer(Modifier.width(4.dp))
                        OutlineAction("✕", onClick = { dueDate = null })
                    }
                    if (dueDialog) {
                        val state = rememberDatePickerState(initialSelectedDateMillis = (dueDate ?: issueDate).toEpochDays().toLong() * 86_400_000L)
                        DatePickerDialog(
                            onDismissRequest = { dueDialog = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    state.selectedDateMillis?.let { dueDate = LocalDate.fromEpochDays((it / 86_400_000L).toInt()) }
                                    dueDialog = false
                                }) { Text("OK") }
                            },
                            dismissButton = { TextButton(onClick = { dueDialog = false }) { Text("Annuler") } },
                        ) { DatePicker(state = state) }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Client")
                    Spacer(Modifier.weight(1f))
                    var clientMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlineAction("Choisir un client", onClick = { clientMenu = true })
                        DropdownMenu(expanded = clientMenu, onDismissRequest = { clientMenu = false }) {
                            if (clients.isEmpty()) {
                                DropdownMenuItem(text = { Text("Aucun client enregistré") }, onClick = { clientMenu = false })
                            }
                            clients.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.displayName) },
                                    onClick = {
                                        clientName = c.displayName
                                        clientAddress = listOfNotNull(c.address.ifBlank { null }, c.city.ifBlank { null }).joinToString(", ")
                                        clientNif = c.nif ?: ""
                                        clientRc = c.rc ?: ""
                                        clientNis = c.nis ?: ""
                                        clientArticle = c.articleImposition ?: ""
                                        clientMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(clientName, { clientName = it }, label = { Text("Nom du client") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(clientAddress, { clientAddress = it }, label = { Text("Adresse") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(clientRc, { clientRc = it }, label = { Text("RC") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(clientNif, { clientNif = it }, label = { Text("NIF") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(clientNis, { clientNis = it }, label = { Text("NIS") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(clientArticle, { clientArticle = it }, label = { Text("Article d'imposition") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Lignes")
                    Spacer(Modifier.weight(1f))
                    var catMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlineAction("+ Catalogue", onClick = { catMenu = true })
                        DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                            if (catalog.isEmpty()) {
                                DropdownMenuItem(text = { Text("Catalogue vide") }, onClick = { catMenu = false })
                            }
                            catalog.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.name} — ${item.defaultPriceHT.format()}") },
                                    onClick = {
                                        drafts.add(
                                            LineDraft(
                                                designation = item.name,
                                                unit = item.unit,
                                                qty = "1",
                                                price = item.defaultPriceHT.format(withSymbol = false),
                                                vat = item.defaultVatPct.toString(),
                                                discount = "0",
                                            ),
                                        )
                                        catMenu = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlineAction("+ Ligne", onClick = { drafts.add(LineDraft()) })
                }
                Spacer(Modifier.height(8.dp))
                drafts.forEachIndexed { i, d ->
                    LineEditor(
                        draft = d,
                        onChange = { drafts[i] = it },
                        onRemove = { if (drafts.size > 1) drafts.removeAt(i) },
                    )
                    if (i < drafts.lastIndex) { Spacer(Modifier.height(6.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant); Spacer(Modifier.height(6.dp)) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                SectionHeader("Options")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mode de règlement :", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    var methodMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlineAction(paymentModeLabel(paymentMethod), onClick = { methodMenu = true })
                        DropdownMenu(expanded = methodMenu, onDismissRequest = { methodMenu = false }) {
                            PaymentMode.entries.forEach { m ->
                                DropdownMenuItem(text = { Text(paymentModeLabel(m)) }, onClick = { paymentMethod = m; methodMenu = false })
                            }
                        }
                    }
                }
                if (paymentMethod == PaymentMode.CHEQUE) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(chequeNumber, { chequeNumber = it }, label = { Text("N° de chèque") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(chequeBank, { chequeBank = it }, label = { Text("Banque") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
                if (cash) {
                    Spacer(Modifier.height(4.dp))
                    Text("Droit de timbre appliqué (paiement en espèces).", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
                ToggleRow("Exonéré de TVA", vatExempt) { vatExempt = it }
                ToggleRow("Appliquer la remise", applyRemise) { applyRemise = it }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))
                TotalLine("Total HT", breakdown.totalHT.format())
                TotalLine(if (vatExempt) "TVA (exonéré)" else "TVA", breakdown.vatAmount.format())
                if (cash) TotalLine("Droit de timbre", breakdown.stampTax.format())
                TotalLine("Total TTC", breakdown.totalTTC.format(), strong = true)
            }
        }

        if (!canSave) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Renseignez le nom du client et au moins une ligne avec désignation.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row {
            GradientButton(if (saving) "Enregistrement…" else if (editing) "Mettre à jour" else "Enregistrer", Gradients.brand, leadingIcon = AppIcon.INVOICES, onClick = {
                if (!saving && canSave) {
                    saving = true
                    scope.launch {
                        val inv = SavedInvoice(
                            id = existing?.id ?: 0,
                            number = number,
                            docType = docType.title,
                            clientName = clientName,
                            clientAddress = clientAddress,
                            clientNif = clientNif.ifBlank { null },
                            clientRc = clientRc.ifBlank { null },
                            clientNis = clientNis.ifBlank { null },
                            clientArticle = clientArticle.ifBlank { null },
                            issueDate = issueDate,
                            cash = cash,
                            vatExempt = vatExempt,
                            applyRemise = applyRemise,
                            lines = lineItems,
                            status = existing?.status ?: DocumentStatus.DRAFT,
                            dueDate = dueDate,
                            paymentMethod = paymentMethod,
                            chequeNumber = if (paymentMethod == PaymentMode.CHEQUE) chequeNumber.ifBlank { null } else null,
                            chequeBank = if (paymentMethod == PaymentMode.CHEQUE) chequeBank.ifBlank { null } else null,
                        )
                        if (editing) repo.update(inv) else repo.save(inv)
                        onDone()
                    }
                }
            })
            Spacer(Modifier.width(12.dp))
            OutlineAction("Annuler", onClick = onDone)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LineEditor(draft: LineDraft, onChange: (LineDraft) -> Unit, onRemove: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(draft.designation, { onChange(draft.copy(designation = it)) }, label = { Text("Désignation") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(draft.qty, { onChange(draft.copy(qty = it)) }, label = { Text("Qté") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(draft.unit, { onChange(draft.copy(unit = it)) }, label = { Text("Unité") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(draft.price, { onChange(draft.copy(price = it)) }, label = { Text("P.U. HT") }, singleLine = true, modifier = Modifier.weight(1.3f))
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(draft.vat, { onChange(draft.copy(vat = it)) }, label = { Text("TVA %") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(draft.discount, { onChange(draft.copy(discount = it)) }, label = { Text("Remise %") }, singleLine = true, modifier = Modifier.weight(1f))
            Spacer(Modifier.weight(1.3f))
            OutlineAction("Retirer", onClick = onRemove)
        }
    }
}

// ── Detail / preview + export ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceDetailView(
    modifier: Modifier,
    invoice: SavedInvoice,
    repo: InvoiceRepository,
    paymentRepo: PaymentRepository,
    compute: ComputeTotals,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val exporter = LocalInvoiceExporter.current
    val companyRepo: CompanyRepository = koinInject()
    val scope = rememberCoroutineScope()
    // Export/print runs off the Compose dispatcher: a modal save dialog pumps Compose's
    // FlushCoroutineDispatcher re-entrantly and corrupts continuations (crashes in dark mode).
    val exportScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    DisposableEffect(Unit) { onDispose { exportScope.cancel() } }
    val today = remember { currentLocalDate() }
    var status by remember { mutableStateOf<String?>(null) }
    var company by remember { mutableStateOf(SgMediaCompany) }
    // Local mutable copy so status/dueDate edits reflect immediately without leaving the screen.
    var inv by remember { mutableStateOf(invoice) }
    var payments by remember { mutableStateOf<List<PaymentRecord>>(emptyList()) }
    LaunchedEffect(Unit) {
        company = companyRepo.load()
        payments = paymentRepo.forInvoice(invoice.id)
    }
    val effective = effectiveLines(invoice)
    val breakdown = compute.compute(effective, if (invoice.cash) PaymentMode.CASH else PaymentMode.BANK_TRANSFER, invoice.vatExempt)
    val ttc = breakdown.totalTTC
    val paidTotal = payments.fold(Money.ZERO) { a, p -> a + p.amount }
    val remaining = (ttc - paidTotal).let { if (it.amountMinor > 0) it else Money.ZERO }
    val effStatus = effectiveStatus(inv.status, ttc, paidTotal, inv.dueDate, today)

    fun runExport(action: suspend (InvoiceExportData) -> ExportResult) {
        status = "Génération en cours…"
        exportScope.launch {
            val logo = runCatching { Res.readBytes("drawable/sg_logo.png") }.getOrNull()
            val data = InvoiceExportData(
                company = company,
                docTitle = invoice.docType,
                number = invoice.number,
                dateText = invoice.issueDate.frShort(),
                clientName = invoice.clientName,
                clientAddress = invoice.clientAddress,
                clientNif = invoice.clientNif,
                clientRc = invoice.clientRc,
                clientNis = invoice.clientNis,
                clientArticle = invoice.clientArticle,
                lines = effective,
                breakdown = breakdown,
                cash = invoice.cash,
                vatExempt = invoice.vatExempt,
                paymentMethod = invoice.paymentMethod,
                chequeNumber = invoice.chequeNumber,
                chequeBank = invoice.chequeBank,
                logoPng = logo,
            )
            status = when (val r = action(data)) {
                is ExportResult.Saved -> "✓ Enregistré : ${r.path}"
                ExportResult.Cancelled -> "Annulé"
                is ExportResult.Failed -> "Erreur : ${r.message}"
                ExportResult.Unsupported -> "Export disponible sur Desktop pour le moment"
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(invoice.number, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                status?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            OutlineAction("Modifier", leadingIcon = AppIcon.ADD, onClick = onEdit)
            Spacer(Modifier.width(8.dp))
            GradientButton("Imprimer", Gradients.brand, leadingIcon = AppIcon.INVOICES, onClick = { runExport { exporter.print(it) } })
            Spacer(Modifier.width(8.dp))
            OutlineAction("PDF", onClick = { runExport { exporter.export(ExportFormat.PDF, it) } })
            Spacer(Modifier.width(8.dp))
            OutlineAction("XLSX", onClick = { runExport { exporter.export(ExportFormat.XLSX, it) } })
            Spacer(Modifier.width(8.dp))
            OutlineAction("DOCX", onClick = { runExport { exporter.export(ExportFormat.DOCX, it) } })
            Spacer(Modifier.width(8.dp))
            OutlineAction("Retour", onClick = onBack)
        }

        // ── Status & payments ──────────────────────────────────────────────
        val s = LocalStrings.current
        var statusMenu by remember { mutableStateOf(false) }
        var dueDialog by remember { mutableStateOf(false) }
        var showAddPayment by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader(s.payments)
                    Spacer(Modifier.width(12.dp))
                    StatusPill(effStatus)
                    Spacer(Modifier.weight(1f))
                    Box {
                        OutlineAction(s.statusLabel, onClick = { statusMenu = true })
                        DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                            listOf(DocumentStatus.DRAFT, DocumentStatus.SENT, DocumentStatus.PAID, DocumentStatus.CANCELLED).forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(statusLabel(st, s)) },
                                    onClick = {
                                        statusMenu = false
                                        scope.launch { repo.updateStatus(inv.id, st); inv = inv.copy(status = st) }
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlineAction(inv.dueDate?.let { "${s.dueDateLabel} : ${it.frShort()}" } ?: s.dueDateLabel, onClick = { dueDialog = true })
                }
                Spacer(Modifier.height(10.dp))
                TotalLine("TTC", ttc.format())
                TotalLine(s.paidLabel, paidTotal.format())
                TotalLine(s.remainingLabel, remaining.format(), strong = true)
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))
                if (payments.isEmpty()) {
                    Text(s.noPayments, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                } else {
                    payments.forEach { p ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(p.date.frShort(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp))
                            Text(paymentModeLabel(p.method), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(p.amount.format(), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(12.dp))
                            OutlineAction(s.remove, onClick = { scope.launch { paymentRepo.delete(p.id); payments = paymentRepo.forInvoice(inv.id) } })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                GradientButton(s.recordPayment, Gradients.brand, leadingIcon = AppIcon.ADD, onClick = { showAddPayment = true })
            }
        }
        Spacer(Modifier.height(12.dp))

        if (dueDialog) {
            val state = rememberDatePickerState(initialSelectedDateMillis = (inv.dueDate ?: today).toEpochDays().toLong() * 86_400_000L)
            DatePickerDialog(
                onDismissRequest = { dueDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let {
                            val d = LocalDate.fromEpochDays((it / 86_400_000L).toInt())
                            scope.launch { repo.updateDueDate(inv.id, d); inv = inv.copy(dueDate = d) }
                        }
                        dueDialog = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { dueDialog = false }) { Text(s.cancel) } },
            ) { DatePicker(state = state) }
        }

        if (showAddPayment) {
            AddPaymentDialog(
                s = s,
                defaultAmount = remaining,
                today = today,
                onDismiss = { showAddPayment = false },
                onConfirm = { amount, method, date ->
                    showAddPayment = false
                    scope.launch {
                        paymentRepo.add(PaymentRecord(invoiceId = inv.id, amount = amount, date = date, method = method))
                        payments = paymentRepo.forInvoice(inv.id)
                    }
                },
            )
        }

        val previewScroll = rememberScrollState()
        Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(
                modifier = Modifier.fillMaxSize().verticalScroll(previewScroll).padding(24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Surface(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(), shape = RoundedCornerShape(4.dp), color = androidx.compose.ui.graphics.Color.White, tonalElevation = 2.dp) {
                    InvoiceDocument(
                        company = company,
                        docTitle = invoice.docType,
                        number = invoice.number,
                        dateText = invoice.issueDate.frShort(),
                        clientName = invoice.clientName,
                        clientAddress = invoice.clientAddress,
                        clientNif = invoice.clientNif,
                        clientRc = invoice.clientRc,
                        clientNis = invoice.clientNis,
                        clientArticle = invoice.clientArticle,
                        lines = effective,
                        breakdown = breakdown,
                        cash = invoice.cash,
                        vatExempt = invoice.vatExempt,
                        paymentMethodText = invoice.paymentMethod.label(invoice.chequeNumber, invoice.chequeBank),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            VScrollbar(
                scrollState = previewScroll,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 6.dp, horizontal = 3.dp),
                thumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun paymentModeLabel(mode: PaymentMode): String = when (mode) {
    PaymentMode.CASH -> "Espèces"
    PaymentMode.BANK_TRANSFER -> "Virement"
    PaymentMode.CHEQUE -> "Chèque"
    PaymentMode.CARD -> "Carte"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentDialog(
    s: AppStrings,
    defaultAmount: Money,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (Money, PaymentMode, LocalDate) -> Unit,
) {
    var amountText by remember { mutableStateOf(defaultAmount.format(withSymbol = false)) }
    var method by remember { mutableStateOf(PaymentMode.BANK_TRANSFER) }
    var date by remember { mutableStateOf(today) }
    var methodMenu by remember { mutableStateOf(false) }
    var dateDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.recordPayment) },
        text = {
            Column {
                OutlinedTextField(
                    amountText, { amountText = it },
                    label = { Text(s.amountLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${s.methodLabel} :", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    Box {
                        OutlineAction(paymentModeLabel(method), onClick = { methodMenu = true })
                        DropdownMenu(expanded = methodMenu, onDismissRequest = { methodMenu = false }) {
                            PaymentMode.entries.forEach { m ->
                                DropdownMenuItem(text = { Text(paymentModeLabel(m)) }, onClick = { method = m; methodMenu = false })
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    OutlineAction(date.frShort(), onClick = { dateDialog = true })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(Money.parse(amountText) ?: Money.ZERO, method, date) }) { Text(s.add) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )

    if (dateDialog) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDays().toLong() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { dateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = LocalDate.fromEpochDays((it / 86_400_000L).toInt()) }
                    dateDialog = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { dateDialog = false }) { Text(s.cancel) } },
        ) { DatePicker(state = state) }
    }
}

// ── small shared bits ────────────────────────────────────────────────────────

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TotalLine(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium)
    }
}
