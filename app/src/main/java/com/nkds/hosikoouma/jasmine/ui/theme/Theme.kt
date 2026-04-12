package com.nkds.hosikoouma.jasmine.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun JasmineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledMode: Boolean = false,
    useDynamicColor: Boolean = true,
    seedColor: Color = Color(0xFF6750A4),
    paletteStyle: String = "TonalSpot",
    typography: Typography = Typography,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    
    val style = try {
        PaletteStyle.valueOf(paletteStyle)
    } catch (e: Exception) {
        PaletteStyle.TonalSpot
    }

    // Настройка статус-бара и навигации для всех режимов темы
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            
            // Прозрачный фон статус-бара (чтобы контент заезжал под него)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            // Настройка цвета иконок: темные для светлой темы, светлые для темной
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val isSystemDynamic = useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    if (isSystemDynamic) {
        val colorScheme = if (darkTheme) {
            dynamicDarkColorScheme(context).let { 
                if (amoledMode) it.copy(surface = Color.Black, background = Color.Black) else it 
            }
        } else {
            dynamicLightColorScheme(context)
        }
        
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    } else {
        DynamicMaterialTheme(
            seedColor = seedColor,
            useDarkTheme = darkTheme,
            style = style,
            animate = true,
            withAmoled = amoledMode,
            typography = typography,
            content = content
        )
    }
}
