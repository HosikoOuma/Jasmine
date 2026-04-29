package com.nkds.hosikoouma.jasmine

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.nkds.hosikoouma.jasmine.ui.JasmineApp
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            val settings by settingsViewModel.settingsState.collectAsState()
            
            // 1. Применяем язык на уровне системы для Android 13+
            LaunchedEffect(settings.language) {
                applyLocaleSystem(settings.language)
            }

            // 2. Создаем конфигурацию и контекст с нужной локалью
            val context = LocalContext.current
            
            val locale = remember(settings.language) {
                if (settings.language == "default") {
                    Resources.getSystem().configuration.locales[0]
                } else {
                    Locale(settings.language)
                }
            }

            val configuration = remember(locale) {
                Configuration(context.resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }

            val localeContext = remember(locale) {
                wrapContextWithLocale(context, locale)
            }

            // 3. Пробрасываем обновленные контекст и конфигурацию.
            // LocalConfiguration ОБЯЗАТЕЛЕН для stringResource()
            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalConfiguration provides configuration,
                LocalActivityResultRegistryOwner provides this@MainActivity,
                LocalLifecycleOwner provides this@MainActivity,
                LocalViewModelStoreOwner provides this@MainActivity,
                LocalSavedStateRegistryOwner provides this@MainActivity,
                LocalOnBackPressedDispatcherOwner provides this@MainActivity
            ) {
                JasmineApp()
            }
        }
    }

    private fun applyLocaleSystem(languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(android.app.LocaleManager::class.java)
            val desiredLocales = if (languageCode == "default") {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList(Locale(languageCode))
            }
            
            if (localeManager.applicationLocales != desiredLocales) {
                localeManager.applicationLocales = desiredLocales
            }
        }
    }

    private fun wrapContextWithLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        val localizedContext = context.createConfigurationContext(config)
        
        return object : android.content.ContextWrapper(context) {
            override fun getResources(): Resources = localizedContext.resources
            override fun getAssets(): android.content.res.AssetManager = localizedContext.assets
            override fun getSystemService(name: String): Any? = localizedContext.getSystemService(name)
        }
    }
}
