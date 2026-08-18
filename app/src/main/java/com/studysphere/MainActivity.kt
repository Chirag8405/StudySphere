package com.studysphere

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysphere.notifications.NotificationScheduler
import com.studysphere.ui.StudySphereApp
import com.studysphere.ui.theme.StudySphereTheme
import com.studysphere.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    // Register before setContent so the launcher is always ready.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Granted or denied — notifications are a convenience feature,
            // the app works fully without them.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule the daily deadline notification job.
        // WorkManager deduplicates via WORK_NAME, so this is safe to call
        // on every launch — it only adjusts the schedule if needed.
        NotificationScheduler.schedule(this)

        // On Android 13+ (API 33), POST_NOTIFICATIONS is a runtime permission.
        // Request it once; if the user denies it, notifications are silently
        // skipped inside DeadlineNotificationWorker.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val viewModel: MainViewModel = viewModel()
            // isDarkModeNullable: null = system default, true = dark, false = light
            val isDarkNullable by viewModel.isDarkModeNullable.collectAsState()
            val systemDark     = isSystemInDarkTheme()
            val isDark         = isDarkNullable ?: systemDark

            // Read start_route extra from widget intents (e.g. "attendance" / "assignments").
            // Null means the app was opened normally (no deep-link needed).
            val startRoute = intent.getStringExtra("start_route")

            StudySphereTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StudySphereApp(viewModel = viewModel, startRoute = startRoute)
                }
            }
        }
    }
}
