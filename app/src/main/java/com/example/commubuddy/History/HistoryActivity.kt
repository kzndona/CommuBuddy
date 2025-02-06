package com.example.commubuddy.History

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.commubuddy.databinding.ActivityHistoryBinding
import com.google.gson.Gson

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyList: List<HistoryItem>
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun loadHistory(): List<HistoryItem> {
        val sharedPreferences = getSharedPreferences("HistoryPrefs", MODE_PRIVATE)
        val historySet = sharedPreferences.getStringSet("history", mutableSetOf()) ?: mutableSetOf()

        return historySet.mapNotNull { Gson().fromJson(it, HistoryItem::class.java) }
    }

    private fun deleteHistory() {
        val sharedPreferences = getSharedPreferences("HistoryPrefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("history").apply()
        updateHistoryList()
    }
}
