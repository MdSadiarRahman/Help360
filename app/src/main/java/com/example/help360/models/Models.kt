package com.example.help360.models

data class SOSRequest(
    val id: String = "",
    val user_id: String = "",
    val request_time: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "Active",
    val description: String = ""
)
