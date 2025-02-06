package com.example.commubuddy.Bookmark

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.commubuddy.AlarmObject
import com.example.commubuddy.databinding.ActivityBookmarksBinding
import com.google.gson.Gson

class BookmarksActivity : AppCompatActivity() {

    private lateinit var bookmarksAdapter: BookmarksAdapter
    private lateinit var bookmarksList: List<BookmarksItem>
    private lateinit var binding: ActivityBookmarksBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.layoutBookmarksRecycler.layoutManager = LinearLayoutManager(this)

        updateBookmarksList()

        binding.buttonBookmarksDeleteBookmarks.setOnClickListener {
            deleteBookmarks()
        }
        binding.buttonBookmarksAddBookmarks.setOnClickListener {
            AlarmObject.status = AlarmObject.BOOKMARKING
            finish()
        }
    }

    private fun updateBookmarksList() {
        bookmarksList = loadBookmarks()
        bookmarksAdapter = BookmarksAdapter(
            bookmarksList,
            onBookmarkClick = { bookmarksItem: BookmarksItem ->
                if (AlarmObject.status == AlarmObject.OFF) {
                    AlarmObject.destinationID = bookmarksItem.destinationID
                    AlarmObject.destinationName = bookmarksItem.destinationName
                    AlarmObject.destinationAddress = bookmarksItem.destinationAddress
                    AlarmObject.destinationLatLng = bookmarksItem.destinationLatLng
                    AlarmObject.ringDistance = bookmarksItem.ringDistance

                    val resultIntent = Intent()
                    resultIntent.putExtra("update_map", true)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    Toast.makeText(this, "An alarm is currently active", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.layoutBookmarksRecycler.adapter = bookmarksAdapter
    }

    private fun loadBookmarks(): List<BookmarksItem> {
        val sharedPreferences = getSharedPreferences("BookmarksPrefs", MODE_PRIVATE)
        val bookmarksSet = sharedPreferences.getStringSet("bookmarks", mutableSetOf()) ?: mutableSetOf()

        return bookmarksSet.mapNotNull { Gson().fromJson(it, BookmarksItem::class.java) }
    }

    private fun deleteBookmarks() {
        val sharedPreferences = getSharedPreferences("BookmarksPrefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("bookmarks").apply()
        updateBookmarksList()
    }
}
