package com.bangla.voicewriter

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BanglaVoiceAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: BanglaVoiceAccessibilityService? = null

        fun isServiceRunning(): Boolean = instance != null

        fun injectText(text: String): Boolean {
            val service = instance ?: return false
            return service.writeTextToActiveField(text)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Keeps track of active window context
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    private fun writeTextToActiveField(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return fallbackToClipboard(text)
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (focusedNode != null && focusedNode.isEditable) {
            // Retrieve current text to append or set
            val existingText = focusedNode.text?.toString() ?: ""
            val newText = if (existingText.isNotEmpty() && !existingText.endsWith(" ")) {
                "$existingText $text "
            } else {
                "$existingText$text "
            }

            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText
                )
            }
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (result) return true
        }

        // Fallback strategy: Copy to clipboard and invoke paste
        return fallbackToClipboard(text, focusedNode)
    }

    private fun fallbackToClipboard(text: String, targetNode: AccessibilityNodeInfo? = null): Boolean {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BanglaVoiceText", text)
        clipboard.setPrimaryClip(clip)

        targetNode?.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        return true
    }
}
