# --- Агрессивная обфускация и оптимизация ---

# Включаем максимальную оптимизацию
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses ''

# --- Android & Compose ---
-keep class androidx.compose.ui.platform.** { *; }
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# --- Retrofit & Gson (Network) ---
# Сохраняем всё, что связано с сетевыми запросами и маппингом данных
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keep @retrofit2.http.** class * { *; }
-keepclassmembers class * {
    @retrofit2.http.** *;
}

# Gson
-keep class com.google.gson.** { *; }
-keep @com.google.gson.annotations.SerializedName class * { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Сохраняем ваши Data-модели (entities), иначе Gson не сможет их заполнить
-keep class com.nkds.hosikoouma.jasmine.datamodels.** { *; }
-keep class com.nkds.hosikoouma.jasmine.data.** { *; }

# --- Media3 / ExoPlayer ---
-keep class androidx.media3.** { *; }

# --- Jaudiotagger (Чтение тегов) ---
-keep class net.jthink.jaudiotagger.** { *; }

# --- Coil (Загрузка картинок) ---
-keep class coil.** { *; }

# --- Удаляем логи и неиспользуемый код ---
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Игнорируем предупреждения от сторонних библиотек, которые мешают сборке
-dontwarn net.jthink.jaudiotagger.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.checkerframework.**
