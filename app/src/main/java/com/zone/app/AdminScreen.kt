package com.zone.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun AdminScreen() {
    val context = LocalContext.current
    val events = Zone.audit.value
    val pending = events.filter { it.status == "pending" }
    val approved = events.count { it.status == "approved" }
    val rejected = events.count { it.status == "rejected" || it.status == "rolledback" }
    val autoApprove = Zone.admin.value.settings.autoApprove
    var filter by remember { mutableStateOf("all") }

    val filtered = events.filter { event ->
        when (filter) {
            "pending" -> event.status == "pending"
            "approved" -> event.status == "approved"
            "restored" -> event.status == "rejected" || event.status == "rolledback"
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Review desk",
                subtitle = "Approvals, audit trail, and workspace controls"
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile("Pending", "${pending.size}", "Awaiting review", Modifier.weight(1f))
                StatTile("Approved", "$approved", "Signed off", Modifier.weight(1f))
                StatTile("Restored", "$rejected", "Rejected or rolled back", Modifier.weight(1f))
                StatTile("Events", "${events.size}", "In the trail", Modifier.weight(1f))
            }
        }

        item {
            ZoneCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                padding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-approve changes", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "When on, new changes are approved immediately. Writes are never gated.",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZoneColors.secondary
                        )
                    }
                    Switch(
                        checked = autoApprove,
                        onCheckedChange = {
                            Zone.setAutoApprove(it)
                            toast(context, if (it) "Auto-approve on." else "Manual review on.")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ZoneColors.green,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = ZoneColors.separator
                        )
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "pending" to "Pending", "approved" to "Approved", "restored" to "Restored").forEach { (value, label) ->
                    FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(label) })
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Check,
                    title = if (events.isEmpty()) "The desk is clear" else "Nothing in this view",
                    message = if (events.isEmpty()) "Changes will appear here as you edit the workspace." else "Try another filter.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
        } else {
            items(filtered) { event ->
                AuditCard(
                    event = event,
                    onApprove = {
                        Zone.approveAudit(event.id)
                        toast(context, "Change approved.")
                    },
                    onReject = {
                        Zone.rejectAudit(event.id)
                        toast(context, "Change rejected and restored.")
                    },
                    onRollback = {
                        Zone.rollbackAudit(event.id)
                        toast(context, "Change rolled back.")
                    },
                    onReapply = {
                        Zone.reapplyAudit(event.id)
                        toast(context, "Change reapplied.")
                    }
                )
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            SecondaryButton(
                text = "Export review report",
                onClick = { shareText(context, auditReport(events), "Zone review report") },
                icon = Icons.Default.Download,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun AuditCard(
    event: AuditEvent,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRollback: () -> Unit,
    onReapply: () -> Unit
) {
    ZoneCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        padding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(ZoneColors.blue.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                ToolIcon(event.section, size = 18.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${event.action} Â· ${Zone.medium(event.at.take(10))} Â· ${event.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZoneColors.secondary
                )
            }
            Text(
                event.status,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor(event.status)
            )
        }
        if (event.detail.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(event.detail, style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (event.status) {
                "pending" -> {
                    PrimaryButton("Approve", onApprove)
                    SecondaryButton("Reject & restore", onReject)
                }
                "approved" -> {
                    SecondaryButton("Roll back", onRollback, icon = Icons.Default.Refresh)
                }
                "rejected", "rolledback" -> {
                    SecondaryButton("Reapply", onReapply, icon = Icons.Default.Refresh)
                }
                else -> {
                    Text(
                        "Resolved",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZoneColors.secondary
                    )
                }
            }
        }
    }
}

private fun statusColor(status: String): Color = when (status) {
    "pending" -> ZoneColors.orange
    "approved" -> ZoneColors.green
    "rejected", "rolledback" -> ZoneColors.red
    else -> ZoneColors.secondary
}

private fun auditReport(events: List<AuditEvent>): String {
    val lines = mutableListOf(
        "Zone review report",
        "Generated ${Zone.medium(Zone.todayISO())}",
        "Events: ${events.size}",
        "Pending: ${events.count { it.status == "pending" }}",
        "Approved: ${events.count { it.status == "approved" }}",
        "Rejected/rolled back: ${events.count { it.status == "rejected" || it.status == "rolledback" }}",
        "",
        "---",
        ""
    )
    events.forEach { event ->
        lines.add("${Zone.medium(event.at.take(10))} Â· ${event.status.uppercase()} Â· ${event.toolLabel()} Â· ${event.title}")
        if (event.detail.isNotBlank()) lines.add("  ${event.detail}")
        lines.add("")
    }
    return lines.joinToString("\n")
}

private fun AuditEvent.toolLabel(): String = section.replaceFirstChar { it.uppercase() }