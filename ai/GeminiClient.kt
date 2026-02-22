package com.example.payman.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig

fun getGenerativeModel(apiKey: String): GenerativeModel {
    return GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = apiKey,
        generationConfig = generationConfig { responseMimeType = "application/json" },
        systemInstruction = content {
            text("You are a professional bill parser. Extract items, quantities, unit prices, tax, service charge, misc fees, and restaurant name from the provided bill. IMPORTANT: Extract any parcel, takeaway, packing, or container charges as individual items so they can be assigned to specific people. Return ONLY a valid JSON object. If quantity is not clear, default to 1. Do not include any conversational text or markdown code blocks.")
        }
    )
}
