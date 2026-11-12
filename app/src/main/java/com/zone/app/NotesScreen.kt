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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
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

private val noteColors = listOf("plain", "sand", "sage", "clay")
private val noteColorLabels = mapOf(
    "plain" to "Plain",
    "sand" to "Sand",
    "sage" to "Sage",
    "clay" to "Clay"
)
private val noteColorValues = mapOf(
    "plain" to Color(0xFF9A8C76),
    "sand" to Color(0xFFB69A63),
    "sage" to Color(0xFF809174),
    "clay" to Color(0xFFB77A63)
)

@Composable
fun NotesScreen() {
    val context = LocalContext.current
    val notes = Zone.notes.value
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<NoteItem?>(null) }
    var viewing by remember { mutableStateOf<NoteItem?>(null) }
    var deleteTarget by remember { mutableStateOf<NoteItem?>(null) }

    val weekStart = Zone.addDays(Zone.todayISO(), -6)
    val filtered = notes.filter { note ->
        val matchesQuery = query.isBlank() ||
            (note.title + " " + note.body).contains(query.trim(), ignoreCase = true)
        val matchesFilter = when (filter) {
            "pinned" -> note.pinned
            "week" -> note.updated >= weekStart
            else -> true
        }
        matchesQuery && matchesFilter
    }.sortedWith(compareByDescending<NoteItem> { it.pinned }.thenByDescending { it.updated })

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Notes",
                subtitle = "${notes.size} notes Â· ${notes.count { it.pinned }} pinned",
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
                            Icon(Icons.Default.Add, contentDescription = "New note", tint = Color.White, modifier = Modifier.size(21.dp))
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
                StatTile("Notes", "${notes.size}", "In the notebook", Modifier.weight(1f))
                StatTile("Pinned", "${notes.count { it.pinned }}", "Kept near the top", Modifier.weight(1f))
                StatTile("Words", Zone.num(notes.sumOf { Zone.words(it.body) }), "Across every note", Modifier.weight(1f))
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                SearchField(query, { query = it }, placeholder = "Search notes")
            }
        }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "pinned" to "Pinned", "week" to "This week").forEach { (value, label) ->
                    item {
                        FilterChip(
                            selected = filter == value,
                            onClick = { filter = value },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }

        if (notes.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Edit,
                    title = "Nothing written yet",
                    message = "A note does not need to be finished. Put down the useful part and leave.",
                    actionLabel = "Write the first note",
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
                    icon = Icons.Default.Edit,
                    title = "No notes match",
                    message = "Try a different word or a wider filter.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                )
            }
        } else {
            items(filtered) { note ->
                NoteCard(
                    note = note,
                    onClick = { viewing = note },
                    onPin = {
                        val next = notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned) else it }
                        Zone.setNotes(next, if (note.pinned) "Note unpinned" else "Note pinned", "edit", audit = false)
                    },
                    onEdit = {
                        editing = note
                        showEditor = true
                    },
                    onDelete = { deleteTarget = note }
                )
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            SecondaryButton(
                text = "Export notebook (Markdown)",
                onClick = { shareText(context, notesMarkdown(notes), "Zone notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }

    if (showEditor) {
        NoteEditor(
            initial = editing,
            onDismiss = {
                showEditor = false
                editing = null
            },
            onSave = { note ->
                val current = Zone.notes.value
                val next = if (current.any { it.id == note.id }) {
                    current.map { if (it.id == note.id) note else it }
                } else {
                    current + note
                }
                Zone.setNotes(next, if (editing == null) "Note saved" else "Note updated", if (editing == null) "add" else "edit")
                showEditor = false
                editing = null
                toast(context, if (editing == null) "Note saved." else "Note updated.")
            }
        )
    }

    viewing?.let { note ->
        NoteViewer(
            note = note,
            onClose = { viewing = null },
            onPin = {
                val next = notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned) else it }
                Zone.setNotes(next, if (note.pinned) "Note unpinned" else "Note pinned", "edit", audit = false)
                viewing = next.first { it.id == note.id }
            },
            onEdit = {
                viewing = null
                editing = note
                showEditor = true
            }
        )
    }

    deleteTarget?.let { note ->
        ConfirmDialog(
            title = "Throw this note away?",
            message = "Delete \"${note.title}\" and its ${Zone.words(note.body)} words? This cannot be undone.",
            confirmLabel = "Delete note",
            onConfirm = {
                Zone.setNotes(Zone.notes.value.filter { it.id != note.id }, "Note deleted", "delete")
                deleteTarget = null
                toast(context, "Note deleted.")
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun NoteCard(
    note: NoteItem,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = noteColorValues[note.color] ?: noteColorValues["plain"]!!
    ZoneCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        padding = 16.dp,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Zone.medium(note.updated),
                        style = MaterialTheme.typography.labelSmall,
                        color = ZoneColors.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.pinned) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = ZoneColors.orange, modifier = Modifier.size(14.dp))
                    }
                }
                Text(
                    note.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    note.body.ifBlank { "No words yet." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZoneColors.secondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${Zone.words(note.body)} words",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZoneColors.secondary
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onPin, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pin", tint = if (note.pinned) ZoneColors.orange else ZoneColors.secondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ZoneColors.secondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditor(
    initial: NoteItem?,
    onDismiss: () -> Unit,
    onSave: (NoteItem) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: "plain") }
    var error by remember { mutableStateOf("") }

    ZoneDialog(
        title = if (initial == null) "New note" else "Edit note",
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Save note" else "Save changes",
        onConfirm = {
            val cleanTitle = title.trim()
            val cleanBody = body.trim()
            when {
                cleanTitle.isEmpty() -> error = "Give the note a title."
                cleanBody.isEmpty() -> error = "Write something in the note."
                else -> onSave(
                    NoteItem(
                        id = initial?.id ?: Zone.uid("note"),
                        title = cleanTitle,
                        body = cleanBody,
                        color = color,
                        pinned = initial?.pinned ?: false,
                        created = initial?.created ?: Zone.todayISO(),
                        updated = Zone.todayISO()
                    )
                )
            }
        }
    ) {
        LabeledField("Title", title, { title = it }, placeholder = "A thought worth keeping")
        LabeledField("Note", body, { body = it }, placeholder = "Start with the useful part.", minLines = 6)
        Text("Paper", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            noteColors.forEach { option ->
                FilterChip(
                    selected = color == option,
                    onClick = { color = option },
                    label = { Text(noteColorLabels[option] ?: option) }
                )
            }
        }
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun NoteViewer(
    note: NoteItem,
    onClose: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = {
            Column {
                Text(note.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${Zone.medium(note.updated)} Â· ${noteColorLabels[note.color] ?: "Plain"} paper Â· ${Zone.words(note.body)} words",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZoneColors.secondary
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    note.body.ifBlank { "No words yet." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (note.body.isBlank()) ZoneColors.secondary else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Close", color = ZoneColors.blue) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onPin) { Text(if (note.pinned) "Unpin" else "Pin", color = ZoneColors.orange) }
                TextButton(onClick = onEdit) { Text("Edit", color = ZoneColors.blue) }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp)
    )
}

private fun notesMarkdown(notes: List<NoteItem>): String {
    val lines = mutableListOf("# Zone notebook", "", "Exported ${Zone.medium(Zone.todayISO())} Â· ${notes.size} notes", "")
    notes.sortedWith(compareByDescending<NoteItem> { it.pinned }.thenByDescending { it.updated }).forEach { note ->
        lines.add("## ${note.title}")
        lines.add("")
        lines.add("_${Zone.medium(note.updated)} Â· ${noteColorLabels[note.color] ?: "Plain"} paper${if (note.pinned) " Â· pinned" else ""}_")
        lines.add("")
        lines.add(note.body.trim().ifEmpty { "_(No words yet.)_" })
        lines.add("")
        lines.add("---")
        lines.add("")
    }
    return lines.joinToString("\n")
}