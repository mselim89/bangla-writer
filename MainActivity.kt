package com.bangla.voicewriter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var switchOverlay: Switch
    private lateinit var switchAccessibility: Switch
    private lateinit var etApiKey: EditText
    private lateinit var btnSaveKey: Button
    private lateinit var btnToggleService: Button
    private lateinit var tvStatus: TextView
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "BanglaVoiceWriterPrefs"
        const val KEY_API_KEY = "apiKey"
        const val KEY_SERVICE_ENABLED = "serviceEnabled"
        private const val REQ_OVERLAY_PERMISSION = 1001
        private const val REQ_AUDIO_PERMISSION = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        switchOverlay = findViewById(R.id.switchOverlay)
        switchAccessibility = findViewById(R.id.switchAccessibility)
        etApiKey = findViewById(R.id.etApiKey)
        btnSaveKey = findViewById(R.id.btnSaveKey)
        btnToggleService = findViewById(R.id.btnToggleService)
        tvStatus = findViewById(R.id.tvStatus)

        etApiKey.setText(prefs.getString(KEY_API_KEY, ""))

        btnSaveKey.setOnClickListener {
            val key = etApiKey.text.toString().trim()
            prefs.edit().putString(KEY_API_KEY, key).apply()
            Toast.makeText(this, "API Key সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
        }

        switchOverlay.setOnClickListener {
            checkAndRequestOverlayPermission()
        }

        switchAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "তালিকায় 'Bangla Voice Writer' চালু (ON) করুন", Toast.LENGTH_LONG).show()
        }

        btnToggleService.setOnClickListener {
            toggleFloatingService()
        }

        requestAudioPermission()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionSwitches()
    }

    private fun updatePermissionSwitches() {
        val hasOverlay = Settings.canDrawOverlays(this)
        switchOverlay.isChecked = hasOverlay

        val hasAccessibility = BanglaVoiceAccessibilityService.isServiceRunning()
        switchAccessibility.isChecked = hasAccessibility

        val isServiceActive = FloatingMicService.isRunning
        if (isServiceActive) {
            btnToggleService.text = "ভাসমান মাইক বাটন বন্ধ করুন"
            tvStatus.text = "স্ট্যাটাস: 🎙️ বাটন সক্রিয় আছে"
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            btnToggleService.text = "ভাসমান মাইক বাটন চালু করুন"
            tvStatus.text = "স্ট্যাটাস: বাটন বন্ধ রয়েছে"
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQ_OVERLAY_PERMISSION)
        }
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQ_AUDIO_PERMISSION
            )
        }
    }

    private fun toggleFloatingService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "অনুগ্রহ করে প্রথমে 'অন্য অ্যাপের উপরে প্রদর্শন' পারমিশন দিন!", Toast.LENGTH_SHORT).show()
            checkAndRequestOverlayPermission()
            return
        }

        if (FloatingMicService.isRunning) {
            stopService(Intent(this, FloatingMicService::class.java))
            FloatingMicService.isRunning = false
        } else {
            val intent = Intent(this, FloatingMicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            FloatingMicService.isRunning = true
        }
        updatePermissionSwitches()
    }
}
