package com.starsearth.five.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.storage.FirebaseStorage

import com.starsearth.five.R
import com.starsearth.five.activity.MainActivity
import com.starsearth.five.application.StarsEarthApplication
import com.starsearth.five.domain.Educator
import com.starsearth.five.domain.SETeachingContent
import com.starsearth.five.fragments.lists.DetailListFragment
import kotlinx.android.synthetic.main.fragment_profile_volunteer.*
import java.util.*


// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_LIST_ITEM = "list-item"
private const val ARG_TEACHING_CONTENT = "teaching-content"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ProfileVolunteerFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ProfileVolunteerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileVolunteerFragment : Fragment() {
    private var listItem: DetailListFragment.ListItem? = null
    private var mTeachingContent: SETeachingContent? = null
    private lateinit var mContext : Context
    private var mImgByteArray : ByteArray? = null
    private var listener: OnProfileEducatorFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_LIST_ITEM)) {
                listItem = DetailListFragment.ListItem.fromString(it.getString(ARG_LIST_ITEM)!!)
            }
            if (it.containsKey(ARG_TEACHING_CONTENT)) {
                mTeachingContent = it.getParcelable(ARG_TEACHING_CONTENT)
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_volunteer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvVolunteerNetworkLbl?.text = "SE Volunteer Network"
        updateProfile()
        llProfile?.visibility = View.VISIBLE

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnProfileEducatorFragmentInteractionListener) {
            mContext = context
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnProfileEducatorFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    //This function changes the status text along with a fade out/fade in animation
    private fun changeText(newText : String) {
        tvStatus?.animate()
                ?.alpha(0f)
                ?.setDuration(1000)
                ?.setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        tvStatus?.setText(newText)
                        tvStatus?.animate()
                                ?.alpha(1f)
                                ?.setDuration(1000)
                    }
                })
    }

    private fun updateProfile() {
        (activity?.application as StarsEarthApplication).getFirebaseRemoteConfigWrapper().volunteerNetworkName?.let {
            tvVolunteerNetworkLbl?.text = it
        }

        (activity as? MainActivity)?.mUser?.name?.let {
            var split = it.split("\\s".toRegex(), 0).toMutableList()
            for (i in 0 until split.size) {
                split[i] = split[i].toLowerCase(Locale.getDefault()).capitalize()
            }
            var finalText = "" //Append all the words of the name together
            for (word in split) {
                finalText += " " + word
            }
            tvName?.text = finalText.trim()
        }

        (activity as? MainActivity)?.mUser?.volunteerOrganization?.let {
            var split = it.split("\\s".toRegex(), 0).toMutableList()
            for (i in 0 until split.size) {
                split[i] = split[i].toLowerCase(Locale.getDefault()).capitalize()
            }
            var finalText = "" //Append all the words of the name together
            for (word in split) {
                finalText += " " + word
            }
            tvVolunteerOrg?.text = finalText.trim()
        }

        (activity as? MainActivity)?.mUser?.pic?.let {
            var profilePicRef = FirebaseStorage.getInstance().reference.child(it)

            val ONE_MEGABYTE: Long = 1024 * 1024
            profilePicRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
                mImgByteArray = it
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                ivProfile?.setImageBitmap(bitmap)
            }.addOnFailureListener {
                // Handle any errors
            }
        }
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
    interface OnProfileEducatorFragmentInteractionListener {
        fun onProfileEducatorCTATapped(parentItemSelected : DetailListFragment.ListItem, educator: Educator, teachingContent: SETeachingContent)
        fun onProfileEducatorStatusChanged()
        fun onViewPermissionsBtnTapped(educator: Educator)
        fun onViewContentBtnTapped()
        fun onProfilePicTapped(imgByteArray: ByteArray)
    }

    companion object {
        val TAG = "PROFILE_EDUCATOR_FRAG"
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param listItem If this call came from DetailListFragment, need to know which card was tapped
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileEducatorFragment.
         */

        @JvmStatic
        fun newInstance() =
                ProfileVolunteerFragment()

        @JvmStatic
        fun newInstance(listItem: DetailListFragment.ListItem?, teachingContent: Parcelable) =
                ProfileVolunteerFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_LIST_ITEM, listItem.toString())
                        putParcelable(ARG_TEACHING_CONTENT, teachingContent)
                    }
                }
    }
}
