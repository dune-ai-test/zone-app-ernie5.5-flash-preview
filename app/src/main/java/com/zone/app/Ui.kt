package com.zone.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/* -------------------------------------------------------------------------- */
/* Building blocks                                                             */
/* -------------------------------------------------------------------------- */

@Composable
fun ZoneCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.6.dp)
    ) {
        Column(
            modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
            verticalArrangement = Arrangement.Top,
            content = {
                Column(modifier = Modifier.padding(padding), content = content)
            }
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZoneColors.secondary
                )
            }
        }
        if (action != null) action()
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) trailing()
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    hint: String? = null,
    modifier: Modifier = Modifier
) {
    ZoneCard(modifier = modifier, padding = 14.dp) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ZoneColors.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (hint != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = ZoneColors.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ZoneColors.blue,
            contentColor = Color.White,
            disabledContainerColor = ZoneColors.separator,
            disabledContentColor = ZoneColors.secondary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZoneColors.blue),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ZoneColors.red,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ZoneColors.blue,
            unfocusedBorderColor = ZoneColors.separator
        )
    )
}

@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) MaterialTheme.colorScheme.onSurface else ZoneColors.secondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun Pill(
    text: String,
    color: Color = ZoneColors.blue,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ZoneProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = ZoneColors.blue,
    height: Dp = 6.dp
) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(3.dp)),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun Monogram(
    text: String,
    size: Dp = 42.dp,
    tone: Int = Zone.toneFor(text)
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(toneColor(tone)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = Zone.initials(text),
            style = MaterialTheme.typography.labelMedium,
            color = ZoneColors.text
        )
    }
}

fun toneColor(tone: Int): Color = when (tone % 6) {
    0 -> Color(0xFFE4DDCC)
    1 -> Color(0xFFDBE1D5)
    2 -> Color(0xFFE8D9D1)
    3 -> Color(0xFFD7E0E3)
    4 -> Color(0xFFE7DFC7)
    else -> Color(0xFFE1DBE3)
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ZoneCard(modifier = modifier, padding = 26.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ZoneColors.secondary, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = ZoneColors.secondary
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                PrimaryButton(actionLabel, onAction)
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = ZoneColors.secondary) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(confirmLabel, color = ZoneColors.red, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, color = ZoneColors.blue)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
fun ZoneDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String = "Save",
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, style = MaterialTheme.typography.labelLarge, color = ZoneColors.blue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, color = ZoneColors.secondary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    readOnly: Boolean = false
) {
    Column(modifier = modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        Spacer(Modifier.height(5.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            minLines = minLines,
            readOnly = readOnly,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(13.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZoneColors.blue,
                unfocusedBorderColor = ZoneColors.separator
            )
        )
    }
}

@Composable
fun DateField(
    label: String,
    value: String,
    onPick: (LocalDate) -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    LabeledField(
        label = label,
        value = Zone.medium(value),
        onValueChange = {},
        modifier = modifier,
        readOnly = true
    )
    Spacer(Modifier.height(2.dp))
    TextButton(
        onClick = { showDatePicker(context, Zone.parseDate(value) ?: Zone.today()) { onPick(it) } }
    ) {
        Text("Change date", color = ZoneColors.blue, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ToolIcon(
    route: String,
    tint: Color = ZoneColors.blue,
    size: Dp = 22.dp
) {
    Icon(
        imageVector = iconFor(route),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size)
    )
}

fun iconFor(route: String): ImageVector = when (route) {
    "home" -> Icons.Default.Dashboard
    "diary" -> Icons.Default.Book
    "lists" -> Icons.Default.List
    "habits" -> Icons.Default.Refresh
    "notes" -> Icons.Default.Edit
    "money" -> Icons.Default.AccountBalanceWallet
    "passbook" -> Icons.Default.AccountBalance
    "split" -> Icons.Default.CallSplit
    "mileage" -> Icons.Default.DirectionsCar
    "watchlist" -> Icons.Default.Movie
    "links" -> Icons.Default.Link
    "subscriptions" -> Icons.Default.CreditCard
    "settings" -> Icons.Default.Settings
    "admin" -> Icons.Default.Shield
    "more" -> Icons.Default.MoreHoriz
    else -> Icons.Default.Dashboard
}

/* -------------------------------------------------------------------------- */
/* Platform helpers                                                            */
/* -------------------------------------------------------------------------- */

fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun showDatePicker(context: Context, initial: LocalDate, onPick: (LocalDate) -> Unit) {
    val date = Zone.parseDate(initial.toString()) ?: Zone.today()
    android.app.DatePickerDialog(
        context,
        { _, year, month, day ->
            onPick(LocalDate.of(year, month + 1, day))
        },
        date.year,
        date.monthValue - 1,
        date.dayOfMonth
    ).show()
}

fun copyText(context: Context, text: String, label: String = "Zone") {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
    toast(context, "Copied to clipboard")
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        toast(context, "No browser available")
    }
}

fun shareText(context: Context, text: String, subject: String = "Zone export") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, subject))
}

fun csv(rows: List<List<String>>): String = rows.joinToString("\r\n") { row ->
    row.joinToString(",") { cell ->
        val text = cell
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else text
    }
}

@Composable
fun BackButton(onBack: () -> Unit) {
    Surface(
        onClick = onBack,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ZoneColors.blue, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CheckMark(done: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (done) ZoneColors.blue else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (done) ZoneColors.blue else ZoneColors.separator),
        modifier = Modifier.size(26.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (done) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun IconTextButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: Color = ZoneColors.blue
) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = tint, style = MaterialTheme.typography.labelMedium)
    }
}