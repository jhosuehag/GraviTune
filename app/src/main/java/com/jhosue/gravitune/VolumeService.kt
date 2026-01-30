package com.jhosue.gravitune

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat

class VolumeService : Service() {

    private lateinit var windowManager: WindowManager
    private var handleWindowView: View? = null
    private var panelWindowView: View? = null
    private var touchContainer: View? = null 
    private var visualHandle: View? = null
    private var audioManager: AudioManager? = null
    
    // Absolute Anchor for Handle Position (User Defined)
    private var savedHandleY = 0
    private var currentStyle = "line"
    private var isPanelVisible = false
    
    // Params for the permanently visible handle
    private lateinit var handleParams: WindowManager.LayoutParams

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideVolumePanel() }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        
        if (handleWindowView != null && handleWindowView?.isAttachedToWindow == true) {
            val screenHeight = resources.displayMetrics.heightPixels
            val minY = dpToPx(50f)
            val maxY = screenHeight - dpToPx(150f) // Keep clear of nav bar
            
            // Re-clamp current position to new dimensions
            val currentY = handleParams.y
            val clampedY = Math.max(minY, Math.min(maxY, currentY))
            
            if (currentY != clampedY) {
                handleParams.y = clampedY
                savedHandleY = clampedY
                windowManager.updateViewLayout(handleWindowView, handleParams)
            }
            
            // Re-check gravity side (screen width changed)
            val screenWidth = resources.displayMetrics.widthPixels
            val prefs = getSharedPreferences("gravitune_prefs", Context.MODE_PRIVATE)
            val isRightSide = prefs.getBoolean("is_right_side", false)
            updateHandleGravity(isRightSide)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        setupHandleOverlay()
    }

    private fun getShiftOffset(): Int {
        val prefs = getSharedPreferences("gravitune_prefs", Context.MODE_PRIVATE)
        val style = prefs.getString("handle_style", "line")
        val panelH = dpToPx(240f)
        val handleH = if (style == "circle") dpToPx(40f) else dpToPx(74f)
        return (panelH - handleH) / 2
    }

    private fun setupHandleOverlay() {
        val prefs = getSharedPreferences("gravitune_prefs", Context.MODE_PRIVATE)
        currentStyle = prefs.getString("handle_style", "line") ?: "line"
        val isRightSide = prefs.getBoolean("is_right_side", false)

        handleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        
        // Initial Position
        handleParams.x = 0 
        var defaultY = 0
        if (currentStyle == "circle") {
             defaultY = dpToPx(150f)
        }
        val rawSavedY = prefs.getInt("saved_y", defaultY)
        
        // Safety Clamp (Rescue if stuck)
        val screenHeight = resources.displayMetrics.heightPixels
        val minY = dpToPx(50f) // Avoid Status Bar
        val maxY = screenHeight - dpToPx(150f) // Avoid Nav Bar
        
        savedHandleY = Math.max(minY, Math.min(maxY, rawSavedY))
        handleParams.y = savedHandleY

        val inflater = LayoutInflater.from(this)
        handleWindowView = inflater.inflate(R.layout.layout_handle, null)
        // handleWindowView IS the container (root of XML), so we cast it directly
        touchContainer = handleWindowView 
        visualHandle = handleWindowView?.findViewById(R.id.visual_handle)
        
        // Apply Style
        updateHandleStyle()
        
        // Set Gravity
        updateHandleGravity(isRightSide)

        setupTouchListener()

        try {
            // Start invisible to avoid pop-in
            handleWindowView?.alpha = 0f
            windowManager.addView(handleWindowView, handleParams)
            handleWindowView?.animate()?.alpha(1f)?.setDuration(300)?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun updateHandleStyle() {
        if (currentStyle == "circle") {
            visualHandle?.setBackgroundResource(R.drawable.bg_handle_circle)
            
            val sizePx = dpToPx(40f) // Touch & Visual are same
            
            // Container
            touchContainer?.layoutParams?.width = sizePx
            touchContainer?.layoutParams?.height = sizePx
            touchContainer?.setBackgroundColor(0x00000000) // Clear for circle (it has its own bg)
            touchContainer?.requestLayout()
            
            // Visual
            val lp = visualHandle?.layoutParams as? FrameLayout.LayoutParams
            lp?.width = sizePx
            lp?.height = sizePx
            lp?.gravity = Gravity.CENTER // Circle always centered in container
            visualHandle?.layoutParams = lp
            
        } else {
            // Line Style
            visualHandle?.setBackgroundResource(R.drawable.bg_handle)
            
            val touchW = dpToPx(50f) // Expanded from 30dp to 50dp for easier grabbing
            val touchH = dpToPx(74f)
            val visualW = dpToPx(4f) // Thinner line (was 6dp)
            
            // Container (Touch Target)
            touchContainer?.layoutParams?.width = touchW
            touchContainer?.layoutParams?.height = touchH
            
            // Critical: Ensure transparent area attracts touches.
            // Some versions optimize away fully transparent clicks. Use 1 alpha.
            touchContainer?.setBackgroundColor(0x01000000)
            
            touchContainer?.requestLayout()
            
            // Visual (Line)
            val lp = visualHandle?.layoutParams as? FrameLayout.LayoutParams
            lp?.width = visualW
            lp?.height = touchH
            // Gravity updated in updateHandleGravity
            visualHandle?.layoutParams = lp
        }
    }
    
    private fun updateHandleGravity(isRightSide: Boolean) {
        if (isRightSide) {
            handleParams.gravity = Gravity.TOP or Gravity.END
        } else {
            handleParams.gravity = Gravity.TOP or Gravity.START
        }
        handleParams.x = 0 // Always flush
        
        // Visual Alignment within Touch Container
        if (currentStyle != "circle") {
            val lp = visualHandle?.layoutParams as? FrameLayout.LayoutParams
            if (lp != null) {
                lp.gravity = Gravity.CENTER_VERTICAL or (if (isRightSide) Gravity.END else Gravity.START)
                
                // "Un poquitito mas salido" -> Add small margin so it's not glued to edge
                val marginPx = dpToPx(7f)
                if (isRightSide) {
                    lp.marginEnd = marginPx
                    lp.marginStart = 0
                } else {
                    lp.marginStart = marginPx
                    lp.marginEnd = 0
                }
                
                visualHandle?.layoutParams = lp
            }
        }
        
        try {
            if (handleWindowView?.isAttachedToWindow == true) {
                windowManager.updateViewLayout(handleWindowView, handleParams)
            }
        } catch (e: Exception) { }
    }

    private fun dpToPx(dp: Float): Int {
        return android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private fun setupTouchListener() {
        touchContainer?.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            private var startY = 0f
            private var initialWindowY = 0
            private var isDragging = false
            
            // Jitter tolerance
            private val LONG_PRESS_JITTER_THRESHOLD = 120 // Very high tolerance for "shaky" holds
            
            private val longPressHandler = Handler(Looper.getMainLooper())
            private val longPressRunnable = Runnable {
                isDragging = true
                touchContainer?.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                touchContainer?.alpha = 0.5f // Drag mode active
                // If panel is open, close it when dragging starts
                if (isPanelVisible) hideVolumePanel()
            }
            
            private val SWIPE_THRESHOLD = 50 
            private val CLICK_THRESHOLD = 15

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        initialWindowY = handleParams.y
                        isDragging = false
                        
                        // Immediate visual feedback "I felt that"
                        touchContainer?.alpha = 0.7f
                        
                        longPressHandler.postDelayed(longPressRunnable, 250) // 250ms: Almost instant
                        return true
                    }
                    
                    MotionEvent.ACTION_MOVE -> {
                        val currX = event.rawX
                        val currY = event.rawY
                        val diffX = currX - startX
                        val diffY = currY - startY
                        
                        if (!isDragging) {
                            // "Instant Drag" Logic: If moving vertically > horizontally, assume Drag Mode immediately
                            // Threshold: 20px slop, and Y must be dominant (2x X)
                            if (Math.abs(diffY) > 20 && Math.abs(diffY) > Math.abs(diffX) * 2) {
                                isDragging = true
                                longPressHandler.removeCallbacks(longPressRunnable)
                                touchContainer?.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                touchContainer?.alpha = 0.5f
                                if (isPanelVisible) hideVolumePanel()
                            }
                            // Only cancel if clearly NOT trying to drag (jitter exceeded without vertical intent)
                            else if (Math.abs(diffX) > LONG_PRESS_JITTER_THRESHOLD || Math.abs(diffY) > LONG_PRESS_JITTER_THRESHOLD) {
                                longPressHandler.removeCallbacks(longPressRunnable)
                            }
                        }
                        
                        // Note: Removed "else" so we can fall through to drag logic immediately in the same event
                        if (isDragging) {
                            // Rail Dragging logic
                            val screenWidth = resources.displayMetrics.widthPixels
                            val isTargetRight = currX > (screenWidth / 2)
                            val currentGravity = handleParams.gravity and Gravity.HORIZONTAL_GRAVITY_MASK
                            val isCurrentRight = (currentGravity == Gravity.END)
                            
                            if (isTargetRight != isCurrentRight) {
                                updateHandleGravity(isTargetRight)
                            }
                            
                            handleParams.x = 0
                            
                            // Safety Clamping for Drag
                            val screenHeight = resources.displayMetrics.heightPixels
                            val minY = dpToPx(50f)
                            val maxY = screenHeight - dpToPx(150f)
                            
                            val rawNewY = initialWindowY + diffY.toInt()
                            handleParams.y = Math.max(minY, Math.min(maxY, rawNewY))
                            
                            savedHandleY = handleParams.y
                            
                            try {
                                windowManager.updateViewLayout(handleWindowView, handleParams)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        return true
                    }
                    
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        longPressHandler.removeCallbacks(longPressRunnable)
                        touchContainer?.alpha = 1.0f // Always reset alpha
                        
                        if (isDragging) {
                            val screenWidth = resources.displayMetrics.widthPixels
                            val isRightSide = event.rawX > (screenWidth / 2)
                            
                            // Ensure final gravity
                            updateHandleGravity(isRightSide)
                            
                            // Save State
                            savedHandleY = handleParams.y 
                            val prefs = getSharedPreferences("gravitune_prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putBoolean("is_right_side", isRightSide)
                                .putInt("saved_y", savedHandleY)
                                .apply()

                            isDragging = false
                            return true
                        }
                        
                        // Click / Swipe logic
                        val prefs = getSharedPreferences("gravitune_prefs", Context.MODE_PRIVATE)
                        val isRightSide = prefs.getBoolean("is_right_side", false)

                        val diffX = event.rawX - startX
                        if (Math.abs(diffX) > SWIPE_THRESHOLD) {
                             if (!isRightSide && diffX > 0) showVolumePanel(isRightSide)
                             else if (isRightSide && diffX < 0) showVolumePanel(isRightSide) // Always pass side
                        } else if (Math.abs(event.rawX - startX) < CLICK_THRESHOLD && Math.abs(event.rawY - startY) < CLICK_THRESHOLD) {
                             v.performClick()
                             if (isPanelVisible) hideVolumePanel() else showVolumePanel(isRightSide)
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun showVolumePanel(isRightSide: Boolean) {
        if (isPanelVisible) return

        val inflater = LayoutInflater.from(this)
        panelWindowView = inflater.inflate(R.layout.layout_panel, null)
        
        // Setup Views inside Panel
        // Setup Views inside Panel
        val btnUp: View? = panelWindowView?.findViewById(R.id.btn_volume_up)
        val btnDown: View? = panelWindowView?.findViewById(R.id.btn_volume_down)
        val seekBar: SeekBar? = panelWindowView?.findViewById(R.id.seekbar_volume)
        val icon: android.widget.ImageView? = panelWindowView?.findViewById(R.id.btn_volume_up) // Using the top button as status icon
        
        // Smart Stream Detection
        val mode = audioManager?.mode
        val isCall = (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION)
        val targetStream = if (isCall) AudioManager.STREAM_VOICE_CALL else AudioManager.STREAM_MUSIC
        
        // Update Icon
        if (isCall) {
            icon?.setImageResource(R.drawable.ic_call)
        } else {
            icon?.setImageResource(R.drawable.ic_volume_up)
        }
        
        // Setup Logic
        val maxVolume = audioManager?.getStreamMaxVolume(targetStream) ?: 100
        val currentVolume = audioManager?.getStreamVolume(targetStream) ?: 0
        
        seekBar?.max = maxVolume
        seekBar?.progress = currentVolume
        
        // Helper to update progress (since updateSeekBar was specific to Music)
        fun refreshProgress() {
            val vol = audioManager?.getStreamVolume(targetStream) ?: 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                seekBar?.setProgress(vol, true)
            } else {
                seekBar?.progress = vol
            }
        }
        
        refreshProgress()

        btnUp?.setOnClickListener {
            audioManager?.adjustStreamVolume(targetStream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            refreshProgress()
            resetHideTimer()
        }
        btnDown?.setOnClickListener {
            audioManager?.adjustStreamVolume(targetStream, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            refreshProgress()
            resetHideTimer()
        }
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager?.setStreamVolume(targetStream, progress, 0)
                    resetHideTimer()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { resetHideTimer() }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { resetHideTimer() }
        })
        
        // Outside Touch Listener to Close
        panelWindowView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hideVolumePanel()
                true
            } else false
        }

        // --- POSITIONING & MARGINS ---
        val panelW = dpToPx(60f)
        val panelH = dpToPx(240f)

        val panelParams = WindowManager.LayoutParams(
            panelW, // Explicit Width
            panelH, // Explicit Height
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        // Horizontal Position (Next to Handle)
        val isCircle = currentStyle == "circle"
        val handleW = if (isCircle) dpToPx(40f) else dpToPx(30f)
        val gap = if (isCircle) dpToPx(20f) else dpToPx(10f)
        val marginX = handleW + gap
        
        if (isRightSide) {
            panelParams.gravity = Gravity.TOP or Gravity.END
            panelParams.x = marginX
        } else {
            panelParams.gravity = Gravity.TOP or Gravity.START
            panelParams.x = marginX
        }
        
        // Vertical Position (Centered on Handle, Clamped)
        val handleH = if (isCircle) dpToPx(40f) else dpToPx(74f)
        val shift = (panelH - handleH) / 2
        
        var targetY = savedHandleY - shift
        
        // Clamping
        val screenHeight = resources.displayMetrics.heightPixels
        if (targetY < 0) targetY = 0
        if (targetY + panelH > screenHeight) targetY = screenHeight - panelH
        
        panelParams.y = targetY

        // Add to Window
        try {
            panelWindowView?.alpha = 0f
            val startTransX = if (isRightSide) 50f else -50f
            panelWindowView?.translationX = startTransX
            
            windowManager.addView(panelWindowView, panelParams)
            
            panelWindowView?.animate()
                ?.alpha(1f)
                ?.translationX(0f)
                ?.setDuration(300)
                ?.start()
                
            isPanelVisible = true
            resetHideTimer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideVolumePanel() {
        if (!isPanelVisible) return
        
        panelWindowView?.animate()
            ?.alpha(0f)
            ?.setDuration(300)
            ?.withEndAction { 
                 try {
                     if (isPanelVisible) {
                        windowManager.removeView(panelWindowView)
                        panelWindowView = null
                        isPanelVisible = false
                     }
                 } catch (e: Exception) {}
            }
            ?.start()
    }
    
    private fun resetHideTimer() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 3000)
    }

    private fun updateSeekBar(seekBar: SeekBar?) {
        val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            seekBar?.setProgress(currentVolume, true)
        } else {
            seekBar?.progress = currentVolume
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (handleWindowView != null) windowManager.removeView(handleWindowView)
        if (panelWindowView != null) windowManager.removeView(panelWindowView)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundService()
        return START_STICKY
    }

    private fun startForegroundService() {
        // ... (Keep existing notification logic)
        val channelId = "VolumeServiceChannel"
        val channelName = "Volume Control Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Volume Control Active")
            .setContentText("Service is running.")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                android.app.PendingIntent.getService(
                    this, 0,
                    Intent(this, VolumeService::class.java).apply { action = "STOP_SERVICE" },
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

        startForeground(1, notificationBuilder.build())
    }
}
