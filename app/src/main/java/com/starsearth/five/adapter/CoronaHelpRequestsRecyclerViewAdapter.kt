package com.starsearth.five.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.starsearth.five.R
import com.starsearth.five.activity.MainActivity
import com.starsearth.five.domain.HelpRequest


import com.starsearth.five.fragments.lists.CoronaHelpRequestsFragment.OnListFragmentInteractionListener
import com.starsearth.five.fragments.lists.dummy.DummyContent.DummyItem

import kotlinx.android.synthetic.main.fragment_coronahelprequests.view.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class CoronaHelpRequestsRecyclerViewAdapter(
        private val mContext: Context,
        private val mValues: ArrayList<HelpRequest>,
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<CoronaHelpRequestsRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as HelpRequest
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onCoronaHelpListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_coronahelprequests, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mTimeView.text = (mContext as? MainActivity)?.convertTimeToIST(Date(item.timestampCompletion))
        holder.mNameView.text =
                if (!item.guestName.isNullOrBlank()) {
                    item.guestName
                } else {
                    item.phone
                }
        holder.mHelpNeededView.text = item.request

        if (item.status == "COMPLETE") {
            //holder.mView.setBackgroundColor(Color.parseColor("#008000"))
        }

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    fun addItem(helpRequestItem: HelpRequest) {
        mValues.add(helpRequestItem)
    }

    fun addItems(items: ArrayList<HelpRequest>) {
        mValues.addAll(items)
    }

    fun removeItem(helpRequestItem: HelpRequest) {
        mValues.remove(helpRequestItem)
    }

    fun removeAllItems() {
        mValues.clear()
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mTimeView: TextView = mView.time
        val mNameView: TextView = mView.name
        val mHelpNeededView: TextView = mView.helpNeeded

        override fun toString(): String {
            return super.toString() + " '" + mNameView.text + "'"
        }
    }
}
