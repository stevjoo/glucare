package com.example.mapmidtermproject.models

import java.util.Date

data class WoundAnalysis(
    var id: String = "",
    val label: String = "",
    val confidence: Float = 0f,
    val localImagePath: String = "",
    val imageBase64: String = "",
    val timestamp: Date = Date()
)