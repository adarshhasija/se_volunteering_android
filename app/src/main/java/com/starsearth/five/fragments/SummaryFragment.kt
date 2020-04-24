package com.starsearth.five.fragments

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import com.starsearth.five.R
import com.starsearth.five.application.StarsEarthApplication
import com.starsearth.five.domain.User
import kotlinx.android.synthetic.main.fragment_summary.*


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SummaryFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SummaryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SummaryFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var mUser: User
    private var mCompleted: Int? = null
    private var mVolunteerOrg: String? = null
    private lateinit var mFormattedDateTime: String
    private var mByteArray: ByteArray? = null
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mUser = it.getParcelable(ARG_USER) as User
            mCompleted = it.getInt(ARG_COMPLETED)
            mVolunteerOrg = it.getString(ARG_VOLUNTEER_ORG)
            mFormattedDateTime = it.getString(ARG_FORMATTED_DATE_TIME) as String
            mByteArray = it.getByteArray(ARG_BYTE_ARRAY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity?.application as StarsEarthApplication).getFirebaseRemoteConfigWrapper().volunteerNetworkName?.let {
            tvAppName?.text = it
        }
        tvDateTimeCurrent?.text = mFormattedDateTime
        if (mVolunteerOrg != null) {
            //This should be from the volunteer org POV
            tvVolunteerOrg?.text = mVolunteerOrg
            tvVolunteerOrg?.visibility = View.VISIBLE
        }
        else {
            //This should be from the individual's POV
            tvName?.text = mUser.name
            tvName?.visibility = View.VISIBLE
        }

        mByteArray?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            ivPic?.setImageBitmap(bitmap)
        }
        tvProcessed?.text = mCompleted.toString()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
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
        //fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        val TAG = "SUMMARY_FRAGMENT"
        const val ARG_USER = "user"
        const val ARG_COMPLETED = "completed"
        const val ARG_VOLUNTEER_ORG = "volunteer_org"
        const val ARG_FORMATTED_DATE_TIME = "formatted_date_time"
        const val ARG_BYTE_ARRAY = "byte_array"
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SummaryFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(map: HashMap<String, Any>) =
                SummaryFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(ARG_USER, map.get(ARG_USER) as Parcelable)
                        putString(ARG_FORMATTED_DATE_TIME, map.get(ARG_FORMATTED_DATE_TIME) as String)
                        putInt(ARG_COMPLETED, map.get(ARG_COMPLETED) as Int)
                        putString(ARG_VOLUNTEER_ORG, map.get(ARG_VOLUNTEER_ORG) as? String)
                        putByteArray(ARG_BYTE_ARRAY, map.get(ARG_BYTE_ARRAY) as? ByteArray)
                    }
                }
    }
}
