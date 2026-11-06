package com.example.warehouselocationscanner.network

import com.google.gson.annotations.SerializedName

/**
 * Payload for POST /scanner/location_update.
 * Field names match the existing server contract exactly; @SerializedName guarantees
 * the JSON keys stay snake_case even if the Kotlin property names change.
 */
data class LocationUpdateRequest(
    @SerializedName("item") val item: String,
    @SerializedName("location") val location: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("user_id") val userId: String
)
