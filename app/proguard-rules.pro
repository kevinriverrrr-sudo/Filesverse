# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.filesverse.app.data.model.** { *; }
-keep class com.filesverse.app.data.local.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-keep class kotlinx.coroutines.** { *; }
