package org.example.project.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.example.project.core.saltedHash
import org.example.project.data.BackupInfo
import org.example.project.data.CompanyRepository
import org.example.project.data.LocalBackupManager
import org.example.project.data.SettingsRepository
import org.example.project.domain.model.CompanyProfile
import org.example.project.presentation.components.AppIcon
import org.example.project.presentation.components.GradientButton
import org.example.project.presentation.components.OutlineAction
import org.example.project.presentation.components.ScrollableColumn
import org.example.project.presentation.components.SectionHeader
import org.example.project.presentation.theme.Gradients
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val companyRepo: CompanyRepository = koinInject()
    val settings: SettingsRepository = koinInject()
    val scope = rememberCoroutineScope()

    ScrollableColumn(modifier = modifier, contentPadding = PaddingValues(28.dp)) {
        Text("Réglages", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(20.dp))
        CompanyProfileCard(companyRepo, scope)
        Spacer(Modifier.height(20.dp))
        BackupCard()
        Spacer(Modifier.height(20.dp))
        SecurityCard(settings, scope)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CompanyProfileCard(repo: CompanyRepository, scope: kotlinx.coroutines.CoroutineScope) {
    var name by remember { mutableStateOf("") }
    var form by remember { mutableStateOf("") }
    var rc by remember { mutableStateOf("") }
    var nif by remember { mutableStateOf("") }
    var nis by remember { mutableStateOf("") }
    var article by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var wilaya by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var footer by remember { mutableStateOf("") }
    var vat by remember { mutableStateOf("19") }
    var vatExempt by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val c = repo.load()
        name = c.name; form = c.legalForm ?: ""; rc = c.rc ?: ""; nif = c.nif ?: ""; nis = c.nis ?: ""
        article = c.articleImposition ?: ""; address = c.address; city = c.city; wilaya = c.wilaya ?: ""
        phone = c.phone ?: ""; email = c.email ?: ""; iban = c.iban ?: ""; bank = c.bankName ?: ""
        footer = c.footerNote ?: ""; vat = c.defaultVatPct.toString(); vatExempt = c.vatExempt
    }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader("Profil entreprise")
            Spacer(Modifier.height(8.dp))
            Field("Raison sociale", name) { name = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Forme juridique", form, Modifier.weight(1f)) { form = it }
                Field("RC", rc, Modifier.weight(1f)) { rc = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("NIF", nif, Modifier.weight(1f)) { nif = it }
                Field("NIS", nis, Modifier.weight(1f)) { nis = it }
                Field("Article d'imposition", article, Modifier.weight(1f)) { article = it }
            }
            Field("Adresse", address) { address = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Ville", city, Modifier.weight(1f)) { city = it }
                Field("Wilaya", wilaya, Modifier.weight(1f)) { wilaya = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Téléphone", phone, Modifier.weight(1f)) { phone = it }
                Field("Email", email, Modifier.weight(1f)) { email = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Banque", bank, Modifier.weight(1f)) { bank = it }
                Field("RIB / IBAN", iban, Modifier.weight(1f)) { iban = it }
            }
            Field("Mention de pied de page", footer) { footer = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field("TVA par défaut %", vat, Modifier.weight(1f)) { vat = it }
                Spacer(Modifier.weight(1f))
                Text("Exonéré de TVA", color = MaterialTheme.colorScheme.onSurface)
                Switch(vatExempt, { vatExempt = it })
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientButton("Enregistrer le profil", Gradients.brand, leadingIcon = AppIcon.SETTINGS, onClick = {
                    if (name.isNotBlank()) {
                        scope.launch {
                            repo.save(
                                CompanyProfile(
                                    name = name, legalForm = form.ifBlank { null }, rc = rc.ifBlank { null },
                                    nif = nif.ifBlank { null }, nis = nis.ifBlank { null }, articleImposition = article.ifBlank { null },
                                    address = address, city = city, wilaya = wilaya.ifBlank { null },
                                    phone = phone.ifBlank { null }, email = email.ifBlank { null },
                                    iban = iban.ifBlank { null }, bankName = bank.ifBlank { null }, footerNote = footer.ifBlank { null },
                                    defaultVatPct = vat.replace(',', '.').toDoubleOrNull() ?: 19.0, vatExempt = vatExempt,
                                ),
                            )
                            status = "✓ Profil enregistré"
                        }
                    } else status = "La raison sociale est requise."
                })
                status?.let { Spacer(Modifier.padding(start = 12.dp)); Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun SecurityCard(settings: SettingsRepository, scope: kotlinx.coroutines.CoroutineScope) {
    var salt by remember { mutableStateOf("") }
    var hash by remember { mutableStateOf<String?>(null) }
    var master by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        salt = settings.get(SettingsRepository.KEY_AUTH_SALT) ?: ""
        hash = settings.get(SettingsRepository.KEY_AUTH_HASH)
        master = settings.get(SettingsRepository.KEY_AUTH_MASTER)
    }
    fun verify(current: String) = hash != null && (saltedHash(salt, current) == hash || saltedHash(salt, current) == master)

    var curPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confPwd by remember { mutableStateOf("") }
    var pwdStatus by remember { mutableStateOf<String?>(null) }

    var curForMaster by remember { mutableStateOf("") }
    var newMaster by remember { mutableStateOf("") }
    var confMaster by remember { mutableStateOf("") }
    var masterStatus by remember { mutableStateOf<String?>(null) }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader("Sécurité — mot de passe")
            Spacer(Modifier.height(8.dp))
            PwdField("Mot de passe actuel (ou maître)", curPwd) { curPwd = it; pwdStatus = null }
            PwdField("Nouveau mot de passe", newPwd) { newPwd = it; pwdStatus = null }
            PwdField("Confirmer", confPwd) { confPwd = it; pwdStatus = null }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientButton("Changer le mot de passe", Gradients.brand, onClick = {
                    pwdStatus = when {
                        !verify(curPwd) -> "Mot de passe actuel incorrect."
                        newPwd.length < 4 -> "Nouveau mot de passe trop court (min. 4)."
                        newPwd != confPwd -> "La confirmation ne correspond pas."
                        else -> { scope.launch { settings.set(SettingsRepository.KEY_AUTH_HASH, saltedHash(salt, newPwd)); hash = saltedHash(salt, newPwd) }; "✓ Mot de passe modifié" }
                    }
                })
                pwdStatus?.let { Spacer(Modifier.padding(start = 12.dp)); Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Sécurité — mot de passe maître")
            Spacer(Modifier.height(8.dp))
            PwdField("Mot de passe actuel (ou maître)", curForMaster) { curForMaster = it; masterStatus = null }
            PwdField("Nouveau mot de passe maître", newMaster) { newMaster = it; masterStatus = null }
            PwdField("Confirmer", confMaster) { confMaster = it; masterStatus = null }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientButton("Changer le mot de passe maître", Gradients.brand, onClick = {
                    masterStatus = when {
                        !verify(curForMaster) -> "Mot de passe actuel incorrect."
                        newMaster.length < 4 -> "Nouveau mot de passe maître trop court (min. 4)."
                        newMaster != confMaster -> "La confirmation ne correspond pas."
                        else -> { scope.launch { settings.set(SettingsRepository.KEY_AUTH_MASTER, saltedHash(salt, newMaster)); master = saltedHash(salt, newMaster) }; "✓ Mot de passe maître modifié" }
                    }
                })
                masterStatus?.let { Spacer(Modifier.padding(start = 12.dp)); Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun BackupCard() {
    val backup = LocalBackupManager.current
    val ioScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    DisposableEffect(Unit) { onDispose { ioScope.cancel() } }

    var folder by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<BackupInfo?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        val i = backup.info(); info = i; folder = i.externalFolder ?: ""
    }
    LaunchedEffect(Unit) { refresh() }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader("Sauvegarde & restauration")
            Spacer(Modifier.height(8.dp))
            Text(
                "Toutes vos données tiennent dans un seul fichier. Une sauvegarde automatique est créée à chaque fermeture et une fois par jour. Pour survivre à un formatage de Windows, choisissez un dossier externe (clé USB, disque D:, OneDrive…).",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Field("Dossier de sauvegarde externe", folder, Modifier.weight(1f)) { folder = it }
                OutlineAction("Parcourir", onClick = { ioScope.launch { backup.browseBackupFolder()?.let { folder = it } } })
            }
            Spacer(Modifier.height(8.dp))
            GradientButton("Définir le dossier", Gradients.brand, leadingIcon = AppIcon.SETTINGS, onClick = {
                ioScope.launch { status = backup.setBackupFolder(folder.ifBlank { null }).message; refresh() }
            })

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GradientButton("Exporter une sauvegarde", Gradients.brand, onClick = {
                    status = "…"; ioScope.launch { status = backup.exportBackup().message; refresh() }
                })
                OutlineAction("Importer une sauvegarde", onClick = {
                    status = "…"; ioScope.launch { status = backup.importBackup().message }
                })
            }
            status?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            info?.let { i ->
                Spacer(Modifier.height(10.dp))
                Text("Sauvegardes locales : ${i.count}  ·  ${i.localBackupsDir}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                i.lastBackup?.let { Text("Dernière sauvegarde : ${it.replace('T', ' ').take(19)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, modifier = modifier.fillMaxWidth().padding(vertical = 4.dp))
}

@Composable
private fun PwdField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}
