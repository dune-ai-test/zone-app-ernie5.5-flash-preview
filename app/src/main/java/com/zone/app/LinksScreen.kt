package com.zone.app

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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
fun LinksScreen() {
    val context = LocalContext.current
    val links = Zone.links.value
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var activeTag by remember { mutableStateOf("all") }
    var sort by remember { mutableStateOf("newest") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LinkItem?>(null) }
    var deleteTarget by remember { mutableStateOf<LinkItem?>(null) }

    val allTags = links.flatMap { it.tags }.distinct().sorted()
    val filtered = links.filter { link ->
        val matchesQuery = query.isBlank() ||
            (link.title + " " + link.url + " " + link.note + " " + link.tags.joinToString(" ") + " " + domainOf(link.url))
                .contains(query.trim(), ignoreCase = true)
        val matchesFavorite = !favoritesOnly || link.favorite
        val matchesTag = activeTag == "all" || link.tags.contains(activeTag)
        matchesQuery && matchesFavorite && matchesTag
    }.sortedWith(
        when (sort) {
            "oldest" -> compareBy { it.saved }
            "title" -> compareBy { it.title.lowercase() }
            "domain" -> compareBy { domainOf(it.url) }
            else -> compareByDescending { it.saved }
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Links",
                subtitle = "${links.size} corners Â· ${allTags.size} subjects",
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
                            Icon(Icons.Default.Add, contentDescription = "Add link", tint = Color.White, modifier = Modifier.size(21.dp))
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
                StatTile("Saved", "${links.size}", "In the library", Modifier.weight(1f))
                StatTile("This month", "${links.count { Zone.monthKey(it.saved) == Zone.currentMonth() }}", "Newly saved", Modifier.weight(1f))
                StatTile("Subjects", "${allTags.size}", if (allTags.isNotEmpty()) allTags.first() else "No tags yet", Modifier.weight(1f))
                StatTile("Favorites", "${links.count { it.favorite }}", "Kept close", Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchField(query, { query = it }, modifier = Modifier.weight(1f), placeholder = "Search links")
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
                item {
                    FilterChip(selected = activeTag == "all", onClick = { activeTag = "all" }, label = { Text("All subjects") })
                }
                items(allTags) { tag ->
                    FilterChip(selected = activeTag == tag, onClick = { activeTag = tag }, label = { Text("#$tag") })
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
                listOf("newest" to "Newest", "oldest" to "Oldest", "title" to "Aâ€“Z", "domain" to "Domain").forEach { (value, label) ->
                    item {
                        FilterChip(selected = sort == value, onClick = { sort = value }, label = { Text(label) })
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Link,
                    title = if (links.isEmpty()) "An empty shelf" else "No corners match",
                    message = if (links.isEmpty()) "Save the first good corner of the web." else "Try another word, subject, or clear the filters.",
                    actionLabel = if (links.isEmpty()) "Add a link" else null,
                    onAction = if (links.isEmpty()) {
                        {
                            editing = null
                            showEditor = true
                        }
                    } else null,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
        } else {
            items(filtered) { link ->
                LinkCard(
                    link = link,
                    onOpen = { openUrl(context, link.url) },
                    onCopy = { copyText(context, link.url) },
                    onFavorite = {
                        val next = links.map { if (it.id == link.id) it.copy(favorite = !it.favorite) else it }
                        Zone.setLinks(next, if (link.favorite) "Removed from favorites" else "Added to favorites", "edit", audit = false)
                    },
                    onEdit = {
                        editing = link
                        showEditor = true
                    },
                    onDelete = { deleteTarget = link }
                )
            }
        }
    }

    if (showEditor) {
        LinkEditor(
            initial = editing,
            allTags = allTags,
            onDismiss = {
                showEditor = false
                editing = null
            },
            onSave = { link ->
                val next = if (links.any { it.id == link.id }) {
                    links.map { if (it.id == link.id) link else it }
                } else {
                    links + link
                }
                Zone.setLinks(next, if (editing == null) "Link saved" else "Link updated", if (editing == null) "add" else "edit")
                showEditor = false
                editing = null
                toast(context, if (editing == null) "Link saved." else "Link updated.")
            }
        )
    }

    deleteTarget?.let { link ->
        ConfirmDialog(
            title = "Let this one go?",
            message = "Delete \"${link.title}\" and its link from the shelf?",
            confirmLabel = "Delete link",
            onConfirm = {
                Zone.setLinks(links.filter { it.id != link.id }, "Link deleted", "delete")
                deleteTarget = null
                toast(context, "Link deleted.")
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun LinkCard(
    link: LinkItem,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ZoneCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        padding = 15.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Monogram(domainOf(link.url), size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(link.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(domainOf(link.url), style = MaterialTheme.typography.labelSmall, color = ZoneColors.blue, maxLines = 1)
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = if (link.favorite) ZoneColors.red else ZoneColors.secondary, modifier = Modifier.size(16.dp))
            }
        }
        if (link.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(link.note, style = MaterialTheme.typography.bodyMedium, color = ZoneColors.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (link.tags.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(link.tags.joinToString(" ") { "#$it" }, style = MaterialTheme.typography.labelSmall, color = ZoneColors.blue, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Zone.rel(link.saved), style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpen, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = ZoneColors.blue, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ZoneColors.secondary, modifier = Modifier.size(16.dp))
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
private fun LinkEditor(
    initial: LinkItem?,
    allTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (LinkItem) -> Unit
) {
    var url by remember { mutableStateOf(initial?.url ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var tags by remember { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var favorite by remember { mutableStateOf(initial?.favorite ?: false) }
    var error by remember { mutableStateOf("") }

    ZoneDialog(
        title = if (initial == null) "Add link" else "Edit link",
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Save link" else "Save changes",
        onConfirm = {
            val cleanUrl = normalizeUrl(url)
            if (cleanUrl == null) {
                error = "That does not look like a web address."
                return@ZoneDialog
            }
            if (title.trim().isEmpty()) {
                error = "Give the link a title."
                return@ZoneDialog
            }
            val cleanTags = tags.split(",")
                .map { it.trim().removePrefix("#").trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
                .take(6)
            onSave(
                LinkItem(
                    id = initial?.id ?: Zone.uid("link"),
                    title = title.trim().take(120),
                    url = cleanUrl,
                    tags = cleanTags,
                    note = note.trim().take(300),
                    favorite = favorite,
                    saved = initial?.saved ?: Zone.todayISO()
                )
            )
        }
    ) {
        LabeledField("URL", url, { url = it }, placeholder = "https://example.com")
        LabeledField("Title", title, { title = it }, placeholder = "Name this corner")
        if (allTags.isNotEmpty()) {
            Text("Saved subjects", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(allTags) { tag ->
                    FilterChip(
                        selected = tags.split(",").map { it.trim().lowercase() }.contains(tag.lowercase()),
                        onClick = {
                            val current = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            tags = if (current.any { it.equals(tag, ignoreCase = true) }) {
                                current.filter { !it.equals(tag, ignoreCase = true) }.joinToString(", ")
                            } else {
                                (current + tag).take(6).joinToString(", ")
                            }
                        },
                        label = { Text("#$tag") }
                    )
                }
            }
        }
        LabeledField("Tags (comma separated, max 6)", tags, { tags = it }, placeholder = "design, reading")
        LabeledField("Note", note, { note = it }, placeholder = "Why it is worth keeping", minLines = 2)
        FilterChip(selected = favorite, onClick = { favorite = !favorite }, label = { Text(if (favorite) "Favorited" else "Favorite") })
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun normalizeUrl(raw: String): String? {
    var value = raw.trim()
    if (value.isEmpty()) return null
    if (!value.startsWith("http://", true) && !value.startsWith("https://", true)) {
        value = "https://$value"
    }
    val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return null
    if (uri.host.isNullOrBlank()) return null
    return value
}

private fun domainOf(url: String): String {
    return runCatching {
        java.net.URI(if (url.startsWith("http") ) url else "https://$url").host
            ?.removePrefix("www.") ?: url
    }.getOrDefault(url)
}