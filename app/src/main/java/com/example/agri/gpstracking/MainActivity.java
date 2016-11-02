package com.example.agri.gpstracking;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String REQUEST_PERMISSION = "stop";
    SupportMapFragment map;
    GoogleMap googleMap;
    final String MAP_FRAGMENT_TAG = "map";
    boolean mBound = false;
    GPSTrackerService gpsTracker;

    public static final String GPSCHECK = "gps_check";
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    protected static final int REQUEST_CHECK_PERMISSION = 0x2;

    private BroadcastReceiver gpsCheck = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Status status = intent.getParcelableExtra("status");
            try {
                status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
            } catch (IntentSender.SendIntentException e) {
                Log.e("eo", "PendingIntent unable to execute request.");
            }
        }
    };

    private BroadcastReceiver stopReq = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("Tstop", "selesai");
            if (gpsTracker.getLatLngs().size() >= 2) {
                googleMap.clear();
                PolygonOptions options = new PolygonOptions()
                        .addAll(gpsTracker.getLatLngs())
                        .strokeColor(Color.RED)
                        .fillColor(0x7F0000FF)
                        .strokeWidth(1);

                googleMap.addPolygon(options);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        map = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.map, map, MAP_FRAGMENT_TAG)
                .commit();
        if (map != null) map.getMapAsync(this);
        final Button button = (Button) findViewById(R.id.toggle);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gpsTracker.isStop()){
                    button.setText("Stop");
                } else {
                    button.setText("Start");
                }
                gpsTracker.toggle();
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(gpsCheck, new IntentFilter(GPSCHECK));
        LocalBroadcastManager.getInstance(this).registerReceiver(stopReq, new IntentFilter(REQUEST_PERMISSION));

    }

    private void bindGPSTracker() {
        getApplicationContext().bindService(new Intent(this, GPSTrackerService.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onMapReady(GoogleMap map) {

        googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(0.6500, 120.6833), 3);
        googleMap.animateCamera(cameraUpdate);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            bindGPSTracker();
            if (googleMap != null && !googleMap.isMyLocationEnabled())
                googleMap.setMyLocationEnabled(true);
        } else {
            if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.M)
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CHECK_PERMISSION);
        }
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsCheck);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stopReq);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("ioi", "User agreed to make required location settings changes.");
                        gpsTracker.setGPSEnabled(true);
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("ioi", "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CHECK_PERMISSION){
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                bindGPSTracker();
            }
        }
    }
}
