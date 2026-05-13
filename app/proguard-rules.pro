# --- Retrofit & Gson ---
-keepattributes Signature, AnnotationDefault, EnclosingMethod, InnerClasses
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class com.nkds.hosikoouma.jasmine.data.LrcLibService { *; }
-keep class com.nkds.hosikoouma.jasmine.datamodels.** { *; }
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# --- Room ---
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-keep class com.nkds.hosikoouma.jasmine.data.**Entity { *; }
-keep class com.nkds.hosikoouma.jasmine.data.**Dao { *; }
-dontwarn androidx.room.**

# --- Hilt / Dagger ---
-keep class dagger.hilt.android.internal.** { *; }
-keep class * extends android.app.Application
-keep class * extends android.app.Activity
-keep class * extends android.app.Service

# --- Media3 / ExoPlayer ---
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- Jasmine Specific ---
-keep class com.nkds.hosikoouma.jasmine.datamodels.** { *; }

# --- Telegram / TDLib ---
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**

# --- Ktor (Streaming Proxy) ---
# Разрешаем R8 удалять неиспользуемый код Ktor, но подавляем варнинги
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.debug.**
-dontwarn com.typesafe.config.**

# Jaudiotagger
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# --- SLF4J ---
-dontwarn org.slf4j.**

# --- Kotlin Serialization ---
-keepclassmembernames class * {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.internal.** { *; }
