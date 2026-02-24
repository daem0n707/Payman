package com.example.payman.domain.bill

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.payman.ai.GroqClient
import com.example.payman.ai.GroqMessage
import com.example.payman.ai.GroqRequest
import com.example.payman.data.local.LogEntry
import com.example.payman.data.local.loadGeminiLogs
import com.example.payman.data.local.loadLogs
import com.example.payman.data.local.saveGeminiLogs
import com.example.payman.data.local.saveLogs
import com.example.payman.data.model.BillItem
import com.example.payman.data.model.ProcessedBill
import com.example.payman.ocr.OcrService
import com.example.payman.util.saveBitmapToInternalStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
            // 1. Perform OCR on all bitmaps
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

            val fullPrompt = "$prompt\n\n---\n$combinedBillText"

            val response = try {
                GroqClient.service.getCompletion(
                    authHeader = "Bearer $apiKey",
                    request = GroqRequest(
                        messages = listOf(GroqMessage(role = "user", content = fullPrompt))
                    )
                )
            } catch (e: Exception) {
                val currentErrorLogs = loadLogs(context).toMutableList()
                currentErrorLogs.add(LogEntry(System.currentTimeMillis(), "Groq API Error: ${e.message}"))
                saveLogs(context, currentErrorLogs)
                throw e
            }

            val rawResponse = response.choices.firstOrNull()?.message?.content ?: ""
            
            val currentGeminiLogs = loadGeminiLogs(context).toMutableList()
            currentGeminiLogs.add(LogEntry(System.currentTimeMillis(), rawResponse))
            saveGeminiLogs(context, currentGeminiLogs)

            val cleaned = rawResponse.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(cleaned)

            val itemsList = mutableListOf<BillItem>()
            val itemsJson = json.getJSONArray("items")
            for (i in 0 until itemsJson.length()) {
                val item = itemsJson.getJSONObject(i)
                itemsList.add(
                    BillItem(
                        name = item.getString("name"),
                        unitPrice = item.optDouble("unitPrice", 0.0),
                        quantity = item.optInt("quantity", 1)
                    )
                )
            }

            // Save only the first bitmap for the thumbnail
            val savedUri = if (bitmaps.isNotEmpty()) saveBitmapToInternalStorage(context, bitmaps[0]) else null

            val bill = ProcessedBill(
                restaurantName = json.optString("restaurantName", "Not available"),
                items = itemsList,
                tax = json.optDouble("tax", 0.0),
                serviceCharge = json.optDouble("serviceCharge", 0.0),
                miscFees = json.optDouble("miscFees", 0.0),
                bookingFees = 0.0,
                imageUri = savedUri,
                bitmap = null
            )
            
            withContext(Dispatchers.Main) {
                onSuccess(bill)
            }
        } catch (e: Exception) {
            val currentErrorLogs = loadLogs(context).toMutableList()
            currentErrorLogs.add(LogEntry(System.currentTimeMillis(), "Processing Error: ${e.message}"))
            saveLogs(context, currentErrorLogs)
            
            withContext(Dispatchers.Main) {
                onError("AI processing failed: ${e.message}")
            }
        }
    }
}
