package com.example.commubuddy.History

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.commubuddy.AlarmItem
import com.example.commubuddy.R
import com.example.commubuddy.databinding.ActivityHistoryBinding
import com.google.gson.Gson

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyList: List<AlarmItem>
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val savedTheme = sharedPreferences.getString("theme", "light")

        when (savedTheme) {
            "dark" -> setTheme(R.style.Theme_App_Dark)
            "paper" -> setTheme(R.style.Theme_App_Paper)
            "blue" -> setTheme(R.style.Theme_App_Blue)
            else -> setTheme(R.style.Theme_App_Default)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imgHistoryBackButton.setOnClickListener {
            finish()
        }

        binding.layoutHistoryRecycler.layoutManager = LinearLayoutManager(this)

        updateHistoryList()

        binding.buttonHistoryDeleteHistory.setOnClickListener {
            deleteHistory()
        }
    }

    private fun updateHistoryList() {
        historyList = loadHistory()
        historyAdapter = HistoryAdapter(historyList)
        binding.layoutHistoryRecycler.adapter = historyAdapter
    }

    private fun loadHistory(): List<AlarmItem> {
        val sharedPreferences = getSharedPreferences("HistoryPrefs", MODE_PRIVATE)
        val historySet = sharedPreferences.getStringSet("history", mutableSetOf()) ?: mutableSetOf()

        return historySet.mapNotNull { Gson().fromJson(it, AlarmItem::class.java) }
    }

    private fun deleteHistory() {
        val sharedPreferences = getSharedPreferences("HistoryPrefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("history").apply()
        updateHistoryList()
    }
}
