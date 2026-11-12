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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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

private val expenseCategories = listOf(
    "Rent", "Groceries", "Food", "Transport", "Utilities", "Shopping",
    "Health", "Entertainment", "Travel", "Education", "Gifts", "Other"
)
private val incomeCategories = listOf("Salary", "Freelance", "Investments", "Refund", "Other")

private val categoryColors = mapOf(
    "Rent" to Color(0xFFC95B3D),
    "Groceries" to Color(0xFF7C8B6E),
    "Transport" to Color(0xFF7B8795),
    "Food" to Color(0xFFC19A50),
    "Utilities" to Color(0xFF667D87),
    "Health" to Color(0xFFA87463),
    "Shopping" to Color(0xFF8A785E),
    "Entertainment" to Color(0xFF907080),
    "Travel" to Color(0xFF5F7A74),
    "Education" to Color(0xFF72828F),
    "Gifts" to Color(0xFFB38961),
    "Other" to Color(0xFFA49C8C),
    "Salary" to Color(0xFF557257),
    "Freelance" to Color(0xFF6B7F8C),
    "Investments" to Color(0xFF9A875B),
    "Refund" to Color(0xFF788D75)
)

@Composable
fun MoneyScreen() {
    val context = LocalContext.current
    val data = Zone.moneyData.value
    var month by remember { mutableStateOf(Zone.currentMonth()) }
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("all") }
    var categoryFilter by remember { mutableStateOf("all") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Txn?>(null) }
    var deleteTarget by remember { mutableStateOf<Txn?>(null) }
    var showBudget by remember { mutableStateOf(false) }

    val monthTxns = data.transactions.filter { Zone.monthKey(it.date) == month }
    val spent = monthTxns.filter { it.type == "expense" }.sumOf { it.amount }
    val earned = monthTxns.filter { it.type == "income" }.sumOf { it.amount }
    val net = earned - spent
    val budget = data.budget
    val remaining = budget - spent
    val budgetPct = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0).toFloat() else 0f

    val filtered = monthTxns.filter { txn ->
        val matchesType = typeFilter == "all" || txn.type == typeFilter
        val matchesCategory = categoryFilter == "all" || txn.category == categoryFilter
        val matchesQuery = query.isBlank() ||
            (txn.note + " " + txn.category + " " + txn.type).contains(query.trim(), ignoreCase = true)
        matchesType && matchesCategory && matchesQuery
    }.sortedWith(compareByDescending<Txn> { it.date }.thenByDescending { it.id })

    val grouped = filtered.groupBy { it.date }
    val dates = grouped.keys.sortedDescending()
    val categories = (expenseCategories + incomeCategories + monthTxns.map { it.category }).distinct().sorted()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Money",
                subtitle = "${Zone.monthLabel(month, long = true)} Â· ${monthTxns.size} entries",
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
                            Icon(Icons.Default.Add, contentDescription = "Add transaction", tint = Color.White, modifier = Modifier.size(21.dp))
                        }
                    }
                }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { month = shiftMonth(month, -1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month", tint = ZoneColors.blue)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Zone.monthLabel(month, long = true), style = MaterialTheme.typography.titleMedium)
                    Text(if (month == Zone.currentMonth()) "This month" else "Closed month", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                }
                IconButton(
                    onClick = { if (month < Zone.currentMonth()) month = shiftMonth(month, 1) },
                    enabled = month < Zone.currentMonth()
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month", tint = if (month < Zone.currentMonth()) ZoneColors.blue else ZoneColors.separator)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile("Spent", Zone.money(spent), "${monthTxns.count { it.type == "expense" }} expenses", Modifier.weight(1f))
                StatTile("Income", Zone.money(earned), "${monthTxns.count { it.type == "income" }} deposits", Modifier.weight(1f))
                StatTile("Kept", Zone.money(net), if (earned > 0) "${(net / earned * 100).toInt()}% of income" else "No income", Modifier.weight(1f))
                StatTile("Budget left", if (budget > 0) Zone.money(remaining) else "â€”", if (budget > 0) "${(spent / budget * 100).toInt()}% used" else "No envelope", Modifier.weight(1f))
            }
        }

        item {
            ZoneCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                padding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Monthly budget", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                        Text(if (budget > 0) Zone.money(budget) else "Not set", style = MaterialTheme.typography.titleLarge)
                    }
                    SecondaryButton(if (budget > 0) "Edit" else "Set budget", { showBudget = true })
                }
                Spacer(Modifier.height(10.dp))
                ZoneProgressBar(budgetPct, color = if (spent > budget && budget > 0) ZoneColors.red else ZoneColors.blue)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (budget > 0) "${Zone.money(spent)} of ${Zone.money(budget)} Â· ${if (remaining < 0) "${Zone.money(-remaining)} over" else "${Zone.money(remaining)} left"}" else "Set a monthly envelope to read the pace.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remaining < 0 && budget > 0) ZoneColors.red else ZoneColors.secondary
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
                listOf("all" to "All", "expense" to "Expenses", "income" to "Income").forEach { (value, label) ->
                    FilterChip(selected = typeFilter == value, onClick = { typeFilter = value }, label = { Text(label) })
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
                items(categories) { category ->
                    FilterChip(selected = categoryFilter == category, onClick = { categoryFilter = category }, label = { Text(category) })
                }
            }
        }

        item {
            SearchField(
                query,
                { query = it },
                placeholder = "Search ledger",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (monthTxns.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "A clean month",
                    message = "No income or expenses recorded for ${Zone.monthLabel(month, true)}.",
                    actionLabel = "Add a transaction",
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
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "No entries match",
                    message = "Try another category, type, or search term.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                )
            }
        } else {
            dates.forEach { date ->
                item {
                    Text(
                        Zone.rel(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = ZoneColors.secondary,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 4.dp)
                    )
                }
                items(grouped[date] ?: emptyList()) { txn ->
                    val color = categoryColors[txn.category] ?: ZoneColors.secondary
                    ZoneCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 3.dp),
                        padding = 14.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(color)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(txn.note.ifBlank { txn.category }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${txn.category} Â· ${if (txn.type == "income") "Income" else "Expense"}", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                            }
                            Text(
                                (if (txn.type == "income") "+" else "âˆ’") + Zone.money(txn.amount),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (txn.type == "income") ZoneColors.green else ZoneColors.red
                            )
                            IconButton(onClick = { editing = txn; showEditor = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ZoneColors.secondary, modifier = Modifier.size(15.dp))
                            }
                            IconButton(onClick = { deleteTarget = txn }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            SecondaryButton(
                text = "Export ${Zone.monthLabel(month)} (CSV)",
                onClick = { shareText(context, moneyCsv(monthTxns), "Zone money") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }

    if (showEditor) {
        TxnEditor(
            initial = editing,
            defaultDate = if (month == Zone.currentMonth()) Zone.todayISO() else "$month-01",
            onDismiss = {
                showEditor = false
                editing = null
            },
            onSave = { txn ->
                val next = if (data.transactions.any { it.id == txn.id }) {
                    data.copy(transactions = data.transactions.map { if (it.id == txn.id) txn else it })
                } else {
                    data.copy(transactions = data.transactions + txn)
                }
                Zone.setMoney(next, if (editing == null) "Transaction added" else "Transaction updated", if (editing == null) "add" else "edit")
                month = Zone.monthKey(txn.date)
                showEditor = false
                editing = null
                toast(context, if (editing == null) "Transaction saved." else "Transaction updated.")
            }
        )
    }

    if (showBudget) {
        var budgetText by remember { mutableStateOf(if (budget > 0) budget.toInt().toString() else "") }
        ZoneDialog(
            title = "Monthly budget",
            onDismiss = { showBudget = false },
            confirmLabel = "Save budget",
            onConfirm = {
                val value = budgetText.toDoubleOrNull() ?: 0.0
                Zone.setMoney(data.copy(budget = value.coerceAtLeast(0.0)), "Budget updated", "edit")
                showBudget = false
                toast(context, if (value > 0) "Monthly budget set." else "Budget cleared.")
            }
        ) {
            LabeledField(
                "Budget amount",
                budgetText,
                { budgetText = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = "Amount per month",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
        }
    }

    deleteTarget?.let { txn ->
        ConfirmDialog(
            title = "Remove this entry?",
            message = "Delete \"${txn.note.ifBlank { txn.category }}\" for ${Zone.money(txn.amount)} on ${Zone.medium(txn.date)}?",
            confirmLabel = "Delete entry",
            onConfirm = {
                Zone.setMoney(data.copy(transactions = data.transactions.filter { it.id != txn.id }), "Transaction deleted", "delete")
                deleteTarget = null
                toast(context, "Transaction deleted.")
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun TxnEditor(
    initial: Txn?,
    defaultDate: String,
    onDismiss: () -> Unit,
    onSave: (Txn) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initial?.type ?: "expense") }
    var amount by remember { mutableStateOf(initial?.amount?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: defaultDate) }
    var category by remember { mutableStateOf(initial?.category ?: "Groceries") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf("") }
    val cats = if (type == "income") incomeCategories else expenseCategories

    ZoneDialog(
        title = if (initial == null) "Add transaction" else "Edit transaction",
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Save transaction" else "Save changes",
        onConfirm = {
            val value = amount.toDoubleOrNull()
            when {
                value == null || value <= 0 -> error = "Enter an amount greater than zero."
                date.isBlank() || date > Zone.todayISO() -> error = "Choose today or an earlier date."
                else -> onSave(
                    Txn(
                        id = initial?.id ?: Zone.uid("txn"),
                        type = if (type == "income") "income" else "expense",
                        amount = value,
                        category = category,
                        date = date,
                        note = note.trim()
                    )
                )
            }
        }
    ) {
        Segmented(
            options = listOf("expense", "income"),
            selected = type,
            label = { if (it == "income") "Income" else "Expense" },
            onSelect = {
                type = it
                category = if (it == "income") "Salary" else "Groceries"
            }
        )
        LabeledField("Amount", amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, placeholder = "0", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        LabeledField("Date", Zone.medium(date), {}, modifier = Modifier.fillMaxWidth(), readOnly = true)
        TextButton(onClick = { showDatePicker(context, Zone.parseDate(date) ?: Zone.today()) { date = it.toString() } }) {
            Text("Change date", color = ZoneColors.blue, style = MaterialTheme.typography.labelMedium)
        }
        Text("Category", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(cats) { option ->
                FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option) })
            }
        }
        LabeledField("Note", note, { note = it }, placeholder = "What was it for?")
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun shiftMonth(month: String, delta: Int): String {
    val date = Zone.parseDate("$month-01") ?: return month
    val next = date.plusMonths(delta.toLong())
    return next.toString().take(7)
}

private fun moneyCsv(txns: List<Txn>): String {
    val rows = mutableListOf(listOf("Date", "Type", "Category", "Amount", "Currency", "Note"))
    txns.sortedBy { it.date }.forEach { txn ->
        rows.add(listOf(txn.date, txn.type, txn.category, txn.amount.toInt().toString(), Zone.settings.value.currency, txn.note))
    }
    return csv(rows)
}