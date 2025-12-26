package com.example.spacedodger

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        loadAllMarkers()
    }

    private fun loadAllMarkers() {
        if (context == null || googleMap == null) return

        val prefs = requireContext().getSharedPreferences("SpaceGamePrefs", Context.MODE_PRIVATE)
        val history = prefs.getStringSet("history", setOf()) ?: setOf()

        for (entry in history) {
            val parts = entry.split("|")
            if (parts.size >= 4) {
                val name = parts[0]
                val score = parts[1]
                val lat = parts[2].toDoubleOrNull() ?: 0.0
                val lng = parts[3].toDoubleOrNull() ?: 0.0

                if (lat != 0.0) {
                    val location = LatLng(lat, lng)
                    googleMap!!.addMarker(MarkerOptions().position(location).title("$name: $score"))
                }
            }
        }

        // Default zoom to Israel
        val israel = LatLng(31.0461, 34.8516)
        googleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(israel, 6f))
    }

    fun focusOnLocation(lat: Double, lng: Double) {
        if (googleMap != null && lat != 0.0) {
            val loc = LatLng(lat, lng)
            googleMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))
        }
    }
}