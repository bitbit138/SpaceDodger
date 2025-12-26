package com.example.spacedodger

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ScoreActivity : AppCompatActivity() {

    private lateinit var listFragment: ScoreListFragment
    private lateinit var mapFragment: MapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        // 1. Init Fragments
        listFragment = ScoreListFragment()
        mapFragment = MapFragment()

        // 2. Set Callback (List -> Map communication)
        listFragment.setCallBack(object : ScoreListFragment.CallBack_Top {
            override fun zoomToLocation(lat: Double, lng: Double) {
                mapFragment.focusOnLocation(lat, lng)
            }
        })

        // 3. Begin Transaction
        supportFragmentManager.beginTransaction()
            .add(R.id.frame_list, listFragment)
            .add(R.id.frame_map, mapFragment)
            .commit()
    }
}