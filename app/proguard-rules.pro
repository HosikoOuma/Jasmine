# --- ЭКСТРЕМАЛЬНАЯ ОБФУСКАЦИЯ И ОПТИМИЗАЦИЯ (JASMINE) ---

# 1. Основные настройки оптимизации
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses ''

# 2. Использование словаря для запутывания имен
-obfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

# 3. Удаление отладочной информации
-renamesourcefileattribute SourceFile
-keepattributes !SourceFile, !LineNumberTable, *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceDebugExtension

# 4. Android & Compose
-keep class androidx.compose.ui.platform.** { *; }
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# 5. Room Database (УЛУЧШЕННАЯ ЗАЩИТА)
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# Сохраняем сгенерированные реализации Room
-keep class *__Impl { *; }
-keep class androidx.room.RoomDatabase { _query(...); }

# Принудительно сохраняем конструкторы сущностей и DAO
-keepclassmembers class * {
    @androidx.room.Database *;
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# 6. Retrofit & Gson (Network)
# Сохраняем всё для корректной работы десериализации
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

-keep interface com.nkds.hosikoouma.jasmine.data.LrcLibService {
    <methods>;
}
-keepclassmembers interface com.nkds.hosikoouma.jasmine.data.LrcLibService {
    @retrofit2.http.** <methods>;
}

# Модели данных: запрещаем удалять конструкторы и менять поля
-keep class com.nkds.hosikoouma.jasmine.datamodels.** { *; }
-keepclassmembers class com.nkds.hosikoouma.jasmine.datamodels.** {
    <init>(...);
    <fields>;
}

# Правила для Gson
-keep class com.google.gson.** { *; }
-keep @interface com.google.gson.annotations.SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 7. Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# 8. Jaudiotagger
-keep class net.jthink.jaudiotagger.** { *; }
-dontwarn net.jthink.jaudiotagger.**

# 9. Coil
-keep class coil.** { *; }

# 10. Удаление логов (ОСТАВЛЯЕМ ERROR И WARN ДЛЯ ОТЛАДКИ)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Игнорируем предупреждения
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.checkerframework.**
-dontwarn androidx.room.paging.**
