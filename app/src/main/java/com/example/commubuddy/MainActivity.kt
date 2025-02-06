package com.example.commubuddy

import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.commubuddy.Direction.DirectionsHelper
import com.example.commubuddy.History.HistoryActivity
import com.example.commubuddy.History.HistoryItem
import com.example.commubuddy.Interfaces.LocationUpdateListener
import com.example.commubuddy.Location.ForegroundLocationHelper
import com.example.commubuddy.Place.PlacePredictionModel
import com.example.commubuddy.Place.PlaceSearchesActivity
import com.example.commubuddy.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationUpdateListener {

    lateinit var map: GoogleMap
    private var destinationMarker: Marker? = null
    private var route: Polyline? = null
    private var mapCircle: Circle? = null

    private lateinit var foregroundLocationHelper: ForegroundLocationHelper
    private lateinit var searchActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityMainBinding

    private val REQUEST_CODE_RINGTONE: Int = 1001
    private var mediaPlayer: MediaPlayer? = null
    private var isFirstLaunch: Boolean = true
    private var isFirstUpdate: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.fabMain.setOnClickListener {
            if (binding.layoutMainDrawer.isDrawerOpen(GravityCompat.START)) {
                binding.layoutMainDrawer.closeDrawer(GravityCompat.START)
            } else {
                binding.layoutMainDrawer.openDrawer(GravityCompat.START)
            }
        }
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(this, "History clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.menu_bookmarks -> {
                    Toast.makeText(this, "Bookmarks clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.menu_themes -> {
                    Toast.makeText(this, "Themes clicked", Toast.LENGTH_SHORT).show()
                }
            }
            binding.layoutMainDrawer.closeDrawer(GravityCompat.START) // Close drawer after item click
            true
        }

        setUpSearchLauncher()

        binding.buttonMainSearchLocation.setOnClickListener {
            val intent = Intent(this, PlaceSearchesActivity::class.java)
            searchActivityResultLauncher.launch(intent)
        }
        binding.seekbarMainRingDistance.setOnSeekBarChangeListener( object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scaledProgress = 50 + (progress / 100.0) * (2000 - 50)
                updateMapCircle(scaledProgress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { return }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { return }
        })
        binding.buttonMainStartAlarm.setOnClickListener {
            when (AlarmObject.alarmStatus) {
                AlarmObject.ON -> cancelAlarm()
                AlarmObject.OFF -> startAlarm()
                AlarmObject.RINGING -> dismissAlarm()
            }
        }
        binding.buttonMainRingtonePicker.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")

                // Load the previously selected ringtone (if any)
                val savedUri = getSavedRingtoneUri()
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, savedUri)
            }
            startActivityForResult(intent, REQUEST_CODE_RINGTONE)
        }
        binding.frameMainAlarmBanner.visibility = View.INVISIBLE
        binding.seekbarMainRingDistance.isEnabled = false
        binding.buttonMainStartAlarm.isEnabled = false
    }

    override fun onResume() {
        super.onResume()

        if (!isFirstLaunch) {
            foregroundLocationHelper.checkLocationPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        foregroundLocationHelper.stopForegroundGPSUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (isFirstLaunch) {
            initializeLocationHelper()
        }
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition(
            LatLng(14.59,121.04), 10.2f, 0f, 0f)))
    }

    private fun startAlarm() {
        AlarmObject.originLatLng = foregroundLocationHelper.userLatLng
        if (AlarmObject.originLatLng == null) {
            Toast.makeText(this, "Unable to get user location.", Toast.LENGTH_SHORT).show()
            return
        }
        if (AlarmObject.destinationLatLng == null) {
            Toast.makeText(this, "Unable to get destination coordinates.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val directionsHelper = DirectionsHelper()
            val urlString = directionsHelper.buildDirectionsURL(AlarmObject.originLatLng!!, AlarmObject.destinationLatLng!!)
            val response = withContext(Dispatchers.IO) {
                directionsHelper.fetchDirectionsData(urlString)
            }

            if (response != null) {
                val routePoints = directionsHelper.parseRoute(response)
                if (routePoints != null) {
                    drawRoute(routePoints)
                    binding.buttonMainStartAlarm.text = "Cancel Alarm"
                    AlarmObject.alarmStatus = AlarmObject.ON

                    binding.fabMain.visibility = View.INVISIBLE
                    binding.buttonMainSearchLocation.visibility = View.INVISIBLE
                    binding.buttonMainRingtonePicker.visibility = View.INVISIBLE
                    binding.seekbarMainRingDistance.visibility = View.INVISIBLE
                    makeHistoryItem()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to parse directions", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "Failed to fetch directions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelAlarm() {
        AlarmObject.originLatLng = null
        route?.remove()
        binding.buttonMainStartAlarm.text = "Start Alarm"
        AlarmObject.alarmStatus = AlarmObject.OFF

        binding.fabMain.visibility = View.VISIBLE
        binding.buttonMainSearchLocation.visibility = View.VISIBLE
        binding.buttonMainRingtonePicker.visibility = View.VISIBLE
        binding.seekbarMainRingDistance.visibility = View.VISIBLE
    }

    private fun dismissAlarm() {
        cancelAlarm()
        binding.frameMainAlarmBanner.visibility = View.INVISIBLE

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun showAlarm() {
        binding.buttonMainStartAlarm.text = "Stop Alarm"
        binding.frameMainAlarmBanner.visibility = View.VISIBLE
        AlarmObject.alarmStatus = AlarmObject.RINGING

        mediaPlayer?.stop()
        mediaPlayer?.release()

        val ringtoneUri: Uri = getSavedRingtoneUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@MainActivity, ringtoneUri)
            setAudioStreamType(AudioManager.STREAM_ALARM)
            isLooping = true
            prepare()
            start()
        }
    }

    private fun setDestination(destinationLatLng: LatLng) {
        destinationMarker?.remove()
        destinationMarker = null
        mapCircle?.remove()
        mapCircle = null

        destinationMarker = map.addMarker(MarkerOptions().position(destinationLatLng))
        updateMapCircle(50.0)
        binding.seekbarMainRingDistance.isEnabled = true
        binding.buttonMainStartAlarm.isEnabled = true
    }

    private fun updateMapCircle(radius: Double) {
        AlarmObject.ringDistance = radius
        val center = AlarmObject.destinationLatLng?: return
        if (mapCircle == null) {
            mapCircle = map.addCircle(
                CircleOptions()
                    .center(center)
                    .radius(radius)
                    .strokeWidth(1f)
                    .fillColor(Color.argb(.2f,0f,0f,255f)))
        } else {
            mapCircle!!.radius = radius
        }
    }

    private fun drawRoute(routePoints: List<LatLng>) {
        val polylineOptions = PolylineOptions().apply {
            addAll(routePoints)
            color(Color.BLUE)
            width(10f)
            zIndex(50f)
        }
        route = map.addPolyline(polylineOptions)
        if (mapCircle != null && AlarmObject.ringDistance != null) {
            updateMapCircle(AlarmObject.ringDistance!!)
        }
    }

    private fun initializeLocationHelper() {
        foregroundLocationHelper = ForegroundLocationHelper(this, this, this)
        foregroundLocationHelper.checkLocationPermission()
        isFirstLaunch = false
    }

    override fun onFirstLocationUpdated(location: LatLng) {
        if (isFirstUpdate) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.2f))
            isFirstUpdate = false
        }
    }

    override fun onShowAlarmDistanceToDestination(distance: Int) {
        binding.textMainAlarmDistance.text = "${distance}m"
    }

    private fun setUpSearchLauncher() {
        searchActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedPlace: PlacePredictionModel? = result.data?.getParcelableExtra("selected_place")
                selectedPlace?.let {
                    AlarmObject.destinationID = it.placeId
                    AlarmObject.destinationName = it.primaryText
                    AlarmObject.destinationAddress = it.secondaryText
                    AlarmObject.destinationLatLng = it.latLng!!
                    setDestination(AlarmObject.destinationLatLng!!)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_RINGTONE && resultCode == RESULT_OK) {
            val ringtoneUri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (ringtoneUri != null) { saveRingtoneUri(ringtoneUri) }
        }
    }

    private fun saveRingtoneUri(uri: Uri) {
        val sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("ringtone_uri", uri.toString()).apply()
    }

    private fun getSavedRingtoneUri(): Uri? {
        val sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        val uriString = sharedPreferences.getString("ringtone_uri", null)
        return uriString?.let { Uri.parse(it) }
    }

    fun makeHistoryItem() {
        val newHistoryItem = HistoryItem(
            AlarmObject.destinationID,
            AlarmObject.destinationName,
            AlarmObject.destinationAddress,
            AlarmObject.destinationLatLng,
            AlarmObject.originLatLng,
            AlarmObject.ringDistance
        )

        saveHistoryItem(newHistoryItem)
    }

    fun saveHistoryItem(historyItem: HistoryItem) {
        val sharedPreferences = getSharedPreferences("HistoryPrefs", MODE_PRIVATE)
        val historyList = sharedPreferences.getStringSet("history", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // Convert HistoryItem to JSON or String format and add it to the list
        val historyJson = Gson().toJson(historyItem)  // You can use Gson to convert to JSON string
        historyList.add(historyJson)

        sharedPreferences.edit().putStringSet("history", historyList).apply()
    }

    // Handle back button to close drawer if it's open
    override fun onBackPressed() {
        if (binding.layoutMainDrawer.isDrawerOpen(GravityCompat.START)) {
            binding.layoutMainDrawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}