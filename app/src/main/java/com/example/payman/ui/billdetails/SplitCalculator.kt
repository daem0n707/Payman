package com.example.payman.ui.billdetails

enum class SplitMethod {
    EQUAL,
    PROPORTIONAL, // Economically Fair
    HYBRID        // Balanced
}

object SplitCalculator {

    fun calculateMiscShares(
        foodShares: Map<String, Double>,
        totalMisc: Double,
        method: SplitMethod,
        personCount: Int
    ): Map<String, Double> {
        if (personCount == 0 || totalMisc <= 0) return foodShares.mapValues { 0.0 }

        val totalFood = foodShares.values.sum()
        
        return when (method) {
            SplitMethod.EQUAL -> {
                foodShares.mapValues { totalMisc / personCount }
            }
            SplitMethod.PROPORTIONAL -> {
                if (totalFood <= 0) foodShares.mapValues { totalMisc / personCount }
                else foodShares.mapValues { (_, food) ->
                    totalMisc * (food / totalFood)
                }
            }
            SplitMethod.HYBRID -> {
                val equalPortionPool = totalMisc * 0.5
                val propPortionPool = totalMisc * 0.5
                foodShares.mapValues { (_, food) ->
                    if (totalFood <= 0) (totalMisc / personCount)
                    else (equalPortionPool / personCount) + (propPortionPool * (food / totalFood))
                }
            }
        }
    }

    fun calculateInequalityPercentage(foodShares: Map<String, Double>, totalMisc: Double, personCount: Int): Double {
        if (foodShares.isEmpty() || totalMisc <= 0 || personCount == 0) return 0.0
        
        val leastSpenderId = foodShares.minByOrNull { it.value }?.key ?: return 0.0
        val fi = foodShares[leastSpenderId] ?: return 0.0
        val totalFood = foodShares.values.sum()

        if (totalFood <= 0) return 0.0

        val oldMisc = totalMisc / personCount
        val newMisc = totalMisc * (fi / totalFood)

        if (newMisc <= 0) return 0.0

        return ((oldMisc - newMisc) / newMisc) * 100.0
    }
}
