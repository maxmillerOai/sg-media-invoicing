package org.example.project.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import org.example.project.core.Money
import org.example.project.core.currentDateTime
import org.example.project.core.currentLocalDate
import org.example.project.core.frShort
import org.example.project.data.CatalogRepository
import org.example.project.data.ClientRepository
import org.example.project.data.InvoiceRepository
import org.example.project.data.PaymentRepository
import org.example.project.data.SavedInvoice
import org.example.project.domain.model.DocumentStatus
import org.example.project.domain.model.PaymentMode
import org.example.project.domain.usecase.ComputeTotals
import org.example.project.domain.usecase.effectiveStatus
import org.example.project.domain.usecase.outstanding
import org.example.project.presentation.i18n.AppStrings
import org.example.project.presentation.i18n.Language
import org.example.project.presentation.i18n.LocalStrings
import org.example.project.presentation.components.AppIcon
import org.example.project.presentation.components.AppIconView
import org.example.project.presentation.components.GradientButton
import org.example.project.presentation.components.AnalogClock
import org.example.project.presentation.components.RevenueTrendChart
import org.example.project.presentation.components.ScrollableColumn
import org.example.project.presentation.components.SectionHeader
import org.example.project.presentation.components.StatusChip
import org.example.project.presentation.components.StatusPill
import org.example.project.presentation.components.statusColor
import org.example.project.presentation.components.TrendPoint
import org.example.project.presentation.theme.AgencyPalette
import org.example.project.presentation.theme.Gradients
import org.koin.compose.koinInject

private fun effective(inv: SavedInvoice) =
    if (inv.applyRemise) inv.lines else inv.lines.map { it.copy(discountPct = 0.0) }

private fun SavedInvoice.isInvoice() = number.startsWith("FAC")

private val frMonths =
    listOf("jan", "fév", "mar", "avr", "mai", "juin", "juil", "aoû", "sep", "oct", "nov", "déc")

/** Number prefix of each document type, in the order shown in the breakdown panel. */
private val typePrefixes = listOf("FAC", "PRO", "BC", "OFF")

private fun typeLabel(prefix: String, s: AppStrings): String = when (prefix) {
    "FAC" -> s.typeInvoices
    "PRO" -> s.typeProformas
    "BC" -> s.typePurchaseOrders
    else -> s.typeOffers
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNewInvoice: () -> Unit = {},
    onNewClient: () -> Unit = {},
    darkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    language: Language = Language.FR,
    onSelectLanguage: (Language) -> Unit = {},
) {
    val s = LocalStrings.current
    val invoiceRepo: InvoiceRepository = koinInject()
    val clientRepo: ClientRepository = koinInject()
    val catalogRepo: CatalogRepository = koinInject()
    val paymentRepo: PaymentRepository = koinInject()
    val compute: ComputeTotals = koinInject()

    var invoices by remember { mutableStateOf<List<SavedInvoice>>(emptyList()) }
    var paidMap by remember { mutableStateOf<Map<Long, Money>>(emptyMap()) }
    var clientCount by remember { mutableStateOf(0) }
    var catalogCount by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(currentDateTime()) }

    // Tick the clock once per second.
    LaunchedEffect(Unit) {
        while (true) {
            now = currentDateTime()
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        invoices = invoiceRepo.list()
        paidMap = paymentRepo.paidByInvoice()
        clientCount = clientRepo.list().size
        catalogCount = catalogRepo.list().size
    }

    // Per-document TTC, computed once and reused across every metric.
    val ttcOf: (SavedInvoice) -> Money = { inv ->
        compute.compute(
            effective(inv),
            if (inv.cash) PaymentMode.CASH else PaymentMode.BANK_TRANSFER,
            inv.vatExempt,
        ).totalTTC
    }
    val ttcMap = remember(invoices) { invoices.associate { it.id to ttcOf(it) } }
    fun ttc(inv: SavedInvoice) = ttcMap[inv.id] ?: Money.ZERO
    fun paid(inv: SavedInvoice) = paidMap[inv.id] ?: Money.ZERO

    val today = currentLocalDate()
    fun effStatus(inv: SavedInvoice): DocumentStatus =
        effectiveStatus(inv.status, ttc(inv), paid(inv), inv.dueDate, today)

    val revenue = invoices.filter { it.isInvoice() }.fold(Money.ZERO) { a, i -> a + ttc(i) }
    // Outstanding = unpaid balance on real invoices (excludes drafts, proformas, cancelled).
    val outstandingTotal = invoices.filter { it.isInvoice() }
        .fold(Money.ZERO) { a, i -> a + outstanding(i.status, ttc(i), paid(i)) }
    val outstandingCount = invoices.count { it.isInvoice() && outstanding(it.status, ttc(it), paid(it)).amountMinor > 0 }
    val overdueInvoices = invoices.filter { it.isInvoice() && effStatus(it) == DocumentStatus.OVERDUE }
    val overdueTotal = overdueInvoices.fold(Money.ZERO) { a, i -> a + outstanding(i.status, ttc(i), paid(i)) }
    val invoiceCount = invoices.count { it.isInvoice() }
    val quoteCount = invoices.size - invoiceCount
    val activeClients = invoices.map { it.clientName.trim().lowercase() }.filter { it.isNotEmpty() }.distinct().size

    // Unpaid invoices with a due date, soonest first (overdue ones surface at the top).
    val deadlines = invoices
        .filter { it.isInvoice() && it.dueDate != null && outstanding(it.status, ttc(it), paid(it)).amountMinor > 0 }
        .sortedBy { it.dueDate }
        .take(6)

    val baseAbs = today.year * 12 + (today.monthNumber - 1)
    val trend = (5 downTo 0).map { back ->
        val idx = baseAbs - back
        val y = idx / 12
        val m = idx % 12 // 0-based
        val sum = invoices.filter { it.isInvoice() && it.issueDate.year == y && it.issueDate.monthNumber == m + 1 }
            .fold(Money.ZERO) { a, i -> a + ttc(i) }
        TrendPoint(frMonths[m], sum)
    }
    val monthRevenue = trend.lastOrNull()?.value ?: Money.ZERO
    val docsThisMonth = invoices.count { it.issueDate.year == today.year && it.issueDate.monthNumber == today.monthNumber }

    ScrollableColumn(modifier = modifier, contentPadding = PaddingValues(28.dp)) {
        Box(Modifier.widthIn(max = 1100.dp).fillMaxWidth()) {
            Column {
                TopBar(
                    s = s,
                    query = query,
                    onQuery = { query = it },
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme,
                    language = language,
                    onSelectLanguage = onSelectLanguage,
                    notifications = docsThisMonth,
                    onNewInvoice = onNewInvoice,
                )
                Spacer(Modifier.height(20.dp))

                BoxWithConstraints {
                    if (maxWidth >= 720.dp) {
                        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ClockHero(s, now, Modifier.weight(1f).fillMaxHeight())
                            WeatherPanel(Modifier.width(272.dp).fillMaxHeight())
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ClockHero(s, now)
                            WeatherPanel(Modifier.fillMaxWidth().height(120.dp))
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                // KPI cards
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(s.kpiRevenue, revenue.format(), s.capInvoiceCount(invoiceCount), Gradients.brand, Modifier.weight(1f))
                    KpiCard(s.kpiPending, outstandingTotal.format(), s.capOutstanding(outstandingCount), Gradients.ocean, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(s.kpiOverdue, overdueTotal.format(), s.capOverdue(overdueInvoices.size), Gradients.sunset, Modifier.weight(1f))
                    KpiCard(s.kpiActiveClients, activeClients.toString(), s.capInPortfolio(clientCount), Gradients.mint, Modifier.weight(1f))
                }

                Spacer(Modifier.height(28.dp))

                // Revenue trend
                PanelCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            SectionHeader(s.revenueTrendTitle)
                            Text(s.revenueTrendSubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(monthRevenue.format(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(18.dp))
                    RevenueTrendChart(points = trend, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(20.dp))

                // Two-column lower section: recent activity + type breakdown.
                BoxWithConstraints {
                    val twoCol = maxWidth >= 760.dp
                    val filtered = invoices.filter {
                        query.isBlank() ||
                            it.clientName.contains(query, true) ||
                            it.number.contains(query, true)
                    }
                    if (twoCol) {
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Box(Modifier.weight(1.5f)) { RecentActivityPanel(s, filtered, ::ttc, ::effStatus) }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                UpcomingDeadlinesPanel(s, deadlines, today, ::ttc, ::paid)
                                TypeBreakdownPanel(s, invoices)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            RecentActivityPanel(s, filtered, ::ttc, ::effStatus)
                            UpcomingDeadlinesPanel(s, deadlines, today, ::ttc, ::paid)
                            TypeBreakdownPanel(s, invoices)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TopBar(
    s: AppStrings,
    query: String,
    onQuery: (String) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    language: Language,
    onSelectLanguage: (Language) -> Unit,
    notifications: Int,
    onNewInvoice: () -> Unit,
) {
    BoxWithConstraints {
        val showSearch = maxWidth >= 820.dp
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.greetingTitle, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(s.greetingSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showSearch) {
                SearchField(s.searchPlaceholder, query, onQuery, Modifier.width(240.dp))
                Spacer(Modifier.width(12.dp))
            }
            CircleButton(if (darkTheme) AppIcon.SUN else AppIcon.MOON, onClick = onToggleTheme)
            Spacer(Modifier.width(10.dp))
            NotificationButton(notifications)
            Spacer(Modifier.width(10.dp))
            LangSelector(language, onSelectLanguage)
            Spacer(Modifier.width(12.dp))
            GradientButton(s.newShort, Gradients.brand, leadingIcon = AppIcon.ADD, onClick = onNewInvoice)
            Spacer(Modifier.width(12.dp))
            Avatar("SG")
        }
    }
}

@Composable
private fun SearchField(placeholder: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconView(AppIcon.SEARCH, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CircleButton(icon: AppIcon, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIconView(icon, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun NotificationButton(count: Int) {
    Box {
        CircleButton(AppIcon.BELL, onClick = {})
        if (count > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(AgencyPalette.Coral, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (count > 9) "9+" else count.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LangSelector(language: Language, onSelect: (Language) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .height(44.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp))
                .clickable { open = true }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(language.code, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Language.entries.forEach { lang ->
                DropdownMenuItem(
                    text = { Text("${lang.code} · ${lang.nativeName}") },
                    onClick = { onSelect(lang); open = false },
                )
            }
        }
    }
}

@Composable
private fun Avatar(initials: String) {
    Box(
        Modifier.size(44.dp).background(Gradients.brand, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun ClockHero(s: AppStrings, now: LocalDateTime, modifier: Modifier = Modifier) {
    val hh = now.hour.toString().padStart(2, '0')
    val mm = now.minute.toString().padStart(2, '0')
    val ss = now.second.toString().padStart(2, '0')
    val weekday = s.weekdays[(now.dayOfWeek.isoDayNumber - 1).coerceIn(0, 6)]
    val month = s.months[(now.monthNumber - 1).coerceIn(0, 11)]

    Row(
        modifier = modifier.fillMaxWidth()
            .background(Gradients.ink, RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // analog clock
        AnalogClock(now, Modifier.size(64.dp), accent = AgencyPalette.Cyan)
        Spacer(Modifier.width(18.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$hh:$mm", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 46.sp, letterSpacing = (-1).sp)
            Spacer(Modifier.width(6.dp))
            Text(":$ss", color = AgencyPalette.Cyan, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(weekday, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.height(2.dp))
            Text("${now.dayOfMonth} $month ${now.year}", color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, caption: String, brush: Brush, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.heightIn(min = 132.dp).background(brush, RoundedCornerShape(20.dp)).padding(20.dp),
    ) {
        Text(label.uppercase(), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(caption, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PanelCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun RecentActivityPanel(
    s: AppStrings,
    invoices: List<SavedInvoice>,
    ttc: (SavedInvoice) -> Money,
    statusOf: (SavedInvoice) -> DocumentStatus,
) {
    PanelCard {
        SectionHeader(s.recentActivity)
        Spacer(Modifier.height(8.dp))
        if (invoices.isEmpty()) {
            Text(s.noDocuments, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val recent = invoices.take(7)
            recent.forEachIndexed { i, inv ->
                ActivityRow(inv, ttc(inv), statusOf(inv))
                if (i < recent.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ActivityRow(inv: SavedInvoice, ttc: Money, status: DocumentStatus) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(inv.clientName, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text("${inv.number}  •  ${inv.issueDate.frShort()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Text(ttc.format(), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(14.dp))
        StatusPill(status)
    }
}

@Composable
private fun UpcomingDeadlinesPanel(
    s: AppStrings,
    deadlines: List<SavedInvoice>,
    today: LocalDate,
    ttc: (SavedInvoice) -> Money,
    paid: (SavedInvoice) -> Money,
) {
    PanelCard {
        SectionHeader(s.upcomingDeadlines)
        Spacer(Modifier.height(8.dp))
        if (deadlines.isEmpty()) {
            Text(s.noDeadlines, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            deadlines.forEachIndexed { i, inv ->
                val due = inv.dueDate ?: return@forEachIndexed
                val days = today.daysUntil(due)
                val remaining = outstanding(inv.status, ttc(inv), paid(inv))
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(inv.clientName, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text("${inv.number}  •  ${due.frShort()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(remaining.format(), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(10.dp))
                    if (days < 0) {
                        StatusChip(s.statusOverdue, statusColor(DocumentStatus.OVERDUE))
                    } else {
                        StatusChip(s.capDaysLeft(days), AgencyPalette.Muted)
                    }
                }
                if (i < deadlines.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun TypeBreakdownPanel(s: AppStrings, invoices: List<SavedInvoice>) {
    PanelCard {
        SectionHeader(s.typeBreakdown)
        Spacer(Modifier.height(12.dp))
        val total = invoices.size.coerceAtLeast(1)
        val colors = listOf(AgencyPalette.Violet, AgencyPalette.Cyan, AgencyPalette.Amber, AgencyPalette.Mint)
        typePrefixes.forEachIndexed { i, prefix ->
            val count = invoices.count { it.number.startsWith(prefix) }
            BreakdownRow(typeLabel(prefix, s), count, count.toFloat() / total, colors[i])
            if (i < typePrefixes.lastIndex) Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun BreakdownRow(label: String, count: Int, fraction: Float, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
            Spacer(Modifier.width(8.dp))
            Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().height(7.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)),
        ) {
            Box(
                Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(7.dp)
                    .background(color, RoundedCornerShape(50)),
            )
        }
    }
}
