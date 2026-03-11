package com.example.applocker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = SharedPrefsHelper(this)

        val etPin = findViewById<EditText>(R.id.etPin)
        val btnSavePin = findViewById<Button>(R.id.btnSavePin)
        val btnUsageAccess = findViewById<Button>(R.id.btnUsageAccess)
        val btnOverlayAccess = findViewById<Button>(R.id.btnOverlayAccess)
        val btnStartService = findViewById<Button>(R.id.btnStartService)
        val etSearchApp = findViewById<EditText>(R.id.etSearchApp)
        val rvApps = findViewById<RecyclerView>(R.id.rvApps)

        etPin.setText(prefs.getPin() ?: "")

        btnSavePin.setOnClickListener {
            val pin = etPin.text.toString()
            if (pin.length >= 4) {
                prefs.savePin(pin)
                Toast.makeText(this, "PIN Saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
            }
        }

        btnUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        btnOverlayAccess.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }

        btnStartService.setOnClickListener {
            if (!hasUsageStatsPermission()) {
                Toast.makeText(this, "Please grant Usage Access first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant Overlay Permission first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val serviceIntent = Intent(this, LockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()
        }

        // Load Apps
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val userApps = packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != packageName }
        
        rvApps.layoutManager = LinearLayoutManager(this)
        val adapter = AppAdapter(userApps, pm, prefs)
        rvApps.adapter = adapter

        etSearchApp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filteredList = userApps.filter {
                    it.loadLabel(pm).toString().lowercase().contains(query)
                }
                adapter.updateList(filteredList)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
