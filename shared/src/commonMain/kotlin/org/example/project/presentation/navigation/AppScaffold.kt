package org.example.project.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.presentation.catalog.CatalogScreen
import org.example.project.presentation.clients.ClientsScreen
import org.example.project.presentation.components.AppIcon
import org.example.project.presentation.components.AppIconView
import org.example.project.presentation.components.BrandMark
import org.example.project.presentation.components.GradientButton
import org.example.project.presentation.dashboard.DashboardScreen
import org.example.project.presentation.i18n.AppStrings
import org.example.project.presentation.i18n.Language
import org.example.project.presentation.i18n.LocalStrings
import org.example.project.presentation.invoices.InvoicesScreen
import org.example.project.presentation.settings.SettingsScreen
import org.example.project.presentation.theme.Gradients

enum class Destination(val icon: AppIcon) {
    DASHBOARD(AppIcon.DASHBOARD),
    CLIENTS(AppIcon.CLIENTS),
    INVOICES(AppIcon.INVOICES),
    CATALOG(AppIcon.CATALOG),
    SETTINGS(AppIcon.SETTINGS),
}

private fun Destination.label(s: AppStrings): String = when (this) {
    Destination.DASHBOARD -> s.navDashboard
    Destination.CLIENTS -> s.navClients
    Destination.INVOICES -> s.navInvoices
    Destination.CATALOG -> s.navCatalog
    Destination.SETTINGS -> s.navSettings
}

@Composable
fun AppScaffold(
    darkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    language: Language = Language.FR,
    onSelectLanguage: (Language) -> Unit = {},
) {
    var current by remember { mutableStateOf(Destination.DASHBOARD) }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                Sidebar(current = current, onSelect = { current = it })
                Box(Modifier.fillMaxSize()) { DestinationContent(current, { current = it }, darkTheme, onToggleTheme, language, onSelectLanguage, Modifier.fillMaxSize()) }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { DestinationContent(current, { current = it }, darkTheme, onToggleTheme, language, onSelectLanguage, Modifier.fillMaxSize()) }
                BottomBar(current = current, onSelect = { current = it })
            }
        }
    }
}

@Composable
private fun Sidebar(current: Destination, onSelect: (Destination) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxHeight().width(248.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxHeight().padding(20.dp)) {
            BrandMark(modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 24.dp))
            Destination.entries.forEach { dest ->
                SidebarItem(dest = dest, selected = current == dest, onClick = { onSelect(dest) })
                Spacer(Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.weight(1f))
            GradientButton(
                text = LocalStrings.current.newInvoice,
                brush = Gradients.brand,
                leadingIcon = AppIcon.ADD,
                onClick = { onSelect(Destination.INVOICES) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SidebarItem(dest: Destination, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent
    val tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconView(dest.icon, tint = tint)
        Spacer(Modifier.width(14.dp))
        Text(
            dest.label(LocalStrings.current),
            color = tint,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun BottomBar(current: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        Destination.entries.forEach { dest ->
            NavigationBarItem(
                selected = current == dest,
                onClick = { onSelect(dest) },
                icon = { AppIconView(dest.icon) },
                label = { Text(dest.label(LocalStrings.current), maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun DestinationContent(
    dest: Destination,
    onSelect: (Destination) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    language: Language,
    onSelectLanguage: (Language) -> Unit,
    modifier: Modifier,
) {
    when (dest) {
        Destination.DASHBOARD -> DashboardScreen(
            modifier = modifier,
            onNewInvoice = { onSelect(Destination.INVOICES) },
            onNewClient = { onSelect(Destination.CLIENTS) },
            darkTheme = darkTheme,
            onToggleTheme = onToggleTheme,
            language = language,
            onSelectLanguage = onSelectLanguage,
        )
        Destination.CLIENTS -> ClientsScreen(modifier)
        Destination.INVOICES -> InvoicesScreen(modifier)
        Destination.CATALOG -> CatalogScreen(modifier)
        Destination.SETTINGS -> SettingsScreen(modifier)
    }
}
