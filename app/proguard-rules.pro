# ── General Optimization & Crash Reporting ─────────────────
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Preserve BuildConfig fields
-keep class com.example.workman.BuildConfig { *; }

# ── Keep Project Data Models & Serialized Fields ───────────
-keep class com.example.workman.models.** { *; }
-keep class com.example.workman.model.** { *; }
-keep class com.example.workman.notificationsModel.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Gson ───────────────────────────────────────────────────
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# ── Retrofit 2 ─────────────────────────────────────────────
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ── OkHttp 3 ───────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ── Firebase ───────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Google Play Services ───────────────────────────────────
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── Mappls (MapmyIndia) SDK ────────────────────────────────
-keep class com.mappls.sdk.** { *; }
-dontwarn com.mappls.sdk.**

# ── Coil & Glide Image Loaders ─────────────────────────────
-keep class io.coilkt.** { *; }
-dontwarn io.coilkt.**

-keep class com.github.bumptech.glide.** { *; }
-dontwarn com.github.bumptech.glide.**

# ── Jetpack Compose ────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
