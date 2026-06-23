package org.example.project.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.core.saltedHash
import org.example.project.data.SettingsRepository
import org.example.project.presentation.components.BrandMark
import org.example.project.presentation.components.GradientButton
import org.example.project.presentation.theme.Gradients
import kotlin.random.Random

private fun hashWith(salt: String, value: String) = saltedHash(salt, value)

private fun newSalt(): String =
    Random.nextBytes(12).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

/** Gates the app behind a local login. First launch sets a password + a master recovery password. */
@Composable
fun AuthGate(content: @Composable () -> Unit) {
    val settings: SettingsRepository = koinInjectSettings()
    var authed by remember { mutableStateOf(false) }
    if (authed) content() else LoginScreen(settings) { authed = true }
}

@Composable
private fun LoginScreen(settings: SettingsRepository, onAuthed: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var salt by remember { mutableStateOf("") }
    var storedHash by remember { mutableStateOf<String?>(null) }
    var storedMaster by remember { mutableStateOf<String?>(null) }
    var storedUser by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        salt = settings.get(SettingsRepository.KEY_AUTH_SALT) ?: ""
        storedHash = settings.get(SettingsRepository.KEY_AUTH_HASH)
        storedMaster = settings.get(SettingsRepository.KEY_AUTH_MASTER)
        storedUser = settings.get(SettingsRepository.KEY_AUTH_USER) ?: ""
        loaded = true
    }

    val setup = loaded && storedHash == null
    var recovery by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var master by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(Gradients.ink), contentAlignment = Alignment.Center) {
        if (!loaded) return@Box
        Card(
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BrandMark(showWordmark = true)
                Spacer(Modifier.height(20.dp))
                Text(
                    when {
                        setup -> "Créer votre accès"
                        recovery -> "Récupération"
                        else -> "Connexion"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    when {
                        setup -> "Premier lancement : définissez vos identifiants et un mot de passe maître de secours."
                        recovery -> "Entrez le mot de passe maître pour récupérer l'accès."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                if (setup) {
                    PasswordField("Identifiant", user, password = false) { user = it; error = null }
                    PasswordField("Mot de passe", pass) { pass = it; error = null }
                    PasswordField("Confirmer le mot de passe", confirm) { confirm = it; error = null }
                    PasswordField("Mot de passe maître (récupération)", master) { master = it; error = null }
                } else if (recovery) {
                    PasswordField("Mot de passe maître", master) { master = it; error = null }
                } else {
                    PasswordField("Identifiant", user, password = false) { user = it; error = null }
                    PasswordField("Mot de passe", pass) { pass = it; error = null }
                }

                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(20.dp))
                GradientButton(
                    text = when {
                        setup -> "Créer le compte"
                        recovery -> "Récupérer l'accès"
                        else -> "Se connecter"
                    },
                    brush = Gradients.brand,
                    onClick = {
                        when {
                            setup -> when {
                                user.isBlank() || pass.isBlank() -> error = "Identifiant et mot de passe requis."
                                pass.length < 4 -> error = "Mot de passe trop court (min. 4 caractères)."
                                pass != confirm -> error = "Les mots de passe ne correspondent pas."
                                master.length < 4 -> error = "Définissez un mot de passe maître (min. 4 caractères)."
                                else -> scope.launch {
                                    val s = newSalt()
                                    settings.set(SettingsRepository.KEY_AUTH_SALT, s)
                                    settings.set(SettingsRepository.KEY_AUTH_USER, user.trim())
                                    settings.set(SettingsRepository.KEY_AUTH_HASH, hashWith(s, pass))
                                    settings.set(SettingsRepository.KEY_AUTH_MASTER, hashWith(s, master))
                                    onAuthed()
                                }
                            }
                            recovery -> {
                                if (storedMaster != null && hashWith(salt, master) == storedMaster) onAuthed()
                                else error = "Mot de passe maître incorrect."
                            }
                            else -> {
                                if (user.trim() == storedUser && hashWith(salt, pass) == storedHash) onAuthed()
                                else error = "Identifiant ou mot de passe incorrect."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!setup) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { recovery = !recovery; error = null }) {
                        Text(if (recovery) "Retour à la connexion" else "Mot de passe oublié ?")
                    }
                }
            }
        }
        Text(
            "SG Media Invoicing",
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
        )
    }
}

@Composable
private fun PasswordField(label: String, value: String, password: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
    )
}

@Composable
private fun koinInjectSettings(): SettingsRepository = org.koin.compose.koinInject()
