package com.example.payman.data.local

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.payman.data.model.Group
import com.example.payman.data.model.Person
import com.example.payman.data.model.ProcessedBill
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

private class UriAdapter : JsonSerializer<Uri>, JsonDeserializer<Uri> {
    override fun serialize(src: Uri, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Uri? {
        return try {
            if (json.isJsonPrimitive) {
                Uri.parse(json.asString)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

private class BitmapAdapter : JsonSerializer<Bitmap>, JsonDeserializer<Bitmap> {
    override fun serialize(src: Bitmap, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = JsonNull.INSTANCE
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Bitmap? = null
}

private val gson = GsonBuilder()
    .registerTypeHierarchyAdapter(Uri::class.java, UriAdapter())
    .registerTypeHierarchyAdapter(Bitmap::class.java, BitmapAdapter())
    .create()

fun loadApiKey(context: Context): String {
    return context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("api_key", "") ?: ""
}

fun saveApiKey(context: Context, key: String) {
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("api_key", key).apply()
}

fun savePeople(context: Context, people: List<Person>) {
    val json = gson.toJson(people)
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("people", json).apply()
}

fun loadPeople(context: Context): List<Person> {
    val json = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("people", "[]") ?: "[]"
    return try {
        val type = object : TypeToken<List<Person>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveGroups(context: Context, groups: List<Group>) {
    val json = gson.toJson(groups)
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("groups", json).apply()
}

fun loadGroups(context: Context): List<Group> {
    val json = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("groups", "[]") ?: "[]"
    return try {
        val type = object : TypeToken<List<Group>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveBills(context: Context, bills: List<ProcessedBill>) {
    val filteredBills = bills.filter { !it.isProcessing }
    val json = gson.toJson(filteredBills)
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("bills", json).apply()
}

fun loadBills(context: Context): List<ProcessedBill> {
    val json = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("bills", "[]") ?: "[]"
    return try {
        val type = object : TypeToken<List<ProcessedBill>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveDeletedBills(context: Context, bills: Map<Long, ProcessedBill>) {
    val json = gson.toJson(bills)
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("deleted_bills", json).apply()
}

fun loadDeletedBills(context: Context): Map<Long, ProcessedBill> {
    val json = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("deleted_bills", "{}") ?: "{}"
    return try {
        val type = object : TypeToken<Map<Long, ProcessedBill>>() {}.type
        gson.fromJson(json, type) ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }
}

fun saveSwiggyHdfcOption(context: Context, enabled: Boolean) {
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putBoolean("swiggy_hdfc_option", enabled).apply()
}

fun loadSwiggyHdfcOption(context: Context): Boolean {
    return context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getBoolean("swiggy_hdfc_option", true)
}

data class LogEntry(val timestamp: Long, val message: String)

fun saveLogs(context: Context, logs: List<LogEntry>) {
    val json = gson.toJson(logs)
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("error_logs", json).apply()
}

fun loadLogs(context: Context): List<LogEntry> {
    val json = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("error_logs", "[]") ?: "[]"
    return try {
        val type = object : TypeToken<List<LogEntry>>() {}.type
        val list: List<LogEntry> = gson.fromJson(json, type) ?: emptyList()
        list.sortedByDescending { it.timestamp }
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveGeminiLogs(context: Context, logs: List<LogEntry>) {
    val json = gson.toJson(logs)
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("gemini_logs", json).apply()
}

fun loadGeminiLogs(context: Context): List<LogEntry> {
    val json = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("gemini_logs", "[]") ?: "[]"
    return try {
        val type = object : TypeToken<List<LogEntry>>() {}.type
        val list: List<LogEntry> = gson.fromJson(json, type) ?: emptyList()
        list.sortedByDescending { it.timestamp }
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveGroqLogs(context: Context, logs: List<LogEntry>) {
    val json = gson.toJson(logs)
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("groq_logs", json).apply()
}

fun loadGroqLogs(context: Context): List<LogEntry> {
    val json = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("groq_logs", "[]") ?: "[]"
    return try {
        val type = object : TypeToken<List<LogEntry>>() {}.type
        val list: List<LogEntry> = gson.fromJson(json, type) ?: emptyList()
        list.sortedByDescending { it.timestamp }
    } catch (e: Exception) {
        emptyList()
    }
}
