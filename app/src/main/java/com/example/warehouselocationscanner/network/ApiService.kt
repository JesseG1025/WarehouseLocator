package com.example.warehouselocationscanner.network

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * Retrofit API Service defining the network endpoints.
 * Includes the /api/login endpoint as requested.
 */
interface ApiService {
    @POST
    suspend fun login(@Url url: String, @Body request: LoginRequest): Response<LoginResponse>

    /**
     * Sends a completed item -> location pairing to the server.
     * Moved from a hand-built JSON string in MainActivity to Retrofit + Gson so values are
     * escaped properly (a barcode containing a quote can no longer break the JSON) and the
     * request reuses the shared OkHttp client (connection pooling + X-API-Key header).
     * Relative path is resolved against BASE_URL, which is built from BuildConfig.SERVER_IP.
     */
    @POST("scanner/location_update")
    suspend fun updateLocation(@Body request: LocationUpdateRequest): Response<Unit>

    /**
     * Looks up an item on the Flask WMS route /api/wms/item_lookup/<item_code>.
     * Returns a raw JsonElement because the reply schema may evolve; the lookup tab renders
     * whatever fields the server sends instead of crashing on an unexpected shape.
     * @Path URL-encodes the barcode automatically, so slashes/spaces in codes are safe.
     */
    @GET("api/wms/item_lookup/{item_code}")
    suspend fun lookupItem(@Path("item_code") itemCode: String): Response<JsonElement>
}
