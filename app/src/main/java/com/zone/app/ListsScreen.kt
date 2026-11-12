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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private data class RenameTarget(
    val kind: String,
    val listId: String,
    val itemId: String = "",
    val stepId: String = "",
    val value: String = ""
)

@Composable
fun ListsScreen() {
    val context = LocalContext.current
    val lists = Zone.lists.value
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var createTitle by remember { mutableStateOf("") }
    var deleteList by remember { mutableStateOf<CheckList?>(null) }
    var renameTarget by remember { mutableStateOf<RenameTarget?>(null) }
    var deleteItem by remember { mutableStateOf<Pair<String, CheckItem>?>(null) }
    var deleteStep by remember { mutableStateOf<Triple<String, String, CheckStep>?>(null) }

    val selected = lists.firstOrNull { it.id == selectedId }

    if (selected != null) {
        ListDetail(
            list = selected,
            onBack = { selectedId = null },
            onDeleteList = { deleteList = selected },
            onRenameList = { renameTarget = RenameTarget("list", selected.id, value = selected.title) },
            onDeleteItem = { deleteItem = selected.id to it },
            onDeleteStep = { itemId, step -> deleteStep = Triple(selected.id, itemId, step) },
            onRenameItem = { item -> renameTarget = RenameTarget("item", selected.id, item.id, value = item.text) },
            onRenameStep = { itemId, step -> renameTarget = RenameTarget("step", selected.id, itemId, step.id, step.text) }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Lists",
                    subtitle = "${lists.size} lists · ${openItems(lists)} open",
                    action = {
                        Surface(
                            onClick = {
                                createTitle = ""
                                showCreate = true
                            },
                            shape = RoundedCornerShape(50),
                            color = ZoneColors.blue,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "New list", tint = Color.White, modifier = Modifier.size(21.dp))
                            }
                        }
                    }
                )
            }

            item {
                SearchField(
                    query,
                    { query = it },
                    placeholder = "Search lists and items",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (lists.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.List,
                        title = "A blank notebook",
                        message = "A list can hold a trip, a launch, a grocery run, or a reading queue. Start with one line.",
                        actionLabel = "Start a list",
                        onAction = {
                            createTitle = ""
                            showCreate = true
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                }
            } else {
                val filtered = lists.filter {
                    query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) ||
                        it.items.any { item -> item.text.contains(query.trim(), ignoreCase = true) }
                }.sortedByDescending { it.createdAt }
                if (filtered.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.List,
                            title = "No matches",
                            message = "Try a different word or create a new list.",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                        )
                    }
                }
                items(filtered) { list ->
                    val progress = listProgress(list)
                    ZoneCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        padding = 16.dp,
                        onClick = { selectedId = list.id }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(list.title.ifBlank { "Untitled list" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${progress.first} of ${progress.second} done · ${progress.third} open",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZoneColors.secondary
                                )
                            }
                            Text(
                                "${progress.fourth}%",
                                style = MaterialTheme.typography.titleLarge,
                                color = ZoneColors.blue
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        ZoneProgressBar(progress.fourth / 100f)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (list.createdAt.isBlank()) "Undated" else "Started ${Zone.medium(list.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZoneColors.secondary
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        ZoneDialog(
            title = "New list",
            onDismiss = { showCreate = false },
            confirmLabel = "Create list",
            onConfirm = {
                val title = createTitle.trim()
                if (title.isNotEmpty()) {
                    val list = CheckList(
                        id = Zone.uid("list"),
                        title = title.take(80),
                        createdAt = Zone.todayISO(),
                        items = templateItems(title)
                    )
                    Zone.setLists(Zone.lists.value + list, "List created", "add")
                }
                showCreate = false
                createTitle = ""
            }
        ) {
            LabeledField("List name", createTitle, { createTitle = it }, placeholder = "Weekend in the hills")
            Text(
                "Starter: Weekend, Trip, Launch, Groceries, Reading",
                style = MaterialTheme.typography.labelSmall,
                color = ZoneColors.secondary
            )
        }
    }

    renameTarget?.let { target ->
        RenameDialog(
            target = target,
            onDismiss = { renameTarget = null },
            onSave = { value ->
                applyRename(target, value)
                renameTarget = null
            }
        )
    }

    deleteList?.let { list ->
        val progress = listProgress(list)
        ConfirmDialog(
            title = "Delete this list?",
            message = "\"${list.title}\" has ${progress.second} items and ${progress.third} open. This cannot be undone.",
            confirmLabel = "Delete list",
            onConfirm = {
                Zone.setLists(Zone.lists.value.filter { it.id != list.id }, "List deleted", "delete")
                deleteList = null
                toast(context, "List deleted.")
            },
            onDismiss = { deleteList = null }
        )
    }

    deleteItem?.let { (listId, item) ->
        ConfirmDialog(
            title = "Delete item?",
            message = "\"${item.text}\" and its ${item.children.size} steps will be removed.",
            confirmLabel = "Delete item",
            onConfirm = {
                val list = Zone.lists.value.firstOrNull { it.id == listId } ?: return@ConfirmDialog
                val next = list.copy(items = list.items.filter { it.id != item.id })
                Zone.setLists(Zone.lists.value.map { if (it.id == listId) next else it }, "Item deleted", "delete")
                deleteItem = null
                toast(context, "Item deleted.")
            },
            onDismiss = { deleteItem = null }
        )
    }

    deleteStep?.let { (listId, itemId, step) ->
        ConfirmDialog(
            title = "Delete step?",
            message = "\"${step.text}\" will be removed.",
            confirmLabel = "Delete step",
            onConfirm = {
                val list = Zone.lists.value.firstOrNull { it.id == listId } ?: return@ConfirmDialog
                val item = list.items.firstOrNull { it.id == itemId } ?: return@ConfirmDialog
                val nextItem = item.copy(children = item.children.filter { it.id != step.id })
                val nextList = list.copy(items = list.items.map { if (it.id == itemId) nextItem else it })
                Zone.setLists(Zone.lists.value.map { if (it.id == listId) nextList else it }, "Step deleted", "delete")
                deleteStep = null
                toast(context, "Step deleted.")
            },
            onDismiss = { deleteStep = null }
        )
    }
}

@Composable
private fun ListDetail(
    list: CheckList,
    onBack: () -> Unit,
    onDeleteList: () -> Unit,
    onRenameList: () -> Unit,
    onDeleteItem: (CheckItem) -> Unit,
    onDeleteStep: (String, CheckStep) -> Unit,
    onRenameItem: (CheckItem) -> Unit,
    onRenameStep: (String, CheckStep) -> Unit
) {
    val context = LocalContext.current
    var itemText by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var addingStepFor by remember { mutableStateOf<String?>(null) }
    var stepText by remember { mutableStateOf("") }
    val progress = listProgress(list)

    fun commit(next: CheckList, title: String, action: String, audit: Boolean = true) {
        Zone.setLists(Zone.lists.value.map { if (it.id == next.id) next else it }, title, action, audit)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onBack)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(list.title.ifBlank { "Untitled list" }, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${progress.first} of ${progress.second} done · ${progress.fourth}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZoneColors.secondary
                    )
                }
                IconButton(onClick = onRenameList) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = ZoneColors.secondary)
                }
                IconButton(onClick = onDeleteList) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red)
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                ZoneProgressBar(progress.fourth / 100f)
                Spacer(Modifier.height(5.dp))
                Text(
                    if (progress.third == 0) "Everything is crossed off." else "${progress.third} open · ${progress.second - progress.first} remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZoneColors.secondary
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabeledField(
                    "Add a line",
                    itemText,
                    { itemText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "Add a line to this list…"
                )
                Spacer(Modifier.width(8.dp))
                PrimaryButton(
                    text = "Add",
                    onClick = {
                        val text = itemText.trim()
                        if (text.isNotEmpty()) {
                            val item = CheckItem(id = Zone.uid("li"), text = text.take(140), done = false, children = emptyList())
                            commit(list.copy(items = list.items + item), "Item added", "add", audit = false)
                            itemText = ""
                        }
                    },
                    enabled = itemText.isNotBlank(),
                    modifier = Modifier.padding(top = 18.dp)
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "open" to "Open", "done" to "Done").forEach { (value, label) ->
                    FilterChip(
                        selected = filter == value,
                        onClick = { filter = value },
                        label = { Text(label) }
                    )
                }
            }
        }

        val visibleItems = list.items.filter { item ->
            when (filter) {
                "open" -> !item.done
                "done" -> item.done
                else -> true
            }
        }

        if (visibleItems.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.List,
                    title = if (list.items.isEmpty()) "A blank page" else "Nothing in this view",
                    message = if (list.items.isEmpty()) "Add the first line. Small steps, kept in order." else "Try another filter or add a new line.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
        }

        items(visibleItems) { item ->
            val doneSteps = item.children.count { it.done }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CheckMark(done = item.done) {
                        val nextDone = !item.done
                        val nextItem = item.copy(
                            done = nextDone,
                            children = if (item.children.isNotEmpty()) item.children.map { it.copy(done = nextDone) } else item.children
                        )
                        commit(list.copy(items = list.items.map { if (it.id == item.id) nextItem else it }), "Item checked", "edit", audit = false)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (item.done) ZoneColors.secondary else MaterialTheme.colorScheme.onSurface
                        )
                        if (item.children.isNotEmpty()) {
                            Text(
                                "$doneSteps of ${item.children.size} steps",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZoneColors.secondary
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            addingStepFor = if (addingStepFor == item.id) null else item.id
                            stepText = ""
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add step", tint = ZoneColors.blue, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = { onRenameItem(item) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = ZoneColors.secondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { onDeleteItem(item) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red, modifier = Modifier.size(15.dp))
                    }
                }

                if (item.children.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Column(modifier = Modifier.padding(start = 36.dp)) {
                        item.children.forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CheckMark(done = step.done) {
                                    val nextChildren = item.children.map {
                                        if (it.id == step.id) it.copy(done = !it.done) else it
                                    }
                                    val nextItem = item.copy(
                                        children = nextChildren,
                                        done = nextChildren.isNotEmpty() && nextChildren.all { it.done }
                                    )
                                    commit(list.copy(items = list.items.map { if (it.id == item.id) nextItem else it }), "Step checked", "edit", audit = false)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    step.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (step.done) ZoneColors.secondary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onRenameStep(item.id, step) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = ZoneColors.secondary, modifier = Modifier.size(13.dp))
                                }
                                IconButton(onClick = { onDeleteStep(item.id, step) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }

                if (addingStepFor == item.id) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabeledField(
                            "New step",
                            stepText,
                            { stepText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "A smaller step…"
                        )
                        Spacer(Modifier.width(8.dp))
                        PrimaryButton(
                            text = "Add",
                            onClick = {
                                val text = stepText.trim()
                                if (text.isNotEmpty()) {
                                    val step = CheckStep(id = Zone.uid("lc"), text = text.take(140), done = false)
                                    val nextItem = item.copy(children = item.children + step, done = false)
                                    commit(list.copy(items = list.items.map { if (it.id == item.id) nextItem else it }), "Step added", "add", audit = false)
                                    stepText = ""
                                    addingStepFor = null
                                }
                            },
                            enabled = stepText.isNotBlank(),
                            modifier = Modifier.padding(top = 18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun openItems(lists: List<CheckList>): Int {
    var count = 0
    lists.forEach { list ->
        list.items.forEach { item ->
            if (!item.done) count++
            item.children.forEach { step -> if (!step.done) count++ }
        }
    }
    return count
}

private data class Progress(
    val first: Int,
    val second: Int,
    val third: Int,
    val fourth: Int
)

private fun listProgress(list: CheckList): Progress {
    var total = 0
    var done = 0
    list.items.forEach { item ->
        total++
        if (item.done) done++
        item.children.forEach { step ->
            total++
            if (step.done) done++
        }
    }
    val open = total - done
    val pct = if (total > 0) done * 100 / total else 0
    return Progress(done, total, open, pct)
}

private fun templateItems(title: String): List<CheckItem> {
    val key = title.lowercase()
    val texts = when {
        "weekend" in key -> listOf("Pick a place", "Book a stay", "Pack a bag", "Plan the route")
        "trip" in key || "travel" in key -> listOf("Book travel", "Reserve a stay", "Check documents", "Pack")
        "launch" in key || "work" in key -> listOf("Scope", "Design", "Build", "Review", "Ship")
        "grocery" in key || "groceries" in key -> listOf("Fruit and vegetables", "Milk and eggs", "Bread", "Coffee")
        "read" in key -> listOf("Start the book", "Take notes", "Finish a chapter")
        else -> return emptyList()
    }
    return texts.map { CheckItem(id = Zone.uid("li"), text = it, done = false, children = emptyList()) }
}

@Composable
private fun RenameDialog(
    target: RenameTarget,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(target) { mutableStateOf(target.value) }
    ZoneDialog(
        title = "Rename ${target.kind}",
        onDismiss = onDismiss,
        confirmLabel = "Save",
        onConfirm = {
            if (value.trim().isNotEmpty()) onSave(value.trim())
            else onDismiss()
        }
    ) {
        LabeledField("Name", value, { value = it }, placeholder = "Untitled")
    }
}

private fun applyRename(target: RenameTarget, value: String) {
    when (target.kind) {
        "list" -> {
            val list = Zone.lists.value.firstOrNull { it.id == target.listId } ?: return
            val next = list.copy(title = value.take(80))
            Zone.setLists(Zone.lists.value.map { if (it.id == target.listId) next else it }, "List renamed", "edit")
        }
        "item" -> {
            val list = Zone.lists.value.firstOrNull { it.id == target.listId } ?: return
            val item = list.items.firstOrNull { it.id == target.itemId } ?: return
            val nextItem = item.copy(text = value.take(140))
            val next = list.copy(items = list.items.map { if (it.id == target.itemId) nextItem else it })
            Zone.setLists(Zone.lists.value.map { if (it.id == target.listId) next else it }, "Item renamed", "edit")
        }
        "step" -> {
            val list = Zone.lists.value.firstOrNull { it.id == target.listId } ?: return
            val item = list.items.firstOrNull { it.id == target.itemId } ?: return
            val step = item.children.firstOrNull { it.id == target.stepId } ?: return
            val nextStep = step.copy(text = value.take(140))
            val nextItem = item.copy(children = item.children.map { if (it.id == target.stepId) nextStep else it })
            val next = list.copy(items = list.items.map { if (it.id == target.itemId) nextItem else it })
            Zone.setLists(Zone.lists.value.map { if (it.id == target.listId) next else it }, "Step renamed", "edit")
        }
    }
}