package com.example.spacedodger

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment

class ScoreListFragment : Fragment() {

    interface CallBack_Top {
        fun zoomToLocation(lat: Double, lng: Double)
    }

    private var callBack: CallBack_Top? = null

    fun setCallBack(callBack: CallBack_Top) {
        this.callBack = callBack
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val scrollView = ScrollView(context)
        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(16, 16, 16, 16) // Padding for container
        scrollView.addView(linearLayout)

        val prefs = requireContext().getSharedPreferences("SpaceGamePrefs", Context.MODE_PRIVATE)
        val history = prefs.getStringSet("history", setOf()) ?: setOf()

        data class S(val n: String, val s: Int, val lat: Double, val lng: Double)

        val list = history.map {
            val p = it.split("|")
            if (p.size >= 4) S(p[0], p[1].toInt(), p[2].toDouble(), p[3].toDouble())
            else S("Unknown", 0, 0.0, 0.0)
        }.sortedByDescending { it.s }.take(10)

        // Generate stylized rows
        list.forEachIndexed { index, score ->
            // Inflate the custom XML layout for each item
            val itemView = inflater.inflate(R.layout.item_score, linearLayout, false)

            val tvRank = itemView.findViewById<TextView>(R.id.tv_rank)
            val tvName = itemView.findViewById<TextView>(R.id.tv_name)
            val tvScore = itemView.findViewById<TextView>(R.id.tv_score)

            tvRank.text = "${index + 1}."
            tvName.text = score.n
            tvScore.text = "Score: ${score.s}"

            itemView.setOnClickListener {
                callBack?.zoomToLocation(score.lat, score.lng)
            }

            linearLayout.addView(itemView)
        }

        return scrollView
    }
}