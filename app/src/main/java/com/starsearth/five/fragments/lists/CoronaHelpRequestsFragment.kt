package com.starsearth.five.fragments.lists

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper

import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.starsearth.five.R
import com.starsearth.five.activity.MainActivity
import com.starsearth.five.adapter.CoronaHelpRequestsRecyclerViewAdapter
import com.starsearth.five.application.StarsEarthApplication
import com.starsearth.five.domain.HelpRequest
import com.starsearth.five.domain.User
import com.starsearth.five.domain.datastructures.RequestComparator
import com.starsearth.five.fragments.SummaryFragment
import com.starsearth.five.managers.FirebaseManager

import kotlinx.android.synthetic.main.fragment_coronahelprequests_list.*
import kotlinx.android.synthetic.main.fragment_coronahelprequests_list.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [CoronaHelpRequestsFragment.OnListFragmentInteractionListener] interface.
 */
class CoronaHelpRequestsFragment : Fragment(), AdapterView.OnItemSelectedListener {

    // TODO: Customize parameters
    private var columnCount = 1

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mContext : Context
    private lateinit var mSelectedAdminArea : String
    private var mVolunteerOrg : String? = null
    private var mSelectedSubLocality : String? = null //Used for the dropdown
    private var mSelectedDateMillis : Long = -1
    private var mSpinnerArrayAdapter : ArrayAdapter<Any>? = null
    private var mCopyOfUser : User? = null
    private var mSubLocalities : LinkedHashMap<String, Int> = LinkedHashMap()
    private var mLastTimeStampForPagination : Long? = null
    private var listener: OnListFragmentInteractionListener? = null

    private var mMode : String? = null

    private val mHelpRequestsListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            llPleaseWait?.visibility = View.GONE
            var isListEmpty = true
            val map = dataSnapshot.value
            if (map != null) {
                //First clear the list so we can repopulate
                if (mLastTimeStampForPagination == null) (view?.list?.adapter as? CoronaHelpRequestsRecyclerViewAdapter)?.removeAllItems() //It means it is a first load, not a pagination load so remove all items
                val helpRequestList : ArrayList<HelpRequest> = ArrayList()
                for (entry in (map as HashMap<*, *>).entries) {
                    val key = entry.key as String
                    val value = entry.value as HashMap<String, Any>
                    var newHelpRequest = HelpRequest(key, value)
                    Log.d(TAG, "*******NEW REQUEST IS: "+newHelpRequest.uid)
                    helpRequestList.add(newHelpRequest)
                    isListEmpty = false
                }
                Collections.sort(helpRequestList, RequestComparator())
                (view?.list?.adapter as? CoronaHelpRequestsRecyclerViewAdapter)?.addItems(helpRequestList)
                (view?.list?.adapter as? CoronaHelpRequestsRecyclerViewAdapter)?.notifyDataSetChanged()
                if (mLastTimeStampForPagination == null) view?.list?.layoutManager?.scrollToPosition(0) //It means its the first load. So we can scroll to the top
                if (isListEmpty == false) {
                    mLastTimeStampForPagination = helpRequestList.last().timestampCompletion
                    list?.visibility = View.VISIBLE
                    tvEmptyList?.visibility = View.GONE
                }

            }
        }

        override fun onCancelled(p0: DatabaseError) {
            llPleaseWait?.visibility = View.GONE
        }

    }

    private val mValuesForDailySummaryListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            llPleaseWait?.visibility = View.GONE
            val valueMap = dataSnapshot.value
            if (valueMap != null) {
                var numberComplete = 0
                val volunteers : HashMap<String, Boolean> = HashMap()
                val postalCodes : HashMap<String?, Boolean> = HashMap()
                for (entry in (valueMap as HashMap<*, *>).entries) {
                    val key = entry.key as String
                    val value = entry.value as HashMap<String, Any>
                    var newHelpRequest = HelpRequest(key, value)
                    numberComplete++
                    volunteers.put(newHelpRequest.completedByUserId, true)
                    postalCodes.put(newHelpRequest.address?.postalCode, true)
                }
                val user = (activity?.application as? StarsEarthApplication)?.mUser
                val date = (activity as? MainActivity)?.convertDateToIST(Date(Calendar.getInstance().timeInMillis))
                val time = (activity as? MainActivity)?.convertTimeToIST(Date(Calendar.getInstance().timeInMillis))
                val map = HashMap<String, Any>()
                user?.let { map.put(SummaryFragment.ARG_USER, it) }
                date?.let { map.put(SummaryFragment.ARG_FORMATTED_DATE, it) }
                time?.let { map.put(SummaryFragment.ARG_FORMATTED_TIME, it) }
                map.put(SummaryFragment.ARG_COMPLETED, numberComplete)
                map.put(SummaryFragment.ARG_NUM_VOLUNTEERS, volunteers.size)
                map.put(SummaryFragment.ARG_NUM_AREAS, postalCodes.size)
                mVolunteerOrg?.let { map.put(SummaryFragment.ARG_VOLUNTEER_ORG, it) }
                listener?.onMenuItemSummaryTapped(map)
            }
            else {
                val alertDialog = (activity?.application as? StarsEarthApplication)?.createAlertDialog(mContext)
                alertDialog?.setTitle(mContext.getString(R.string.error))
                alertDialog?.setMessage("There was an issue. Please try again later")
                alertDialog?.setPositiveButton(getString(android.R.string.ok), null)
                alertDialog?.show()
            }
        }

        override fun onCancelled(p0: DatabaseError) {
            llPleaseWait?.visibility = View.GONE
            val alertDialog = (activity?.application as? StarsEarthApplication)?.createAlertDialog(mContext)
            alertDialog?.setTitle(mContext.getString(R.string.error))
            alertDialog?.setMessage("There was an issue. Please try again later")
            alertDialog?.setPositiveButton(getString(android.R.string.ok), null)
            alertDialog?.show()
        }

    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (mSelectedSubLocality != null) {
                Log.d(TAG, "**********mSelectedSubLocality IS: "+mSelectedSubLocality)
                //We really dont need this logic processed again if we already have a locality saved. Call should only happen once
                return
            }
            Log.d(TAG, " ********** LOCATION CALLBACK ***********")
            val location = locationResult.lastLocation
            location?.let {
                getAddressFromLocation(it.latitude, it.longitude).get(0)?.let {
                    mSelectedSubLocality = it.subLocality
                    if (mSelectedSubLocality != null) {
                        mSubLocalities[mSelectedSubLocality!!] = mSubLocalities[mSelectedSubLocality!!] ?: 1
                        mSpinnerArrayAdapter?.add(mSelectedSubLocality)
                        mSpinnerArrayAdapter?.notifyDataSetChanged()
                    }
                    else {
                        // THis is a hack to avoid a crash. If we do not have a sublocality, we will just show a zip code
                        val zipCode : String = it.postalCode
                        mSubLocalities[zipCode] = mSubLocalities[zipCode] ?: 1
                        mSpinnerArrayAdapter?.add(zipCode)
                        mSpinnerArrayAdapter?.notifyDataSetChanged()
                    }
                    mSelectedAdminArea = it.adminArea
                    //loadHelpRequests(mSelectedLocality) //This call is not needed as the spinner.add() call above triggers the loadHelpRequests calls
                }
            }
        }
    }

    private fun isDateMatching(timeMillis : Long) : Boolean {
        val selectedDate = Calendar.getInstance()
        selectedDate.timeInMillis = mSelectedDateMillis //This is the date user has selected in the filter
        val downloadedDate = Calendar.getInstance()
        downloadedDate.timeInMillis = timeMillis
        return selectedDate.get(Calendar.YEAR) == downloadedDate.get(Calendar.YEAR)
                && selectedDate.get(Calendar.MONTH) == downloadedDate.get(Calendar.MONTH)
                && selectedDate.get(Calendar.DATE) == downloadedDate.get(Calendar.DATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)

        mSelectedDateMillis = Calendar.getInstance().timeInMillis

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
            mCopyOfUser = it.getParcelable(ARG_USER)
            mVolunteerOrg = mCopyOfUser?.volunteerOrganization

            mMode = it.getString(ARG_MODE)
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_coronahelprequests_list, container, false)
        Log.d(TAG, "**********ON CREATE VIEW************")

        // Set the adapter
        if (view.list is RecyclerView) {
            with(view.list) {
                (view.list as RecyclerView).layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                view.list.addItemDecoration(DividerItemDecoration(context,
                        DividerItemDecoration.VERTICAL))
                val mainList : ArrayList<HelpRequest> = ArrayList()
                (view.list as RecyclerView).adapter = CoronaHelpRequestsRecyclerViewAdapter(mContext, mainList, listener)
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "**********ON VIEW CREATED**************")

        if (mVolunteerOrg != null) {
            // Viewing requests at volunteer org level, not individual level
            tvVolunteerOrg?.text = mVolunteerOrg
            tvVolunteerOrg?.visibility = View.VISIBLE
        }
        mSpinnerArrayAdapter = ArrayAdapter(mContext,android.R.layout.simple_spinner_item, ArrayList<String>().toMutableList() as List<Any>)
        mSpinnerArrayAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocality?.setAdapter(mSpinnerArrayAdapter)
        spinnerLocality?.onItemSelectedListener = this

        if (mMode == MODE_CHANGE_DATE) {
            llRowButtons?.visibility = View.GONE
            btnDate?.visibility = View.VISIBLE
            btnDate?.text = (activity as? MainActivity)?.getFormattedDate(mSelectedDateMillis)
            btnDate?.setOnClickListener {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = mSelectedDateMillis
                val day = calendar.get(Calendar.DAY_OF_MONTH);
                val month = calendar.get(Calendar.MONTH);
                val year = calendar.get(Calendar.YEAR);
                // date picker dialog
                val picker = DatePickerDialog(mContext,
                        DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                            val cal2 = Calendar.getInstance()
                            cal2.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            cal2.set(Calendar.MONTH, monthOfYear)
                            cal2.set(Calendar.YEAR, year)
                            mSelectedDateMillis = cal2.timeInMillis
                            val dateFormat = SimpleDateFormat("dd-MMM-yyyy")
                            //dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
                            btnDate?.text = dateFormat.format(cal2.time)
                            Log.d(TAG, "*******DATE PICKER*************")
                            mLastTimeStampForPagination = null //We do this so that the list can be populated for the new date
                            loadHelpRequestsForToday(mSelectedDateMillis)
                        }, year, month, day);
                picker.show();
            }
        }
        else {
            llRowButtons?.visibility = View.VISIBLE
            btnDate?.visibility = View.GONE
            ivDelivery?.setOnClickListener {
                listener?.onCoronaHelpListFragmentAddButtonTapped()
            }
            ivReport?.setOnClickListener {
                llPleaseWait?.visibility = View.VISIBLE
                loadHelpRequestsForDailySummary(mSelectedDateMillis)
              /*  val user = (activity?.application as? StarsEarthApplication)?.mUser
                val dateTime = (activity as? MainActivity)?.convertDateTimeToIST(Date(Calendar.getInstance().timeInMillis))
                val map = HashMap<String, Any>()
                user?.let { map.put(SummaryFragment.ARG_USER, it) }
                dateTime?.let { map.put(SummaryFragment.ARG_FORMATTED_DATE_TIME, it) }
                map.put(SummaryFragment.ARG_COMPLETED, mNumberComplete)
                map.put(SummaryFragment.ARG_NUM_VOLUNTEERS, mVolunteers.size)
                map.put(SummaryFragment.ARG_NUM_AREAS, mPostalCodes.size)
                mVolunteerOrg?.let { map.put(SummaryFragment.ARG_VOLUNTEER_ORG, it) }
                listener?.onMenuItemSummaryTapped(map)  */



             /*   llPleaseWait?.visibility = View.VISIBLE
                val picUrl : String? = (activity?.application as? StarsEarthApplication)?.mUser?.pic
                if (picUrl != null) {
                    var profilePicRef = FirebaseStorage.getInstance().reference.child(picUrl)
                    val ONE_MEGABYTE: Long = 1024 * 1024
                    profilePicRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
                        llPleaseWait?.visibility = View.GONE
                        map.put(SummaryFragment.ARG_BYTE_ARRAY, it)
                        listener?.onMenuItemSummaryTapped(map)
                    }.addOnFailureListener {
                        // Handle any errors
                        llPleaseWait?.visibility = View.GONE
                        listener?.onMenuItemSummaryTapped(map)
                        /*   val alertDialog = (activity?.application as? StarsEarthApplication)?.createAlertDialog(mContext)
                           alertDialog?.setTitle(mContext.getString(R.string.error))
                           alertDialog?.setMessage("Operation failed. Please try again later")
                           alertDialog?.setPositiveButton(getString(android.R.string.ok), null)
                           alertDialog?.show() */
                    }
                }
                else {
                    llPleaseWait?.visibility = View.GONE
                    listener?.onMenuItemSummaryTapped(map)
                    /*   val alertDialog = (activity?.application as? StarsEarthApplication)?.createAlertDialog(mContext)
                       alertDialog?.setTitle(mContext.getString(R.string.error))
                       alertDialog?.setMessage("You need a photo in order to produce a summary sheet. Please contact hasijaadarsh@gmail.com for more information")
                       alertDialog?.setPositiveButton(getString(android.R.string.ok), null)
                       alertDialog?.show() */
                }   */
            }
            ivCalendar?.setOnClickListener {
                listener?.onModeDateChangeRequested()
            }
        }

        list?.addOnScrollListener(object : RecyclerView.OnScrollListener(){

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                val layoutManager = (view.list as RecyclerView).layoutManager as LinearLayoutManager

                if(recyclerView.canScrollVertically(-1) == false && dy != 0) { //dy != 0 means user was scrolling rather than the first load of the page happening
                    // We have reached the end of the recycler view.
                    Log.d("TAG", "*********** REFRESH POINT DETECTED *************")
                    //llPleaseWait?.visibility = View.VISIBLE
                    //mLastTimeStampForPagination = null
                    //loadHelpRequestsForToday(mSelectedDateMillis)
                }
                else if(layoutManager.findLastVisibleItemPosition() == layoutManager.itemCount-1 && !recyclerView.canScrollVertically(1)){ //1 = down
                    // We have reached the end of the recycler view.
                    llPleaseWait?.visibility = View.VISIBLE
                    Log.d("TAG", "*********** PAGINATION POINT DETECTED *************")
                    (activity?.application as StarsEarthApplication)?.analyticsManager?.sendAnalyticsForPagination()
                    loadHelpRequestsForToday(mSelectedDateMillis)
                }

                super.onScrolled(recyclerView, dx, dy)
            }
        })

        list?.visibility = View.GONE
        tvEmptyList?.visibility = View.VISIBLE
        llPleaseWait?.visibility = View.VISIBLE
        mLastTimeStampForPagination = null
        loadHelpRequestsForToday(mSelectedDateMillis)
      /*  if (mSelectedSubLocality == null) {
            getLastLocation()
        }
        else {
            mSubLocalities[mSelectedSubLocality!!] = mSubLocalities[mSelectedSubLocality!!] ?: 1
            mSpinnerArrayAdapter?.add(mSelectedSubLocality!!)
            mSpinnerArrayAdapter?.notifyDataSetChanged()
        }   */
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        val selectedItem : String = parent.getItemAtPosition(pos) as String
        var fullText = selectedItem.split("(")
        mSelectedSubLocality = fullText?.get(0)?.trim()
        Log.d(TAG, "**********ON ITEM SELECTED**********"+mSelectedSubLocality)
        loadHelpRequests(mSelectedAdminArea)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }


    //adminArea = State
    fun loadHelpRequests(adminArea: String) {
        Log.d(TAG, "********* LOAD HELP REQUESTS CALLED ************" + adminArea)
        val firebaseManager = FirebaseManager("help_requests")
        val query = firebaseManager.getQueryForState(adminArea)
        query.addListenerForSingleValueEvent(mHelpRequestsListener)
    }

    fun loadHelpRequestsForToday(todayMillis: Long) {
        var paginationLimit = 10
        (activity?.application as StarsEarthApplication).getFirebaseRemoteConfigWrapper().paginationLimit?.let {
            Log.d(TAG, "*******PAGINATION LIMIT IS: "+it)
            paginationLimit = it.toInt()
        }

        val calToday = Calendar.getInstance()
        calToday.timeInMillis = todayMillis
        calToday.set(Calendar.HOUR, 0)
        calToday.set(Calendar.MINUTE, 0)
        calToday.set(Calendar.SECOND, 0)
        calToday.set(Calendar.MILLISECOND, 0)
        val todayTimeMillis = calToday.timeInMillis
        val firebaseManager = FirebaseManager("requests")

        if (mLastTimeStampForPagination != null) {
            //This becomes our start time
            Log.d(TAG, "*********** PAGINATED *************")
            val tsMinusOneMS = Calendar.getInstance()
            tsMinusOneMS.timeInMillis = mLastTimeStampForPagination!!
            tsMinusOneMS.add(Calendar.MILLISECOND, -1) //We add -1 so that we do not get a repeat of the last time in the list
            val query = firebaseManager.getQueryForRequestsCompletedBetweenDatesWithPagination(todayTimeMillis, tsMinusOneMS.timeInMillis, paginationLimit)
            query.addListenerForSingleValueEvent(mHelpRequestsListener)
        }
        else {
            //Take yerterday as our date
            Log.d(TAG, "************** NOT PAGINATED ***************")
            val calTomorrow = Calendar.getInstance()
            calTomorrow.timeInMillis = todayMillis
            calTomorrow.add(Calendar.DATE, 1)
            calTomorrow.set(Calendar.HOUR, 0)
            calTomorrow.set(Calendar.MINUTE, 0)
            calTomorrow.set(Calendar.SECOND, 0)
            calTomorrow.set(Calendar.MILLISECOND, 0)
            val endTimeMillis = calTomorrow.timeInMillis

            val query = firebaseManager.getQueryForRequestsCompletedBetweenDatesWithPagination(todayTimeMillis, endTimeMillis, paginationLimit)
            query.addListenerForSingleValueEvent(mHelpRequestsListener)
        }
    }

    // We obtain all the help requests in the day but not to store them. We need them to just calculate values for the daily summary
    fun loadHelpRequestsForDailySummary(todayMillis: Long) {
        val calToday = Calendar.getInstance()
        calToday.timeInMillis = todayMillis
        calToday.set(Calendar.HOUR, 0)
        calToday.set(Calendar.MINUTE, 0)
        calToday.set(Calendar.SECOND, 0)
        calToday.set(Calendar.MILLISECOND, 0)
        val yesterdayTimeMillis = calToday.timeInMillis
        val calTomorrow = Calendar.getInstance()
        calTomorrow.timeInMillis = todayMillis
        calTomorrow.add(Calendar.DATE, 1)
        calTomorrow.set(Calendar.HOUR, 0)
        calTomorrow.set(Calendar.MINUTE, 0)
        calTomorrow.set(Calendar.SECOND, 0)
        calTomorrow.set(Calendar.MILLISECOND, 0)
        val endTimeMillis = calTomorrow.timeInMillis
        val firebaseManager = FirebaseManager("requests")
        val query = firebaseManager.getQueryForRequestsCompletedBetweenDates(yesterdayTimeMillis, endTimeMillis)
        query.addListenerForSingleValueEvent(mValuesForDailySummaryListener)
    }



    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            mContext = context
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater?.inflate(R.menu.fragment_help_requests_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item!!.itemId) {
            R.id.add -> {
                listener?.onCoronaHelpListFragmentAddButtonTapped()
                return true
            }
        }

        return false
    }

    // LOCATION RELATED FUNCTIONS

    //@SuppressLint("MissingPermission")
    fun locationPermissionReceived() {
        requestNewLocationData()
     /*   mFusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    // Got last known city. In some rare situations this can be null.
                    if (location == null) {
                        requestNewLocationData()
                    }
                    location?.let {
                        getAddressFromLocation(it).get(0)?.let {
                            Log.d("TAG", "******* ADDRESS IS: " + it.subLocality)
                            mAddressFromPhone = SEAddress(it)
                            mSubAdminAreas[mAddressFromPhone!!.subLocality] = mSubAdminAreas[mAddressFromPhone!!.subLocality] ?: 1
                            var list = ArrayList<String>()
                            list.add(mAddressFromPhone!!.subLocality)

                            val aa = ArrayAdapter(mContext,android.R.layout.simple_spinner_item,list.toArray())
                            aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            //Setting the ArrayAdapter data on the Spinner
                            spinnerLocality.setAdapter(aa)
                            spinnerLocality.setSelection(0)

                            loadHelpRequests(mAddressFromPhone!!)
                        }
                    }
                }   */

        //val mLocationManager = mContext.getSystemService(LOCATION_SERVICE) as LocationManager
        //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1.0f, mLocationListener);
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        Log.d(TAG, "************REQUEST NEW LOCATION DATA*********s")
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        )
    }

    private fun getAddressFromLocation(latitude : Double, longitude : Double) : List<Address?> {
        val geocoder: Geocoder
        val addresses: List<Address>
        geocoder = Geocoder(mContext, Locale.getDefault())
        addresses = geocoder.getFromLocation(latitude, longitude, 1) // Here 1 represent max city result to returned, by documents it recommended 1 to 5
        //tvCity?.text = addresses.size.toString()
        return addresses
    }

    private fun checkPermissions(): Boolean {
        return if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            true
        } else false
    }

    private fun isLocationEnabled() : Boolean {
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun getLastLocation() {
        Log.d(TAG, "********GET LAST LOCATION************")
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                locationPermissionReceived()
            } else {
                Toast.makeText(mContext, "Turn on Location in Settings", Toast.LENGTH_LONG).show()
                //val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                //startActivity(intent)
            }
        } else {
            listener?.requestLocationToViewHelpRequests()
        }
    }

    // END OF LOCATION RELATED FUNCTIONS

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onCoronaHelpListFragmentInteraction(item: HelpRequest)
        fun onCoronaHelpListFragmentAddButtonTapped()
        fun requestLocationToViewHelpRequests()
        fun onMenuItemSummaryTapped(hashMap: HashMap<String, Any>)
        fun onModeDateChangeRequested()
    }

    companion object {

        // TODO: Customize parameter argument names
        val TAG = "CORONA_HELP_REQ_FRAG"
        const val ARG_COLUMN_COUNT = "column-count"
        const val ARG_USER = "user"
        const val ARG_ORG = "org"
        const val ARG_MODE = "mode"
        const val MODE_CHANGE_DATE = "mode_change_date"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance() =
                CoronaHelpRequestsFragment()

        @JvmStatic
        fun newInstance(columnCount: Int, user : User?) =
                CoronaHelpRequestsFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                        putParcelable(ARG_USER, user)
                    }
                }

     /*   @JvmStatic
        fun newInstance(volunteerOrg: String) =
                CoronaHelpRequestsFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_ORG, volunteerOrg)
                    }
                }   */

        @JvmStatic
        fun newInstance(mode: String) =
                CoronaHelpRequestsFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_MODE, mode)
                    }
                }
    }
}
