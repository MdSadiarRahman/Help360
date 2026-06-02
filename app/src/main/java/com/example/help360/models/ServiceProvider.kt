package com.example.help360.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ServiceProvider(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val categoryName: String = "",
    val address: String = "",
    val district: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val current_location: String? = null,
    val favorites: List<String> = emptyList(),
    val sosSound: String = "default",
    val customSOSMessage: String = "Emergency! I need help at my current location.",
    val trustedContacts: List<TrustedContact> = emptyList(),
    val locationAccuracy: String = "high"
)

data class TrustedContact(
    val id: String = "",
    val name: String = "",
    val phone: String = ""
)
