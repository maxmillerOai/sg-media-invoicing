package org.example.project.presentation.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.data.ClientRepository
import org.example.project.domain.model.Client
import org.example.project.presentation.components.AppIcon
import org.example.project.presentation.components.GradientButton
import org.example.project.presentation.components.OutlineAction
import org.example.project.presentation.components.ScrollableColumn
import org.example.project.presentation.components.SectionHeader
import org.example.project.presentation.theme.Gradients
import org.koin.compose.koinInject

@Composable
fun ClientsScreen(modifier: Modifier = Modifier) {
    val repo: ClientRepository = koinInject()
    var editing by remember { mutableStateOf<Client?>(null) }
    var creating by remember { mutableStateOf(false) }

    if (creating || editing != null) {
        ClientEditor(repo, editing, onDone = { creating = false; editing = null }, modifier = modifier)
    } else {
        ClientList(repo, onNew = { creating = true }, onEdit = { editing = it }, modifier = modifier)
    }
}

@Composable
private fun ClientList(repo: ClientRepository, onNew: () -> Unit, onEdit: (Client) -> Unit, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    suspend fun reload() { clients = repo.list(); loaded = true }
    LaunchedEffect(Unit) { reload() }

    Column(modifier.fillMaxSize().padding(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Clients", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            GradientButton("Nouveau client", Gradients.brand, leadingIcon = AppIcon.ADD, onClick = onNew)
        }
        Spacer(Modifier.height(20.dp))
        if (loaded && clients.isEmpty()) {
            Text("Aucun client. Ajoutez-en un avec « Nouveau client ».", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            clients.forEach { c ->
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.displayName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            val sub = listOfNotNull(c.city.ifBlank { null }, c.nif?.let { "NIF $it" }).joinToString("  •  ")
                            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlineAction("Modifier", onClick = { onEdit(c) })
                        Spacer(Modifier.width(8.dp))
                        OutlineAction("Suppr.", onClick = { scope.launch { repo.delete(c.id); reload() } })
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientEditor(repo: ClientRepository, existing: Client?, onDone: () -> Unit, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf(existing?.displayName ?: "") }
    var isPersonal by remember { mutableStateOf(existing?.isPersonal ?: false) }
    var legalForm by remember { mutableStateOf(existing?.legalForm ?: "") }
    var rc by remember { mutableStateOf(existing?.rc ?: "") }
    var nif by remember { mutableStateOf(existing?.nif ?: "") }
    var nis by remember { mutableStateOf(existing?.nis ?: "") }
    var article by remember { mutableStateOf(existing?.articleImposition ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var city by remember { mutableStateOf(existing?.city ?: "") }
    var wilaya by remember { mutableStateOf(existing?.wilaya ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var iban by remember { mutableStateOf(existing?.iban ?: "") }
    var bankName by remember { mutableStateOf(existing?.bankName ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    val nifValid = nif.isBlank() || nif.filter { it.isDigit() }.length == 15

    ScrollableColumn(modifier, contentPadding = PaddingValues(28.dp)) {
        Text(if (existing == null) "Nouveau client" else "Modifier le client", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Field("Nom / Raison sociale", displayName) { displayName = it }
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Particulier (mode personnel)", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    Switch(isPersonal, { isPersonal = it })
                }
                if (!isPersonal) {
                    Field("Forme juridique (SARL, EURL…)", legalForm) { legalForm = it }
                    Field("RC", rc) { rc = it }
                    Field("NIF (15 chiffres)", nif, isError = !nifValid, supporting = if (!nifValid) "Le NIF doit comporter 15 chiffres" else null) { nif = it }
                    Field("NIS", nis) { nis = it }
                    Field("Article d'imposition", article) { article = it }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                SectionHeader("Coordonnées")
                Spacer(Modifier.height(8.dp))
                Field("Adresse", address) { address = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Ville", city, modifier = Modifier.weight(1f)) { city = it }
                    Field("Wilaya", wilaya, modifier = Modifier.weight(1f)) { wilaya = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Téléphone", phone, modifier = Modifier.weight(1f)) { phone = it }
                    Field("Email", email, modifier = Modifier.weight(1f)) { email = it }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                SectionHeader("Banque & notes")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Banque", bankName, modifier = Modifier.weight(1f)) { bankName = it }
                    Field("RIB / IBAN", iban, modifier = Modifier.weight(1f)) { iban = it }
                }
                Field("Notes", notes) { notes = it }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row {
            GradientButton("Enregistrer", Gradients.brand, leadingIcon = AppIcon.CLIENTS, onClick = {
                if (displayName.isNotBlank() && nifValid) {
                    scope.launch {
                        repo.save(
                            Client(
                                id = existing?.id ?: 0,
                                displayName = displayName,
                                legalForm = legalForm.ifBlank { null },
                                rc = if (isPersonal) null else rc.ifBlank { null },
                                nif = if (isPersonal) null else nif.ifBlank { null },
                                nis = if (isPersonal) null else nis.ifBlank { null },
                                articleImposition = if (isPersonal) null else article.ifBlank { null },
                                address = address,
                                city = city,
                                wilaya = wilaya.ifBlank { null },
                                phone = phone.ifBlank { null },
                                email = email.ifBlank { null },
                                iban = iban.ifBlank { null },
                                bankName = bankName.ifBlank { null },
                                isPersonal = isPersonal,
                                notes = notes.ifBlank { null },
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
private fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supporting: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supporting?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}
