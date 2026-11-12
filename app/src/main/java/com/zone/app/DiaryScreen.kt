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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val moods = listOf("low", "flat", "good", "bright", "peak")
private val moodLabels = mapOf(
    "low" to "Low",
    "flat" to "Flat",
    "good" to "Good",
    "bright" to "Bright",
    "peak" to "Peak"
)
private val moodColors = mapOf(
    "low" to Color(0xFF9A6755),
    "flat" to Color(0xFF8B8B80),
    "good" to Color(0xFF6B8468),
    "bright" to Color(0xFFC09A49),
    "peak" to ZoneColors.red
)

private fun moodOf(value: String): String = if (value in moods) value else "good"

@Composable
fun DiaryScreen() {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var activeMood by remember { mutableStateOf("all") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DiaryEntry?>(null) }
    var deleteTarget by remember { mutableStateOf<DiaryEntry?>(null) }

    val entries = Zone.diary.value
    val today = Zone.todayISO()
    val words = entries.sumOf { Zone.words(it.body) }
    val sorted = entries.sortedWith(compareByDescending<DiaryEntry> { it.date }.thenByDescending { it.id })
    val filtered = sorted.filter { entry ->
        val matchesMood = activeMood == "all" || moodOf(entry.mood) == activeMood
        val matchesQuery = query.isBlank() ||
            (entry.title + " " + entry.body + " " + entry.tags.joinToString(" ")).contains(query.trim(), ignoreCase = true)
        matchesMood && matchesQuery
    }

    val grouped = filtered.groupBy { Zone.monthKey(it.date) }
    val monthKeys = grouped.keys.sortedDescending()

    val days = entries.map { it.date }.toSet()
    var cursor = today
    if (cursor !in days) cursor = Zone.addDays(cursor, -1)
    var streak = 0
    while (cursor in days) {
        streak++
        cursor = Zone.addDays(cursor, -1)
    }

    val topMood = moods.map { mood -> mood to entries.count { moodOf(it.mood) == mood } }
        .filter { it.second > 0 }
        .maxByOrNull { it.second }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Diary",
                subtitle = "${entries.size} pages Â· $streak day streak",
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
                            Icon(Icons.Default.Add, contentDescription = "New entry", tint = Color.White, modifier = Modifier.size(21.dp))
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
                StatTile("Pages", "${entries.size}", "${entries.count { Zone.monthKey(it.date) == Zone.currentMonth() }} this month", Modifier.weight(1f))
                StatTile("Streak", "$streak d", "Longest ${bestStreak(entries)} days", Modifier.weight(1f))
                StatTile("Words", Zone.num(words), topMood?.let { "${moodLabels[it.first]} weather" } ?: "No mood yet", Modifier.weight(1f))
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                SearchField(query, { query = it }, placeholder = "Search entries, tags")
            }
        }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = activeMood == "all",
                        onClick = { activeMood = "all" },
                        label = { Text("All") }
                    )
                }
                items(moods) { mood ->
                    FilterChip(
                        selected = activeMood == mood,
                        onClick = { activeMood = mood },
                        label = { Text(moodLabels[mood] ?: mood) }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        if (entries.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Edit,
                    title = "A blank page, for now",
                    message = "A line a day is enough. Start with where you were, what you noticed, and who was there.",
                    actionLabel = "Write the first entry",
                    onAction = {
                        editing = null
                        showEditor = true
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                )
            }
        } else if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Download,
                    title = "No entries match",
                    message = "Try a different word, mood, or clear the filters.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                )
            }
        } else {
            monthKeys.forEach { key ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Zone.monthLabel(key, long = true),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${grouped[key]?.size ?: 0} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZoneColors.secondary
                        )
                    }
                }
                items(grouped[key] ?: emptyList()) { entry ->
                    DiaryCard(
                        entry = entry,
                        onEdit = {
                            editing = entry
                            showEditor = true
                        },
                        onDelete = { deleteTarget = entry },
                        onCopy = { copyText(context, entryText(entry)) }
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            SecondaryButton(
                text = "Export journal (CSV)",
                onClick = { shareText(context, diaryCsv(entries), "Zone diary") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }

    if (showEditor) {
        DiaryEditor(
            initial = editing,
            onDismiss = {
                showEditor = false
                editing = null
            },
            onSave = { entry ->
                val current = Zone.diary.value
                val next = if (current.any { it.id == entry.id }) {
                    current.map { if (it.id == entry.id) entry else it }
                } else {
                    current + entry
                }
                Zone.setDiary(next, if (editing == null) "Entry saved" else "Entry updated", if (editing == null) "add" else "edit")
                showEditor = false
                editing = null
                toast(context, if (editing == null) "Entry saved." else "Entry updated.")
            }
        )
    }

    deleteTarget?.let { entry ->
        ConfirmDialog(
            title = "Let this page go?",
            message = "This removes \"${entry.title.ifBlank { "this entry" }}\" from the diary. There is no undo.",
            confirmLabel = "Delete entry",
            onConfirm = {
                Zone.setDiary(Zone.diary.value.filter { it.id != entry.id }, "Entry deleted", "delete")
                deleteTarget = null
                toast(context, "Entry deleted.")
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun DiaryCard(
    entry: DiaryEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val mood = moodOf(entry.mood)
    val color = moodColors[mood] ?: ZoneColors.green
    ZoneCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        padding = 16.dp
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.10f))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    Zone.parseDate(entry.date)?.dayOfMonth?.toString() ?: "â€”",
                    style = MaterialTheme.typography.titleLarge,
                    color = color
                )
                Text(
                    Zone.short(entry.date).takeLast(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Zone.rel(entry.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = ZoneColors.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ZoneColors.secondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ZoneColors.secondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red, modifier = Modifier.size(15.dp))
                    }
                }
                Text(
                    entry.title.ifBlank { "Untitled entry" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.body.ifBlank { "No words on this page yet." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZoneColors.secondary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        moodLabels[mood] ?: mood,
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                    if (entry.tags.isNotEmpty()) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            entry.tags.take(3).joinToString(" ") { "#$it" },
                            style = MaterialTheme.typography.labelSmall,
                            color = ZoneColors.blue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${Zone.words(entry.body)} words",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZoneColors.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryEditor(
    initial: DiaryEntry?,
    onDismiss: () -> Unit,
    onSave: (DiaryEntry) -> Unit
) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(initial?.date ?: Zone.todayISO()) }
    var mood by remember { mutableStateOf(moodOf(initial?.mood ?: "good")) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    var tags by remember { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }
    var error by remember { mutableStateOf("") }

    ZoneDialog(
        title = if (initial == null) "New entry" else "Edit entry",
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Save entry" else "Update entry",
        onConfirm = {
            val cleanTitle = title.trim()
            val cleanBody = body.trim()
            when {
                date.isBlank() -> error = "Choose a valid date."
                date > Zone.todayISO() -> error = "Choose today or an earlier day."
                cleanTitle.isEmpty() -> error = "Give the day a title."
                cleanBody.isEmpty() -> error = "Write something â€” even a single line."
                else -> {
                    val cleanTags = tags.split(",")
                        .map { it.trim().removePrefix("#").trim() }
                        .filter { it.isNotEmpty() }
                        .distinctBy { it.lowercase() }
                        .take(8)
                    onSave(
                        DiaryEntry(
                            id = initial?.id ?: Zone.uid("diary"),
                            date = date,
                            mood = moodOf(mood),
                            title = cleanTitle,
                            body = cleanBody,
                            tags = cleanTags
                        )
                    )
                }
            }
        }
    ) {
        LabeledField("Date", Zone.medium(date), {}, modifier = Modifier.fillMaxWidth(), readOnly = true)
        TextButton(onClick = { showDatePicker(context, Zone.parseDate(date) ?: Zone.today()) { date = it.toString() } }) {
            Text("Change date", color = ZoneColors.blue, style = MaterialTheme.typography.labelMedium)
        }
        Text("Mood", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            moods.forEach { option ->
                FilterChip(
                    selected = mood == option,
                    onClick = { mood = option },
                    label = { Text(moodLabels[option] ?: option) }
                )
            }
        }
        LabeledField("Title", title, { title = it }, placeholder = "Give the day a title")
        LabeledField("Entry", body, { body = it }, placeholder = "Write something honest.", minLines = 5)
        LabeledField("Threads (comma separated)", tags, { tags = it }, placeholder = "work, family, idea")
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun bestStreak(entries: List<DiaryEntry>): Int {
    val days = entries.map { it.date }.distinct().sorted()
    if (days.isEmpty()) return 0
    var best = 1
    var run = 1
    for (i in 1 until days.size) {
        if (Zone.daysBetween(days[i - 1], days[i]) == 1L) run++ else run = 1
        if (run > best) best = run
    }
    return best
}

private fun entryText(entry: DiaryEntry): String {
    val tags = if (entry.tags.isNotEmpty()) "\n\n" + entry.tags.joinToString(" ") { "#$it" } else ""
    return "${entry.title}\n${Zone.medium(entry.date)} Â· ${moodLabels[moodOf(entry.mood)]}\n\n${entry.body}$tags"
}

private fun diaryCsv(entries: List<DiaryEntry>): String {
    val rows = mutableListOf(listOf("Date", "Mood", "Title", "Threads", "Words", "Entry"))
    entries.sortedBy { it.date }.forEach { entry ->
        rows.add(
            listOf(
                entry.date,
                moodLabels[moodOf(entry.mood)] ?: entry.mood,
                entry.title,
                entry.tags.joinToString(", "),
                Zone.words(entry.body).toString(),
                entry.body
            )
        )
    }
    return csv(rows)
}