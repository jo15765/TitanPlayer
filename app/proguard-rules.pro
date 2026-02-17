# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Retrofit / Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn retrofit2.**
-keep class com.example.xtreamtvapp.data.** { *; }

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
