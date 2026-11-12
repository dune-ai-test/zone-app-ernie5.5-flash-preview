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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val subscriptionCycles = listOf("Monthly", "Yearly", "Weekly")
private val subscriptionCategories = listOf(
    "Design", "Music", "Storage", "Health", "Reading", "Web", "Software", "Video", "News", "Other"
)

@Composable
fun SubscriptionsScreen() {
    val context = LocalContext.current
    val subs = Zone.subscriptions.value
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var categoryFilter by remember { mutableStateOf("all") }
    var sort by remember { mutableStateOf("next") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Subscription?>(null) }
    var deleteTarget by remember { mutableStateOf<Subscription?>(null) }

    val active = subs.filter { it.status != "Paused" }
    val monthly = active.sumOf { monthlyOf(it) }
    val annual = monthly * 12
    val avg = if (active.isNotEmpty()) monthly / active.size else 0.0
    val dueSoon = active.filter {
        val days = Zone.daysBetween(Zone.todayISO(), it.next)
        days in 0..7
    }.sumOf { it.amount }
    val categoryTotals = active.groupBy { it.category }
        .map { (name, list) -> name to list.sumOf { monthlyOf(it) } }
        .sortedByDescending { it.second }
    val biggest = categoryTotals.firstOrNull()

    val filtered = subs.filter { sub ->
        val matchesStatus = when (statusFilter) {
            "active" -> sub.status != "Paused"
            "paused" -> sub.status == "Paused"
            else -> true
        }
        val matchesCategory = categoryFilter == "all" || sub.category == categoryFilter
        val matchesQuery = query.isBlank() ||
            (sub.name + " " + sub.category + " " + sub.cycle).contains(query.trim(), ignoreCase = true)
        matchesStatus && matchesCategory && matchesQuery
    }.sortedWith(
        when (sort) {
            "monthly" -> compareByDescending { monthlyOf(it) }
            "name" -> compareBy { it.name.lowercase() }
            else -> compareBy<Subscription> { if (it.status == "Paused") 1 else 0 }.thenBy { it.next }.thenBy { it.name }
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Subscriptions",
                subtitle = "${active.size} active Â· ${Zone.money(monthly)}/month",
                action = {
                    Surface(
                        onClick = {
                            editing = null
                            showEditor = true
                        },
                        shape = RoundedCornerShape(50),
                        color = ZoneColors.blue,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Add subscription", tint = Color.White, modifier = Modifier.size(21.dp))
                        }
                    }
                }
            )
        }

        item {
            ZoneCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                padding = 18.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Monthly burn", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                        Text(Zone.money(monthly), style = MaterialTheme.typography.displayLarge)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Annual", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                        Text(Zone.money(annual), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    subscriptionCycles.forEach { cycle ->
                        val amount = active.filter { it.cycle == cycle }.sumOf { monthlyOf(it) }
                        if (amount > 0) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cycle, style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                                Text(Zone.money(amount), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (active.isEmpty()) "Nothing active yet." else "${active.size} active Â· ${subs.size - active.size} paused",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZoneColors.secondary
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile("Active", "${active.size}", "${subs.size - active.size} paused", Modifier.weight(1f))
                StatTile("Due in 7 days", Zone.money(dueSoon), "Raw amounts due", Modifier.weight(1f))
                StatTile("Average", Zone.money(avg), "Per active service", Modifier.weight(1f))
                StatTile("Biggest", biggest?.first ?: "â€”", biggest?.let { "${Zone.money(it.second)}/mo" } ?: "No split", Modifier.weight(1f))
            }
        }

        item { SectionTitle("Next up") }

        item {
            val renewals = active.filter { it.next.isNotBlank() }.sortedWith(compareBy({ it.next }, { it.name })).take(6)
            if (renewals.isEmpty()) {
                ZoneCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    padding = 16.dp
                ) {
                    Text("All quiet.", style = MaterialTheme.typography.bodyLarge)
                    Text("Nothing renews in the list.", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    renewals.forEach { sub ->
                        val days = Zone.daysBetween(Zone.todayISO(), sub.next)
                        ZoneCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            padding = 12.dp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sub.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${sub.cycle} Â· ${sub.category}", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Zone.money(sub.amount), style = MaterialTheme.typography.bodyLarge)
                                    Text(dueLabel(days), style = MaterialTheme.typography.labelSmall, color = dueColor(days))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchField(query, { query = it }, modifier = Modifier.weight(1f), placeholder = "Search services")
            }
        }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "active" to "Active", "paused" to "Paused").forEach { (value, label) ->
                    item {
                        FilterChip(selected = statusFilter == value, onClick = { statusFilter = value }, label = { Text(label) })
                    }
                }
                listOf("next" to "Next", "monthly" to "Monthly", "name" to "Aâ€“Z").forEach { (value, label) ->
                    item {
                        FilterChip(selected = sort == value, onClick = { sort = value }, label = { Text(label) })
                    }
                }
            }
        }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(selected = categoryFilter == "all", onClick = { categoryFilter = "all" }, label = { Text("All categories") })
                }
                items((subscriptionCategories + subs.map { it.category }).distinct().sorted()) { category ->
                    FilterChip(selected = categoryFilter == category, onClick = { categoryFilter = category }, label = { Text(category) })
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.CreditCard,
                    title = if (subs.isEmpty()) "Nothing on the shelf" else "Nothing matches",
                    message = if (subs.isEmpty()) "Add the first quiet monthly cost." else "Try another status, category, or search term.",
                    actionLabel = if (subs.isEmpty()) "Add a subscription" else null,
                    onAction = if (subs.isEmpty()) {
                        {
                            editing = null
                            showEditor = true
                        }
                    } else null,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
        } else {
            items(filtered) { sub ->
                ZoneCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    padding = 14.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Monogram(sub.name, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sub.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${sub.cycle} Â· ${sub.category}${if (sub.status == "Paused") " Â· paused" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sub.status == "Paused") ZoneColors.secondary else ZoneColors.blue
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Zone.money(sub.amount), style = MaterialTheme.typography.titleMedium)
                            Text(dueLabel(Zone.daysBetween(Zone.todayISO(), sub.next)), style = MaterialTheme.typography.labelSmall, color = dueColor(Zone.daysBetween(Zone.todayISO(), sub.next)))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        SecondaryButton(
                            if (sub.status == "Paused") "Resume" else "Pause",
                            {
                                val next = subs.map { if (it.id == sub.id) it.copy(status = if (it.status == "Paused") "Active" else "Paused") else it }
                                Zone.setSubscriptions(next, if (sub.status == "Paused") "Subscription resumed" else "Subscription paused", "edit", audit = false)
                                toast(context, if (sub.status == "Paused") "Subscription resumed." else "Subscription paused.")
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        SecondaryButton("Edit", {
                            editing = sub
                            showEditor = true
                        })
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { deleteTarget = sub }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red, modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            SecondaryButton(
                text = "Export subscriptions (CSV)",
                onClick = { shareText(context, subscriptionsCsv(subs), "Zone subscriptions") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }

    if (showEditor) {
        SubscriptionEditor(
            initial = editing,
            onDismiss = {
                showEditor = false
                editing = null
            },
            onSave = { sub ->
                val next = if (subs.any { it.id == sub.id }) {
                    subs.map { if (it.id == sub.id) sub else it }
                } else {
                    subs + sub
                }
                Zone.setSubscriptions(next, if (editing == null) "Subscription added" else "Subscription updated", if (editing == null) "add" else "edit")
                showEditor = false
                editing = null
                toast(context, if (editing == null) "Subscription saved." else "Subscription updated.")
            }
        )
    }

    deleteTarget?.let { sub ->
        ConfirmDialog(
            title = "Remove ${sub.name}?",
            message = "${Zone.money(sub.amount)} ${sub.cycle.lowercase()} subscription will be removed from the shelf.",
            confirmLabel = "Remove subscription",
            onConfirm = {
                Zone.setSubscriptions(subs.filter { it.id != sub.id }, "Subscription removed", "delete")
                deleteTarget = null
                toast(context, "Subscription removed.")
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun SubscriptionEditor(
    initial: Subscription?,
    onDismiss: () -> Unit,
    onSave: (Subscription) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.takeIf { it > 0 }?.toString() ?: "") }
    var cycle by remember { mutableStateOf(initial?.cycle ?: "Monthly") }
    var next by remember { mutableStateOf(initial?.next ?: Zone.addDays(Zone.todayISO(), 7)) }
    var category by remember { mutableStateOf(initial?.category ?: "Software") }
    var status by remember { mutableStateOf(initial?.status ?: "Active") }
    var error by remember { mutableStateOf("") }

    val preview = amount.toDoubleOrNull()?.let { monthlyOf(Subscription(amount = it, cycle = cycle)) }

    ZoneDialog(
        title = if (initial == null) "Add subscription" else "Edit subscription",
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Save subscription" else "Save changes",
        onConfirm = {
            val value = amount.toDoubleOrNull()
            if (name.trim().isEmpty()) {
                error = "Name the service."
                return@ZoneDialog
            }
            if (value == null || value <= 0) {
                error = "Enter an amount greater than zero."
                return@ZoneDialog
            }
            if (next.isBlank()) {
                error = "Choose the next charge date."
                return@ZoneDialog
            }
            onSave(
                Subscription(
                    id = initial?.id ?: Zone.uid("sub"),
                    name = name.trim().take(60),
                    amount = value,
                    cycle = cycle,
                    next = next,
                    category = category,
                    status = status
                )
            )
        }
    ) {
        LabeledField("Service", name, { name = it }, placeholder = "Streaming, software, storageâ€¦")
        LabeledField("Amount", amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, placeholder = "0", keyboardType = KeyboardType.Number)
        Text("Cycle", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            subscriptionCycles.forEach { option ->
                FilterChip(selected = cycle == option, onClick = { cycle = option }, label = { Text(option) })
            }
        }
        LabeledField("Next charge", Zone.medium(next), {}, modifier = Modifier.fillMaxWidth(), readOnly = true)
        TextButton(onClick = { showDatePicker(context, Zone.parseDate(next) ?: Zone.today()) { next = it.toString() } }) {
            Text("Change date", color = ZoneColors.blue, style = MaterialTheme.typography.labelMedium)
        }
        Text("Category", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(subscriptionCategories) { option ->
                FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option) })
            }
        }
        Text("Status", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Active", "Paused").forEach { option ->
                FilterChip(selected = status == option, onClick = { status = option }, label = { Text(option) })
            }
        }
        Text(
            if (preview != null && preview > 0) "Monthly share: ${Zone.money(preview)}" else "Monthly share: â€”",
            style = MaterialTheme.typography.labelSmall,
            color = ZoneColors.secondary
        )
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun monthlyOf(sub: Subscription): Double {
    return when (sub.cycle) {
        "Yearly" -> sub.amount / 12.0
        "Weekly" -> sub.amount * 52.0 / 12.0
        else -> sub.amount
    }
}

private fun dueLabel(days: Long): String = when {
    days < 0 -> "${-days}d late"
    days == 0L -> "today"
    else -> "in ${days}d"
}

private fun dueColor(days: Long): Color = when {
    days < 0 -> ZoneColors.red
    days <= 7 -> ZoneColors.orange
    else -> ZoneColors.secondary
}

private fun subscriptionsCsv(subs: List<Subscription>): String {
    val rows = mutableListOf(listOf("Name", "Amount", "Cycle", "Next", "Category", "Status", "Monthly", "Currency"))
    subs.sortedBy { it.next }.forEach { sub ->
        rows.add(
            listOf(
                sub.name,
                sub.amount.toString(),
                sub.cycle,
                sub.next,
                sub.category,
                sub.status,
                monthlyOf(sub).toString(),
                Zone.settings.value.currency
            )
        )
    }
    return csv(rows)
}