package com.zone.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/* -------------------------------------------------------------------------- */
/* Models                                                                     */
/* -------------------------------------------------------------------------- */

@Serializable
data class DiaryEntry(
    val id: String = "",
    val date: String = "",
    val mood: String = "good",
    val title: String = "",
    val body: String = "",
    val tags: List<String> = emptyList()
)

@Serializable
data class CheckStep(
    val id: String = "",
    val text: String = "",
    val done: Boolean = false
)

@Serializable
data class CheckItem(
    val id: String = "",
    val text: String = "",
    val done: Boolean = false,
    val children: List<CheckStep> = emptyList()
)

@Serializable
data class CheckList(
    val id: String = "",
    val title: String = "",
    val createdAt: String = "",
    val items: List<CheckItem> = emptyList()
)

@Serializable
data class Habit(
    val id: String = "",
    val name: String = "",
    val target: Int = 7,
    val createdAt: String = "",
    val checks: Map<String, Boolean> = emptyMap()
)

@Serializable
data class HabitsData(
    val habits: List<Habit> = emptyList()
)

@Serializable
data class NoteItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val color: String = "plain",
    val pinned: Boolean = false,
    val created: String = "",
    val updated: String = ""
)

@Serializable
data class Txn(
    val id: String = "",
    val type: String = "expense",
    val amount: Double = 0.0,
    val category: String = "Other",
    val date: String = "",
    val note: String = ""
)

@Serializable
data class MoneyData(
    val budget: Double = 0.0,
    val transactions: List<Txn> = emptyList()
)

@Serializable
data class Account(
    val id: String = "",
    val name: String = "",
    val type: String = "Bank",
    val number: String = "",
    val opening: Double = 0.0
)

@Serializable
data class PassTxn(
    val id: String = "",
    val accountId: String = "",
    val type: String = "deposit",
    val amount: Double = 0.0,
    val category: String = "Other",
    val description: String = "",
    val date: String = ""
)

@Serializable
data class PassbookData(
    val accounts: List<Account> = emptyList(),
    val transactions: List<PassTxn> = emptyList()
)

@Serializable
data class Member(
    val id: String = "",
    val name: String = ""
)

@Serializable
data class Expense(
    val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val date: String = "",
    val sharedWith: List<String> = emptyList(),
    val category: String = "other"
)

@Serializable
data class SplitGroup(
    val id: String = "",
    val name: String = "",
    val kind: String = "home",
    val members: List<Member> = emptyList(),
    val expenses: List<Expense> = emptyList()
)

@Serializable
data class SplitData(
    val groups: List<SplitGroup> = emptyList()
)

@Serializable
data class Trip(
    val id: String = "",
    val vehicle: String = "",
    val date: String = "",
    val start: Double = 0.0,
    val end: Double = 0.0,
    val price: Double = 0.0,
    val paid: Double = 0.0,
    val note: String = ""
)

@Serializable
data class Vehicle(
    val id: String = "",
    val name: String = "",
    val type: String = "car",
    val fuel: String = "petrol",
    val createdAt: String = ""
)

@Serializable
data class Season(
    val number: Int = 1,
    val episodes: Int = 0,
    val watched: Int = 0
)

@Serializable
data class WatchItem(
    val id: String = "",
    val title: String = "",
    val type: String = "Film",
    val year: Int? = null,
    val genre: String = "",
    val status: String = "Queued",
    val rating: Double = 0.0,
    val watched: Int = 0,
    val total: Int = 0,
    val service: String = "",
    val favorite: Boolean = false,
    val note: String = "",
    val added: String = "",
    val seasons: List<Season> = emptyList()
)

@Serializable
data class LinkItem(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
    val favorite: Boolean = false,
    val saved: String = ""
)

@Serializable
data class Subscription(
    val id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val cycle: String = "Monthly",
    val next: String = "",
    val category: String = "Other",
    val status: String = "Active"
)

@Serializable
data class Settings(
    val currency: String = "INR",
    val theme: String = "system"
)

@Serializable
data class AdminSettings(
    val autoApprove: Boolean = false
)

@Serializable
data class AdminData(
    val settings: AdminSettings = AdminSettings()
)

@Serializable
data class AuditEvent(
    val id: String = "",
    val at: String = "",
    val section: String = "",
    val title: String = "",
    val detail: String = "",
    val action: String = "edit",
    val status: String = "pending",
    val source: String = "user",
    val beforeJson: String = "",
    val afterJson: String = ""
)

/* -------------------------------------------------------------------------- */
/* Store                                                                      */
/* -------------------------------------------------------------------------- */

object Zone {

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private lateinit var prefs: SharedPreferences

    var settings by mutableStateOf(Settings())
        private set

    var diary by mutableStateOf<List<DiaryEntry>>(emptyList())
        private set

    var lists by mutableStateOf<List<CheckList>>(emptyList())
        private set

    var habits by mutableStateOf<HabitsData>(HabitsData())
        private set

    var notes by mutableStateOf<List<NoteItem>>(emptyList())
        private set

    var moneyData by mutableStateOf<MoneyData>(MoneyData())
        private set

    var passbook by mutableStateOf<PassbookData>(PassbookData())
        private set

    var split by mutableStateOf<SplitData>(SplitData())
        private set

    var trips by mutableStateOf<List<Trip>>(emptyList())
        private set

    var vehicles by mutableStateOf<List<Vehicle>>(emptyList())
        private set

    var watchlist by mutableStateOf<List<WatchItem>>(emptyList())
        private set

    var links by mutableStateOf<List<LinkItem>>(emptyList())
        private set

    var subscriptions by mutableStateOf<List<Subscription>>(emptyList())
        private set

    var admin by mutableStateOf<AdminData>(AdminData())
        private set

    var audit by mutableStateOf<List<AuditEvent>>(emptyList())
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("zone.v1", Context.MODE_PRIVATE)
        settings = read("settings", Settings())
        diary = read("diary", emptyList())
        lists = read("lists", emptyList())
        habits = read("habits", HabitsData())
        notes = read("notes", emptyList())
        moneyData = read("money", MoneyData())
        passbook = read("passbook", PassbookData())
        split = read("split", SplitData())
        trips = read("trips", emptyList())
        vehicles = read("vehicles", emptyList())
        watchlist = read("watchlist", emptyList())
        links = read("links", emptyList())
        subscriptions = read("subscriptions", emptyList())
        admin = read("admin", AdminData())
        audit = read("audit", emptyList())
    }

    private inline fun <reified T> read(key: String, fallback: T): T {
        val raw = prefs.getString(key, null) ?: return fallback
        return try {
            json.decodeFromString<T>(raw)
        } catch (e: Exception) {
            fallback
        }
    }

    private inline fun <reified T> write(key: String, value: T) {
        prefs.edit().putString(key, json.encodeToString(value)).apply()
    }

    inline fun <reified T> encode(value: T): String = json.encodeToString(value)

    fun uid(prefix: String): String =
        "$prefix-${System.currentTimeMillis().toString(36)}-${(1000..9999).random()}"

    /* ---------------------------------------------------------------------- */
    /* Audit                                                                   */
    /* ---------------------------------------------------------------------- */

    private fun saveAudit() {
        write("audit", audit.value)
    }

    fun record(
        section: String,
        title: String,
        action: String,
        beforeJson: String,
        afterJson: String,
        source: String = "user",
        detail: String = ""
    ) {
        val approved = admin.value.settings.autoApprove
        val event = AuditEvent(
            id = uid("chg"),
            at = Instant.now().toString(),
            section = section,
            title = title,
            detail = detail,
            action = action,
            status = if (approved) "approved" else "pending",
            source = source,
            beforeJson = beforeJson,
            afterJson = afterJson
        )
        audit.value = listOf(event) + audit.value
        saveAudit()
    }

    fun setAutoApprove(enabled: Boolean) {
        val before = encode(admin.value)
        val next = AdminData(AdminSettings(enabled))
        admin.value = next
        write("admin", next)
        if (enabled) {
            val now = Instant.now().toString()
            audit.value = audit.value.map {
                if (it.status == "pending") it.copy(status = "approved", detail = it.detail.ifEmpty { "Approved by policy." }) else it
            }
            saveAudit()
        }
        record("admin", if (enabled) "Auto-approve enabled" else "Auto-approve disabled", "policy", before, encode(next), "admin")
    }

    fun approveAudit(id: String) {
        val target = audit.value.firstOrNull { it.id == id } ?: return
        audit.value = audit.value.map {
            if (it.id == id) it.copy(status = "approved", detail = it.detail.ifEmpty { "Approved by admin." }) else it
        }
        saveAudit()
    }

    fun rejectAudit(id: String) {
        val target = audit.value.firstOrNull { it.id == id } ?: return
        if (!restoreSection(target.section, target.beforeJson)) return
        audit.value = audit.value.map {
            if (it.id == id) it.copy(
                status = "rejected",
                detail = it.detail.ifEmpty { "Rejected and restored." }
            ) else it
        }
        saveAudit()
    }

    fun rollbackAudit(id: String) {
        val target = audit.value.firstOrNull { it.id == id } ?: return
        if (!restoreSection(target.section, target.beforeJson)) return
        audit.value = audit.value.map {
            if (it.id == id) it.copy(
                status = "rolledback",
                detail = it.detail.ifEmpty { "Rolled back by admin." }
            ) else it
        }
        saveAudit()
    }

    fun reapplyAudit(id: String) {
        val target = audit.value.firstOrNull { it.id == id } ?: return
        if (!restoreSection(target.section, target.afterJson)) return
        audit.value = audit.value.map {
            if (it.id == id) it.copy(status = "approved", detail = "Reapplied by admin.") else it
        }
        saveAudit()
    }

    private fun restoreSection(section: String, raw: String): Boolean {
        if (raw.isBlank()) return false
        return try {
            when (section) {
                "diary" -> diary = json.decodeFromString<List<DiaryEntry>>(raw).also { write("diary", it) }
                "lists" -> lists = json.decodeFromString<List<CheckList>>(raw).also { write("lists", it) }
                "habits" -> habits = json.decodeFromString<HabitsData>(raw).also { write("habits", it) }
                "notes" -> notes = json.decodeFromString<List<NoteItem>>(raw).also { write("notes", it) }
                "money" -> moneyData = json.decodeFromString<MoneyData>(raw).also { write("money", it) }
                "passbook" -> passbook = json.decodeFromString<PassbookData>(raw).also { write("passbook", it) }
                "split" -> split = json.decodeFromString<SplitData>(raw).also { write("split", it) }
                "trips" -> trips = json.decodeFromString<List<Trip>>(raw).also { write("trips", it) }
                "vehicles" -> vehicles = json.decodeFromString<List<Vehicle>>(raw).also { write("vehicles", it) }
                "watchlist" -> watchlist = json.decodeFromString<List<WatchItem>>(raw).also { write("watchlist", it) }
                "links" -> links = json.decodeFromString<List<LinkItem>>(raw).also { write("links", it) }
                "subscriptions" -> subscriptions = json.decodeFromString<List<Subscription>>(raw).also { write("subscriptions", it) }
                "settings" -> settings = json.decodeFromString<Settings>(raw).also { write("settings", it) }
                "admin" -> admin = json.decodeFromString<AdminData>(raw).also { write("admin", it) }
                else -> return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Typed setters                                                           */
    /* ---------------------------------------------------------------------- */

    fun setDiary(list: List<DiaryEntry>, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(diary.value) else ""
        diary = list
        write("diary", list)
        if (audit) record("diary", title, action, before, encode(list))
    }

    fun setLists(list: List<CheckList>, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(lists.value) else ""
        lists = list
        write("lists", list)
        if (audit) record("lists", title, action, before, encode(list))
    }

    fun setHabits(data: HabitsData, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(habits.value) else ""
        habits = data
        write("habits", data)
        if (audit) record("habits", title, action, before, encode(data))
    }

    fun setNotes(list: List<NoteItem>, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(notes.value) else ""
        notes = list
        write("notes", list)
        if (audit) record("notes", title, action, before, encode(list))
    }

    fun setMoney(data: MoneyData, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(moneyData.value) else ""
        moneyData = data
        write("money", data)
        if (audit) record("money", title, action, before, encode(data))
    }

    fun setPassbook(data: PassbookData, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(passbook.value) else ""
        passbook = data
        write("passbook", data)
        if (audit) record("passbook", title, action, before, encode(data))
    }

    fun setSplit(data: SplitData, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(split.value) else ""
        split = data
        write("split", data)
        if (audit) record("split", title, action, before, encode(data))
    }

    fun setTrips(list: List<Trip>, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(trips.value) else ""
        trips = list
        write("trips", list)
        if (audit) record("trips", title, action, before, encode(list))
    }

    fun setVehicles(list: List<Vehicle>, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(vehicles.value) else ""
        vehicles = list
        write("vehicles", list)
        if (audit) record("vehicles", title, action, before, encode(list))
    }

    fun setWatchlist(list: List<WatchItem>, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(watchlist.value) else ""
        watchlist = list
        write("watchlist", list)
        if (audit) record("watchlist", title, action, before, encode(list))
    }

    fun setLinks(list: List<LinkItem>, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(links.value) else ""
        links = list
        write("links", list)
        if (audit) record("links", title, action, before, encode(list))
    }

    fun setSubscriptions(list: List<Subscription>, title: String, action: String, audit: Boolean = true) {
        val before = if (audit) encode(subscriptions.value) else ""
        subscriptions = list
        write("subscriptions", list)
        if (audit) record("subscriptions", title, action, before, encode(list))
    }

    fun setSettings(next: Settings, title: String = "Settings updated", action: String = "edit") {
        val before = encode(settings)
        settings = next
        write("settings", next)
        record("settings", title, action, before, encode(next))
    }

    fun setAdmin(data: AdminData, title: String, action: String) {
        val before = encode(admin.value)
        admin = data
        write("admin", data)
        record("admin", title, action, before, encode(data), "admin")
    }

    /* ---------------------------------------------------------------------- */
    /* Backup                                                                  */
    /* ---------------------------------------------------------------------- */

    fun backupJson(): String {
        val data = buildJsonObject {
            put("settings", json.parseToJsonElement(encode(settings)))
            put("diary", json.parseToJsonElement(encode(diary.value)))
            put("lists", json.parseToJsonElement(encode(lists.value)))
            put("habits", json.parseToJsonElement(encode(habits.value)))
            put("notes", json.parseToJsonElement(encode(notes.value)))
            put("money", json.parseToJsonElement(encode(moneyData.value)))
            put("passbook", json.parseToJsonElement(encode(passbook.value)))
            put("split", json.parseToJsonElement(encode(split.value)))
            put("trips", json.parseToJsonElement(encode(trips.value)))
            put("vehicles", json.parseToJsonElement(encode(vehicles.value)))
            put("watchlist", json.parseToJsonElement(encode(watchlist.value)))
            put("links", json.parseToJsonElement(encode(links.value)))
            put("subscriptions", json.parseToJsonElement(encode(subscriptions.value)))
            put("admin", json.parseToJsonElement(encode(admin.value)))
        }
        val root = buildJsonObject {
            put("app", "Zone")
            put("version", 1)
            put("exportedAt", Instant.now().toString())
            put("data", data)
        }
        return json.encodeToString(JsonObject.serializer(), root)
    }

    fun importBackup(raw: String): Int {
        val root = try {
            json.parseToJsonElement(raw).jsonObject
        } catch (e: Exception) {
            return -1
        }
        val data: JsonObject = root["data"]?.jsonObject ?: root
        var count = 0

        readSection<Settings>(data, "settings")?.let {
            settings = it; write("settings", it); count++
        }
        readSection<List<DiaryEntry>>(data, "diary")?.let {
            diary = it; write("diary", it); count++
        }
        readSection<List<CheckList>>(data, "lists")?.let {
            lists = it; write("lists", it); count++
        }
        readSection<HabitsData>(data, "habits")?.let {
            habits = it; write("habits", it); count++
        }
        readSection<List<NoteItem>>(data, "notes")?.let {
            notes = it; write("notes", it); count++
        }
        readSection<MoneyData>(data, "money")?.let {
            moneyData = it; write("money", it); count++
        }
        readSection<PassbookData>(data, "passbook")?.let {
            passbook = it; write("passbook", it); count++
        }
        readSection<SplitData>(data, "split")?.let {
            split = it; write("split", it); count++
        }
        readSection<List<Trip>>(data, "trips")?.let {
            trips = it; write("trips", it); count++
        }
        readSection<List<Vehicle>>(data, "vehicles")?.let {
            vehicles = it; write("vehicles", it); count++
        }
        readSection<List<WatchItem>>(data, "watchlist")?.let {
            watchlist = it; write("watchlist", it); count++
        }
        readSection<List<LinkItem>>(data, "links")?.let {
            links = it; write("links", it); count++
        }
        readSection<List<Subscription>>(data, "subscriptions")?.let {
            subscriptions = it; write("subscriptions", it); count++
        }
        readSection<AdminData>(data, "admin")?.let {
            admin = it; write("admin", it); count++
        }

        if (count > 0) {
            audit.value = listOf(
                AuditEvent(
                    id = uid("chg"),
                    at = Instant.now().toString(),
                    section = "admin",
                    title = "Backup imported",
                    detail = "$count sections restored from a Zone backup.",
                    action = "import",
                    status = "approved",
                    source = "admin"
                )
            ) + audit.value
            saveAudit()
        }
        return count
    }

    private inline fun <reified T> readSection(obj: JsonObject, key: String): T? {
        val element = obj[key] ?: return null
        return try {
            json.decodeFromJsonElement(serializer<T>(), element)
        } catch (e: Exception) {
            null
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Formatting helpers                                                      */
    /* ---------------------------------------------------------------------- */

    fun today(): LocalDate = LocalDate.now()

    fun todayISO(): String = today().toString()

    fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value.take(10))
        } catch (e: Exception) {
            null
        }
    }

    private val mediumFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)
    private val shortFormat = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    private val monthFormat = DateTimeFormatter.ofPattern("MMM yyyy", Locale.UK)
    private val longMonthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK)
    private val weekdayFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.UK)

    fun medium(value: String?): String = parseDate(value)?.format(mediumFormat) ?: (value ?: "")

    fun short(value: String?): String = parseDate(value)?.format(shortFormat) ?: (value ?: "")

    fun weekday(value: String?): String = parseDate(value)?.format(weekdayFormat) ?: (value ?: "")

    fun monthKey(value: String?): String = value?.take(7) ?: ""

    fun monthLabel(key: String, long: Boolean = false): String {
        val date = parseDate("$key-01") ?: return key
        return date.format(if (long) longMonthFormat else monthFormat)
    }

    fun addDays(value: String, days: Long): String =
        parseDate(value)?.plusDays(days)?.toString() ?: value

    fun daysBetween(from: String, to: String): Long {
        val a = parseDate(from) ?: return 0
        val b = parseDate(to) ?: return 0
        return ChronoUnit.DAYS.between(a, b)
    }

    fun rel(value: String?): String {
        val date = parseDate(value) ?: return value ?: ""
        val diff = ChronoUnit.DAYS.between(date, today()).toInt()
        return when {
            diff == 0 -> "Today"
            diff == 1 -> "Yesterday"
            diff in 2..6 -> "$diff days ago"
            diff == -1 -> "Tomorrow"
            diff < -1 && diff >= -6 -> "In ${-diff} days"
            else -> medium(value)
        }
    }

    fun startOfWeek(date: LocalDate): LocalDate {
        val offset = ((date.dayOfWeek.value + 5) % 7).toLong()
        return date.minusDays(offset)
    }

    fun weekDates(offsetWeeks: Int = 0): List<String> {
        val start = startOfWeek(today()).plusWeeks(offsetWeeks.toLong())
        return (0..6).map { start.plusDays(it.toLong()).toString() }
    }

    fun currentMonth(): String = todayISO().take(7)

    fun currencySymbol(): String = when (settings.currency) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "INR" -> "₹"
        "AED" -> "AED "
        "AUD" -> "A$"
        "CAD" -> "C$"
        "SGD" -> "S$"
        else -> settings.currency
    }

    fun money(amount: Number, decimals: Int = 0): String {
        val locale = when (settings.currency) {
            "USD" -> Locale.US
            "EUR" -> Locale.GERMANY
            "GBP" -> Locale.UK
            "AED" -> Locale("en", "AE")
            "AUD" -> Locale("en", "AU")
            "CAD" -> Locale.CANADA
            "SGD" -> Locale("en", "SG")
            else -> Locale("en", "IN")
        }
        return try {
            val format = NumberFormat.getCurrencyInstance(locale)
            format.currency = Currency.getInstance(settings.currency)
            format.minimumFractionDigits = decimals
            format.maximumFractionDigits = decimals
            format.format(amount)
        } catch (e: Exception) {
            "${settings.currency} ${"%,.${decimals}f".format(amount)}"
        }
    }

    fun num(amount: Number, decimals: Int = 0): String {
        val locale = if (settings.currency == "INR") Locale("en", "IN") else Locale.US
        val format = NumberFormat.getInstance(locale)
        format.minimumFractionDigits = decimals
        format.maximumFractionDigits = decimals
        return format.format(amount)
    }

    fun initials(text: String, count: Int = 2): String {
        val parts = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "?"
        if (parts.size == 1) return parts[0].take(count).uppercase()
        return (parts.first().take(1) + parts.last().take(1)).uppercase()
    }

    fun toneFor(text: String): Int {
        var sum = 0
        text.forEach { sum = (sum + it.code) % 997 }
        return sum % 6
    }

    fun words(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    fun uidDate(): String = todayISO()
}