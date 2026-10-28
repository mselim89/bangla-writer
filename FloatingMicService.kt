package com.bangla.voicewriter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingMicService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var micIcon: ImageView
    private lateinit var voiceEngine: VoiceRecognitionEngine

    private var isRecording = false

    companion object {
        var isRunning = false
        private const val CHANNEL_ID = "BanglaVoiceFloatingChannel"
        private const val NOTIF_ID = 2001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        voiceEngine = VoiceRecognitionEngine(this)

        startForeground(NOTIF_ID, createNotification())
        initFloatingView()
    }

    private fun initFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_mic, null)
        micIcon = floatingView.findViewById(R.id.ivFloatingMic)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            handleMicClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)
    }

    private fun handleMicClick() {
        if (!isRecording) {
            startVoiceCapture()
        } else {
            stopVoiceCaptureAndProcess()
        }
    }

    private fun startVoiceCapture() {
        isRecording = true
        micIcon.setImageResource(R.drawable.ic_mic_recording)
        Toast.makeText(this, "শুনছি... বলুন", Toast.LENGTH_SHORT).show()
        voiceEngine.startRecording()
    }

    private fun stopVoiceCaptureAndProcess() {
        isRecording = false
        micIcon.setImageResource(R.drawable.ic_mic)
        Toast.makeText(this, "প্রসেসিং...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            val audioData = voiceEngine.stopRecording()
            if (audioData != null) {
                // Call AI Speech-to-Text & Punctuation pipeline
                val transcribedText = voiceEngine.transcribeBanglaAudio(audioData)
                val polishedText = PunctuationEngine.formatBanglaText(transcribedText)

                withContext(Dispatchers.Main) {
                    val success = BanglaVoiceAccessibilityService.injectText(polishedText)
                    if (!success) {
                        Toast.makeText(
                            this@FloatingMicService,
                            "কোনো সক্রিয় ইনপুট ফিল্ড পাওয়া যায়নি! টেক্সট কপি করা হয়েছে।",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bangla Voice Writer Floating Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bangla AI Voice Writer সক্রিয়")
            .setContentText("যেকোনো অ্যাপে 🎙️ বাটনে ট্যাপ করে কথা বলুন")
            .setSmallIcon(R.drawable.ic_mic)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
