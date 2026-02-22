# Payman 🧾💸

**Payman** is a modern, AI-powered Android application designed to take the headache out of splitting restaurant bills and tracking group expenses. Unlike traditional splitters, Payman uses a combination of **On-Device OCR** and **LLM Intelligence** to accurately parse complex bills, handle multi-page receipts, and simplify debts across entire trips or events.

Note that I completely vibe coded this app over the weekend, I had no idea how an android app works until two days ago. If the code looks janky, please do not mind.

## ✨ Features

### 🧠 Intelligent Bill Processing
*   **Multi-Image Support:** Capture long receipts by taking multiple photos. Payman automatically merges the text and de-duplicates overlapping items.
*   **AI Parsing:** Leverages the Groq API to convert raw OCR text into structured data, identifying item names, unit prices, quantities, taxes, and service charges.
*   **Local OCR:** Uses Google ML Kit for fast, privacy-focused text recognition directly on your device.

### 🍱 Organized Bill Management
*   **Sections:** Group your bills into custom categories like "Trip to Goa" or "Weekend Brunch" to keep your history organized.
*   **Drag & Drop:** Move bills between sections with a long-press and drag gesture.
*   **Recycle Bin:** Deleted bills are held for 30 days before permanent removal, allowing for easy restoration.

### ⚖️ Advanced Splitting Logic
*   **Individual Item Assignment:** Assign specific items to specific people. If multiple people share an item, the cost (and quantity) is split perfectly among them.
*   **Payee Tracking:** Designate who paid the bill. The app automatically tracks who owes money to the payer.
*   **Smart Split:** A simple algorithm that simplifies debts across multiple bills. If A owes B ₹100 and B owes A ₹40, Payman simplifies it to "A owes B ₹60."
*   **Comprehensive Discounts:** Supports both percentage and fixed-amount discounts for dineout offers from Swiggy and Zomato. It even includes a dedicated toggle inside settings for the **Swiggy Dineout Card (10%)** discount in case you own the card and would like to split the cashback recieved from it.
*   **Misc Fees:** Handles convenience fees or dineout booking charges separately, ensuring they aren't discounted with the dineout offer.

### 👥 People & Groups
*   **Collapsible Groups:** Create groups for your friend circles. You can expand a group in Settings to see exactly who is in it.
*   **Quick Add:** Add entire groups to a bill with a single click.

### 📊 Spending Insights
*   **Monthly Breakdown:** View your total spending month-by-month.
*   **Category-wise History:** See exactly where your money went, grouped by bill sections.
*   **Individual Stats:** Filter stats by person to see their specific shares and contribution history.

### 🛠️ Utilities
*   **In-App Calculator:** A handy dark-themed calculator for quick math.
*   **Detailed Logs:** View AI responses and error logs to troubleshoot any processing issues.
*   **Usage Guide:** A built-in guide explaining the nuances of the splitting logic and the app's features.

## 🚀 Tech Stack

*   **Language:** 100% Kotlin
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Concurrency:** Kotlin Coroutines
*   **Networking:** Retrofit & OkHttp
*   **AI/ML:** 
    *   Google ML Kit (Text Recognition)
    *   Groq API (Generative AI for JSON Extraction)
*   **Image Loading:** Coil
*   **Local Storage:** SharedPreferences (with JSON serialization)

## 🛠️ Setup

1.  Download the APK file from releases and install the app.
2.  Obtain an API key from [Groq](https://console.groq.com/).
3.  Open the app and navigate to **Settings** (Sidebar).
4.  Paste your **Groq API Key** into the provided field.
5.  Start scanning!

## 📸 Screenshots

*(Add your screenshots here)*

---
*Built with ❤️ for people who are tired of manual bill calculations.*