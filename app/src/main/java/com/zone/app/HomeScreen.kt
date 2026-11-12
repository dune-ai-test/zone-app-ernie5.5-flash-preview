package com.zone.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalTime

private data class ToolCardData(
    val route: String,
    val label: String,
    val description: String,
    val meta: String
)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val hour = remember { LocalTime.now().hour }
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    val diary = Zone.diary.value
    val lists = Zone.lists.value
    val habits = Zone.habits.value.habits
    val money = Zone.moneyData.value
    val trips = Zone.trips.value
    val watchlist = Zone.watchlist.value
    val subs = Zone.subscriptions.value
    val notes = Zone.notes.value
    val links = Zone.links.value
    val passbook = Zone.passbook.value
    val split = Zone.split.value

    val today = Zone.todayISO()
    val month = Zone.currentMonth()
    val monthTxns = money.transactions.filter { Zone.monthKey(it.date) == month }
    val spent = monthTxns.filter { it.type == "expense" }.sumOf { it.amount }
    val earned = monthTxns.filter { it.type == "income" }.sumOf { it.amount }
    val budget = money.budget
    val budgetPct = if (budget > 0) (spent / budget).coerceAtMost(1.5).toFloat() else 0f

    val diaryDays = remember(diary) { diary.map { it.date }.toSet() }
    var cursor = today
    if (cursor !in diaryDays) cursor = Zone.addDays(cursor, -1)
    var streak = 0
    while (cursor in diaryDays) {
        streak++
        cursor = Zone.addDays(cursor, -1)
    }

    var openItems = 0
    lists.forEach { list ->
        list.items.forEach { item ->
            if (!item.done) openItems++
            item.children.forEach { step -> if (!step.done) openItems++ }
        }
    }

    val habitsDone = habits.count { it.checks[today] == true }
    val words = diary.sumOf { Zone.words(it.body) }
    val monthlyBurn = subs.filter { it.status != "Paused" }.sumOf {
        when (it.cycle) {
            "Yearly" -> it.amount / 12.0
            "Weekly" -> it.amount * 52.0 / 12.0
            else -> it.amount
        }
    }

    val validTrips = trips.filter {
        val distance = it.end - it.start
        val litres = if (it.price > 0) it.paid / it.price else 0.0
        distance > 0 && litres > 0
    }
    val avgMileage = if (validTrips.isNotEmpty()) {
        val distance = validTrips.sumOf { it.end - it.start }
        val litres = validTrips.sumOf { if (it.price > 0) it.paid / it.price else 0.0 }
        if (litres > 0) distance / litres else 0.0
    } else 0.0

    val tools = listOf(
        ToolCardData("diary", "Diary", "A daily record of what happened", "${diary.size} entries"),
        ToolCardData("money", "Money", "Where the month goes", Zone.money(spent)),
        ToolCardData("lists", "Lists", "Nested checklists with steps", "$openItems open"),
        ToolCardData("habits", "Habits", "Small repetitions, made visible", "$habitsDone of ${habits.size} today"),
        ToolCardData("notes", "Notes", "A scratchpad with a memory", "${notes.size} notes"),
        ToolCardData("passbook", "Passbook", "Every account, one statement", "${passbook.accounts.size} accounts"),
        ToolCardData("split", "Split", "Shared costs, squared", "${split.groups.size} tables"),
        ToolCardData("mileage", "Mileage", "Distance, fuel, cost per km", "${avgMileage.toInt()} km/l avg"),
        ToolCardData("watchlist", "Watchlist", "What to watch, and what was worth it", "${watchlist.size} titles"),
        ToolCardData("links", "Links", "A shelf for good corners of the web", "${links.size} saved"),
        ToolCardData("subscriptions", "Subscriptions", "The quiet monthly cost of it all", "${Zone.money(monthlyBurn)}/mo"),
        ToolCardData("admin", "Review desk", "Approvals, audit trail, backups", "${Zone.audit.value.count { it.status == "pending" }} pending"),
        ToolCardData("settings", "Settings", "Appearance and display currency", Zone.settings.currency)
    )

    val filtered = if (query.isBlank()) tools else tools.filter {
        (it.label + " " + it.description + " " + it.meta).contains(query.trim(), ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Zone",
                subtitle = "$greeting · ${Zone.medium(today)}"
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(
                    label = "Spent this month",
                    value = Zone.money(spent),
                    hint = if (budget > 0) "${(spent / budget * 100).toInt()}% of ${Zone.money(budget)}" else "No budget set",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Diary streak",
                    value = "$streak days",
                    hint = "${diary.size} entries · ${Zone.num(words)} words",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(
                    label = "Open list items",
                    value = "$openItems",
                    hint = "${lists.size} lists in progress",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Average mileage",
                    value = "${"%.1f".format(avgMileage)} km/l",
                    hint = "${validTrips.size} complete fills",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                ZoneProgressBar(fraction = budgetPct)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (budget > 0) "${Zone.money(spent)} of ${Zone.money(budget)} budget" else "Set a monthly envelope in Money",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZoneColors.secondary
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ZoneCard(modifier = Modifier.weight(1f), onClick = { onNavigate("money") }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ZoneColors.green.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = ZoneColors.green, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Earned", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                            Text("+" + Zone.money(earned), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                ZoneCard(modifier = Modifier.weight(1f), onClick = { onNavigate("habits") }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ZoneColors.orange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ZoneColors.orange, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Habits today", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                            Text("$habitsDone of ${habits.size}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        item { SectionTitle("Your desk", modifier = Modifier.padding(top = 10.dp)) }

        item {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search tools",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        items(filtered.chunked(2)) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { tool ->
                    ToolTile(tool, Modifier.weight(1f)) { onNavigate(tool.route) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.CalendarMonth,
                    title = "Nothing found",
                    message = "Try a different word — diary, money, mileage.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolTile(
    tool: ToolCardData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ZoneCard(modifier = modifier, padding = 16.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(ZoneColors.blue.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                ToolIcon(tool.route, size = 19.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tool.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    tool.meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = ZoneColors.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            tool.description,
            style = MaterialTheme.typography.bodyMedium,
            color = ZoneColors.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    val groups = listOf(
        "Personal" to listOf(
            ToolCardData("diary", "Diary", "A daily record of what happened", ""),
            ToolCardData("lists", "Lists", "Nested checklists with steps", ""),
            ToolCardData("habits", "Habits", "Small repetitions, made visible", ""),
            ToolCardData("notes", "Notes", "A scratchpad with a memory", "")
        ),
        "Money" to listOf(
            ToolCardData("money", "Money", "Where the month goes", ""),
            ToolCardData("passbook", "Passbook", "Every account, one statement", ""),
            ToolCardData("split", "Split", "Shared costs, squared", ""),
            ToolCardData("mileage", "Mileage", "Distance, fuel, cost per km", "")
        ),
        "Library" to listOf(
            ToolCardData("watchlist", "Watchlist", "What to watch, and what was worth it", ""),
            ToolCardData("links", "Links", "A shelf for good corners of the web", ""),
            ToolCardData("subscriptions", "Subscriptions", "The quiet monthly cost of it all", "")
        ),
        "System" to listOf(
            ToolCardData("admin", "Review desk", "Approvals, audit trail, backups", ""),
            ToolCardData("settings", "Settings", "Appearance and display currency", "")
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "More",
                subtitle = "Every tool on the desk"
            )
        }
        groups.forEach { (group, tools) ->
            item {
                SectionTitle(group)
            }
            items(tools) { tool ->
                ZoneCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    padding = 14.dp,
                    onClick = { onNavigate(tool.route) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ZoneColors.blue.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ToolIcon(tool.route, size = 20.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tool.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                tool.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = ZoneColors.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = ZoneColors.separator,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}