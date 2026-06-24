package org.example.project.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import org.example.project.data.SettingsRepository
import org.example.project.data.WeatherInfo
import org.example.project.data.WeatherService
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private val frWeekdayShort = listOf("lun", "mar", "mer", "jeu", "ven", "sam", "dim")

private fun weekdayShort(iso: String): String = runCatching {
    frWeekdayShort[(LocalDate.parse(iso).dayOfWeek.isoDayNumber - 1).coerceIn(0, 6)]
}.getOrDefault("")

/** Live weather (Open-Meteo) for a user-chosen city, shown next to the dashboard clock. */
@Composable
fun WeatherPanel(modifier: Modifier = Modifier) {
    val weatherService: WeatherService = koinInject()
    val settings: SettingsRepository = koinInject()
    val scope = rememberCoroutineScope()

    var city by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }

    suspend fun load(c: String) {
        loading = true; error = null
        weatherService.fetch(c)
            .onSuccess { weather = it }
            .onFailure { error = it.message ?: "Météo indisponible" }
        loading = false
    }

    LaunchedEffect(Unit) {
        city = settings.get(SettingsRepository.KEY_WEATHER_CITY)
        loaded = true
        city?.takeIf { it.isNotBlank() }?.let { load(it) }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MÉTÉO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                weather?.let { Text(it.emoji, fontSize = 16.sp) }
                Spacer(Modifier.weight(1f))
                Text(
                    "Changer",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { editing = true },
                )
            }
            Spacer(Modifier.height(10.dp))
            when {
                !loaded || loading -> Text("Chargement…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                city.isNullOrBlank() -> Text(
                    "Définir une ville",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { editing = true },
                )
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                weather != null -> {
                    val w = weather!!
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(w.emoji, fontSize = 30.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("${w.temperatureC.roundToInt()}°", fontWeight = FontWeight.Bold, fontSize = 34.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(w.label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Text(
                        "${w.city}${w.country?.let { ", $it" } ?: ""}  ·  ${w.windKmh.roundToInt()} km/h",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    if (w.forecast.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            w.forecast.forEach { day ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(weekdayShort(day.date), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(3.dp))
                                    Text(day.emoji, fontSize = 18.sp)
                                    Spacer(Modifier.height(3.dp))
                                    Text("${day.tempMaxC.roundToInt()}°", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${day.tempMinC.roundToInt()}°", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editing) {
        var input by remember { mutableStateOf(city ?: "") }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Ville pour la météo") },
            text = {
                OutlinedTextField(
                    input, { input = it },
                    label = { Text("Ville (ex. Alger)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val c = input.trim()
                    editing = false
                    scope.launch {
                        settings.set(SettingsRepository.KEY_WEATHER_CITY, c)
                        city = c
                        weather = null
                        if (c.isNotBlank()) load(c) else error = null
                    }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Annuler") } },
        )
    }
}
