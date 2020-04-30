package com.starsearth.five.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.*
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import com.starsearth.five.R
import com.starsearth.five.activity.MainActivity
import com.starsearth.five.application.StarsEarthApplication
import com.starsearth.five.domain.HelpRequest
import com.starsearth.five.domain.SEAddress
import com.starsearth.five.domain.User
import kotlinx.android.synthetic.main.fragment_corona_help_request_form.*
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_HOST_NUMBER = "host_number"
private const val ARG_HOST_NAME = "host_name"
private const val ARG_GUEST_NUMBER = "guest_number"
private const val ARG_GUEST_NAME = "guest_name"
private const val ARG_HELP_REQUEST = "help_request"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [CoronaHelpRequestFormFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [CoronaHelpRequestFormFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CoronaHelpRequestFormFragment : Fragment(), AdapterView.OnItemSelectedListener {
    // TODO: Rename and change types of parameters
    private lateinit var mContext : Context
    private var mHostPhone: String? = null //If we are re-opening the form in "onBehalfOf" mode.
    private var mHostName: String? = null
    private var mGuestPhone: String? = null
    private var mGuestName: String? = null
    private var mHelpRequest : HelpRequest? = null
    private var mAddressFromPhone : SEAddress? = null
    private var mRequest : String? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var listener: OnFragmentInteractionListener? = null

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.d("TAG", " ********** LOCATION CALLBACK ***********"+ locationResult)
            val lastLocation = locationResult.lastLocation
            lastLocation?.let {
                getAddressFromLocation(it.latitude, it.longitude).get(0)?.let {
                    mAddressFromPhone = SEAddress(it)
                    val addressLine =   it.getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                    val city = it.locality
                    val state = it.adminArea
                    val country = it.countryName
                    val postalCode = it.postalCode
                    val knownName = it.featureName // Only if available else return NULL
                    val locality = it.locality
                    val subLocality = it.subLocality // This is the area
                    val premesis = it.premises
                    val subAdminArea = it.subAdminArea
                    tvSublocality?.text = addressLine //+ "\n" + city + "\n" + state + "\n" + country + "\n" + postalCode
                    tvSublocality?.visibility = View.VISIBLE
                    tvLocationLbl?.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)

        arguments?.let {
            mHostPhone = it.getString(ARG_HOST_NUMBER)
            mHostName = it.getString(ARG_HOST_NAME)
            mGuestPhone = it.getString(ARG_GUEST_NUMBER)
            mGuestName = it.getString(ARG_GUEST_NAME)
            mHelpRequest = it.getParcelable(ARG_HELP_REQUEST)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_corona_help_request_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mHelpRequest != null) {
            //It is an existing request. Populate
            llLogoTitle?.visibility = View.VISIBLE
            tvVolunteerNetworkLbl?.text = "SE Volunteer Network"
            (activity?.application as StarsEarthApplication).getFirebaseRemoteConfigWrapper().volunteerNetworkName?.let {
                tvVolunteerNetworkLbl?.text = it
            }
            if (mHelpRequest!!.status == "COMPLETE" && mHelpRequest!!.timestampCompletion > 0) {
                llDeliveryStatus?.visibility = View.VISIBLE
                tvDeliveryDate?.visibility = View.VISIBLE
                tvDeliveryDate?.text = (activity as? MainActivity)?.convertDateTimeToIST(Date(mHelpRequest!!.timestampCompletion))
                mHelpRequest!!.completedByPhone?.let {
                    var textToShow = it
                    if (mHelpRequest!!.completedByName != null) {
                        textToShow += " - " + mHelpRequest!!.completedByName
                    }
                    tvDeliveredByNameNumber?.visibility = View.VISIBLE //We need to have at least a phone number in order to display this
                    tvDeliveredByNameNumber?.text = "Delivered by: " + textToShow
                }
            }
            //tvPhoneNumberLbl?.visibility = View.VISIBLE
            //tvPhoneNumber?.text = mHelpRequest!!.phone
            //tvPhoneNumber?.visibility = View.VISIBLE
            if (mHelpRequest!!.request == "GROCERIES") {
                llGroceries?.setBackgroundColor(Color.YELLOW)
            }
            else if (mHelpRequest!!.request == "FOOD") {
                llFood?.setBackgroundColor(Color.YELLOW)
            }
            tvLocationLbl?.visibility = View.VISIBLE
            tvSublocality?.text = mHelpRequest!!.address?.addressLine //+ "\n" + mHelpRequest!!.address.locality + "\n" + mHelpRequest!!.address.adminArea + "\n" + mHelpRequest!!.address.countryName + "\n" + mHelpRequest!!.address.postalCode
            tvSublocality?.visibility = View.VISIBLE
            //etName?.visibility = View.GONE
            //tvNameLabel?.visibility = View.VISIBLE
          /*  tvName?.text =
                    if (mHelpRequest!!.name.isNullOrEmpty()) {
                        "Not given"
                    }
                    else {
                        mHelpRequest!!.name
                    }   */
            //tvName?.visibility = View.VISIBLE
         /*   if (mHelpRequest!!.guestPhone != null || mHelpRequest!!.guestName != null) {
                mGuestPhone = mHelpRequest!!.guestPhone
                mGuestName = mHelpRequest!!.guestName
                tvOnBehalfOf?.text = "ON BEHALF OF:\n" + mGuestPhone + "\n" + mGuestName
                tvOnBehalfOf?.visibility = View.VISIBLE
            }
            etLandmark?.visibility = View.GONE
            tvLandmarkEnetered?.visibility = View.VISIBLE
            tvLandmarkEnetered?.text = mHelpRequest!!.landmark
            tvLandmarkEnetered?.visibility = View.VISIBLE
            tvNeedHelpWithLbl?.visibility = View.VISIBLE
            tvSelectedRequest?.text = mHelpRequest!!.request
            tvSelectedRequest?.visibility = View.VISIBLE
            if (mHelpRequest!!.request == "DISTRIBUTION") {
                tvNoOfFamilyMembersLbl?.visibility = View.VISIBLE
                tvNoOfFamilyMembers?.visibility = View.VISIBLE
                tvNoOfFamilyMembers?.text = mHelpRequest!!.noOfFamilyMembers
                tvAidTypeLbl?.visibility = View.VISIBLE
                tvAidType?.visibility = View.VISIBLE
                tvAidType?.text = mHelpRequest!!.aidType
                tvRationCardLbl?.visibility = View.VISIBLE
                tvRationCard?.visibility = View.VISIBLE
                tvRationCard?.text = mHelpRequest!!.rationCard
            }
            mHelpRequest!!.volunteerOrganization?.let {
                tvYourOrganizationLbl?.visibility = View.VISIBLE
                tvYourOrganization?.visibility = View.VISIBLE
                tvYourOrganization?.text =
                        if (it.isNotEmpty()) {
                            it
                        }
                        else {
                            "Not given"
                        }
                etOrganization?.visibility = View.GONE
            }
            spinnerRequest?.visibility = View.GONE
            btnSubmit?.visibility = View.GONE
            btnMap?.visibility = View.VISIBLE
            btnComplete?.visibility =
                    if ((activity?.application as? StarsEarthApplication)?.mUser?.volunteerOrganization == mHelpRequest!!.volunteerOrganization) { //Only volunteers from the same organization can declare it complete
                        View.VISIBLE
                    }
                    else {
                        View.GONE
                    }
            btnCancel?.visibility =
                    if (FirebaseAuth.getInstance().currentUser?.phoneNumber == mHelpRequest!!.phone) { //Only the creator is allowed to declare it complete
                        View.VISIBLE
                    }
                    else {
                        View.GONE
                    }   */


            btnMap?.setOnClickListener {
                val uri = String.format(Locale.ENGLISH, "geo:%f,%f", mHelpRequest!!.address.latitude, mHelpRequest!!.address.longitude);
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                mContext.startActivity(intent)
            }

         /*   if (mHelpRequest!!.status == "COMPLETE") {
                btnComplete?.visibility = View.GONE
            }
            else {
                btnComplete?.visibility = View.VISIBLE
                btnComplete?.setOnClickListener {
                    val alertDialog = (activity?.application as StarsEarthApplication)?.createAlertDialog(mContext)
                    alertDialog.setTitle("Open camera?")
                    alertDialog.setMessage("To declare this complete, you must click a selfie with the items you are delivering and the people you are delivering to. Shall we open the camera?")
                    alertDialog.setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()

                        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            // Permission is not granted
                            listener?.requestCameraAccessToConfirmCompletionOfHelpRequest()
                        }
                        else {
                            cameraPermissionReceived()
                        }
                    })
                    alertDialog.setNegativeButton(android.R.string.no, DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                    })
                    alertDialog.show()

                }
            }


            if (mHelpRequest!!.status == "COMPLETE") {
                btnCancel?.visibility = View.GONE
            }
            else {
                btnCancel?.visibility = View.VISIBLE
                btnCancel?.setOnClickListener {
                    val alertDialog = (activity?.application as StarsEarthApplication)?.createAlertDialog(mContext)
                    alertDialog.setTitle("Are you sure?")
                    alertDialog.setMessage("This cannot be undone")
                    alertDialog.setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()

                        val mDatabase = FirebaseDatabase.getInstance().getReference("help_requests/" + mHelpRequest!!.uid)
                        mDatabase.removeValue().addOnSuccessListener {
                            listener?.requestCompleted()
                        }.addOnFailureListener {
                            val alertDialog2 = (activity?.application as StarsEarthApplication)?.createAlertDialog(mContext)
                            alertDialog2.setTitle("Error")
                            alertDialog2.setMessage("Failed to delete. Please try again")
                            alertDialog2.setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                            })
                            alertDialog2.show()
                        }
                    })
                    alertDialog.setNegativeButton(android.R.string.no, DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                    })
                    alertDialog.show()
                }
            }   */


            mHelpRequest!!.picCompleteUrl?.let {
                llPicOfCompletion?.visibility = View.VISIBLE
                pbPicShow?.visibility = View.VISIBLE
                var profilePicRef = FirebaseStorage.getInstance().reference.child(it)

                val ONE_MEGABYTE: Long = 1024 * 1024
                profilePicRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
                    pbPicShow?.visibility = View.GONE
                    ivPicOfCompletion?.visibility = View.VISIBLE
                    val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    ivPicOfCompletion?.setImageBitmap(bitmap)
                }.addOnFailureListener {
                    // Handle any errors
                    pbPicShow?.visibility = View.GONE
                    tvPicError?.visibility = View.VISIBLE
                }
            }

            return
        }
        else if (mHostPhone != null && mHostName != null) {
            //Original form is being filled on behalf of someone.
            //Therefore show only fields related to name, phone and location
            etPhoneNumber?.visibility = View.VISIBLE
            etPhoneNumber?.setText(mGuestPhone, TextView.BufferType.EDITABLE)
            etName?.visibility = View.VISIBLE
            etName?.hint = "Name of person or head of family"
            etName?.setText(mGuestName, TextView.BufferType.EDITABLE)
            btnSubmit?.visibility = View.VISIBLE
            btnSubmit?.text = "DONE"
            btnSubmit?.setOnClickListener {
                val phone = etPhoneNumber?.text?.toString() ?: ""
                val name = etName?.text?.toString() ?: ""
                listener?.onBehalfOfDetailsEntered(phone, name)
            }
            return
        }

        getLastLocation()
    /*    tvPhoneNumberLbl?.visibility = View.VISIBLE
        if (phoneNumber != null) {
            tvPhoneNumber?.visibility = View.VISIBLE
            tvPhoneNumber?.text = phoneNumber
        }
        else {
            etPhoneNumber?.visibility = View.VISIBLE
        }
        val userName = (activity as? MainActivity)?.mUser?.name
        if (userName.isNullOrEmpty()) {
            tvNameLabel.visibility = View.GONE
            tvName.visibility = View.GONE
            etName.visibility = View.VISIBLE
        }
        else {
            tvNameLabel.visibility = View.VISIBLE
            tvName.visibility = View.VISIBLE
            tvName.text = userName
            etName.visibility = View.GONE
        }
        btnOnBehalf?.setOnClickListener {
            val pn = tvPhoneNumber?.text?.toString() ?: ""
            val name = tvName?.text?.toString() ?: ""
            listener?.onBehalfOfFormRequested(pn, name, mGuestPhone, mGuestName)
        }
        val volunteerOrganization = (activity as? MainActivity)?.mUser?.volunteerOrganization
        if (volunteerOrganization != null) {
            tvYourOrganizationLbl?.visibility = View.VISIBLE
            tvYourOrganization?.visibility = View.VISIBLE
            tvYourOrganization?.text = volunteerOrganization
        }
        else {
            etOrganization?.visibility = View.VISIBLE
        }
        btnOnBehalf?.visibility = View.VISIBLE
        etLandmark?.visibility = View.VISIBLE
        etPhoneNumber?.visibility = View.VISIBLE
        etPhoneNumber?.hint = "Recipient Phone Number"
        etName?.visibility = View.VISIBLE
        etName?.hint = "Recipient Name"
        etLandmark?.visibility = View.VISIBLE   */
        val phoneNumber = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
        val spinnerList = ArrayList<String>()
        spinnerList.add("FOOD")
        spinnerList.add("GROCERIES")
        spinnerList.add("MEDICAL")
        //spinnerList.add("DISTRIBUTION")

        //Creating the ArrayAdapter instance having the country list
        val aa = ArrayAdapter(mContext,android.R.layout.simple_spinner_item,spinnerList.toArray())
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spinnerRequest.setAdapter(aa);
        spinnerRequest.setSelection(0)
        //tvNeedHelpWithLbl?.visibility = View.VISIBLE
        //spinnerRequest?.visibility = View.VISIBLE

        //spinnerRequest?.onItemSelectedListener = this
        llRequest?.visibility = View.VISIBLE
        llGroceries?.setBackgroundColor(Color.YELLOW)
        mRequest = "GROCERIES"
        llGroceries?.setOnClickListener {
            mRequest = "GROCERIES"
            llGroceries?.setBackgroundColor(Color.YELLOW)
            llFood?.setBackgroundColor(Color.WHITE)
        }
        llFood?.setOnClickListener {
            mRequest = "FOOD"
            llFood?.setBackgroundColor(Color.YELLOW)
            llGroceries?.setBackgroundColor(Color.WHITE)
        }
        btnComplete?.visibility = View.VISIBLE
        btnComplete?.setOnClickListener {
            val alertDialog = (activity?.application as StarsEarthApplication)?.createAlertDialog(mContext)
            alertDialog.setTitle("Open camera?")
            alertDialog.setMessage("To declare this complete, you must click a selfie with the items you are delivering and the people you are delivering to. Shall we open the camera?")
            alertDialog.setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()

                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted
                    listener?.requestCameraAccessToConfirmCompletionOfHelpRequest()
                }
                else {
                    cameraPermissionReceived()
                }
            })
            alertDialog.setNegativeButton(android.R.string.no, DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            })
            alertDialog.show()

        }

        btnSubmit?.setOnClickListener {
            val userName = (activity?.application as? StarsEarthApplication)?.mUser?.name
            val volunteerOrganization = (activity?.application as? StarsEarthApplication)?.mUser?.volunteerOrganization
            val name = if (userName.isNullOrEmpty()) {
                etName?.text.toString().toUpperCase()
                }
                else {
                    userName
                }
            if (name.isNullOrBlank()) {
                val alertDialog = (activity?.application as StarsEarthApplication)?.createAlertDialog(mContext)
                alertDialog.setTitle("No name entered")
                alertDialog.setMessage("Please enter your full name")
                alertDialog.setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                    etName?.requestFocus()
                })
                alertDialog.show()
                return@setOnClickListener
            }

            val landmark = etLandmark?.text.toString().toUpperCase(Locale.getDefault())
            val newlyEnteredOrganization = etOrganization?.text.toString().toUpperCase(Locale.getDefault())
            val request = (spinnerRequest?.selectedItem as String).toUpperCase(Locale.getDefault())

            val mDatabase = FirebaseDatabase.getInstance().getReference()
            val key: String? = mDatabase.push().getKey()
            val userId = (activity?.application as? StarsEarthApplication)?.mUser?.uid
            val map = HashMap<String, Any>()
            //map.put("uid", key)
            map.put("userId", userId!!)
            phoneNumber?.let { map.put("phone", it) }
            map.put("name", name)
            mAddressFromPhone?.let { map.put("address", it) }
            map.put("landmark", landmark)
            map.put("volunteer_organization", volunteerOrganization ?: newlyEnteredOrganization)
            map.put("request", request)
            if (request == "DISTRIBUTION") {
                val noOfFamilyMembers = etNumberOfFamilyMembers?.text.toString()
                val aidType = (spinnerAidType?.selectedItem as String).toUpperCase(Locale.getDefault())
                val rationCard = (spinnerRationCard?.selectedItem as String).toUpperCase(Locale.getDefault())
                map.put("no_of_family_members", noOfFamilyMembers)
                map.put("aid_type", aidType)
                map.put("ration_card", rationCard)
            }
            map.put("status", "ACTIVE")
            mGuestPhone?.let { map.put("guest_phone", it) }
            mGuestName?.let { map.put("guest_name", it) }
            map["timestamp"] = ServerValue.TIMESTAMP

            if (key == null) {
                val alertDialog = (activity?.application as StarsEarthApplication)?.createAlertDialog(mContext)
                alertDialog.setTitle(getString(R.string.error))
                alertDialog.setMessage("Could not save. Please try again")
                alertDialog.setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })
                alertDialog.show()
                return@setOnClickListener
            }

            val childUpdates: MutableMap<String, Any> = HashMap()
            childUpdates["help_requests/"+key] = map
            if (userName.isNullOrBlank() && !name.isNullOrBlank()) {
                //********We are not saving this here. We are getting it from the user's volunteer profile
                //User had not set username before. Should save it now for future convinience
                //As per logic above, if username is black, then name is the value from the edittext
                //(activity as? MainActivity)?.mUser?.name = name
                //childUpdates["users/"+userId+"/name"] = name
            }
            if (volunteerOrganization.isNullOrBlank() && !newlyEnteredOrganization.isNullOrBlank()) {
                //*********We are not saving this here right now. We are getting it from the user's volunteer profile*****
                //User has not set their volunteer organization yet. Should save it now for future convinience
                //(activity as? MainActivity)?.mUser?.volunteerOrganization = newlyEnteredOrganization
                //childUpdates["users/"+userId+"/volunteer_organization"] = newlyEnteredOrganization
                //childUpdates["organizations/"+newlyEnteredOrganization+"/exists"] = true
                //childUpdates["organizations/"+newlyEnteredOrganization+"/people/"+userId+"/name"] = name
                //childUpdates["organizations/"+newlyEnteredOrganization+"/people/"+userId+"/phone"] = phoneNumber
            }
            mAddressFromPhone?.let {
                //childUpdates["help_request_locations/"+it.countryName.toUpperCase()+"/"+it.adminArea] = true
            }

            llPleaseWait?.visibility = View.VISIBLE
            mDatabase.updateChildren(childUpdates).addOnSuccessListener {
                llPleaseWait?.visibility = View.GONE
                listener?.onNewHelpRequestMade()
            }.addOnFailureListener {
                val alertDialog = (activity?.application as StarsEarthApplication)?.createAlertDialog(mContext)
                alertDialog.setTitle(getString(R.string.error))
                alertDialog.setMessage("Could not save. Please try again")
                alertDialog.setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })
                alertDialog.show()
            }
        }
        //btnSubmit?.visibility = View.VISIBLE
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mContext = context
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

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
                            mAddressFromPhone = SEAddress(it)
                            val addressLine =   it.getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                            val city = it.locality
                            val state = it.adminArea
                            val country = it.countryName
                            val postalCode = it.postalCode
                            val knownName = it.featureName // Only if available else return NULL
                            val locality = it.locality
                            val subLocality = it.subLocality // This is the area
                            val premesis = it.premises
                            val subAdminArea = it.subAdminArea
                            tvSublocality?.text = addressLine + "\n" + city + "\n" + state + "\n" + country + "\n" + postalCode
                        }
                    }
                }   */

        //val mLocationManager = mContext.getSystemService(LOCATION_SERVICE) as LocationManager
        //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1.0f, mLocationListener);
    }

    fun cameraPermissionReceived() {
        listener?.dispatchTakePictureIntent()
    }

    fun receivedImageBitmap(bitmap: Bitmap) {
        ivPicOfCompletion?.setImageBitmap(bitmap)
        llPicOfCompletion?.visibility = View.VISIBLE
        ivPicOfCompletion?.visibility = View.VISIBLE

        llPleaseWait?.visibility = View.VISIBLE
        val key = FirebaseDatabase.getInstance().getReference("requests").push().getKey();
        val storageReference = FirebaseStorage.getInstance().reference.child("images/requests/"+key+".jpg")
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val data = baos.toByteArray()
        val uploadTask = storageReference.putBytes(data)
        uploadTask.addOnSuccessListener {
            val childUpdates: MutableMap<String, Any> = HashMap()
            childUpdates["requests/" + key + "/status"] = "COMPLETE"
            mRequest?.let { childUpdates["requests/" + key + "/request"] = it }
            mAddressFromPhone?.let { childUpdates["requests/" + key + "/address"] = it }
            childUpdates["requests/" + key + "/userId"] =
                    if ((activity?.application as? StarsEarthApplication)?.mUser != null) {
                        ((activity?.application as? StarsEarthApplication)?.mUser as User).uid
                    }
                    else if (FirebaseAuth.getInstance().currentUser != null) {
                        FirebaseAuth.getInstance().currentUser!!.uid
                    }
                    else {
                        ""
                    }
            childUpdates["requests/" + key + "/phone"] =
                    if ((activity?.application as? StarsEarthApplication)?.mUser != null) {
                        ((activity?.application as? StarsEarthApplication)?.mUser as User).phone
                    }
                    else if (FirebaseAuth.getInstance().currentUser?.phoneNumber != null) {
                        FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
                    }
                    else {
                        ""
                    }
            childUpdates["requests/" + key + "/completed_user_id"] =
                    if ((activity?.application as? StarsEarthApplication)?.mUser != null) {
                        ((activity?.application as? StarsEarthApplication)?.mUser as User).uid
                    }
                    else if (FirebaseAuth.getInstance().currentUser != null) {
                        FirebaseAuth.getInstance().currentUser!!.uid
                    }
                    else {
                        ""
                    }
            childUpdates["requests/" + key + "/completed_user_phone"] =
                    if ((activity?.application as? StarsEarthApplication)?.mUser != null) {
                        ((activity?.application as? StarsEarthApplication)?.mUser as User).phone
                    }
                    else if (FirebaseAuth.getInstance().currentUser?.phoneNumber != null) {
                        FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
                    }
                    else {
                        ""
                    }
            (activity?.application as? StarsEarthApplication)?.mUser?.let {
                it.name?.let { childUpdates["requests/" + key + "/name"] = it }
                it.volunteerOrganization?.let { childUpdates["requests/" + key + "/volunteer_organization"] = it }
                it.name?.let { childUpdates["requests/" + key + "/completed_user_name"] = it }
            }

            childUpdates["requests/" + key + "/pic_complete_url"] = "images/requests/"+key+".jpg"
            childUpdates["requests/" + key + "/timestamp_completion"] = ServerValue.TIMESTAMP
            val mDatabase = FirebaseDatabase.getInstance().getReference()
            mDatabase.updateChildren(childUpdates).addOnSuccessListener {
                llPleaseWait?.visibility = View.GONE
                btnComplete?.visibility = View.GONE
                btnCancel?.visibility = View.GONE
                llLogoTitle?.visibility = View.VISIBLE
                (activity?.application as StarsEarthApplication).getFirebaseRemoteConfigWrapper().volunteerNetworkName?.let {
                    tvVolunteerNetworkLbl?.text = it
                }
                llDeliveryStatus?.visibility = View.VISIBLE //We do not want to exit once the save is complete. We will just show that the delivery successfully completed
                tvDeliveryDate?.visibility = View.VISIBLE
                tvDeliveryDate?.text = (activity as? MainActivity)?.getFormattedDateAndTime(Calendar.getInstance().timeInMillis) //This is just to display now as the real timestamp will only be processed at the server side
                val userPhoneNumber =
                        if ((activity?.application as? StarsEarthApplication)?.mUser != null) {
                            ((activity?.application as? StarsEarthApplication)?.mUser as User).phone
                        }
                        else {
                            FirebaseAuth.getInstance().currentUser?.phoneNumber
                        }
                val name =
                        if ((activity?.application as? StarsEarthApplication)?.mUser != null) {
                            ((activity?.application as? StarsEarthApplication)?.mUser as User).name
                        }
                        else {
                            null
                        }
                var finalText = "Delivered by: " + userPhoneNumber
                if (name != null) {
                    finalText += " - " + name
                }
                tvDeliveredByNameNumber?.visibility = View.VISIBLE
                tvDeliveredByNameNumber?.text = finalText
                svMain?.scrollTo(0,0) //Scroll back to the top
                //listener?.requestCompleted()
            }.addOnFailureListener {
                llPleaseWait?.visibility = View.GONE
                val alertDialog2 = (activity?.application as StarsEarthApplication)?.createAlertDialog(mContext)
                alertDialog2.setTitle("Error")
                alertDialog2.setMessage("Failed to save. Please try again")
                alertDialog2.setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })
                alertDialog2.show()
            }
        }
                .addOnFailureListener {
                    llPleaseWait?.visibility = View.GONE
                    val alertDialog2 = (activity?.application as StarsEarthApplication)?.createAlertDialog(mContext)
                    alertDialog2.setTitle("Error")
                    alertDialog2.setMessage("Failed to save. Please try again")
                    alertDialog2.setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                    })
                    alertDialog2.show()
                }




    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
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
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                locationPermissionReceived()
            } else {
                Toast.makeText(mContext, "Turn on Location in Settings", Toast.LENGTH_LONG).show()
                //val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                //startActivity(intent)
            }
        } else {
            listener?.requestLocationForHelpRequest()
        }
    }

    fun updateOnBehalfOfPersonDetails(phone: String, name: String) {
        mGuestPhone = phone
        mGuestName = name
        tvOnBehalfOf?.text = "ON BEHALF OF: \n" + phone + "\n" + name
        tvOnBehalfOf?.visibility = View.VISIBLE
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onNewHelpRequestMade()
        fun requestLocationForHelpRequest()
        fun requestCompleted()
        fun onBehalfOfFormRequested(hostPhone: String, hostName: String, guestPhone: String?, guestName: String?)
        fun onBehalfOfDetailsEntered(name : String, phone : String)
        fun requestCameraAccessToConfirmCompletionOfHelpRequest()
        fun dispatchTakePictureIntent()
    }

    companion object {

        val TAG = "HELP_REQ_FORM_FRAGMENT"
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CoronaHelpRequestFormFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
                CoronaHelpRequestFormFragment()

        @JvmStatic
        fun newInstance(hostNumber: String, hostName: String, guestPhone: String?, guestName: String?) =
                CoronaHelpRequestFormFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_HOST_NUMBER, hostNumber)
                        putString(ARG_HOST_NAME, hostName)
                        putString(ARG_GUEST_NUMBER, guestPhone)
                        putString(ARG_GUEST_NAME, guestName)
                    }
                }

        @JvmStatic
        fun newInstance(helpRequest: HelpRequest) =
                CoronaHelpRequestFormFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(ARG_HELP_REQUEST, helpRequest)
                    }
                }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
        if (index == 3) {
            val spinnerList = ArrayList<String>()
            spinnerList.add("FOOD PACKET")
            spinnerList.add("RATION KIT")
            spinnerList.add("MEDICINES")

            //Creating the ArrayAdapter instance having the country list
            val aaAidType = ArrayAdapter(mContext,android.R.layout.simple_spinner_item,spinnerList.toArray())
            aaAidType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            //Setting the ArrayAdapter data on the Spinner
            spinnerAidType.setAdapter(aaAidType)
            spinnerAidType.setSelection(0)

            val spinnerListCardType = ArrayList<String>()
            spinnerListCardType.add("APL")
            spinnerListCardType.add("BPL")
            spinnerListCardType.add("ANTHYODAYA")

            //Creating the ArrayAdapter instance having the country list
            val aaRationCard = ArrayAdapter(mContext,android.R.layout.simple_spinner_item,spinnerListCardType.toArray())
            aaRationCard.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            //Setting the ArrayAdapter data on the Spinner
            spinnerRationCard.setAdapter(aaRationCard)
            spinnerRationCard.setSelection(0)

            etNumberOfFamilyMembers?.visibility = View.VISIBLE
            tvAidTypeLbl?.visibility = View.VISIBLE
            spinnerAidType?.visibility = View.VISIBLE
            tvRationCardLbl?.visibility = View.VISIBLE
            spinnerRationCard?.visibility = View.VISIBLE
        }
        else {
            etNumberOfFamilyMembers?.visibility = View.GONE
            tvAidTypeLbl?.visibility = View.GONE
            spinnerAidType?.visibility = View.GONE
            tvRationCardLbl?.visibility = View.GONE
            spinnerRationCard?.visibility = View.GONE
        }
    }
}
