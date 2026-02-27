package com.example.payman.domain.bill

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.payman.ai.GroqClient
import com.example.payman.ai.GroqMessage
import com.example.payman.ai.GroqRequest
import com.example.payman.data.local.LogEntry
import com.example.payman.data.local.loadGroqLogs
import com.example.payman.data.local.loadLogs
import com.example.payman.data.local.saveGroqLogs
import com.example.payman.data.local.saveLogs
import com.example.payman.data.model.BillItem
import com.example.payman.data.model.ProcessedBill
import com.example.payman.ocr.OcrService
import com.example.payman.util.saveBitmapToInternalStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.JsonObject

private val gson = Gson()

suspend fun processBillImage(
    context: Context,
    bitmaps: List<Bitmap>,
    apiKey: String,
    imageUris: List<Uri?>,
    onSuccess: (ProcessedBill) -> Unit,
    onError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val extractedTexts = bitmaps.map { bitmap ->
                OcrService.recognizeText(bitmap).await().text
            }
            val combinedBillText = extractedTexts.joinToString("\n\n---\n\n")

            val prompt = """
                Extract unique items, unit prices, and quantities from this bill.
                De-duplicate overlapping items.
                Extract restaurantName, tax, serviceCharge, and miscFees.
                Return as JSON:
                {
                  "restaurantName": "string",
                  "items": [{"name": "string", "unitPrice": number, "quantity": number}],
                  "tax": number,
                  "serviceCharge": number,
                  "miscFees": number
                }
            """.trimIndent()

            val response = try {
                GroqClient.service.getCompletion(
                    authHeader = "Bearer $apiKey",
                    request = GroqRequest(
                        messages = listOf(GroqMessage(role = "user", content = "$prompt\n\n---\n$combinedBillText"))
                    )
                )
            } catch (e: Exception) {
                logError(context, "Groq API Error: ${e.message}")
                throw e
            }

            val rawResponse = response.choices.firstOrNull()?.message?.content ?: ""
            logGroqResponse(context, rawResponse)

            val cleaned = rawResponse.replace("```json", "").replace("```", "").trim()
            val json = gson.fromJson(cleaned, JsonObject::class.java)

            val itemsList = json.getAsJsonArray("items").map {
                val item = it.asJsonObject
                BillItem(
                    name = item.get("name").asString,
                    unitPrice = item.get("unitPrice")?.asDouble ?: 0.0,
                    quantity = item.get("quantity")?.asInt ?: 1
                )
            }.toMutableList()

            val savedUri = bitmaps.firstOrNull()?.let { saveBitmapToInternalStorage(context, it) }

            val bill = ProcessedBill(
                restaurantName = json.get("restaurantName")?.asString ?: "Not available",
                items = itemsList,
                tax = json.get("tax")?.asDouble ?: 0.0,
                serviceCharge = json.get("serviceCharge")?.asDouble ?: 0.0,
                miscFees = json.get("miscFees")?.asDouble ?: 0.0,
                bookingFees = 0.0,
                imageUri = savedUri,
                bitmap = null
            )
            
            withContext(Dispatchers.Main) { onSuccess(bill) }
        } catch (e: Exception) {
            logError(context, "Processing Error: ${e.message}")
            withContext(Dispatchers.Main) { onError("AI processing failed: ${e.message}") }
        }
    }
}

private fun logError(context: Context, message: String) {
    val logs = loadLogs(context).toMutableList().apply { add(LogEntry(System.currentTimeMillis(), message)) }
    saveLogs(context, logs)
}

private fun logGroqResponse(context: Context, message: String) {
    val logs = loadGroqLogs(context).toMutableList().apply { add(LogEntry(System.currentTimeMillis(), message)) }
    saveGroqLogs(context, logs)
}
