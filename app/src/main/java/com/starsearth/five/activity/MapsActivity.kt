package com.starsearth.five.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.starsearth.five.R
import com.starsearth.five.domain.HelpRequest
import com.starsearth.five.domain.SEOneListItem
import com.starsearth.five.managers.FirebaseManager
import java.util.*
import kotlin.collections.HashMap

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private lateinit var mMap: GoogleMap
    val TAG = "MAPS_ACTIVITY"

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            location?.let {
                val currentLocation = LatLng(it.latitude, it.longitude)
             /*   mMap.addMarker(MarkerOptions()
                        .position(currentLocation)
                        .title("Your Current Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )   */
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15f))
                getAddressFromLocation(it).get(0)?.let {
                    loadHelpRequests(it.adminArea)
                }
            }

        }
    }

    private val mHelpRequestsListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val map = dataSnapshot?.value
            if (map != null) {

                for (entry in (map as HashMap<*, *>).entries) {
                    val key = entry.key as String
                    val value = entry.value as HashMap<String, Any>
                    var newHelpRequest = HelpRequest(key, value)
                    val latLng = LatLng(newHelpRequest.address.latitude, newHelpRequest.address.longitude)
                    val marker = mMap.addMarker(MarkerOptions()
                            .position(latLng)
                            .title(newHelpRequest.status.toUpperCase())
                            .snippet("Request by: " + newHelpRequest.phone + "... Tap to see more details")
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                    if (newHelpRequest.status == "COMPLETE") {
                                        BitmapDescriptorFactory.HUE_GREEN
                                    }
                                    else {
                                        BitmapDescriptorFactory.HUE_RED
                                    }
                            ))
                    )
                    mHelpRequestsMap.put(newHelpRequest.uid, newHelpRequest)
                    marker.tag = newHelpRequest.uid
                }


            }

        }

        override fun onCancelled(p0: DatabaseError) {

        }

    }

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val mHelpRequestsMap : HashMap<String, HelpRequest> = HashMap()
    val LOCATION_PERMISSION_ID = 100
    val VIEW_HELP_REQUEST = 200
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        //mMap.setOnMarkerClickListener(this)
        mMap.setOnInfoWindowClickListener(this)

        getLastLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_ID) {
            if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // Granted. Start getting the city information
                locationPermissionReceived()
            }
            else {
              /*  val alertDialog = (application as StarsEarthApplication)?.createAlertDialog(this)
                alertDialog.setTitle("Error")
                alertDialog.setMessage("We did not get location permission")
                alertDialog.setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })
                alertDialog.show()  */
            }
        }
    }

    //adminArea = State
    fun loadHelpRequests(adminArea: String) {
        Log.d(TAG, "********* LOAD HELP REQUESTS CALLED ************" + adminArea)
        val firebaseManager = FirebaseManager("help_requests")
        val query = firebaseManager.getQueryForState(adminArea)
        query.addListenerForSingleValueEvent(mHelpRequestsListener)

    }

    private fun getAddressFromLocation(location : Location) : List<Address?> {
        val geocoder: Geocoder
        val addresses: List<Address>
        geocoder = Geocoder(this, Locale.getDefault())
        addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1) // Here 1 represent max city result to returned, by documents it recommended 1 to 5
        //tvCity?.text = addresses.size.toString()
        return addresses
    }

    private fun checkPermissions(): Boolean {
        return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            true
        } else false
    }

    private fun isLocationEnabled() : Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun getLastLocation() {
        Log.d(TAG, "********GET LAST LOCATION************")
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                locationPermissionReceived()
            } else {
                Toast.makeText(this, "Turn on Location in Settings", Toast.LENGTH_LONG).show()
                //val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                //startActivity(intent)
            }
        } else {
            requestLocationPermission()
        }
    }

    fun locationPermissionReceived() {
        requestNewLocationData()
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        Log.d(TAG, "************REQUEST NEW LOCATION DATA*********s")
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        )
    }

    fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_ID
        )
    }

    override fun onInfoWindowClick(marker: Marker?) {
        marker?.tag?.let {
            val helpRequest = mHelpRequestsMap.get(it)
            val intent = Intent(this, MainActivity::class.java)
            val bundle = Bundle()
            bundle.putString("action", "VIEW_HELP_REQUEST")
            bundle.putParcelable("help_request", helpRequest)
            intent.putExtras(bundle)
            startActivityForResult(intent, VIEW_HELP_REQUEST)
        }
    }
}
