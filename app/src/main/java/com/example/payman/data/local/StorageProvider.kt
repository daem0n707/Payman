package com.example.payman.data.local

import android.content.Context
import android.net.Uri
import com.example.payman.data.model.BillItem
import com.example.payman.data.model.Group
import com.example.payman.data.model.Person
import com.example.payman.data.model.ProcessedBill
import org.json.JSONArray
import org.json.JSONObject

fun loadApiKey(context: Context): String {
    return context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("api_key", "") ?: ""
}

fun saveApiKey(context: Context, key: String) {
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("api_key", key).apply()
}

fun savePeople(context: Context, people: List<Person>) {
    val arr = JSONArray()
    people.forEach { p ->
        val obj = JSONObject()
        obj.put("id", p.id)
        obj.put("name", p.name)
        arr.put(obj)
    }
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("people", arr.toString()).apply()
}

fun loadPeople(context: Context): List<Person> {
    val str = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("people", "[]") ?: "[]"
    val list = mutableListOf<Person>()
    val arr = JSONArray(str)
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(Person(obj.getString("id"), obj.getString("name")))
    }
    return list
}

fun saveGroups(context: Context, groups: List<Group>) {
    val arr = JSONArray()
    groups.forEach { g ->
        val obj = JSONObject()
        obj.put("id", g.id)
        obj.put("name", g.name)
        val members = JSONArray()
        g.memberIds.forEach { members.put(it) }
        obj.put("members", members)
        arr.put(obj)
    }
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("groups", arr.toString()).apply()
}

fun loadGroups(context: Context): List<Group> {
    val str = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("groups", "[]") ?: "[]"
    val list = mutableListOf<Group>()
    val arr = JSONArray(str)
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val membersArr = obj.getJSONArray("members")
        val members = mutableListOf<String>()
        for (j in 0 until membersArr.length()) members.add(membersArr.getString(j))
        list.add(Group(obj.getString("id"), obj.getString("name"), members))
    }
    return list
}

fun saveBills(context: Context, bills: List<ProcessedBill>) {
    val arr = JSONArray()
    bills.filter { !it.isProcessing }.forEach { b ->
        arr.put(processedBillToJson(b))
    }
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("bills", arr.toString()).apply()
}

fun loadBills(context: Context): List<ProcessedBill> {
    val str = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("bills", "[]") ?: "[]"
    val list = mutableListOf<ProcessedBill>()
    val arr = JSONArray(str)
    for (i in 0 until arr.length()) {
        list.add(jsonToProcessedBill(arr.getJSONObject(i)))
    }
    return list
}

fun saveDeletedBills(context: Context, bills: Map<Long, ProcessedBill>) {
    val arr = JSONArray()
    bills.forEach { (timestamp, bill) ->
        val obj = JSONObject()
        obj.put("deletionTimestamp", timestamp)
        obj.put("bill", processedBillToJson(bill))
        arr.put(obj)
    }
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("deleted_bills", arr.toString()).apply()
}

fun loadDeletedBills(context: Context): Map<Long, ProcessedBill> {
    val str = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("deleted_bills", "[]") ?: "[]"
    val map = mutableMapOf<Long, ProcessedBill>()
    val arr = JSONArray(str)
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val bill = jsonToProcessedBill(obj.getJSONObject("bill"))
        map[obj.getLong("deletionTimestamp")] = bill
    }
    return map
}

private fun processedBillToJson(b: ProcessedBill): JSONObject {
    val obj = JSONObject()
    obj.put("id", b.id)
    obj.put("restaurantName", b.restaurantName)
    obj.put("payeeName", b.payeeName)
    obj.put("payeeId", b.payeeId)
    obj.put("tax", b.tax)
    obj.put("serviceCharge", b.serviceCharge)
    obj.put("miscFees", b.miscFees)
    obj.put("discountPercentage", b.discountPercentage)
    obj.put("discountAmount", b.discountAmount)
    obj.put("isDiscountApplied", b.isDiscountApplied)
    obj.put("isDiscountFixedAmount", b.isDiscountFixedAmount)
    obj.put("isSwiggyDineoutApplied", b.isSwiggyDineoutApplied)
    obj.put("imageUri", b.imageUri?.toString())
    obj.put("sectionName", b.sectionName)
    obj.put("timestamp", b.timestamp)

    val itemsArr = JSONArray()
    b.items.forEach { item ->
        val itemObj = JSONObject()
        itemObj.put("id", item.id)
        itemObj.put("name", item.name)
        itemObj.put("unitPrice", item.unitPrice)
        itemObj.put("quantity", item.quantity)
        val assignedIds = JSONArray()
        item.assignedPersonIds.forEach { assignedIds.put(it) }
        itemObj.put("assignedPersonIds", assignedIds)
        itemsArr.put(itemObj)
    }
    obj.put("items", itemsArr)

    val participatingIds = JSONArray()
    b.participatingPersonIds.forEach { participatingIds.put(it) }
    obj.put("participatingPersonIds", participatingIds)
    return obj
}

private fun jsonToProcessedBill(obj: JSONObject): ProcessedBill {
    val itemsArr = obj.getJSONArray("items")
    val items = mutableListOf<BillItem>()
    for (j in 0 until itemsArr.length()) {
        val itemObj = itemsArr.getJSONObject(j)
        val assignedArr = itemObj.getJSONArray("assignedPersonIds")
        val assigned = mutableListOf<String>()
        for (k in 0 until assignedArr.length()) assigned.add(assignedArr.getString(k))
        items.add(
            BillItem(
                itemObj.getString("id"),
                itemObj.getString("name"),
                itemObj.getDouble("unitPrice"),
                itemObj.getInt("quantity"),
                assigned
            )
        )
    }

    val participatingArr = obj.getJSONArray("participatingPersonIds")
    val participating = mutableListOf<String>()
    for (j in 0 until participatingArr.length()) participating.add(participatingArr.getString(j))

    return ProcessedBill(
        id = obj.getString("id"),
        restaurantName = obj.getString("restaurantName"),
        payeeName = obj.optString("payeeName", ""),
        payeeId = obj.optString("payeeId", null),
        items = items.toMutableList(),
        tax = obj.getDouble("tax"),
        serviceCharge = obj.getDouble("serviceCharge"),
        miscFees = obj.getDouble("miscFees"),
        discountPercentage = obj.optDouble("discountPercentage", 0.0),
        discountAmount = obj.optDouble("discountAmount", 0.0),
        isDiscountApplied = obj.optBoolean("isDiscountApplied", false),
        isDiscountFixedAmount = obj.optBoolean("isDiscountFixedAmount", false),
        isSwiggyDineoutApplied = obj.optBoolean("isSwiggyDineoutApplied", false),
        imageUri = obj.optString("imageUri", null)?.let { Uri.parse(it) },
        participatingPersonIds = participating,
        sectionName = obj.optString("sectionName", null),
        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
    )
}

fun saveSwiggyDineoutOption(context: Context, enabled: Boolean) {
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putBoolean("swiggy_dineout_option", enabled).apply()
}

fun loadSwiggyDineoutOption(context: Context): Boolean {
    return context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getBoolean("swiggy_dineout_option", true)
}


data class LogEntry(val timestamp: Long, val message: String)

fun saveLogs(context: Context, logs: List<LogEntry>) {
    val arr = JSONArray()
    logs.forEach { log ->
        val obj = JSONObject()
        obj.put("timestamp", log.timestamp)
        obj.put("message", log.message)
        arr.put(obj)
    }
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("error_logs", arr.toString()).apply()
}

fun loadLogs(context: Context): List<LogEntry> {
    val str = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("error_logs", "[]") ?: "[]"
    val list = mutableListOf<LogEntry>()
    val arr = JSONArray(str)
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(LogEntry(obj.getLong("timestamp"), obj.getString("message")))
    }
    return list.sortedByDescending { it.timestamp }
}

fun saveGeminiLogs(context: Context, logs: List<LogEntry>) {
    val arr = JSONArray()
    logs.forEach { log ->
        val obj = JSONObject()
        obj.put("timestamp", log.timestamp)
        obj.put("message", log.message)
        arr.put(obj)
    }
    context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).edit().putString("gemini_logs", arr.toString()).apply()
}

fun loadGeminiLogs(context: Context): List<LogEntry> {
    val str = context.getSharedPreferences("payman_prefs", Context.MODE_PRIVATE).getString("gemini_logs", "[]") ?: "[]"
    val list = mutableListOf<LogEntry>()
    val arr = JSONArray(str)
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(LogEntry(obj.getLong("timestamp"), obj.getString("message")))
    }
    return list.sortedByDescending { it.timestamp }
}
