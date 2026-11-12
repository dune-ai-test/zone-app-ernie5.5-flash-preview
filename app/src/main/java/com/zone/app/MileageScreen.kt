package com.zone.app

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.DirectionsCar
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val vehicleTypeOptions = listOf("bike", "scooter", "car", "suv", "truck")
private val fuelOptions = listOf("petrol", "diesel")

@Composable
fun MileageScreen() {
    val context = LocalContext.current
    val trips = Zone.trips.value
    val vehicles = Zone.vehicles.value
    var showTrip by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<Trip?>(null) }
    var showVehicle by remember { mutableStateOf(false) }
    var deleteTrip by remember { mutableStateOf<Trip?>(null) }
    var deleteVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var query by remember { mutableStateOf("") }
    var vehicleFilter by remember { mutableStateOf("all") }

    val valid = trips.filter { tripStats(it).valid }
    val distance = valid.sumOf { it.end - it.start }
    val litres = valid.sumOf { if (it.price > 0) it.paid / it.price else 0.0 }
    val spend = valid.sumOf { it.paid }
    val avg = if (litres > 0) distance / litres else 0.0
    val latest = valid.maxByOrNull { it.date }
    val latestMileage = latest?.let { tripStats(it).mileage } ?: 0.0
    val best = valid.map { tripStats(it).mileage }.maxOrNull() ?: 0.0

    val filtered = trips.filter {
        val matchesVehicle = vehicleFilter == "all" || it.vehicle.equals(vehicleFilter, ignoreCase = true)
        val matchesQuery = query.isBlank() ||
            (it.vehicle + " " + it.note + " " + it.date + " " + it.start.toInt() + " " + it.end.toInt()).contains(query.trim(), ignoreCase = true)
        matchesVehicle && matchesQuery
    }.sortedWith(compareByDescending<Trip> { it.date }.thenByDescending { it.id })

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            ScreenHeader(
                title = "Mileage",
                subtitle = "${trips.size} fills Â· ${vehicles.size} vehicles",
                action = {
                    Row {
                        IconButton(onClick = { showVehicle = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add vehicle", tint = ZoneColors.blue)
                        }
                        Surface(
                            onClick = {
                                editingTrip = null
                                showTrip = true
                            },
                            shape = RoundedCornerShape(50),
                            color = ZoneColors.blue,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Log a fill", tint = Color.White, modifier = Modifier.size(21.dp))
                            }
                        }
                    }
                }
            )
        }

        item {
            ZoneCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                padding = 18.dp
            ) {
                GaugeCard(
                    value = latestMileage,
                    average = avg,
                    latestDistance = latest?.let { it.end - it.start } ?: 0.0,
                    latestLitres = latest?.let { if (it.price > 0) it.paid / it.price else 0.0 } ?: 0.0,
                    completeFills = valid.size
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile("Distance", "${Zone.num(distance)} km", "${valid.size} complete fills", Modifier.weight(1f))
                StatTile("Average", "${"%.1f".format(avg)} km/l", "Fleet lifetime", Modifier.weight(1f))
                StatTile("Best tank", "${"%.1f".format(best)}", "km per litre", Modifier.weight(1f))
                StatTile("Fuel spend", Zone.money(spend), "Across complete fills", Modifier.weight(1f))
            }
        }

        item { SectionTitle("Garage") }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(selected = vehicleFilter == "all", onClick = { vehicleFilter = "all" }, label = { Text("All vehicles") })
                }
                items(vehicles) { vehicle ->
                    FilterChip(
                        selected = vehicleFilter.equals(vehicle.name, ignoreCase = true),
                        onClick = { vehicleFilter = vehicle.name },
                        label = { Text(vehicle.name) }
                    )
                }
                item {
                    FilterChip(selected = false, onClick = { showVehicle = true }, label = { Text("+ Add vehicle") })
                }
            }
        }

        if (vehicles.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    vehicles.forEach { vehicle ->
                        val vehicleTrips = valid.filter { it.vehicle.equals(vehicle.name, ignoreCase = true) }
                        val vehicleDistance = vehicleTrips.sumOf { it.end - it.start }
                        val vehicleLitres = vehicleTrips.sumOf { if (it.price > 0) it.paid / it.price else 0.0 }
                        val vehicleAvg = if (vehicleLitres > 0) vehicleDistance / vehicleLitres else 0.0
                        ZoneCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), padding = 14.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(vehicle.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${vehicle.type} Â· ${vehicle.fuel} Â· ${vehicleTrips.size} fills Â· ${"%.1f".format(vehicleAvg)} km/l",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ZoneColors.secondary
                                    )
                                }
                                IconButton(onClick = { deleteVehicle = vehicle }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete vehicle", tint = ZoneColors.red, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
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
                SearchField(query, { query = it }, modifier = Modifier.weight(1f), placeholder = "Search logbook")
                SecondaryButton("Add fill", {
                    editingTrip = null
                    showTrip = true
                })
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.DirectionsCar,
                    title = if (trips.isEmpty()) "The road starts here" else "No fills match",
                    message = if (trips.isEmpty()) "Log the first fill: odometer readings, price per litre, and total paid." else "Try another vehicle or search term.",
                    actionLabel = if (trips.isEmpty()) "Log a fill" else null,
                    onAction = if (trips.isEmpty()) {
                        {
                            editingTrip = null
                            showTrip = true
                        }
                    } else null,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
        } else {
            items(filtered) { trip ->
                val stats = tripStats(trip)
                ZoneCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 3.dp),
                    padding = 14.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${trip.vehicle} Â· ${Zone.medium(trip.date)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZoneColors.secondary
                            )
                            Text(
                                "${trip.start.toInt()} â†’ ${trip.end.toInt()} km",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (trip.note.isNotBlank()) {
                                Text(trip.note, style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${"%.1f".format(stats.mileage)} km/l", style = MaterialTheme.typography.titleMedium, color = ZoneColors.blue)
                            Text(Zone.money(trip.paid), style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
                        }
                        IconButton(onClick = { editingTrip = trip; showTrip = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ZoneColors.secondary, modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = { deleteTrip = trip }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZoneColors.red, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            SecondaryButton(
                text = "Export logbook (CSV)",
                onClick = { shareText(context, mileageCsv(trips, vehicles), "Zone mileage") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }

    if (showTrip) {
        TripEditor(
            initial = editingTrip,
            vehicles = vehicles,
            onDismiss = {
                showTrip = false
                editingTrip = null
            },
            onSave = { trip ->
                val next = if (trips.any { it.id == trip.id }) {
                    trips.map { if (it.id == trip.id) trip else it }
                } else {
                    trips + trip
                }
                val vehicleName = trip.vehicle.trim()
                val exists = vehicles.any { it.name.equals(vehicleName, ignoreCase = true) }
                val nextVehicles = if (exists) vehicles else vehicles + Vehicle(
                    id = Zone.uid("veh"),
                    name = vehicleName,
                    type = "car",
                    fuel = "petrol",
                    createdAt = Zone.todayISO()
                )
                Zone.setTrips(next, if (editingTrip == null) "Fill logged" else "Fill updated", if (editingTrip == null) "add" else "edit")
                if (!exists) Zone.setVehicles(nextVehicles, "Vehicle added", "add", audit = false)
                showTrip = false
                editingTrip = null
                toast(context, if (editingTrip == null) "Fill logged." else "Fill updated.")
            }
        )
    }

    if (showVehicle) {
        VehicleEditor(
            onDismiss = { showVehicle = false },
            onSave = { vehicle ->
                Zone.setVehicles(vehicles + vehicle, "Vehicle added", "add")
                showVehicle = false
                toast(context, "Vehicle added.")
            }
        )
    }

    deleteTrip?.let { trip ->
        ConfirmDialog(
            title = "Delete this fill?",
            message = "Remove the ${trip.vehicle} fill on ${Zone.medium(trip.date)}? The next fill will not be re-linked automatically.",
            confirmLabel = "Delete fill",
            onConfirm = {
                Zone.setTrips(trips.filter { it.id != trip.id }, "Fill deleted", "delete")
                deleteTrip = null
                toast(context, "Fill deleted.")
            },
            onDismiss = { deleteTrip = null }
        )
    }

    deleteVehicle?.let { vehicle ->
        ConfirmDialog(
            title = "Delete ${vehicle.name}?",
            message = "This removes the vehicle and its ${trips.count { it.vehicle.equals(vehicle.name, ignoreCase = true) }} fills.",
            confirmLabel = "Delete vehicle",
            onConfirm = {
                val nextTrips = trips.filter { !it.vehicle.equals(vehicle.name, ignoreCase = true) }
                Zone.setTrips(nextTrips, "Vehicle deleted", "delete")
                Zone.setVehicles(vehicles.filter { it.id != vehicle.id }, "Vehicle deleted", "delete")
                if (vehicleFilter.equals(vehicle.name, ignoreCase = true)) vehicleFilter = "all"
                deleteVehicle = null
                toast(context, "Vehicle deleted.")
            },
            onDismiss = { deleteVehicle = null }
        )
    }
}

@Composable
private fun GaugeCard(
    value: Double,
    average: Double,
    latestDistance: Double,
    latestLitres: Double,
    completeFills: Int
) {
    val fraction = (value / 40.0).coerceIn(0.0, 1.0).toFloat()
    val color = if (value > 0) ZoneColors.blue else ZoneColors.separator
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (value > 0) "Live tank" else "Awaiting data",
            style = MaterialTheme.typography.labelSmall,
            color = ZoneColors.secondary
        )
        Box(modifier = Modifier.size(width = 260.dp, height = 145.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                val stroke = 14.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height * 2 - stroke)
                val topLeft = Offset(stroke / 2, size.height - arcSize.height + stroke / 2)
                drawArc(
                    color = ZoneColors.separator,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize.width, arcSize.height),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                if (fraction > 0f) {
                    drawArc(
                        color = color,
                        startAngle = 180f,
                        sweepAngle = 180f * fraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize.width, arcSize.height),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                val center = Offset(size.width / 2, size.height)
                val angle = Math.PI * (1.0 - fraction)
                val needleLength = size.height * 0.62
                val end = Offset(
                    (center.x + needleLength * cos(angle)).toFloat(),
                    (center.y - needleLength * sin(angle)).toFloat()
                )
                drawLine(
                    color = color,
                    start = center,
                    end = end,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${"%.1f".format(value)}", style = MaterialTheme.typography.displayLarge, color = color)
                Text("km/l", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (latestDistance > 0) "This fill Â· ${"%.0f".format(latestDistance)} km on ${"%.2f".format(latestLitres)} L" else "No complete tank yet",
            style = MaterialTheme.typography.labelSmall,
            color = ZoneColors.secondary
        )
        Text(
            "Average ${"%.1f".format(average)} km/l across $completeFills fills",
            style = MaterialTheme.typography.labelSmall,
            color = ZoneColors.secondary
        )
    }
}

@Composable
private fun TripEditor(
    initial: Trip?,
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (Trip) -> Unit
) {
    val context = LocalContext.current
    var vehicle by remember { mutableStateOf(initial?.vehicle ?: vehicles.firstOrNull()?.name ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: Zone.todayISO()) }
    var start by remember { mutableStateOf(initial?.start?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }
    var end by remember { mutableStateOf(initial?.end?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }
    var price by remember { mutableStateOf(initial?.price?.takeIf { it > 0 }?.toString() ?: "") }
    var paid by remember { mutableStateOf(initial?.paid?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf("") }

    ZoneDialog(
        title = if (initial == null) "Log a fill" else "Edit fill",
        onDismiss = onDismiss,
        confirmLabel = if (initial == null) "Save fill" else "Save changes",
        onConfirm = {
            val startValue = start.toDoubleOrNull()
            val endValue = end.toDoubleOrNull()
            val priceValue = price.toDoubleOrNull()
            val paidValue = paid.toDoubleOrNull()
            when {
                vehicle.trim().isEmpty() -> error = "Choose or name a vehicle."
                startValue == null || startValue < 0 -> error = "Enter a valid start reading."
                endValue == null || endValue <= startValue -> error = "End reading must be above start."
                priceValue == null || priceValue <= 0 -> error = "Enter a price per litre above zero."
                paidValue == null || paidValue <= 0 -> error = "Enter the total paid above zero."
                else -> onSave(
                    Trip(
                        id = initial?.id ?: Zone.uid("trip"),
                        vehicle = vehicle.trim(),
                        date = date,
                        start = startValue,
                        end = endValue,
                        price = priceValue,
                        paid = paidValue,
                        note = note.trim().take(90)
                    )
                )
            }
        }
    ) {
        LabeledField("Vehicle", vehicle, { vehicle = it }, placeholder = "Car, bike, scooterâ€¦")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(vehicles) { item ->
                FilterChip(selected = vehicle.equals(item.name, ignoreCase = true), onClick = { vehicle = item.name }, label = { Text(item.name) })
            }
        }
        LabeledField("Date", Zone.medium(date), {}, modifier = Modifier.fillMaxWidth(), readOnly = true)
        TextButton(onClick = { showDatePicker(context, Zone.parseDate(date) ?: Zone.today()) { date = it.toString() } }) {
            Text("Change date", color = ZoneColors.blue, style = MaterialTheme.typography.labelMedium)
        }
        LabeledField("Start odometer (km)", start, { start = it.filter { c -> c.isDigit() || c == '.' } }, keyboardType = KeyboardType.Number)
        LabeledField("End odometer (km)", end, { end = it.filter { c -> c.isDigit() || c == '.' } }, keyboardType = KeyboardType.Number)
        LabeledField("Price per litre", price, { price = it.filter { c -> c.isDigit() || c == '.' } }, keyboardType = KeyboardType.Number)
        LabeledField("Total paid", paid, { paid = it.filter { c -> c.isDigit() || c == '.' } }, keyboardType = KeyboardType.Number)
        LabeledField("Note (optional)", note, { note = it }, placeholder = "Highway, monsoon, etc.")
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun VehicleEditor(
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("car") }
    var fuel by remember { mutableStateOf("petrol") }
    var error by remember { mutableStateOf("") }

    ZoneDialog(
        title = "Add vehicle",
        onDismiss = onDismiss,
        confirmLabel = "Add vehicle",
        onConfirm = {
            if (name.trim().length < 2) {
                error = "Name the vehicle."
                return@ZoneDialog
            }
            onSave(
                Vehicle(
                    id = Zone.uid("veh"),
                    name = name.trim().take(40),
                    type = type,
                    fuel = fuel,
                    createdAt = Zone.todayISO()
                )
            )
        }
    ) {
        LabeledField("Vehicle name", name, { name = it }, placeholder = "Bike, Swift, etc.")
        Text("Type", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(vehicleTypeOptions) { option ->
                FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option.replaceFirstChar { it.uppercase() }) })
            }
        }
        Text("Fuel", style = MaterialTheme.typography.labelSmall, color = ZoneColors.secondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            fuelOptions.forEach { option ->
                FilterChip(selected = fuel == option, onClick = { fuel = option }, label = { Text(option.replaceFirstChar { it.uppercase() }) })
            }
        }
        if (error.isNotEmpty()) {
            Text(error, color = ZoneColors.red, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private data class TripStats(
    val distance: Double,
    val litres: Double,
    val mileage: Double,
    val costPerKm: Double,
    val valid: Boolean
)

private fun tripStats(trip: Trip): TripStats {
    val distance = trip.end - trip.start
    val litres = if (trip.price > 0) trip.paid / trip.price else 0.0
    val mileage = if (litres > 0) distance / litres else 0.0
    val cost = if (distance > 0) trip.paid / distance else 0.0
    val valid = distance > 0 && trip.price > 0 && trip.paid > 0 && litres > 0 && mileage > 0 && cost > 0
    return TripStats(distance, litres, mileage, cost, valid)
}

private fun mileageCsv(
    trips: List<Trip>,
    vehicles: List<Vehicle>
): String {
    val rows = mutableListOf(
        listOf("Date", "Vehicle", "Type", "Fuel", "Start km", "End km", "Distance km", "Price/litre", "Total paid", "Litres", "Mileage", "Cost/km", "Currency", "Note")
    )
    trips.sortedByDescending { it.date }.forEach { trip ->
        val stats = tripStats(trip)
        val vehicle = vehicles.firstOrNull { it.name.equals(trip.vehicle, ignoreCase = true) }
        rows.add(
            listOf(
                trip.date,
                trip.vehicle,
                vehicle?.type ?: "car",
                vehicle?.fuel ?: "petrol",
                trip.start.toInt().toString(),
                trip.end.toInt().toString(),
                stats.distance.toInt().toString(),
                trip.price.toString(),
                trip.paid.toString(),
                stats.litres.toString(),
                stats.mileage.toString(),
                stats.costPerKm.toString(),
                Zone.settings.value.currency,
                trip.note
            )
        )
    }
    return csv(rows)
}