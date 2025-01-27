package com.example.commubuddy

import android.os.Bundle
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity

class LocationSearchesActivity : AppCompatActivity() {

    lateinit var searchLocationBar : SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_searches)

        searchLocationBar = findViewById(R.id.search_location_searches_search_location)
        // Listen for queries then request for Places API autocompletes using query
        searchLocationBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String?): Boolean {
                query?.let { handleQueery(it) }
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { handleQueery(it) }
                return true
            }
        })
    }

    private fun handleQueery(query: String) {
        // TODO: Request query to Places API
    }
}