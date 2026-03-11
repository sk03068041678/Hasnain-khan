package com.example.applocker

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LockActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPrefsHelper
    private var lockedPackageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        prefs = SharedPrefsHelper(this)
        lockedPackageName = intent.getStringExtra("PACKAGE_NAME")

        val ivIcon = findViewById<ImageView>(R.id.ivLockedAppIcon)
        val etPin = findViewById<EditText>(R.id.etUnlockPin)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)

        lockedPackageName?.let { pkg ->
            try {
                val icon = packageManager.getApplicationIcon(pkg)
                ivIcon.setImageDrawable(icon)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        btnUnlock.setOnClickListener {
            val enteredPin = etPin.text.toString()
            val savedPin = prefs.getPin()

            if (enteredPin == savedPin) {
                lockedPackageName?.let { pkg ->
                    prefs.setTemporarilyUnlocked(pkg)
                }
                finish() // Close lock screen, revealing the app underneath
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                etPin.setText("")
            }
        }
    }

    override fun onBackPressed() {
        // Prevent user from bypassing lock screen using back button
        // Instead, take them to the home screen
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
