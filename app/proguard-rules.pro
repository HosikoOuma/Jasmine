# --- General ---
-keepattributes Signature, AnnotationDefault, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations

# --- Jasmine Models & Entities ---
# Сохраняем все модели данных для сериализации и Room
-keep class com.nkds.hosikoouma.jasmine.datamodels.** { *; }
-keep class com.nkds.hosikoouma.jasmine.data.**Entity { *; }
-keep class com.nkds.hosikoouma.jasmine.data.**Dao { *; }

# --- Retrofit & Gson ---
-keep class com.nkds.hosikoouma.jasmine.data.LrcLibService { *; }
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# --- Room ---
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-dontwarn androidx.room.**

# --- Hilt / Dagger ---
# Hilt обычно поставляет свои правила, но базовые компоненты лучше сохранить явно
-keep class * extends android.app.Application
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *

# --- Media3 / ExoPlayer ---
# Сохраняем всё для работы MediaSession и Player
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- Telegram / TDLib (Критично для JNI) ---
# TDLib использует нативные методы, которые нельзя переименовывать
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**

# --- Ktor (Streaming Proxy) & CIO ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.debug.**
-dontwarn com.typesafe.config.**
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# Jaudiotagger (Чтение метаданных)
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Coil (Загрузка изображений)
-keep class coil.** { *; }
-dontwarn coil.**

# --- Kotlin Serialization ---
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class **$serializer {
    public static final **$serializer INSTANCE;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# --- SLF4J ---
-dontwarn org.slf4j.**


-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}