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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

private val watchStatuses = listOf("Queued", "Watching", "Watched", "Dropped")

@Composable
fun WatchlistScreen() {
    val context = LocalContext.current
    val items = Zone.watchlist.value
    var statusFilter by remember { mutableStateOf("All") }
    var typeFilter by remember { mutableStateOf("All") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WatchItem?>(null) }
    var viewing by remember { mutableStateOf<WatchItem?>(null) }
    var deleteTarget by remember { mutableStateOf<WatchItem?>(null) }

    val queued = items.filter { it.status == "Queued" }
    val watching = items.filter { it.status == "Watching" }
    val finished = items.filter { it.status == "Watched" }
    val rated = items.filter { it.rating > 0 }
    val avgRating = if (rated.isNotEmpty()) rated.sumOf { it.rating } / rated.size else 0.0
    val pick = queued.sortedWith(compareBy({ it.added }, { it.title })).firstOrNull()

    val filtered = items.filter { item ->
        val statusOk = statusFilter == "All" || item.status == statusFilter
        val typeOk = typeFilter == "All" || item.type == typeFilter
        val favOk = !favoritesOnly || item.favorite
        val searchOk = query.isBlank() ||
            (item.title + " " + item.genre + " " + item.service + " " + item.note + " " + item.type + " " + (item.year?.toString() ?: ""))
                .contains(query.trim(), ignoreCase = true)
        statusOk && typeOk && favOk && searchOk
    }.sortedWith(compareByDescending<WatchItem> { it.favorite }.thenByDescending { it.added })

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Watchlist",
                subtitle = "${items.size} titles Â· ${queued.size} waiting Â· ${watching.size} in rotation",
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
                            Icon(Icons.Default.Add, contentDescription = "Add title", tint = Color.White, modifier = Modifier.size(21.dp))
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
                StatTile("In queue", "${queued.size}", "Waiting to start", Modifier.weight(1f))
                StatTile("In rotation", "${watching.size}", "Currently watching", Modifier.weight(1f))
                StatTile("Finished", "${finished.size}", "${if (items.isNotEmpty()) finished.size * 100 / items.size else 0}% of shelf", Modifier.weight(1f))
                StatTile("Avg rating", if (avgRating > 0) "${"%.1f".format(avgRating)}" else "â€”", "Out of 10", Modifier.weight(1f))
            }
        }

        if (pick != null) {
            item {
                ZoneCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    padding = 16.dp
                ) {
                    Text("Tonight's pick", style = MaterialTheme.typography.labelSmall, color = ZoneColors.blue)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Monogram(pick.title, size = 54.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pick.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${pick.type}${pick.year?.let { " Â· $it" } ?: ""}${if (pick.genre.isNotBlank()) " Â· ${pick.genre}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZoneColors.secondary
                            )
                            Text(
                                "Added ${Zone.rel(pick.added)}${if (pick.service.isNotBlank()) " Â· ${pick.service}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZoneColors.secondary
                            )
                        }
                    }
                    if (pick.note.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(pick.note, style = MaterialTheme.typography.bodyMedium, color = ZoneColors.secondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row {
                        PrimaryButton(
                            text = "Start watching",
                            onClick = { updateStatus(pick, "Watching", context) },
                            icon = Icons.Default.PlayArrow
                        )
                        Spacer(Modifier.width(8.dp))
                        SecondaryButton("Details", { viewing = pick })
                    }
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
                listOf("All", "Queued", "Watching", "Watched", "Dropped").forEach { status ->
                    FilterChip(selected = statusFilter == status, onClick = { statusFilter = status }, label = { Text(status) })
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchField(query, { query = it }, modifier = Modifier.weight(1f), placeholder = "Search titles")
                FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("Favorites") })
            }
        }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Film", "Series").forEach { type ->
                    item {
                        FilterChip(selected = typeFilter == type, onClick = { typeFilter = type }, label = { Text(type) })
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Movie,
                    title = if (items.isEmpty()) "The shelf is empty" else "Nothing matches",
                    message = if (items.isEmpty()) "Add the first film or series you want to watch." else "Try another status, type, or search term.",
                    actionLabel = if (items.isEmpty()) "Add a title" else null,
                    onAction = if (items.isEmpty()) {
                        {
                            editing = null
                            showEditor = true
                        }
                    } else null,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
        } else {
            items(filtered) { item ->
                WatchCard(
                    item = item,
                    onClick = { viewing = item },
                    onPrimary = { primaryAction(item, context) },
                    onFavorite = { toggleFavorite(item, context) },
                    onEdit = {
                        editing = item
                        showEditor = true
                    },
                    onDelete = { deleteTarget = item }
                )
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            SecondaryButton(
                text = "Export shelf (CSV)",
                onClick = { shareText(context, watchlistCsv(items), "Zone watchlist") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }

    if (showEditor) {
        WatchEditor(
            initial = editing,
            onDismiss = {
                showEditor = false
                editing = null
            },
            onSave = { item ->
                val next = if (items.any { it.id == item.id }) {
                    items.map { if (it.id == item.id) item else it }
                } else {
                    items + item
                }
                Zone.setWatchlist(next, if (editing == null) "Title added" else "Title updated", if (editing == null) "add" else "edit")
                showEditor = false
                editing = null
                toast(context, if (editing == null) "Title saved." else "Title updated.")
            }
        )
    }

    viewing?.let { item ->
        WatchDetail(
            item = item,
            onClose = { viewing = null },
            onPrimary = { primaryAction(item, context) },
            onFavorite = {
                val next = items.map { if (it.id == item.id) it.copy(favorite = !it.favorite) else it }
                Zone.setWatchlist(next, if (item.favorite) "Removed from favorites" else "Added to favorites", "edit", audit = false)
                viewing = next.first { it.id == item.id }
            },
            onEdit = {
                viewing = null
                editing = item
                showEditor = true
            }
        )
    }

    deleteTarget?.let { item ->
        ConfirmDialog(
            title = "Remove ${item.title}?",
            message = "This removes the title and its progress from the shelf.",
            confirmLabel = "Remove title",
            onConfirm = {
                Zone.setWatchlist(items.filter { it.id != item.id }, "Title removed", "delete")
                deleteTarget = null
                toast(context, "Title removed.")
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun WatchCard(
    item: WatchItem,
    onClick: () -> Unit,
    onPrimary: () -> Unit,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val total = totalOf(item)
    val watched = watchedOf(item)
    val progress = if (total > 0) watched.toFloat() / total else 0f
    ZoneCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        padding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Monogram(item.title, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.favorite) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = ZoneColors.red, modifier = Modifier.size(15.dp))
                    }
                }
                Text(
                    "${item.type}${item.year?.let { " Â· $it" } ?: ""}${if (item.genre.isNotBlank()) " Â· ${item.genre}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZoneColors.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                if (total > 0) {
                    ZoneProgressBar(progress)
                    Spacer(Modifier.height(4.dp))
                    Text("$watched of $total episodes", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                } else {
                    Text(item.status, style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (item.rating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = ZoneColors.orange, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("${"%.1f".format(item.rating)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text(item.status, style = MaterialTheme.typography.labelSmall, color = statusColor(item.status))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            PrimaryButton(
                text = primaryLabel(item),
                onClick = onPrimary,
                enabled = item.status != "Watched" && item.status != "Dropped"
            )
            Spacer(Modifier.width(8.dp))
            SecondaryButton("Details", onClick)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onFavorite, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = if (item.favorite) ZoneColors.red else ZoneColors.secondary, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ZoneColors.secondary, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun WatchDetail(
    item: WatchItem,
    onClose: () -> Unit,
    onPrimary: () -> Unit,
    onFavorite: () -> Unit,
    onEdit: () -> Unit
) {
    val total = totalOf(item)
    val watched = watchedOf(item)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = {
            Column {
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${item.type}${item.year?.let { " Â· $it" } ?: ""}${if (item.genre.isNotBlank()) " Â· ${item.genre}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZoneColors.secondary
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (total > 0) {
                    ZoneProgressBar(if (total > 0) watched.toFloat() / total else 0f)
                    Spacer(Modifier.height(4.dp))
                    Text("$watched of $total episodes Â· ${item.status}", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                }
                if (item.service.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Where: ${item.service}", style = MaterialTheme.typography.bodyMedium)
                }
                if (item.note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(item.note, style = MaterialTheme.typography.bodyMedium, color = ZoneColors.secondary)
                }
                if (item.rating > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Rating: ${"%.1f".format(item.rating)} / 10", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text("Added ${Zone.medium(item.added)}", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
            }
        },
        confirmButton = {
            PrimaryButton(primaryLabel(item), {
                onPrimary()
                onClose()
            }, enabled = item.status != "Watched" && item.status != "Dropped")
        },
        dismissButton = {
            Row {
                TextButton(onClick = onFavorite) { Text(if (item.favorite) "Unfavorite" else "Favorite", color = ZoneColors.orange) }
                TextButton(onClick = {
                    onClose()
                    onEdit()
                }) { Text("Edit", color = ZoneColors.blue) }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun WatchEditor(
    initial: WatchItem?,
    onDismiss: () -> Unit,
    onSave: (WatchItem) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "Film") }
    var year by remember { mutableStateOf(initial?.year?.toString() ?: "") }
    var genre by remember { mutableStateOf(initial?.genre ?: "") }
    var status by remember { mutableStateOf(initial?.status ?: "Queued") }
    var service by remember { mutableStateOf(initial?.service ?: "") }
    var rating by remember { mutableStateOf(initial?.rating?.takeIf { it > 0 }?.toString() ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var favorite by remember { mutableStateOf(initial?.favorite ?: false) }
    var seasons by remember {
        mutableStateOf(
            initial?.seasons?.takeIf { it.isNotEmpty() }
                ?: if ((initial?.type ?: "Film") == "Series") listOf(Season(1, 0, 0)) else emptyList()
        )
    }
    var error by remember { mutableStateOf("") }

    ZoneDialog(
        title = if (initial == null) "Add a title" else "Edit title",
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Save title" else "Save changes",
        onConfirm = {
            val cleanTitle = title.trim()
            if (cleanTitle.isEmpty()) {
                error = "Give the title a name."
                return@ZoneDialog
            }
            val parsedYear = year.toIntOrNull()
            if (parsedYear != null && (parsedYear < 1900 || parsedYear > 2100)) {
                error = "Year must be between 1900 and 2100."
                return@ZoneDialog
            }
            val parsedRating = rating.toDoubleOrNull()?.coerceIn(0.0, 10.0)
            val cleanSeasons = seasons.filter { it.episodes > 0 }.mapIndexed { index, season ->
                season.copy(number = index + 1, watched = season.watched.coerceIn(0, season.episodes))
            }
            onSave(
                WatchItem(
                    id = initial?.id ?: Zone.uid("watch"),
                    title = cleanTitle.take(120),
                    type = if (type == "Series") "Series" else "Film",
                    year = parsedYear,
                    genre = genre.trim(),
                    status = status,
                    rating = parsedRating ?: 0.0,
                    watched = if (type == "Series") cleanSeasons.sumOf { it.watched } else (initial?.watched ?: 0),
                    total = if (type == "Series") cleanSeasons.sumOf { it.episodes } else (initial?.total ?: 0),
                    service = service.trim(),
                    favorite = favorite,
                    note = note.trim(),
                    added = initial?.added ?: Zone.todayISO(),
                    seasons = cleanSeasons
                )
            )
        }
    ) {
        LabeledField("Title", title, { title = it }, placeholder = "Film or series name")
        Segmented(
            options = listOf("Film", "Series"),
            selected = type,
            label = { it },
            onSelect = {
                type = it
                if (it == "Series" && seasons.isEmpty()) seasons = listOf(Season(1, 0, 0))
            }
        )
        LabeledField("Year", year, { year = it.filter { c -> c.isDigit() } }, placeholder = "2024", keyboardType = KeyboardType.Number)
        LabeledField("Genre", genre, { genre = it }, placeholder = "Drama, documentaryâ€¦")
        Text("Status", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(watchStatuses) { option ->
                FilterChip(selected = status == option, onClick = { status = option }, label = { Text(option) })
            }
        }
        LabeledField("Where to watch", service, { service = it }, placeholder = "Streaming service")
        LabeledField("Rating (0â€“10)", rating, { rating = it.filter { c -> c.isDigit() || c == '.' } }, placeholder = "0", keyboardType = KeyboardType.Number)
        if (type == "Series") {
            Text("Seasons", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
            seasons.forEachIndexed { index, season ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("S${index + 1}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(36.dp))
                    OutlinedTextField(
                        value = season.episodes.takeIf { it > 0 }?.toString() ?: "",
                        onValueChange = { value ->
                            val episodes = value.toIntOrNull()?.coerceAtLeast(0) ?: 0
                            seasons = seasons.mapIndexed { i, item -> if (i == index) item.copy(episodes = episodes, watched = item.watched.coerceAtMost(episodes)) else item }
                        },
                        placeholder = { Text("Episodes") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(
                        value = season.watched.toString(),
                        onValueChange = { value ->
                            val watched = value.toIntOrNull()?.coerceAtLeast(0) ?: 0
                            seasons = seasons.mapIndexed { i, item -> if (i == index) item.copy(watched = item.watched.coerceAtMost(item.episodes)) else item }
                        },
                        placeholder = { Text("Watched") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    IconButton(onClick = {
                        if (seasons.size > 1) seasons = seasons.filterIndexed { i, _ -> i != index }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove season", tint = ZoneColors.red, modifier = Modifier.size(15.dp))
                    }
                }
            }
            TextButton(onClick = { seasons = seasons + Season(seasons.size + 1, 0, 0) }) {
                Text("+ Add season", color = ZoneColors.blue)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = favorite, onClick = { favorite = !favorite }, label = { Text(if (favorite) "Favorited" else "Favorite") })
        }
        LabeledField("Note", note, { note = it }, placeholder = "Why it is on the list", minLines = 2)
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun totalOf(item: WatchItem): Int {
    if (item.type == "Series" && item.seasons.isNotEmpty()) return item.seasons.sumOf { it.episodes }
    return item.total
}

private fun watchedOf(item: WatchItem): Int {
    if (item.type == "Series" && item.seasons.isNotEmpty()) return item.seasons.sumOf { it.watched }
    return item.watched
}

private fun statusColor(status: String): Color = when (status) {
    "Watching" -> ZoneColors.blue
    "Watched" -> ZoneColors.green
    "Dropped" -> ZoneColors.secondary
    else -> ZoneColors.orange
}

private fun primaryLabel(item: WatchItem): String = when (item.status) {
    "Queued" -> "Start watching"
    "Watching" -> {
        if (item.type == "Series" && item.seasons.isNotEmpty()) {
            val next = nextEpisode(item)
            if (next != null) "Log S${next.first}E${next.second}" else "Next episode"
        } else "Log episode"
    }
    "Watched" -> "Watched"
    else -> "Requeue"
}

private fun nextEpisode(item: WatchItem): Pair<Int, Int>? {
    if (item.type != "Series" || item.seasons.isEmpty()) return null
    item.seasons.forEach { season ->
        if (season.watched < season.episodes) return season.number to (season.watched + 1)
    }
    return null
}

private fun primaryAction(item: WatchItem, context: android.content.Context) {
    when (item.status) {
        "Queued" -> updateStatus(item, "Watching", context)
        "Watching" -> {
            if (item.type == "Series" && item.seasons.isNotEmpty()) {
                val next = nextEpisode(item)
                if (next != null) {
                    val updated = item.seasons.map { season ->
                        if (season.number == next.first) season.copy(watched = (season.watched + 1).coerceAtMost(season.episodes)) else season
                    }
                    val allDone = updated.all { it.watched >= it.episodes }
                    val nextItem = item.copy(
                        seasons = updated,
                        watched = updated.sumOf { it.watched },
                        total = updated.sumOf { it.episodes },
                        status = if (allDone) "Watched" else "Watching"
                    )
                    saveWatch(nextItem, "Episode logged", context)
                }
            } else {
                val nextItem = item.copy(watched = item.watched + 1)
                saveWatch(nextItem, "Episode logged", context)
            }
        }
        "Dropped" -> updateStatus(item, "Queued", context)
        "Watched" -> {
            if (item.type == "Series" && item.seasons.isNotEmpty()) {
                val updated = item.seasons.map { it.copy(watched = it.episodes) }
                saveWatch(item.copy(seasons = updated, watched = updated.sumOf { it.watched }, total = updated.sumOf { it.episodes }), "Marked finished", context)
            } else {
                updateStatus(item, "Watched", context)
            }
        }
    }
}

private fun updateStatus(item: WatchItem, status: String, context: android.content.Context) {
    val nextItem = if (status == "Watched" && item.type == "Series" && item.seasons.isNotEmpty()) {
        val updated = item.seasons.map { it.copy(watched = it.episodes) }
        item.copy(seasons = updated, watched = updated.sumOf { it.watched }, total = updated.sumOf { it.episodes }, status = status)
    } else {
        item.copy(status = status)
    }
    saveWatch(nextItem, "Status updated", context)
}

private fun toggleFavorite(item: WatchItem, context: android.content.Context) {
    val next = Zone.watchlist.value.map { if (it.id == item.id) it.copy(favorite = !it.favorite) else it }
    Zone.setWatchlist(next, if (item.favorite) "Removed from favorites" else "Added to favorites", "edit", audit = false)
    toast(context, if (item.favorite) "Removed from favorites." else "Added to favorites.")
}

private fun saveWatch(item: WatchItem, title: String, context: android.content.Context) {
    Zone.setWatchlist(Zone.watchlist.value.map { if (it.id == item.id) item else it }, title, "edit", audit = false)
    toast(context, "Saved.")
}

private fun watchlistCsv(items: List<WatchItem>): String {
    val rows = mutableListOf(listOf("Title", "Type", "Year", "Genre", "Status", "Rating", "Watched", "Total", "Service", "Favorite", "Note", "Added"))
    items.sortedByDescending { it.added }.forEach { item ->
        rows.add(
            listOf(
                item.title,
                item.type,
                item.year?.toString() ?: "",
                item.genre,
                item.status,
                item.rating.toString(),
                watchedOf(item).toString(),
                totalOf(item).toString(),
                item.service,
                if (item.favorite) "yes" else "no",
                item.note,
                item.added
            )
        )
    }
    return csv(rows)
}