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

private val accountTypes = listOf("Bank", "Cash", "Credit", "Wallet")
private val passExpenseCategories = listOf(
    "Rent", "Groceries", "Food", "Transport", "Utilities", "Shopping",
    "Health", "Entertainment", "Travel", "Education", "Gifts", "Transfer", "Other"
)
private val passIncomeCategories = listOf("Salary", "Freelance", "Investments", "Refund", "Transfer", "Other")

@Composable
fun PassbookScreen() {
    val context = LocalContext.current
    val book = Zone.passbook.value
    var selectedAccount by remember { mutableStateOf("all") }
    var month by remember { mutableStateOf("all") }
    var typeFilter by remember { mutableStateOf("all") }
    var query by remember { mutableStateOf("") }
    var showAccount by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var showEntry by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<PassTxn?>(null) }
    var deleteAccount by remember { mutableStateOf<Account?>(null) }
    var deleteEntry by remember { mutableStateOf<PassTxn?>(null) }

    val total = book.accounts.sumOf { accountBalance(book, it) }
    val months = (book.transactions.map { Zone.monthKey(it.date) } + Zone.currentMonth()).distinct().sortedDescending()

    val scoped = book.transactions.filter { txn ->
        val accountOk = selectedAccount == "all" || txn.accountId == selectedAccount
        val monthOk = month == "all" || Zone.monthKey(txn.date) == month
        val typeOk = typeFilter == "all" || txn.type == typeFilter
        val searchOk = query.isBlank() ||
            (txn.description + " " + txn.category + " " + txn.type + " " + (book.accounts.firstOrNull { it.id == txn.accountId }?.name ?: ""))
                .contains(query.trim(), ignoreCase = true)
        accountOk && monthOk && typeOk && searchOk
    }.sortedWith(compareByDescending<PassTxn> { it.date }.thenByDescending { it.id })

    val moneyIn = scoped.filter { it.type == "deposit" }.sumOf { it.amount }
    val moneyOut = scoped.filter { it.type == "withdraw" }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Passbook",
                subtitle = "${book.accounts.size} accounts Â· ${Zone.money(total)}",
                action = {
                    Row {
                        IconButton(onClick = {
                            editingAccount = null
                            showAccount = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add account", tint = ZoneColors.blue)
                        }
                        Surface(
                            onClick = {
                                if (book.accounts.isEmpty()) {
                                    editingAccount = null
                                    showAccount = true
                                } else {
                                    editingEntry = null
                                    showEntry = true
                                }
                            },
                            shape = RoundedCornerShape(50),
                            color = ZoneColors.blue,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Add entry", tint = Color.White, modifier = Modifier.size(21.dp))
                            }
                        }
                    }
                }
            )
        }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    AccountCard(
                        title = "All accounts",
                        subtitle = "${book.accounts.size} accounts Â· ${book.transactions.size} entries",
                        balance = total,
                        active = selectedAccount == "all",
                        onClick = { selectedAccount = "all" }
                    )
                }
                items(book.accounts) { account ->
                    AccountCard(
                        title = account.name,
                        subtitle = "${account.type} Â· ${account.number.ifBlank { "No number" }}",
                        balance = accountBalance(book, account),
                        active = selectedAccount == account.id,
                        onClick = { selectedAccount = account.id }
                    )
                }
                item {
                    ZoneCard(
                        modifier = Modifier.width(160.dp),
                        padding = 16.dp,
                        onClick = {
                            editingAccount = null
                            showAccount = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ZoneColors.blue, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Add account", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item {
            ZoneCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                padding = 16.dp
            ) {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Money in", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                        Text("+" + Zone.money(moneyIn), style = MaterialTheme.typography.titleMedium, color = ZoneColors.green)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Money out", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                        Text("âˆ’" + Zone.money(moneyOut), style = MaterialTheme.typography.titleMedium, color = ZoneColors.red)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Net", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                        Text(Zone.money(moneyIn - moneyOut), style = MaterialTheme.typography.titleMedium, color = if (moneyIn - moneyOut >= 0) ZoneColors.green else ZoneColors.red)
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
                listOf("all" to "All", "deposit" to "Deposits", "withdraw" to "Withdrawals").forEach { (value, label) ->
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
                    FilterChip(selected = month == "all", onClick = { month = "all" }, label = { Text("All time") })
                }
                items(months) { key ->
                    FilterChip(
                        selected = month == key,
                        onClick = { month = key },
                        label = { Text(Zone.monthLabel(key)) }
                    )
                }
            }
        }

        item {
            SearchField(
                query,
                { query = it },
                placeholder = "Search entries",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (scoped.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.AccountBalance,
                    title = if (book.accounts.isEmpty()) "A new book" else "Nothing in this view",
                    message = if (book.accounts.isEmpty()) "Add an account, then record the first deposit or withdrawal." else "There are no entries for this account and month.",
                    actionLabel = if (book.accounts.isEmpty()) "Add an account" else "Add an entry",
                    onAction = {
                        if (book.accounts.isEmpty()) {
                            editingAccount = null
                            showAccount = true
                        } else {
                            editingEntry = null
                            showEntry = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                )
            }
        } else {
            items(scoped) { txn ->
                val account = book.accounts.firstOrNull { it.id == txn.accountId }
                ZoneCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 3.dp),
                    padding = 14.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(txn.description.ifBlank { txn.category }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${if (txn.type == "deposit") "Deposit" else "Withdrawal"} Â· ${txn.category} Â· ${account?.name ?: "Unknown"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZoneColors.secondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                (if (txn.type == "deposit") "+" else "âˆ’") + Zone.money(txn.amount),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (txn.type == "deposit") ZoneColors.green else ZoneColors.red
                            )
                            Text(Zone.medium(txn.date), style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                        }
                        IconButton(onClick = { editingEntry = txn; showEntry = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ZoneColors.secondary, modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = { deleteEntry = txn }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            SecondaryButton(
                text = "Export statement (CSV)",
                onClick = { shareText(context, passbookCsv(book, scoped), "Zone passbook") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }

    if (showAccount) {
        AccountEditor(
            initial = editingAccount,
            onDismiss = {
                showAccount = false
                editingAccount = null
            },
            onDelete = {
                deleteAccount = editingAccount
                showAccount = false
            },
            onSave = { account ->
                val next = if (book.accounts.any { it.id == account.id }) {
                    book.copy(accounts = book.accounts.map { if (it.id == account.id) account else it })
                } else {
                    book.copy(accounts = book.accounts + account)
                }
                Zone.setPassbook(next, if (editingAccount == null) "Account opened" else "Account updated", if (editingAccount == null) "add" else "edit")
                showAccount = false
                editingAccount = null
                toast(context, if (editingAccount == null) "Account saved." else "Account updated.")
            }
        )
    }

    if (showEntry) {
        EntryEditor(
            initial = editingEntry,
            accounts = book.accounts,
            onDismiss = {
                showEntry = false
                editingEntry = null
            },
            onSave = { txn ->
                val next = if (book.transactions.any { it.id == txn.id }) {
                    book.copy(transactions = book.transactions.map { if (it.id == txn.id) txn else it })
                } else {
                    book.copy(transactions = book.transactions + txn)
                }
                Zone.setPassbook(next, if (editingEntry == null) "Entry recorded" else "Entry updated", if (editingEntry == null) "add" else "edit")
                showEntry = false
                editingEntry = null
                toast(context, if (editingEntry == null) "Entry saved." else "Entry updated.")
            }
        )
    }

    deleteAccount?.let { account ->
        ConfirmDialog(
            title = "Remove ${account.name}?",
            message = "This removes the account and ${book.transactions.count { it.accountId == account.id }} entries from the passbook.",
            confirmLabel = "Remove account",
            onConfirm = {
                val next = book.copy(
                    accounts = book.accounts.filter { it.id != account.id },
                    transactions = book.transactions.filter { it.accountId != account.id }
                )
                Zone.setPassbook(next, "Account removed", "delete")
                selectedAccount = "all"
                deleteAccount = null
                toast(context, "Account removed.")
            },
            onDismiss = { deleteAccount = null }
        )
    }

    deleteEntry?.let { txn ->
        ConfirmDialog(
            title = "Remove this line?",
            message = "Delete \"${txn.description.ifBlank { txn.category }}\" for ${Zone.money(txn.amount)} on ${Zone.medium(txn.date)}?",
            confirmLabel = "Delete entry",
            onConfirm = {
                Zone.setPassbook(book.copy(transactions = book.transactions.filter { it.id != txn.id }), "Entry deleted", "delete")
                deleteEntry = null
                toast(context, "Entry deleted.")
            },
            onDismiss = { deleteEntry = null }
        )
    }
}

@Composable
private fun AccountCard(
    title: String,
    subtitle: String,
    balance: Double,
    active: Boolean,
    onClick: () -> Unit
) {
    ZoneCard(
        modifier = Modifier.width(190.dp),
        padding = 16.dp,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) ZoneColors.blue else ZoneColors.secondary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (active) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ZoneColors.blue)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(Zone.money(balance), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AccountEditor(
    initial: Account?,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "Bank") }
    var number by remember { mutableStateOf(initial?.number ?: "") }
    var opening by remember { mutableStateOf(initial?.opening?.takeIf { it != 0.0 }?.toInt()?.toString() ?: "") }
    var error by remember { mutableStateOf("") }

    ZoneDialog(
        title = if (initial == null) "Add account" else "Account details",
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Add account" else "Save changes",
        onConfirm = {
            if (name.trim().isEmpty()) {
                error = "Give the account a name."
                return@ZoneDialog
            }
            onSave(
                Account(
                    id = initial?.id ?: Zone.uid("acc"),
                    name = name.trim(),
                    type = type,
                    number = number.trim(),
                    opening = opening.toDoubleOrNull() ?: 0.0
                )
            )
        }
    ) {
        LabeledField("Account name", name, { name = it }, placeholder = "Everyday account")
        Text("Type", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            accountTypes.forEach { option ->
                FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option) })
            }
        }
        LabeledField("Account number (optional)", number, { number = it }, placeholder = "â€¢â€¢â€¢â€¢ 1234")
        LabeledField("Opening balance", opening, { opening = it.filter { c -> c.isDigit() || c == '.' } }, placeholder = "0", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        if (initial != null) {
            TextButton(onClick = onDelete) { Text("Remove account", color = ZoneColors.red) }
        }
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EntryEditor(
    initial: PassTxn?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (PassTxn) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initial?.type ?: "deposit") }
    var amount by remember { mutableStateOf(initial?.amount?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: Zone.todayISO()) }
    var accountId by remember { mutableStateOf(initial?.accountId ?: accounts.firstOrNull()?.id ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Salary") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var error by remember { mutableStateOf("") }
    val cats = if (type == "deposit") passIncomeCategories else passExpenseCategories

    ZoneDialog(
        title = if (initial == null) "Add entry" else "Edit entry",
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Save entry" else "Save changes",
        onConfirm = {
            val value = amount.toDoubleOrNull()
            when {
                value == null || value <= 0 -> error = "Enter an amount greater than zero."
                accountId.isBlank() -> error = "Choose an account."
                description.trim().isEmpty() -> error = "Write a short description."
                date > Zone.todayISO() -> error = "Choose today or an earlier date."
                else -> onSave(
                    PassTxn(
                        id = initial?.id ?: Zone.uid("pb"),
                        accountId = accountId,
                        type = if (type == "withdraw") "withdraw" else "deposit",
                        amount = value,
                        category = category,
                        description = description.trim(),
                        date = date
                    )
                )
            }
        }
    ) {
        Segmented(
            options = listOf("deposit", "withdraw"),
            selected = type,
            label = { if (it == "deposit") "Deposit" else "Withdrawal" },
            onSelect = {
                type = it
                category = if (it == "deposit") "Salary" else "Groceries"
            }
        )
        LabeledField("Amount", amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, placeholder = "0", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        LabeledField("Description", description, { description = it }, placeholder = "What was it for?")
        LabeledField("Date", Zone.medium(date), {}, modifier = Modifier.fillMaxWidth(), readOnly = true)
        TextButton(onClick = { showDatePicker(context, Zone.parseDate(date) ?: Zone.today()) { date = it.toString() } }) {
            Text("Change date", color = ZoneColors.blue, style = MaterialTheme.typography.labelMedium)
        }
        Text("Account", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(accounts) { account ->
                FilterChip(selected = accountId == account.id, onClick = { accountId = account.id }, label = { Text(account.name) })
            }
        }
        Text("Category", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(cats) { option ->
                FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option) })
            }
        }
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun accountBalance(book: PassbookData, account: Account): Double {
    var balance = account.opening
    book.transactions.forEach { txn ->
        if (txn.accountId == account.id) {
            balance += if (txn.type == "deposit") txn.amount else -txn.amount
        }
    }
    return balance
}

private fun passbookCsv(book: PassbookData, txns: List<PassTxn>): String {
    val rows = mutableListOf(listOf("Date", "Account", "Description", "Category", "Type", "Amount", "Currency"))
    txns.sortedBy { it.date }.forEach { txn ->
        val account = book.accounts.firstOrNull { it.id == txn.accountId }
        rows.add(listOf(txn.date, account?.name ?: "", txn.description, txn.category, txn.type, txn.amount.toInt().toString(), Zone.settings.value.currency))
    }
    return csv(rows)
}