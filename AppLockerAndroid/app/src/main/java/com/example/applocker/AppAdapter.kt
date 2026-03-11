package com.example.applocker

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private var apps: List<ApplicationInfo>,
    private val pm: PackageManager,
    private val prefs: SharedPrefsHelper
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    fun updateList(newApps: List<ApplicationInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val switchLock: Switch = view.findViewById(R.id.switchLock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.tvAppName.text = app.loadLabel(pm)
        holder.ivIcon.setImageDrawable(app.loadIcon(pm))
        
        holder.switchLock.setOnCheckedChangeListener(null)
        holder.switchLock.isChecked = prefs.getLockedApps().contains(app.packageName)
        
        holder.switchLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                prefs.lockApp(app.packageName)
            } else {
                prefs.unlockApp(app.packageName)
            }
        }
    }

    override fun getItemCount(): Int = apps.size
}
