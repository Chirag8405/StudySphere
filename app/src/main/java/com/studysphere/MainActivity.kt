package com.studysphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysphere.ui.StudySphereApp
import com.studysphere.ui.theme.StudySphereTheme
import com.studysphere.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            // isDarkModeNullable: null = system default, true = dark, false = light
            val isDarkNullable by viewModel.isDarkModeNullable.collectAsState()
            val systemDark     = isSystemInDarkTheme()
            val isDark         = isDarkNullable ?: systemDark

            StudySphereTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StudySphereApp(viewModel = viewModel)
                }
            }
        }
    }
}
