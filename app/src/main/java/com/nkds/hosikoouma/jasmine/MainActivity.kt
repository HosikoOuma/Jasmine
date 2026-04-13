package com.nkds.hosikoouma.jasmine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nkds.hosikoouma.jasmine.ui.JasmineApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            // Теперь MainActivity — это просто точка входа.
            // Вся логика темы и контента вынесена в JasmineApp.
            JasmineApp()
        }
    }
}
