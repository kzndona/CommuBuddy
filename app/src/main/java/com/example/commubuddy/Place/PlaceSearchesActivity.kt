package com.example.commubuddy.Place

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.commubuddy.AlarmItem
import com.example.commubuddy.AlarmObject
import com.example.commubuddy.Bookmark.BookmarksAdapter
import com.example.commubuddy.BuildConfig
import com.example.commubuddy.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.gson.Gson

class PlaceSearchesActivity : AppCompatActivity() {

    private val apiKey = BuildConfig.MAPS_API_KEY
    private lateinit var searchLocationBar : SearchView
    private lateinit var placesClient : PlacesClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlacePredictionAdapter
    private val predictions = mutableListOf<PlacePredictionModel>()

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
        setContentView(R.layout.activity_location_searches)

        val backButton = findViewById<ImageView>(R.id.img_search_location_back_button)
        backButton.setOnClickListener { finish() }

        // Log an error if apiKey is not set.
        if (apiKey.isEmpty() || apiKey == "DEFAULT_API_KEY") {
            Log.e("Places test", "No api key")
            finish()
            return
        }

        // Initialize the SDK
        Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
        // Create a new PlacesClient instance
        placesClient = Places.createClient(this)


        searchLocationBar = findViewById(R.id.search_location_searches_search_location)
        // Listen for queries then request for Places API autocompletes using query
        searchLocationBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String?): Boolean {
                query?.let { handleQuery(it) }
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { handleQuery(it) }
                return true
            }
        })

        // Initialize Recycler View
        recyclerView = findViewById(R.id.layout_location_searches_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set up Adapter
        adapter = PlacePredictionAdapter(predictions) { prediction ->
            fetchPlaceDetails(prediction.placeId) { latLng ->
                val resultIntent = Intent()
                resultIntent.putExtra("selected_place", PlacePredictionModel(
                    placeId = prediction.placeId,
                    primaryText = prediction.primaryText,
                    secondaryText = prediction.secondaryText,
                    latLng = latLng
                ))
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
        showBookmarks()
    }

    private fun handleQuery(query: String) {
        if (query.isEmpty()) {
            // If search query is empty, display bookmarks
            showBookmarks()
        } else {
            // Request query to Places API and display predictions
            fetchPlacePredictions(query)
        }
    }

    private fun showBookmarks() {
        val bookmarksList = loadBookmarks()
        val bookmarksAdapter = BookmarksAdapter(
            bookmarksList,
            onBookmarkClick = { bookmarksItem: AlarmItem ->
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
        recyclerView.adapter = bookmarksAdapter
    }

    private fun loadBookmarks(): List<AlarmItem> {
        val sharedPreferences = getSharedPreferences("BookmarksPrefs", MODE_PRIVATE)
        val bookmarksSet = sharedPreferences.getStringSet("bookmark", mutableSetOf()) ?: mutableSetOf()

        return bookmarksSet.mapNotNull { Gson().fromJson(it, AlarmItem::class.java) }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchPlacePredictions(query: String) {
        recyclerView.adapter = adapter
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setRegionCode("ph")
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                predictions.clear()
                response.autocompletePredictions.forEach { prediction ->
                    predictions.add(
                        PlacePredictionModel(
                            placeId = prediction.placeId,
                            primaryText = prediction.getPrimaryText(null).toString(),
                            secondaryText = prediction.getSecondaryText(null).toString()
                        )
                    )
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, exception.toString(), Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchPlaceDetails(placeId: String, callback: (LatLng) -> Unit) {
        val placeFields = listOf(Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val latLng = place.latLng
                latLng?.let { callback(it) }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, exception.toString(), Toast.LENGTH_SHORT).show()
            }
    }
}