package com.tether.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tether.app.core.designsystem.theme.PixelPaperBg
import com.tether.app.core.designsystem.theme.TetherTheme
import com.tether.app.ui.TetherAppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Anti-tamper & anti-preview di App Switcher (aktif di mode release)
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        setContent {
            TetherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PixelPaperBg
                ) {
                    TetherAppNavHost()
                }
            }
        }
    }
}
