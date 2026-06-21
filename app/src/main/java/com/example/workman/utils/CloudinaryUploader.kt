package com.example.workman.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Free image hosting via Cloudinary (no Firebase Storage / Blaze billing needed).
 *
 * SETUP (one-time, free):
 * 1. Go to https://cloudinary.com and create a free account.
 * 2. On your Dashboard, copy your "Cloud name" → paste into CLOUD_NAME below.
 * 3. Go to Settings (gear) → Upload → Upload presets → "Add upload preset".
 *    - Set "Signing Mode" to **Unsigned**.
 *    - Copy the preset name → paste into UPLOAD_PRESET below.
 *
 * That's it. Unsigned uploads need no API secret in the app (safe for client side).
 */
object CloudinaryUploader {

    // ⬇️ REPLACE THESE TWO VALUES WITH YOURS FROM CLOUDINARY ⬇️
    private const val CLOUD_NAME = "dxnxihs29"
    private const val UPLOAD_PRESET = "workman_unsigned"
    // ⬆️ ----------------------------------------------------- ⬆️

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = CLOUD_NAME != "YOUR_CLOUD_NAME" && UPLOAD_PRESET != "YOUR_UNSIGNED_PRESET"

    /**
     * Uploads a single image Uri to Cloudinary.
     * @param folder optional Cloudinary folder (e.g. "workOffers", "profiles", "portfolio").
     * @return the secure HTTPS URL of the uploaded image, or null on failure.
     */
    suspend fun uploadImage(
        context: Context,
        uri: Uri,
        folder: String = "workman"
    ): String? = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext null
        }
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "image_${System.currentTimeMillis()}.jpg",
                    bytes.toRequestBody("image/*".toMediaTypeOrNull())
                )
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("folder", folder)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                JSONObject(body).optString("secure_url").ifEmpty { null }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Uploads multiple images sequentially. Returns only the URLs that succeeded.
     */
    suspend fun uploadImages(
        context: Context,
        uris: List<Uri>,
        folder: String = "workman"
    ): List<String> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uploadImage(context, it, folder) }
    }
}

