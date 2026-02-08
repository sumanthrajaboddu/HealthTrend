package com.healthtrend.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.healthtrend.app.ui.navigation.HealthTrendNavHost
import com.healthtrend.app.ui.theme.HealthTrendTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * POST_NOTIFICATIONS runtime permission launcher (API 33+).
     * Denial is handled gracefully — reminders simply won't fire (AC #5).
     * No error UI shown on denial per UX constraints.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // No-op: permission granted → notifications work.
            // Permission denied → notifications silently won't fire.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            HealthTrendTheme {
                HealthTrendNavHost()
            }
        }
    }

    /**
     * Request POST_NOTIFICATIONS permission on API 33+ if not already granted.
     * Only POST_NOTIFICATIONS — no camera, location, storage, or contacts (AC #5).
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }
}
