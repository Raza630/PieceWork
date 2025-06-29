package com.example.workman

//class OverlayService {
//}
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var blackOverlay: View

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        blackOverlay = View(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
        }
        windowManager.addView(blackOverlay, blackOverlay.layoutParams)

        // Remove overlay after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            stopSelf()
        }, 3000) // Adjust delay as needed

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(blackOverlay)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}