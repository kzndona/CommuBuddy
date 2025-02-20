package com.example.commubuddy

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.commubuddy.databinding.ActivityThemesBinding

class ThemesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityThemesBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val savedTheme = sharedPreferences.getString("theme", "light")

        when (savedTheme) {
            "dark" -> setTheme(R.style.Theme_App_Dark)
            "paper" -> setTheme(R.style.Theme_App_Paper)
            "blue" -> setTheme(R.style.Theme_App_Blue)
            else -> setTheme(R.style.Theme_App_Default)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityThemesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set click listeners for theme selection
        binding.imgHistoryBackButton.setOnClickListener { finish() }
        binding.lightTheme.setOnClickListener { applyTheme("commubuddy") }
        binding.darkTheme.setOnClickListener { applyTheme("dark") }
        binding.paperTheme.setOnClickListener { applyTheme("paper") }
        binding.blueTheme.setOnClickListener { applyTheme("blue") }
    }

    private fun applyTheme(theme: String) {
        sharedPreferences.edit().putString("theme", theme).apply()

        // Restart the activity to apply the new theme
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
