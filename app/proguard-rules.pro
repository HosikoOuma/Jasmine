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
# Не обфусцируем модели данных, так как они сериализуются/десериализуются
-keep class com.nkds.hosikoouma.jasmine.datamodels.** { *; }
-keep class com.nkds.hosikoouma.jasmine.data.** { *; }

# Jaudiotagger (для тегов)
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**
