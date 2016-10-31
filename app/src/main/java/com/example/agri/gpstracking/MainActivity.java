package com.example.agri.gpstracking;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    SupportMapFragment map;
    GoogleMap googleMap;
    final String MAP_FRAGMENT_TAG = "map";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        map = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.map, map, MAP_FRAGMENT_TAG)
                .commit();
        if (map != null) map.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        /* only for testing */
//        googleMap.setOnMapClickListener(this);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        //googleMap.setOnMarkerDragListener(this);
        //CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(0.6500, 120.6833), 3);
        //googleMap.animateCamera(cameraUpdate);
    }
}
