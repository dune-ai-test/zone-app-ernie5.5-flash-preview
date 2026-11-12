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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val weekLetters = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun HabitsScreen() {
    val context = LocalContext.current
    val habits = Zone.habits.value.habits
    val today = Zone.todayISO()
    var weekOffset by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var deleteTarget by remember { mutableStateOf<Habit?>(null) }

    val week = Zone.weekDates(weekOffset)
    val visibleHabits = if (query.isBlank()) habits else habits.filter { it.name.contains(query.trim(), ignoreCase = true) }

    val doneToday = habits.count { it.checks[today] == true }
    val weekPct = if (habits.isEmpty()) 0 else {
        val elapsed = week.count { it <= today }
        var met = 0
        var slots = 0
        habits.forEach { habit ->
            val counted = week.take(elapsed).count { habit.checks[it] == true }
            val cap = minOf(habit.target, elapsed)
            met += minOf(counted, cap)
            slots += cap
        }
        if (slots > 0) met * 100 / slots else 0
    }
    val best = habits.maxByOrNull { streakOf(it, today) }
    val recentCheckins = habits.sumOf { habit ->
        habit.checks.count { (date, checked) ->
            checked && date >= Zone.addDays(today, -55) && date <= today
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Habits",
                subtitle = "${habits.size} habits Â· $doneToday done today",
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
                            Icon(Icons.Default.Add, contentDescription = "New habit", tint = Color.White, modifier = Modifier.size(21.dp))
                        }
                    }
                }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile("Today", "$doneToday/${habits.size}", "Habits checked", Modifier.weight(1f))
                StatTile("This week", "$weekPct%", "Target pace", Modifier.weight(1f))
                StatTile("Best streak", "${best?.let { streakOf(it, today) } ?: 0} d", best?.name ?: "No streak yet", Modifier.weight(1f))
                StatTile("Check-ins", Zone.num(recentCheckins), "Last 8 weeks", Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { weekOffset-- }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous week", tint = ZoneColors.blue)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${Zone.short(week.first())} â€“ ${Zone.short(week.last())}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (weekOffset == 0) "This week" else "${-weekOffset} weeks ago",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZoneColors.secondary
                    )
                }
                IconButton(
                    onClick = { if (weekOffset < 0) weekOffset++ },
                    enabled = weekOffset < 0
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next week",
                        tint = if (weekOffset < 0) ZoneColors.blue else ZoneColors.separator
                    )
                }
                if (weekOffset != 0) {
                    TextButton(onClick = { weekOffset = 0 }) { Text("Today") }
                }
            }
        }

        item {
            SearchField(
                query,
                { query = it },
                placeholder = "Search habits",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        if (habits.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Refresh,
                    title = "Begin with one small thing",
                    message = "Choose something small enough to do on a bad day. The rest can wait.",
                    actionLabel = "Add your first habit",
                    onAction = {
                        editing = null
                        showEditor = true
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                )
            }
        } else {
            items(visibleHabits) { habit ->
                HabitCard(
                    habit = habit,
                    week = week,
                    today = today,
                    onToggle = { day ->
                        val checks = habit.checks.toMutableMap()
                        if (checks[day] == true) checks.remove(day) else checks[day] = true
                        val next = habits.map { if (it.id == habit.id) it.copy(checks = checks) else it }
                        Zone.setHabits(HabitsData(next), "Habit check-in", "edit", audit = false)
                    },
                    onEdit = {
                        editing = habit
                        showEditor = true
                    },
                    onDelete = { deleteTarget = habit }
                )
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            SecondaryButton(
                text = "Export habits (CSV)",
                onClick = { shareText(context, habitsCsv(habits), "Zone habits") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }

    if (showEditor) {
        HabitEditor(
            initial = editing,
            onDismiss = {
                showEditor = false
                editing = null
            },
            onSave = { habit ->
                val next = if (habits.any { it.id == habit.id }) {
                    habits.map { if (it.id == habit.id) habit else it }
                } else {
                    habits + habit
                }
                Zone.setHabits(
                    HabitsData(next),
                    if (editing == null) "Habit added" else "Habit updated",
                    if (editing == null) "add" else "edit"
                )
                showEditor = false
                editing = null
                toast(context, if (editing == null) "Habit saved." else "Habit updated.")
            }
        )
    }

    deleteTarget?.let { habit ->
        ConfirmDialog(
            title = "Let this row go?",
            message = "This removes \"${habit.name}\" and all of its check-ins. There is no undo.",
            confirmLabel = "Delete habit",
            onConfirm = {
                Zone.setHabits(HabitsData(habits.filter { it.id != habit.id }), "Habit deleted", "delete")
                deleteTarget = null
                toast(context, "Habit deleted.")
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    week: List<String>,
    today: String,
    onToggle: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val checkedThisWeek = week.count { habit.checks[it] == true }
    val streak = streakOf(habit, today)
    ZoneCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        padding = 16.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Monogram(habit.name, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "$checkedThisWeek of ${habit.target} this week${if (streak > 0) " Â· $streak day streak" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZoneColors.secondary
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ZoneColors.secondary, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            week.forEachIndexed { index, day ->
                val done = habit.checks[day] == true
                val future = day > today
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        weekLetters[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = ZoneColors.secondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        onClick = { if (!future) onToggle(day) },
                        enabled = !future,
                        shape = RoundedCornerShape(10.dp),
                        color = if (done) ZoneColors.blue else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (done) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            } else {
                                Text(
                                    Zone.parseDate(day)?.dayOfMonth?.toString() ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (future) ZoneColors.separator else ZoneColors.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitEditor(
    initial: Habit?,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var target by remember { mutableStateOf((initial?.target ?: 7).toString()) }
    var error by remember { mutableStateOf("") }

    ZoneDialog(
        title = if (initial == null) "New habit" else "Edit habit",
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Add habit" else "Save changes",
        onConfirm = {
            val clean = name.trim()
            if (clean.isEmpty()) {
                error = "Give the habit a name."
                return@ZoneDialog
            }
            val parsedTarget = target.toIntOrNull()?.coerceIn(1, 7) ?: 7
            onSave(
                Habit(
                    id = initial?.id ?: Zone.uid("habit"),
                    name = clean,
                    target = parsedTarget,
                    createdAt = initial?.createdAt ?: Zone.todayISO(),
                    checks = initial?.checks ?: emptyMap()
                )
            )
        }
    ) {
        LabeledField("Habit", name, { name = it }, placeholder = "Read, walk, stretchâ€¦")
        LabeledField("Target per week (1â€“7)", target, { target = it.filter { c -> c.isDigit() } }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun streakOf(habit: Habit, today: String): Int {
    var cursor = today
    if (habit.checks[cursor] != true) cursor = Zone.addDays(cursor, -1)
    var count = 0
    while (habit.checks[cursor] == true) {
        count++
        cursor = Zone.addDays(cursor, -1)
    }
    return count
}

private fun habitsCsv(habits: List<Habit>): String {
    val rows = mutableListOf(listOf("Habit", "Target per week", "Created", "Checked dates"))
    habits.forEach { habit ->
        rows.add(
            listOf(
                habit.name,
                habit.target.toString(),
                habit.createdAt,
                habit.checks.filterValues { it }.keys.sorted().joinToString("; ")
            )
        )
    }
    return csv(rows)
}