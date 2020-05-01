package com.starsearth.five.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.starsearth.five.R
import com.starsearth.five.activity.auth.AddEditPhoneNumberActivity
import com.starsearth.five.application.StarsEarthApplication
import com.starsearth.five.domain.HelpRequest
import com.starsearth.five.domain.User
import com.starsearth.five.managers.FirebaseManager
import java.util.*
import kotlin.collections.HashMap

class MapsActivity :
        AppCompatActivity(),
        OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener,
        PlaceSelectionListener {

    private lateinit var mMap: GoogleMap
    val TAG = "MAPS_ACTIVITY"

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            location?.let {
                val currentLocation = LatLng(it.latitude, it.longitude)
                mMap.addMarker(MarkerOptions()
                        .position(currentLocation)
                        .title("Your Current Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15f))
                getAddressFromLocation(it.latitude, it.longitude).get(0)?.let {
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

    private val mUserValueChangeListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val key = dataSnapshot?.key
            val value = dataSnapshot?.value as Map<String, Any?>
            if (key != null && value != null) {
                (application as? StarsEarthApplication)?.mUser = User(key, value)
            }
        }

        override fun onCancelled(p0: DatabaseError) {

        }

    }

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val mHelpRequestsMap : HashMap<String, HelpRequest> = HashMap()
    val LOCATION_PERMISSION_ID = 100
    val HELP_REQUEST = 200
    val NEW_HELP_REQUEST_NO_PHONE_NUMBER_FOUND = 300
    val VIEW_PROFILE = 400
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        if ((application as? StarsEarthApplication)?.mUser == null) {
            updatedUserProperties()
        }

        // Initialize the SDK
    /*    val apiKey = "AIzaSyBQcoAuwEbU_RsV3064XvKwpvD-xvF8RCM"
        Places.initialize(getApplicationContext(), apiKey)

        // Create a new Places client instance
        val placesClient = Places.createClient(this)    */ //Temporarily removing this because of security issues

        // Initialize the AutocompleteSupportFragment.
        //val autocompleteFragment = getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        // Specify the types of place data to return.
        //autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        //autocompleteFragment.setOnPlaceSelectedListener(this)


    /*    btnNewEntry?.setOnClickListener {
            if (mUser != null) {
                val intent = Intent(this, MainActivity::class.java)
                val bundle = Bundle()
                bundle.putString("action", "NEW_HELP_REQUEST")
                intent.putExtras(bundle)
                startActivityForResult(intent, NEW_HELP_REQUEST)
            }
            else {
                val alertDialog = (application as StarsEarthApplication)?.createAlertDialog(this)
                alertDialog.setTitle("Register Phone Number")
                alertDialog.setMessage("You need to register a phone number to continue")
                alertDialog.setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                    val intent = Intent(this, AddEditPhoneNumberActivity::class.java)
                    val bundle = Bundle()
                    intent.putExtras(bundle)
                    startActivityForResult(intent, NEW_HELP_REQUEST_NO_PHONE_NUMBER_FOUND)
                })
                alertDialog.setPositiveButton(android.R.string.cancel, DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })
                alertDialog.show()
            }
        }   */

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        //mapFragment.getMapAsync(this)
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
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Activity.RESULT_OK) {
            if (resultCode == NEW_HELP_REQUEST_NO_PHONE_NUMBER_FOUND) {
                updatedUserProperties()
                val intent = Intent(this, MainActivity::class.java)
                val bundle = Bundle()
                bundle.putString("action", "NEW_HELP_REQUEST")
                intent.putExtras(bundle)
                startActivityForResult(intent, HELP_REQUEST)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.activity_maps, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId

        if (id == R.id.profile) {
            val intent = Intent(this, MainActivity::class.java)
            val bundle = Bundle()
            bundle.putString("action", "VIEW_PROFILE")
            intent.putExtras(bundle)
            startActivityForResult(intent, VIEW_PROFILE)
            return true
        }
        if (id == R.id.logout) {
            FirebaseAuth.getInstance().signOut()
            Log.d(TAG, "************* LOGOUT SUCCESSFUL ************")
            finish()
            intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            return true;
        }

        return super.onOptionsItemSelected(item)
    }

    //This is called from any fragment whenever User object is updated
    fun updatedUserProperties() {
        Log.d("TAG", "*******MAPS ACTIVITY UPDATE USER PROPERTIES*********")
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            val firebaseManager = FirebaseManager("users")
            val query = firebaseManager.getQueryForUserObject(it.uid)
            query.addListenerForSingleValueEvent(mUserValueChangeListener)
        }
    }

    //adminArea = State
    fun loadHelpRequests(googlePlaceId: String) {
        Log.d(TAG, "********* MAPS ACTIVITY LOAD HELP REQUESTS CALLED ************" + googlePlaceId)
        val firebaseManager = FirebaseManager("help_requests")
        val query = firebaseManager.getQueryForGooglePlaceId(googlePlaceId)
        query.addListenerForSingleValueEvent(mHelpRequestsListener)

    }

    private fun getAddressFromLocation(latitude : Double, longitude : Double) : List<Address?> {
        val geocoder: Geocoder
        val addresses: List<Address>
        geocoder = Geocoder(this, Locale.getDefault())
        addresses = geocoder.getFromLocation(latitude, longitude, 1) // Here 1 represent max city result to returned, by documents it recommended 1 to 5
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
        val intent = Intent(this, MainActivity::class.java)
        val bundle = Bundle()
        bundle.putString("action", "HELP_REQUEST")
        marker?.tag?.let {
            //it = uid of help request
            val helpRequest = mHelpRequestsMap[it]
            helpRequest?.let { bundle.putParcelable("help_request", it) }
        }
        intent.putExtras(bundle)
        startActivityForResult(intent, HELP_REQUEST)
     /*   if (mUser != null) {

        }
        else {
            val alertDialog = (application as StarsEarthApplication)?.createAlertDialog(this)
            alertDialog.setTitle("Register Phone Number")
            alertDialog.setMessage("You need to register a phone number to continue")
            alertDialog.setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                val intent = Intent(this, AddEditPhoneNumberActivity::class.java)
                val bundle = Bundle()
                intent.putExtras(bundle)
                startActivityForResult(intent, NEW_HELP_REQUEST_NO_PHONE_NUMBER_FOUND)
            })
            alertDialog.setNegativeButton(android.R.string.cancel, DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            })
            alertDialog.show()
        }   */



    }

    //OnPlaceSelectedListener
    override fun onPlaceSelected(place: Place) {
        Log.d(TAG, "**********place: "+place.latLng?.latitude + "************" + place.latLng?.longitude+"**************"+place.types)
        place.latLng?.let {
            val marker = mMap.addMarker(MarkerOptions()
                    .position(it)
                    .title("Tap to create help request")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLng(it))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15f))
            place.id?.let {
                loadHelpRequests(it)
            }
          /*  getAddressFromLocation(it.latitude, it.longitude).get(0)?.let {
                loadHelpRequests(it.adminArea)
            }   */
        }

    }

    //OnPlaceSelectedListener
    override fun onError(status: Status) {
        Log.d(TAG, "**********ERROR IS: " + status.statusMessage)
    }

}
