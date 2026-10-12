package com.cardlock.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.widget.Toast

object ClipboardHelper {

    private const val CLEAR_DELAY_MS = 30_000L // 30 seconds
    private val handler = Handler(Looper.getMainLooper())
    private var pendingRunnable: Runnable? = null

    /**
     * Copies text to the clipboard with a user warning toast and schedules automatic clearing.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)

        // Mark as sensitive on Android 13+ (API 33+) using PersistableBundle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
            clip.description.extras = extras
        }

        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            context,
            "$label copied to clipboard. Will auto-clear in 30s for privacy.",
            Toast.LENGTH_LONG
        ).show()

        // Schedule auto clear
        pendingRunnable?.let { handler.removeCallbacks(it) }
        val clearRunnable = Runnable {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        pendingRunnable = clearRunnable
        handler.postDelayed(clearRunnable, CLEAR_DELAY_MS)
    }
}
