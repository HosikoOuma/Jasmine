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
# Модели данных храним (важно для сериализации)
-keep class com.nkds.hosikoouma.jasmine.datamodels.** { *; }

# --- Telegram / TDLib ---
# Сохраняем нативные методы для связи с libtdjni.so
-keepclasseswithmembernames class * {
    native <methods>;
}
# Саму библиотеку TDLib не трогаем (она JNI-зависима)
-keep class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**

# --- Ktor (Streaming Proxy) ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.debug.**

# Jaudiotagger
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**
