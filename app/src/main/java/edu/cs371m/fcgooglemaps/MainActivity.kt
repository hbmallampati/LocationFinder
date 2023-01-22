package edu.cs371m.fcgooglemaps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import edu.cs371m.fcgooglemaps.databinding.ActivityMainBinding
import edu.cs371m.fcgooglemaps.databinding.ContentMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*


class MainActivity
    : AppCompatActivity(),
    OnMapReadyCallback
{
    private lateinit var map: GoogleMap
    private lateinit var geocoder: Geocoder
    private var locationPermissionGranted = false
    private lateinit var binding: ContentMainBinding

    private val nearHarryRansomCenter = LatLng(30.2843324, -97.741221)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        setSupportActionBar(activityMainBinding.toolbar)
        binding = activityMainBinding.contentMain

        checkGooglePlayServices()
        requestPermission()

        // XXX Write me.
        binding.clearBut.setOnClickListener{
            map.clear()
        }

        binding.goBut.setOnClickListener {
            val addr = binding.mapET.text.toString()
            if(addr.isNotEmpty())
            {
                getLatLongFromAddress(addr)
            }
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFrag) as SupportMapFragment
        mapFragment.getMapAsync(this)


        // This code is correct, but it assumes an EditText in your layout
        // called mapET and a go button called goBut
        binding.mapET.setOnEditorActionListener { /*v*/_, actionId, event ->
            // If user has pressed enter, or if they hit the soft keyboard "send" button
            // (which sends DONE because of the XML)
            if ((event != null
                        &&(event.action == KeyEvent.ACTION_DOWN)
                        &&(event.keyCode == KeyEvent.KEYCODE_ENTER))
                || (actionId == EditorInfo.IME_ACTION_DONE)) {
                hideKeyboard()
                binding.goBut.callOnClick()
            }
            false
        }

    }

    
    private fun getLatLongFromAddress(addr : String) {
        geocoder = Geocoder(this)
        try {
            val latlong = geocoder.getFromLocationName(addr, 1)

            if(latlong.isNotEmpty())
            {
                val lat = latlong[0].latitude
                val long = latlong[0].longitude
                val newAddr = LatLng(lat, long)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(newAddr, 15.0f))
            }
        }
        catch (e : IOException) {
            Log.e(javaClass.simpleName, e.localizedMessage)
        }

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        map = googleMap
        if( locationPermissionGranted )
        {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            map.setOnMapLongClickListener {
                // Goodbye markers
                map.clear()
            }
            map.setOnMapClickListener {
                map.addMarker(
                    MarkerOptions()
                        .position(it)
                        .title("%.3f".format(it.latitude) + ", "+ "%.3f".format(it.longitude))
                )
            }
        }
        else
        {
            val permission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                if (!locationPermissionGranted) {
                    Toast.makeText(this,
                        "Unable to show location - permission required",
                        Toast.LENGTH_LONG).show()
                    return
                }
            }
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            map.setOnMapLongClickListener {
                // Goodbye markers
                map.clear()
                }
            map.setOnMapClickListener {
                map.addMarker(
                    MarkerOptions()
                        .position(it)
                        .title("%.3f".format(it.latitude) + ", "+ "%.3f".format(it.longitude))
                )
            }
        }

        // XXX Write me.
        // Start the map at the Harry Ransom center

        // Go to initial location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(nearHarryRansomCenter, 15.0f))
    }

    // Everything below here is correct

    // An Android nightmare
    // https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
    // https://stackoverflow.com/questions/7789514/how-to-get-activitys-windowtoken-without-view
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.rootView.windowToken, 0);
    }

    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode =
            googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 257)?.show()
            } else {
                Log.i(javaClass.simpleName,
                    "This device must install Google Play Services.")
                finish()
            }
        }
    }

    private fun requestPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    locationPermissionGranted = true;
                } else -> {
                Toast.makeText(this,
                    "Unable to show location - permission required",
                    Toast.LENGTH_LONG).show()
            }
            }
        }
        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }
}

