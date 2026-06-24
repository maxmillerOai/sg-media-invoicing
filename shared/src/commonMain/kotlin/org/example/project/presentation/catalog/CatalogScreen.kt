package org.example.project.presentation.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.core.Money
import org.example.project.data.CatalogRepository
import org.example.project.domain.model.CatalogItem
import org.example.project.presentation.components.AppIcon
import org.example.project.presentation.components.GradientButton
import org.example.project.presentation.components.OutlineAction
import org.example.project.presentation.components.ScrollableColumn
import org.example.project.presentation.theme.AgencyPalette
import org.example.project.presentation.theme.Gradients
import org.koin.compose.koinInject

@Composable
fun CatalogScreen(modifier: Modifier = Modifier) {
    val repo: CatalogRepository = koinInject()
    var editing by remember { mutableStateOf<CatalogItem?>(null) }
    var creating by remember { mutableStateOf(false) }

    if (creating || editing != null) {
        CatalogEditor(repo, editing, onDone = { creating = false; editing = null }, modifier = modifier)
    } else {
        CatalogList(repo, onNew = { creating = true }, onEdit = { editing = it }, modifier = modifier)
    }
}

@Composable
private fun CatalogList(repo: CatalogRepository, onNew: () -> Unit, onEdit: (CatalogItem) -> Unit, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<CatalogItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    suspend fun reload() { items = repo.list(); loaded = true }
    LaunchedEffect(Unit) { reload() }

    Column(modifier.fillMaxSize().padding(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Catalogue", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            GradientButton("Nouvel article", Gradients.brand, leadingIcon = AppIcon.ADD, onClick = onNew)
        }
        Spacer(Modifier.height(20.dp))
        if (loaded && items.isEmpty()) {
            Text("Aucun article. Ajoutez vos services/produits réutilisables.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val palette = listOf(AgencyPalette.Violet, AgencyPalette.Cyan, AgencyPalette.Amber, AgencyPalette.Mint, AgencyPalette.Coral)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            items.forEachIndexed { idx, it0 ->
                val color = palette[idx % palette.size]
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    // Two rows: coloured tag + name + price on top, actions below (keeps the name from being squeezed).
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).background(color, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(it0.name.trim().take(1).uppercase().ifBlank { "•" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(it0.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                it0.description?.takeIf { d -> d.isNotBlank() }?.let { d ->
                                    Text(d, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(it0.defaultPriceHT.format(), fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.weight(1f))
                            OutlineAction("Modifier", onClick = { onEdit(it0) })
                            Spacer(Modifier.width(8.dp))
                            OutlineAction("Suppr.", onClick = { scope.launch { repo.delete(it0.id); reload() } })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogEditor(repo: CatalogRepository, existing: CatalogItem?, onDone: () -> Unit, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "u") }
    var price by remember { mutableStateOf(existing?.defaultPriceHT?.format(withSymbol = false) ?: "") }
    var vat by remember { mutableStateOf((existing?.defaultVatPct ?: 19.0).toString()) }
    var description by remember { mutableStateOf(existing?.description ?: "") }

    ScrollableColumn(modifier, contentPadding = PaddingValues(28.dp)) {
        Text(if (existing == null) "Nouvel article" else "Modifier l'article", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Fld("Désignation", name, Modifier.fillMaxWidth()) { name = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Fld("Unité", unit, Modifier.weight(1f)) { unit = it }
                    Fld("Prix HT", price, Modifier.weight(1.4f)) { price = it }
                    Fld("TVA %", vat, Modifier.weight(1f)) { vat = it }
                }
                Fld("Description", description, Modifier.fillMaxWidth()) { description = it }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row {
            GradientButton("Enregistrer", Gradients.brand, leadingIcon = AppIcon.CATALOG, onClick = {
                if (name.isNotBlank()) {
                    scope.launch {
                        repo.save(
                            CatalogItem(
                                id = existing?.id ?: 0,
                                name = name,
                                unit = unit.ifBlank { "u" },
                                defaultPriceHT = Money.parse(price) ?: Money.ZERO,
                                defaultVatPct = vat.replace(',', '.').toDoubleOrNull() ?: 19.0,
                                description = description.ifBlank { null },
                            ),
                        )
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
private fun Fld(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, modifier = modifier.padding(vertical = 4.dp))
}
