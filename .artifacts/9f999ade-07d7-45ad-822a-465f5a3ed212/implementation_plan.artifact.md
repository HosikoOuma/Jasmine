# Удаление SplashScreen API

Удаление интеграции `androidx.core:core-splashscreen`, так как она считается избыточной при наличии внутренних индикаторов загрузки (например, в `TracksScreen`).

## Proposed Changes

### [Component Name] Android Manifest & Themes

#### [MODIFY] [AndroidManifest.xml](file:///C:/PROG/Jasmine/app/src/main/AndroidManifest.xml)
- Изменить тему `MainActivity` с `Theme.Jasmine.Starting` на основную тему приложения `Theme.Jasmine`.

#### [MODIFY] [themes.xml](file:///C:/PROG/Jasmine/app/src/main/res/values/themes.xml)
- Удалить стиль `Theme.Jasmine.Starting`.

#### [MODIFY] [themes.xml (night)](file:///C:/PROG/Jasmine/app/src/main/res/values-night/themes.xml)
- Удалить стиль `Theme.Jasmine.Starting`.

### [Component Name] MainActivity

#### [MODIFY] [MainActivity.kt](file:///C:/PROG/Jasmine/app/src/main/java/com/nkds/hosikoouma/jasmine/MainActivity.kt)
- Удалить вызов `installSplashScreen()`.
- Удалить `setOnExitAnimationListener` и всю связанную логику анимации.
- Удалить неиспользуемые импорты (`androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen`).

### [Component Name] Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///C:/PROG/Jasmine/app/build.gradle.kts)
- Удалить зависимость `libs.androidx.core.splashscreen`.

## Verification Plan

### Manual Verification
- Запустить приложение и убедиться, что оно сразу переходит к `MainActivity` и показывает Compose-интерфейс.
- Убедиться, что при холодном старте показывается системный Splash (стандартное поведение Android), который исчезает сразу после готовности первого кадра, не дожидаясь завершения сканирования.
- Проверить, что `CircularProgressIndicator` в `TracksScreen` по-прежнему отображается, если библиотека еще не загружена.
