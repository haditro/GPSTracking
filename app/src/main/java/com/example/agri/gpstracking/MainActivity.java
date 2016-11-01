package com.example.agri.gpstracking;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    SupportMapFragment map;
    GoogleMap googleMap;
    final String MAP_FRAGMENT_TAG = "map";
    boolean mBound = false;
    GPSTrackerService gpsTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        map = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.map, map, MAP_FRAGMENT_TAG)
                .commit();
        if (map != null) map.getMapAsync(this);
        
        //bindGPSTracker();
    }

    private void bindGPSTracker() {
        getApplicationContext().bindService(new Intent(this, GPSTrackerService.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        //googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        /* only for testing */
        //googleMap.getUiSettings().setZoomControlsEnabled(true);
        //CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(0.6500, 120.6833), 3);
        //googleMap.animateCamera(cameraUpdate);
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            gpsTracker = ((GPSTrackerService.GPSBinder) iBinder).getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
            gpsTracker = null;
        }
    };

    @Override
    protected void onDestroy() {
        if (mBound){
            getApplicationContext().unbindService(mConnection);
        }
        super.onDestroy();
    }
}
