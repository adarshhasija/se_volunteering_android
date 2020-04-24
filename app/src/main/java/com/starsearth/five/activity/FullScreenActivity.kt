package com.starsearth.five.activity

import android.net.Uri

import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.starsearth.five.R
import com.starsearth.five.domain.User
import com.starsearth.five.fragments.HighScoreFragment
import com.starsearth.five.fragments.ProfilePicFragment
import com.starsearth.five.fragments.SummaryFragment
import kotlinx.android.synthetic.main.activity_full_screen.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FullScreenActivity : AppCompatActivity(),
        SummaryFragment.OnFragmentInteractionListener,
        ProfilePicFragment.OnFragmentInteractionListener,
        HighScoreFragment.OnFragmentInteractionListener {

    override fun onProfilePicFragmentInteraction() {

    }

    override fun onFragmentInteraction(uri: Uri) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = View.OnTouchListener { _, _ ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_full_screen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mVisible = true

        // Set up the user interaction to manually show or hide the system UI.
        //fullscreen_content.setOnClickListener { toggle() }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        dummy_button.setOnTouchListener(mDelayHideTouchListener)

        val extras = intent.extras
        val mTask = extras?.get(TASK)
        val mResult = extras?.get(RESULT)
        val view_type = extras?.get(VIEW_TYPE)
        if (view_type == VIEW_TYPE_HIGH_SCORE) {
            val fragment = HighScoreFragment.newInstance((mTask as Parcelable), (mResult as Parcelable))
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container_main, fragment).commit()
        }
        else if (view_type == VIEW_TYPE_PROFILE_PIC) {
            extras.getByteArray(IMG_BYTE_ARRAY)?.let {
                val fragment = ProfilePicFragment.newInstance(it)
                supportFragmentManager.beginTransaction()
                        .add(R.id.fragment_container_main, fragment).commit()
            }

        }
        else if (view_type == VIEW_TYPE_DAILY_SUMMARY) {
            val hashMap = extras.getSerializable("MAP") as HashMap<String, Any>
            val fragment = SummaryFragment.newInstance(hashMap)
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container_main, fragment).commit()
          /*  val byteArray = extras.getByteArray(IMG_BYTE_ARRAY)
            val totalCompleted = extras.getInt(TOTAL_COMPLETED)
            val volunteerOrg = extras.getString(VOLUNTEER_ORG)
            val formattedDateTime = extras.getString(FORMATTED_DATE_TIME)
            val user = extras.getParcelable(USER) as User

            if (user != null && formattedDateTime != null && totalCompleted != null) {
                val hashMap = HashMap<String, Any>()
                hashMap.put(SummaryFragment.ARG_USER, user)
                hashMap.put(SummaryFragment.ARG_FORMATTED_DATE_TIME, formattedDateTime)
                hashMap.put(SummaryFragment.ARG_COMPLETED, totalCompleted)
                hashMap.put(SummaryFragment.ARG_VOLUNTEER_ORG, volunteerOrg)
                hashMap.put(SummaryFragment.ARG_BYTE_ARRAY, byteArray)
                val fragment = SummaryFragment.newInstance(hashMap)
                supportFragmentManager.beginTransaction()
                        .add(R.id.fragment_container_main, fragment).commit()
            }   */

        }

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreen_content_controls.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300

        val VIEW_TYPE = "view_type"
        val VIEW_TYPE_HIGH_SCORE = "view_type_high_score"
        val TASK = "task"
        val RESULT = "result"

        val VIEW_TYPE_PROFILE_PIC = "view_type_profile_pic"
        val IMG_BYTE_ARRAY = "img_byte_array"

        val VIEW_TYPE_DAILY_SUMMARY = "view_type_daily_summary"
        val TOTAL_COMPLETED = "total_completed"
        val VOLUNTEER_ORG = "volunteer_org"
        val FORMATTED_DATE_TIME = "formatted_date_time"
        val USER = "user"
    }
}
