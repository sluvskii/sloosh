# sloosh release rules.
# The app relies on reflection in three places: Gson model (de)serialization,
# Retrofit service interfaces, and WebView JS bridges. Everything else is
# regular Kotlin/Compose and shrinks freely.

# --- Crash logs stay readable ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Gson models (data layer is serialized wholesale via TypeToken) ---
-keep class com.slooshfilm.app.data.api.** { *; }
-keep class com.slooshfilm.app.data.alloha.** { *; }
-keep class com.slooshfilm.app.data.storage.** { *; }
-keep class com.slooshfilm.app.data.messenger.** { *; }
-keep class com.slooshfilm.app.data.repository.** { *; }
# Generic signatures are required by Gson TypeToken / Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# Gson also instantiates models via reflection: keep a no-arg ctor reachable
-keepclassmembers class com.slooshfilm.app.data.** {
    <init>();
}

# --- WebView JS bridges ---
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.slooshfilm.app.data.alloha.AllohaParser* { *; }
-keep class com.slooshfilm.app.data.alloha.AllohaRuntimeResolver* { *; }

# --- Retrofit (consumer rules exist upstream, these are belt-and-suspenders) ---
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- kotlinx-serialization (declared for future use; harmless if unused) ---
-dontnote kotlinx.serialization.**

# --- Enums used via values()/valueOf() in reflective paths ---
-keepclassmembers enum com.slooshfilm.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
