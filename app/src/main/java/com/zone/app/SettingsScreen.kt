package com.zone.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val currencies = listOf("INR", "USD", "EUR", "GBP", "AED", "AUD", "CAD", "SGD")

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = Zone.settings

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text != null) {
                val count = Zone.importBackup(text)
                toast(
                    context,
                    if (count > 0) "Backup imported — $count sections restored." else "That file is not a valid Zone backup."
                )
            } else {
                toast(context, "Could not read that file.")
            }
        }
    }

    val recordCount = Zone.diary.value.size +
        Zone.lists.value.size +
        Zone.habits.value.habits.size +
        Zone.notes.value.size +
        Zone.moneyData.value.transactions.size +
        Zone.passbook.value.accounts.size +
        Zone.passbook.value.transactions.size +
        Zone.split.value.groups.size +
        Zone.trips.value.size +
        Zone.vehicles.value.size +
        Zone.watchlist.value.size +
        Zone.links.value.size +
        Zone.subscriptions.value.size

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Settings",
                subtitle = "Appearance and display currency"
            )
        }

        item { SectionTitle("Appearance") }

        item {
            ZoneCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                padding = 16.dp
            ) {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Segmented(
                    options = listOf("light", "dark", "system"),
                    selected = settings.theme,
                    label = { it.replaceFirstChar { c -> c.uppercase() } },
                    onSelect = { theme ->
                        Zone.setSettings(settings.copy(theme = theme), "Theme set to $theme")
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "System follows the device light/dark setting.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZoneColors.secondary
                )
            }
        }

        item { SectionTitle("Currency") }

        item {
            ZoneCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                padding = 8.dp
            ) {
                currencies.forEach { currency ->
                    val active = settings.currency == currency
                    Surface(
                        onClick = {
                            if (!active) {
                                Zone.setSettings(settings.copy(currency = currency), "Currency set to $currency")
                            }
                        },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(currency, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    when (currency) {
                                        "INR" -> "Indian rupee · en-IN"
                                        "USD" -> "US dollar"
                                        "EUR" -> "Euro"
                                        "GBP" -> "Pound sterling"
                                        "AED" -> "UAE dirham"
                                        "AUD" -> "Australian dollar"
                                        "CAD" -> "Canadian dollar"
                                        else -> "Singapore dollar"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZoneColors.secondary
                                )
                            }
                            Text(
                                Zone.money(1234, if (currency == "INR") 0 else 2),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ZoneColors.secondary
                            )
                            if (active) {
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = ZoneColors.blue, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        item { SectionTitle("Backup") }

        item {
            ZoneCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                padding = 16.dp
            ) {
                Text("Keep a copy", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "The backup is a single JSON file with every section, including settings and the review trail.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZoneColors.secondary
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        text = "Export backup",
                        onClick = { shareText(context, Zone.backupJson(), "Zone backup") },
                        icon = Icons.Default.Download
                    )
                    SecondaryButton(
                        text = "Import backup",
                        onClick = { importLauncher.launch("*/*") },
                        icon = Icons.Default.Upload
                    )
                }
            }
        }

        item { SectionTitle("About") }

        item {
            ZoneCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                padding = 16.dp
            ) {
                AboutRow("Version", "1.0.0")
                AboutRow("Currency", settings.currency)
                AboutRow("Theme", settings.theme.replaceFirstChar { it.uppercase() })
                AboutRow("Records", recordCount.toString())
                AboutRow("Audit events", Zone.audit.value.size.toString())
                AboutRow("Pending review", Zone.audit.value.count { it.status == "pending" }.toString())
                Spacer(Modifier.height(8.dp))
                Text(
                    "Local-first workspace. Nothing leaves this device unless you export it.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZoneColors.secondary
                )
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            SecondaryButton(
                text = "Export all data (JSON)",
                onClick = { shareText(context, Zone.backupJson(), "Zone backup") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = ZoneColors.secondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}