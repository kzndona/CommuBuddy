package com.example.commubuddy

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
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
import com.example.commubuddy.Bookmark.BookmarksActivity
import com.example.commubuddy.Direction.DirectionsHelper
import com.example.commubuddy.History.HistoryActivity
import com.example.commubuddy.Interfaces.LocationUpdateListener
import com.example.commubuddy.Service.ForegroundServiceHelper
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
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
    private var markerDestination: Marker? = null
    private var polylineRoute: Polyline? = null
    private var circleRange: Circle? = null

    private lateinit var fgServiceHelper: ForegroundServiceHelper
    private lateinit var resultsLauncher: ActivityResultLauncher<Intent>
    private lateinit var bookmarksLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityMainBinding
    // TODO: BGServiceHelper in the future

    private val requestCodeRingtone: Int = 1001
    private var mediaPlayer: MediaPlayer? = null
    private var isFirstLaunch: Boolean = true
    private var isFirstUpdate: Boolean = true
    private var shouldAnimateCamera: Boolean = false

    private lateinit var alarmItem: AlarmItem
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupNavigationDrawer()
        setupBookmarksLauncher()
        setupSearchLauncher()
        setupClickListeners()

        binding.seekbarMainRingDistance.isEnabled = false
    }

    private fun setupNavigationDrawer() {
        binding.fabMain.setOnClickListener {
            with (binding.layoutMainDrawer) {
                if (isDrawerOpen(GravityCompat.START)) closeDrawer(GravityCompat.START) else openDrawer(GravityCompat.START)
            }
        }
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_history -> startActivity(Intent(this, HistoryActivity::class.java))
                R.id.menu_bookmarks -> bookmarksLauncher.launch(Intent(this, BookmarksActivity::class.java))
                R.id.menu_themes -> startActivity(Intent(this, ThemesActivity::class.java))
            }
            binding.layoutMainDrawer.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupBookmarksLauncher() {
        bookmarksLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getBooleanExtra("update_map", false)?.takeIf { it && AlarmObject.destinationLatLng != null }?.let {
                    setDestination(AlarmObject.destinationLatLng!!)
                    updateMapCircle(AlarmObject.ringDistance ?: 50.0)
                }
            }
        }
    }

    private fun setupSearchLauncher() {
        resultsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedPlace: PlacePredictionModel? = result.data?.getParcelableExtra("selected_place")
                if (selectedPlace != null) {
                    AlarmObject.destinationID = selectedPlace.placeId
                    AlarmObject.destinationName = selectedPlace.primaryText
                    AlarmObject.destinationAddress = selectedPlace.secondaryText
                    AlarmObject.destinationLatLng = selectedPlace.latLng!!
                    setDestination(AlarmObject.destinationLatLng!!)
                } else {
                    val shouldUpdateMap = result.data?.getBooleanExtra("update_map", false) ?: false
                    if (shouldUpdateMap && AlarmObject.destinationLatLng != null) setDestination(AlarmObject.destinationLatLng!!)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.layoutMainSearchLocation.setOnClickListener {
            resultsLauncher.launch(Intent(this, PlaceSearchesActivity::class.java))
            isFirstUpdate = false
        }
        binding.seekbarMainRingDistance.setOnSeekBarChangeListener( object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scaledProgress = 50 + (progress / 100.0) * (2000 - 50)
                updateMapCircle(scaledProgress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.buttonMainAlarm.setOnClickListener { switchAlarmButton() }
        binding.buttonMainRingtonePicker.setOnClickListener { pickRingtone() }
    }

    override fun onResume() {
        super.onResume()
        if (!isFirstLaunch) fgServiceHelper.checkLocationPermission()
        if (AlarmObject.status == AlarmObject.BOOKMARKING) binding.buttonMainAlarm.text = "Bookmark"
    }

    override fun onPause() {
        super.onPause()
        fgServiceHelper.stopForegroundGPSUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val currentTheme = sharedPreferences.getString("theme", "light") // Retrieve saved theme

        val styleJson = when (currentTheme) {
            "dark" -> R.raw.map_style_dark
            "paper" -> R.raw.map_style_paper
            "blue" -> R.raw.map_style_blue
            else -> R.raw.map_style_default
        }
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, styleJson))

        if (isFirstLaunch) initializeLocationHelper()
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition(LatLng(14.59,121.04), 10.2f, 0f, 0f)))
    }


    private fun initializeLocationHelper() {
        if (!::fgServiceHelper.isInitialized) fgServiceHelper = ForegroundServiceHelper(this, this, this)
        fgServiceHelper.checkLocationPermission()
        isFirstLaunch = false
    }

    private fun switchAlarmButton() {
        when (AlarmObject.status) {
            AlarmObject.ON -> cancelAlarm()
            AlarmObject.OFF -> startAlarm()
            AlarmObject.RINGING -> dismissAlarm()
            AlarmObject.BOOKMARKING -> saveItemAs("BookmarksPrefs", "bookmark")
        }
    }

    private fun startAlarm() {
        AlarmObject.originLatLng = fgServiceHelper.userLatLng ?: return showToast("Unable to get user location.")
        AlarmObject.destinationLatLng ?: return showToast("Unable to get destination location.")

        CoroutineScope(Dispatchers.Main).launch {
            val directionsHelper = DirectionsHelper()
            val urlString = directionsHelper.buildDirectionsURL(AlarmObject.originLatLng!!, AlarmObject.destinationLatLng!!)
            val response = withContext(Dispatchers.IO) {
                directionsHelper.fetchDirectionsData(urlString)
            }

            response?.let {
                directionsHelper.parseRoute(it)?. let { routePoints ->
                    drawRoute(routePoints)
                    animateUpdateCameraBounds(routePoints)
                    updateAlarmUI(true)
                    saveItemAs("HistoryPrefs", "history")
                } ?: showToast("Failed to parse directions")
            } ?: showToast("Failed to fetch directions")
        }
    }

    private fun cancelAlarm() {
        AlarmObject.originLatLng = null
        polylineRoute?.remove()
        updateAlarmUI(false)
    }

    private fun updateAlarmUI(isAlarmActive: Boolean) {
        binding.buttonMainAlarm.text = if (isAlarmActive) "Cancel Alarm" else "Start Alarm"
        AlarmObject.status = if (isAlarmActive) AlarmObject.ON else AlarmObject.OFF
        val visibility = if (isAlarmActive) View.INVISIBLE else View.VISIBLE
        binding.fabMain.visibility = visibility
        binding.layoutMainSearchLocation.visibility = visibility
        binding.layoutMainSecondary.visibility = visibility
    }

    fun showAlarm() {
        binding.buttonMainAlarm.text = "Stop Alarm"
        binding.frameMainAlarmBanner.visibility = View.VISIBLE
        AlarmObject.status = AlarmObject.RINGING

        resetMediaPlayer()
        val ringtoneUri: Uri = getSavedRingtoneUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@MainActivity, ringtoneUri)
            setAudioStreamType(AudioManager.STREAM_ALARM)
            isLooping = true
            prepare()
            start()
        }

        shouldAnimateCamera = true
    }

    private fun dismissAlarm() {
        cancelAlarm()
        binding.frameMainAlarmBanner.visibility = View.INVISIBLE
        resetMediaPlayer()
        shouldAnimateCamera = false
    }

    private fun setDestination(destinationLatLng: LatLng) {
        markerDestination?.remove()
        markerDestination = null
        circleRange?.remove()
        circleRange = null

        markerDestination = map.addMarker(MarkerOptions().position(destinationLatLng))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng,15.2f))
        if (AlarmObject.ringDistance != null) {
            val seekProgress = (AlarmObject.ringDistance!! - 50) / (2000 - 50) * 100
            binding.seekbarMainRingDistance.progress = seekProgress.toInt()
            updateMapCircle(AlarmObject.ringDistance!!)
        } else {
            updateMapCircle(50.0)
        }
        binding.textMainSearchLocationPrimary.text = AlarmObject.destinationName
        binding.textMainSearchLocationSecondary.text = AlarmObject.destinationAddress
        binding.textMainAlarmName.text = AlarmObject.destinationName
        binding.textMainAlarmAddress.text = AlarmObject.destinationAddress
        binding.seekbarMainRingDistance.isEnabled = true
        binding.buttonMainAlarm.isEnabled = true
    }

    private fun updateMapCircle(radius: Double) {
        AlarmObject.ringDistance = radius
        val center = AlarmObject.destinationLatLng?: return

        if (circleRange != null) circleRange!!.radius = radius
        else {
            circleRange = map.addCircle(CircleOptions().
            center(center).radius(radius).strokeWidth(1f).fillColor(Color.argb(0.2f,0f,0.4f,1f)))
        }

        if (radius > 999) binding.textMainRingDistance.text = "Ring alarm in: ${String.format("%.2f", radius / 1000)}km"
        else binding.textMainRingDistance.text = "Ring alarm in: ${radius.toInt()}m"
    }

    private fun drawRoute(routePoints: List<LatLng>) {
        val polylineOptions = PolylineOptions().apply {
            addAll(routePoints)
            color(Color.BLUE)
            width(10f)
            zIndex(50f)
        }
        polylineRoute = map.addPolyline(polylineOptions)
        if (circleRange != null && AlarmObject.ringDistance != null) {
            updateMapCircle(AlarmObject.ringDistance!!)
        }
    }

    private fun saveItemAs(name: String, key: String) {
        alarmItem = AlarmItem(
            AlarmObject.destinationID,
            AlarmObject.destinationName,
            AlarmObject.destinationAddress,
            AlarmObject.destinationLatLng,
            AlarmObject.originLatLng,
            AlarmObject.ringDistance
        )
        val sharedPreferences = getSharedPreferences(name, MODE_PRIVATE)
        val mutableSet = sharedPreferences.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val jsonItem = Gson().toJson(alarmItem)

        mutableSet.add(jsonItem)
        sharedPreferences.edit().putStringSet(key, mutableSet).apply()
        if (key == "bookmark") {
            cancelAlarm()
            bookmarksLauncher.launch(Intent(this, BookmarksActivity::class.java))
        }
    }

    private fun animateUpdateCameraBounds(routePoints: List<LatLng>) {
        if(polylineRoute == null) return

        val boundsBuilder = LatLngBounds.builder()
        routePoints.forEach { boundsBuilder.include(it) }

        val bounds = boundsBuilder.build()
        val padding = 240

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }

    private fun pickRingtone() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getSavedRingtoneUri())
        }
        startActivityForResult(intent, requestCodeRingtone)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestCodeRingtone && resultCode == RESULT_OK) {
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

    private fun resetMediaPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onLocationUpdate(location: LatLng) {
        if (isFirstUpdate) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.2f))
            isFirstUpdate = false
        }

        if (shouldAnimateCamera) map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16.2f))
    }

    override fun onShowAlarmDistanceToDestination(distance: Float) {
        if (distance > 999) binding.textMainAlarmDistance.text = "${String.format("%.2f", distance / 1000)}km"
        else binding.textMainAlarmDistance.text = "${distance.toInt()}m"
    }

    private fun showToast(string: String) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (binding.layoutMainDrawer.isDrawerOpen(GravityCompat.START)) binding.layoutMainDrawer.closeDrawer(GravityCompat.START) else super.onBackPressed()
    }
}