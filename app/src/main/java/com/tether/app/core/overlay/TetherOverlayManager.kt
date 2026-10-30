package com.tether.app.core.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.tether.app.MainActivity
import com.tether.app.core.designsystem.theme.TetherTheme
import com.tether.app.core.sensor.HapticResonanceEngine

object TetherOverlayManager {
    private var windowManager: WindowManager? = null
    private var containerView: android.view.View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun showTugOverlay(
        context: Context,
        partnerName: String,
        partnerAvatarUri: String? = null,
        onTugBack: () -> Unit = {}
    ) {
        mainHandler.post {
            dismiss()

            Log.d("TetherOverlay", "showTugOverlay executing on main thread. canDrawOverlays=${canDrawOverlays(context)}")
            if (!canDrawOverlays(context)) {
                Log.d("TetherOverlay", "Overlay permission not granted! Requesting...")
                requestOverlayPermission(context)
                return@post
            }

            // Getaran Haptic saat tarikan benang diterima
            HapticResonanceEngine(context).playTugHaptic()

            val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 80
            }

            val owner = OverlayLifecycleOwner()
            owner.onCreate()
            owner.onResume()
            lifecycleOwner = owner

            val container = android.widget.FrameLayout(context.applicationContext).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
            }

            val composeView = ComposeView(context.applicationContext).apply {
                setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    TetherTheme {
                        TetherTugOverlayView(
                            partnerName = partnerName,
                            partnerAvatarUri = partnerAvatarUri,
                            onTugBack = {
                                HapticResonanceEngine(context).playTugHaptic()
                                onTugBack()
                                dismiss()
                            },
                            onOpenChat = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                context.startActivity(intent)
                                dismiss()
                            },
                            onDismiss = {
                                dismiss()
                            }
                        )
                    }
                }
            }

            container.addView(
                composeView,
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )

            containerView = container
            try {
                wm.addView(container, params)
                Log.d("TetherOverlay", "wm.addView SUCCESS with FrameLayout container!")
            } catch (e: Exception) {
                Log.e("TetherOverlay", "wm.addView FAILED", e)
            }

            // Auto dismiss setelah 8 detik jika tidak disentuh
            autoDismissRunnable = Runnable { dismiss() }
            mainHandler.postDelayed(autoDismissRunnable!!, 8000)
        }
    }

    fun dismiss() {
        mainHandler.post {
            autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
            autoDismissRunnable = null

            containerView?.let { view ->
                try {
                    windowManager?.removeView(view)
                } catch (_: Exception) {}
            }
            containerView = null

            lifecycleOwner?.apply {
                onPause()
                onDestroy()
            }
            lifecycleOwner = null
            windowManager = null
        }
    }
}
