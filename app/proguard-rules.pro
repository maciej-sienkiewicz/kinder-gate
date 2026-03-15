# KinderGate ProGuard Rules

# Keep domain models (serialized to JSON for persistence)
-keep class pl.kindergate.domain.model.** { *; }

# Keep Room entities
-keep class pl.kindergate.data.local.db.entity.** { *; }

# Keep Hilt generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }

# EncryptedSharedPreferences / Tink
-keep class com.google.crypto.tink.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker

# Keep service and receiver classes referenced in manifest
-keep class pl.kindergate.service.** { *; }
-keep class pl.kindergate.receiver.** { *; }
