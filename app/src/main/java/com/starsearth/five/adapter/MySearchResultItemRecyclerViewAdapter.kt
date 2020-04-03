package com.starsearth.five.adapter

import android.content.Context
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.starsearth.five.R
import com.starsearth.five.domain.TagListItem
import com.starsearth.five.domain.User


import com.starsearth.five.fragments.SearchResultItemFragment.OnListFragmentInteractionListener
import com.starsearth.five.fragments.dummy.DummyContent.DummyItem

import kotlinx.android.synthetic.main.fragment_searchresultitem.view.*
import java.util.*

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 *
 */
class MySearchResultItemRecyclerViewAdapter(
        private val mContext: Context,
        private val mValues: List<Parcelable>,
        private val mResultType: String?,
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<MySearchResultItemRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as Parcelable
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onSearchResultListFragmentInteraction(item, mResultType)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_searchresultitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        if (item is User) {
            holder.mTitleView.visibility = View.VISIBLE
            holder.mTitleView.text = item.name
        }
        else if (item is TagListItem) {
            holder.mTitleView.visibility = View.VISIBLE
            holder.mTitleView.text = item.name.toLowerCase(Locale.getDefault()).capitalize()
        }

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mImgProfilePic: ImageView = mView.ivProfilePic
        val mTitleView: TextView = mView.tvTitle

        override fun toString(): String {
            return super.toString() + " '" + mTitleView.text + "'"
        }
    }
}
