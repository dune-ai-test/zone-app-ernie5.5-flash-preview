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
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

private val splitCategories = listOf("home", "food", "travel", "bills", "fun", "other", "settlement")
private val splitCategoryLabels = mapOf(
    "home" to "Home",
    "food" to "Food",
    "travel" to "Travel",
    "bills" to "Bills",
    "fun" to "Fun",
    "other" to "Other",
    "settlement" to "Settlement"
)

@Composable
fun SplitScreen() {
    val context = LocalContext.current
    val groups = Zone.split.value.groups
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showGroup by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var membersText by remember { mutableStateOf("") }
    var showMember by remember { mutableStateOf(false) }
    var memberName by remember { mutableStateOf("") }
    var showExpense by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var deleteGroup by remember { mutableStateOf<SplitGroup?>(null) }
    var deleteExpense by remember { mutableStateOf<Expense?>(null) }

    val selected = groups.firstOrNull { it.id == selectedId }

    if (selected == null) {
        val total = groups.sumOf { groupTotal(it) }
        val open = groups.sumOf { openToSettle(it) }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Split",
                    subtitle = "${groups.size} tables Â· ${Zone.money(total)} shared",
                    action = {
                        Surface(
                            onClick = {
                                groupName = ""
                                membersText = ""
                                showGroup = true
                            },
                            shape = RoundedCornerShape(50),
                            color = ZoneColors.blue,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "New table", tint = Color.White, modifier = Modifier.size(21.dp))
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
                    StatTile("Shared", Zone.money(total), "Across all tables", Modifier.weight(1f))
                    StatTile("Open to settle", if (open > 0.5) Zone.money(open) else "Settled", "Transfers still due", Modifier.weight(1f))
                }
            }

            if (groups.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.CallSplit,
                        title = "Start with the people",
                        message = "Add a table, name the people at it, then record what was shared.",
                        actionLabel = "Start a table",
                        onAction = {
                            groupName = ""
                            membersText = ""
                            showGroup = true
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                }
            } else {
                items(groups) { group ->
                    val balance = computeGroup(group)
                    ZoneCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        padding = 16.dp,
                        onClick = { selectedId = group.id }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(group.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${group.members.size} people Â· ${group.expenses.size} entries",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZoneColors.secondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(Zone.money(balance.total), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (openToSettle(group) > 0.5) "to settle" else "settled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (openToSettle(group) > 0.5) ZoneColors.orange else ZoneColors.green
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            group.members.joinToString(" Â· ") { it.name },
                            style = MaterialTheme.typography.labelSmall,
                            color = ZoneColors.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    } else {
        GroupDetail(
            group = selected,
            onBack = { selectedId = null },
            onDeleteGroup = { deleteGroup = selected },
            onAddMember = { showMember = true },
            onAddExpense = {
                editingExpense = null
                showExpense = true
            },
            onEditExpense = { expense ->
                editingExpense = expense
                showExpense = true
            },
            onDeleteExpense = { deleteExpense = it }
        )
    }

    if (showGroup) {
        ZoneDialog(
            title = "Start a table",
            onDismiss = { showGroup = false },
            confirmLabel = "Create table",
            onConfirm = {
                val name = groupName.trim()
                val people = membersText.split("\n", ",")
                    .map { it.trim().replace(Regex("\\s+"), " ") }
                    .filter { it.isNotEmpty() }
                    .distinctBy { it.lowercase() }
                if (name.length < 2) {
                    toast(context, "Give the table a name.")
                    return@ZoneDialog
                }
                if (people.size < 2) {
                    toast(context, "Add at least two people.")
                    return@ZoneDialog
                }
                val group = SplitGroup(
                    id = Zone.uid("group"),
                    name = name.take(50),
                    kind = "home",
                    members = people.map { Member(id = Zone.uid("sm"), name = it) },
                    expenses = emptyList()
                )
                Zone.setSplit(SplitData(Zone.split.value.groups + group), "Table created", "add")
                selectedId = group.id
                showGroup = false
                groupName = ""
                membersText = ""
            }
        ) {
            LabeledField("Table name", groupName, { groupName = it }, placeholder = "Flat 4B")
            LabeledField("People (one per line or comma separated)", membersText, { membersText = it }, placeholder = "You\nAsha\nRavi", minLines = 4)
            Text("Two to twelve people. Names are case-insensitive.", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        }
    }

    if (showMember && selected != null) {
        ZoneDialog(
            title = "Add person",
            onDismiss = { showMember = false },
            confirmLabel = "Add person",
            onConfirm = {
                val name = memberName.trim()
                if (name.length < 2) {
                    toast(context, "Name needs at least two characters.")
                    return@ZoneDialog
                }
                if (selected.members.size >= 12) {
                    toast(context, "A table can hold at most 12 people.")
                    return@ZoneDialog
                }
                if (selected.members.any { it.name.equals(name, ignoreCase = true) }) {
                    toast(context, "That person is already at the table.")
                    return@ZoneDialog
                }
                val member = Member(id = Zone.uid("sm"), name = name.take(40))
                val next = selected.copy(members = selected.members + member)
                Zone.setSplit(SplitData(Zone.split.value.groups.map { if (it.id == selected.id) next else it }), "Person added", "add")
                showMember = false
                memberName = ""
            }
        ) {
            LabeledField("Name", memberName, { memberName = it }, placeholder = "Asha")
        }
    }

    if (showExpense && selected != null) {
        ExpenseEditor(
            group = selected,
            initial = editingExpense,
            onDismiss = {
                showExpense = false
                editingExpense = null
            },
            onSave = { expense ->
                val next = if (selected.expenses.any { it.id == expense.id }) {
                    selected.copy(expenses = selected.expenses.map { if (it.id == expense.id) expense else it })
                } else {
                    selected.copy(expenses = selected.expenses + expense)
                }
                Zone.setSplit(SplitData(Zone.split.value.groups.map { if (it.id == selected.id) next else it }), if (editingExpense == null) "Expense added" else "Expense updated", if (editingExpense == null) "add" else "edit")
                showExpense = false
                editingExpense = null
                toast(context, if (editingExpense == null) "Expense recorded." else "Expense updated.")
            }
        )
    }

    deleteGroup?.let { group ->
        ConfirmDialog(
            title = "Delete ${group.name}?",
            message = "This removes the table, its ${group.members.size} people, and ${group.expenses.size} entries.",
            confirmLabel = "Delete table",
            onConfirm = {
                Zone.setSplit(SplitData(Zone.split.value.groups.filter { it.id != group.id }), "Table deleted", "delete")
                selectedId = null
                deleteGroup = null
                toast(context, "Table deleted.")
            },
            onDismiss = { deleteGroup = null }
        )
    }

    deleteExpense?.let { expense ->
        ConfirmDialog(
            title = "Delete expense?",
            message = "Delete \"${expense.description}\" for ${Zone.money(expense.amount)}?",
            confirmLabel = "Delete expense",
            onConfirm = {
                if (selected != null) {
                    val next = selected.copy(expenses = selected.expenses.filter { it.id != expense.id })
                    Zone.setSplit(SplitData(Zone.split.value.groups.map { if (it.id == selected.id) next else it }), "Expense deleted", "delete")
                }
                deleteExpense = null
                toast(context, "Expense deleted.")
            },
            onDismiss = { deleteExpense = null }
        )
    }
}

@Composable
private fun GroupDetail(
    group: SplitGroup,
    onBack: () -> Unit,
    onDeleteGroup: () -> Unit,
    onAddMember: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    val context = LocalContext.current
    val balance = computeGroup(group)
    val plan = planFor(group, balance)
    val youNet = group.members.filter { it.name.trim().equals("you", ignoreCase = true) }.sumOf { balance.net[it.id] ?: 0.0 }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("all") }
    var sort by remember { mutableStateOf("newest") }

    val categories = group.expenses.map { it.category }.distinct().sorted()
    val filtered = group.expenses.filter { expense ->
        val payer = group.members.firstOrNull { it.id == expense.paidBy }?.name ?: ""
        val matchesQuery = search.isBlank() ||
            (expense.description + " " + payer + " " + expense.category + " " + Zone.medium(expense.date) + " " + expense.amount.toInt())
                .contains(search.trim(), ignoreCase = true)
        val matchesCategory = category == "all" || expense.category == category
        matchesQuery && matchesCategory
    }.sortedWith(
        when (sort) {
            "oldest" -> compareBy { it.date }
            "largest" -> compareByDescending { it.amount }
            else -> compareByDescending<Expense> { it.date }.thenByDescending { it.id }
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onBack)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${group.members.size} people Â· ${group.expenses.size} entries",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZoneColors.secondary
                    )
                }
                IconButton(onClick = onDeleteGroup) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete table", tint = ZoneColors.red)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile("Total shared", Zone.money(balance.total), "Non-settlement costs", Modifier.weight(1f))
                StatTile("Your position", if (youNet > 0.005) "+${Zone.money(youNet)}" else if (youNet < -0.005) "âˆ’${Zone.money(-youNet)}" else "Even", "Member named \"you\"", Modifier.weight(1f))
                StatTile("Open to settle", if (plan.isNotEmpty()) Zone.money(plan.sumOf { it.amount }) else "Settled", "${plan.size} transfers", Modifier.weight(1f))
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
                    Text("Settle up", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    SecondaryButton("Add person", onAddMember)
                }
                Spacer(Modifier.height(8.dp))
                if (plan.isEmpty()) {
                    Text("The table is square. Everyone starts even.", style = MaterialTheme.typography.bodyMedium, color = ZoneColors.green)
                } else {
                    plan.forEach { transfer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${memberName(group, transfer.from)} â†’ ${memberName(group, transfer.to)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(Zone.money(transfer.amount), style = MaterialTheme.typography.titleMedium, color = ZoneColors.orange)
                            }
                            SecondaryButton(
                                text = "Settle",
                                onClick = {
                                    settle(group, transfer, context)
                                }
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle("People") }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(group.members) { member ->
                    val net = balance.net[member.id] ?: 0.0
                    FilterChip(
                        selected = false,
                        onClick = {},
                        label = {
                            Text("${member.name} Â· ${if (net > 0.005) "+" else if (net < -0.005) "âˆ’" else ""}${Zone.money(kotlin.math.abs(net))}")
                        }
                    )
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
                SearchField(search, { search = it }, modifier = Modifier.weight(1f), placeholder = "Search entries")
                SecondaryButton("Add", onAddExpense)
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
                    FilterChip(selected = category == "all", onClick = { category = "all" }, label = { Text("All") })
                }
                items(categories) { option ->
                    FilterChip(selected = category == option, onClick = { category = option }, label = { Text(splitCategoryLabels[option] ?: option) })
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
                listOf("newest" to "Newest", "oldest" to "Oldest", "largest" to "Largest").forEach { (value, label) ->
                    FilterChip(selected = sort == value, onClick = { sort = value }, label = { Text(label) })
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.CallSplit,
                    title = if (group.expenses.isEmpty()) "An empty ledger" else "No matching entries",
                    message = if (group.expenses.isEmpty()) "Record the first shared cost." else "Try another category or search term.",
                    actionLabel = if (group.expenses.isEmpty()) "Add an expense" else null,
                    onAction = if (group.expenses.isEmpty()) onAddExpense else null,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
        }

        items(filtered) { expense ->
            val payer = group.members.firstOrNull { it.id == expense.paidBy }?.name ?: "Unknown"
            val shared = expense.sharedWith.mapNotNull { id -> group.members.firstOrNull { it.id == id }?.name }
            ZoneCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 3.dp),
                padding = 14.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(expense.description.ifBlank { "Shared cost" }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${splitCategoryLabels[expense.category] ?: expense.category} Â· paid by $payer Â· ${Zone.medium(expense.date)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZoneColors.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (shared.isNotEmpty()) {
                            Text(
                                shared.joinToString(", "),
                                style = MaterialTheme.typography.labelSmall,
                                color = ZoneColors.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(Zone.money(expense.amount), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { onEditExpense(expense) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ZoneColors.secondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { onDeleteExpense(expense) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseEditor(
    group: SplitGroup,
    initial: Expense?,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    val context = LocalContext.current
    val defaultCategory = when (group.kind) {
        "trip" -> "travel"
        "work" -> "food"
        else -> "home"
    }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.takeIf { it > 0 }?.let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() } ?: "") }
    var paidBy by remember { mutableStateOf(initial?.paidBy ?: group.members.firstOrNull()?.id ?: "") }
    var sharedWith by remember { mutableStateOf(initial?.sharedWith ?: group.members.map { it.id }) }
    var date by remember { mutableStateOf(initial?.date ?: Zone.todayISO()) }
    var category by remember { mutableStateOf(initial?.category ?: defaultCategory) }
    var error by remember { mutableStateOf("") }

    ZoneDialog(
        title = if (initial == null) "Add expense" else "Edit expense",
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Record expense" else "Save changes",
        onConfirm = {
            val value = amount.toDoubleOrNull()
            when {
                description.trim().length < 2 -> error = "Write a description."
                value == null || value <= 0 -> error = "Enter an amount greater than zero."
                paidBy.isBlank() -> error = "Choose who paid."
                sharedWith.isEmpty() -> error = "Choose at least one person to share with."
                date > Zone.todayISO() -> error = "Choose today or an earlier date."
                else -> onSave(
                    Expense(
                        id = initial?.id ?: Zone.uid("gx"),
                        description = description.trim().take(70),
                        amount = (value * 100).roundToInt() / 100.0,
                        paidBy = paidBy,
                        date = date,
                        sharedWith = sharedWith,
                        category = category
                    )
                )
            }
        }
    ) {
        LabeledField("Description", description, { description = it }, placeholder = "Groceries, taxi, dinnerâ€¦")
        LabeledField("Amount", amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, placeholder = "0", keyboardType = KeyboardType.Number)
        LabeledField("Date", Zone.medium(date), {}, modifier = Modifier.fillMaxWidth(), readOnly = true)
        TextButton(onClick = { showDatePicker(context, Zone.parseDate(date) ?: Zone.today()) { date = it.toString() } }) {
            Text("Change date", color = ZoneColors.blue, style = MaterialTheme.typography.labelMedium)
        }
        Text("Paid by", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(group.members) { member ->
                FilterChip(selected = paidBy == member.id, onClick = { paidBy = member.id }, label = { Text(member.name) })
            }
        }
        Text("Shared with", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(group.members) { member ->
                FilterChip(
                    selected = member.id in sharedWith,
                    onClick = {
                        sharedWith = if (member.id in sharedWith) sharedWith - member.id else sharedWith + member.id
                    },
                    label = { Text(member.name) }
                )
            }
        }
        Text("Category", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(splitCategories) { option ->
                FilterChip(selected = category == option, onClick = { category = option }, label = { Text(splitCategoryLabels[option] ?: option) })
            }
        }
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private data class Transfer(val from: String, val to: String, val amount: Double)

private data class GroupBalance(
    val total: Double,
    val paid: Map<String, Double>,
    val share: Map<String, Double>,
    val net: Map<String, Double>
)

private fun validShared(group: SplitGroup, expense: Expense): List<String> {
    val ids = group.members.map { it.id }.toSet()
    return expense.sharedWith.filter { it in ids }.distinct()
}

private fun computeGroup(group: SplitGroup): GroupBalance {
    val paid = group.members.associate { it.id to 0.0 }.toMutableMap()
    val share = group.members.associate { it.id to 0.0 }.toMutableMap()
    var total = 0.0
    group.expenses.forEach { expense ->
        if (expense.amount <= 0) return@forEach
        val participants = validShared(group, expense)
        if (participants.isEmpty()) return@forEach
        if (expense.category != "settlement") total += expense.amount
        if (paid.containsKey(expense.paidBy)) {
            paid[expense.paidBy] = (paid[expense.paidBy] ?: 0.0) + expense.amount
        }
        val each = expense.amount / participants.size
        participants.forEach { id ->
            share[id] = (share[id] ?: 0.0) + each
        }
    }
    val net = group.members.associate { member ->
        member.id to ((paid[member.id] ?: 0.0) - (share[member.id] ?: 0.0))
    }
    return GroupBalance(total, paid, share, net)
}

private fun planFor(group: SplitGroup, balance: GroupBalance): List<Transfer> {
    val creditors = balance.net.filter { it.value > 0.005 }
        .toList().sortedByDescending { it.second }
        .map { it.first to it.second }.toMutableList()
    val debtors = balance.net.filter { it.value < -0.005 }
        .toList().sortedByDescending { it.second }
        .map { it.first to -it.second }.toMutableList()
    val transfers = mutableListOf<Transfer>()
    var ci = 0
    var di = 0
    while (ci < creditors.size && di < debtors.size) {
        val (to, credit) = creditors[ci]
        val (from, debt) = debtors[di]
        val amount = minOf(credit, debt)
        if (amount > 0.005) {
            transfers.add(Transfer(from, to, (amount * 100).roundToInt() / 100.0))
        }
        creditors[ci] = to to (credit - amount)
        debtors[di] = from to (debt - amount)
        if (creditors[ci].second <= 0.005) ci++
        if (debtors[di].second <= 0.005) di++
    }
    return transfers
}

private fun openToSettle(group: SplitGroup): Double {
    val balance = computeGroup(group)
    return planFor(group, balance).sumOf { it.amount }
}

private fun groupTotal(group: SplitGroup): Double = computeGroup(group).total

private fun memberName(group: SplitGroup, id: String): String =
    group.members.firstOrNull { it.id == id }?.name ?: "Unknown"

private fun settle(group: SplitGroup, transfer: Transfer, context: android.content.Context) {
    val fromName = memberName(group, transfer.from)
    val toName = memberName(group, transfer.to)
    val expense = Expense(
        id = Zone.uid("gx"),
        description = "Settlement â€” $fromName to $toName",
        amount = transfer.amount,
        paidBy = transfer.from,
        date = Zone.todayISO(),
        sharedWith = listOf(transfer.to),
        category = "settlement"
    )
    val next = group.copy(expenses = group.expenses + expense)
    Zone.setSplit(SplitData(Zone.split.value.groups.map { if (it.id == group.id) next else it }), "Settlement recorded", "add")
    toast(context, "Settlement recorded.")
}

private fun round(value: Double): Double = (value * 100).roundToInt() / 100.0