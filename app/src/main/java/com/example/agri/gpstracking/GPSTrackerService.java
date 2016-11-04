package com.example.agri.gpstracking;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by msk-1196 on 11/3/16.
 */

public class GPSTrackerService extends Service implements LocationListener {

    private final IBinder mBinder = new GPSBinder();
    private LocationManager locationManager;
    private State currentState;

    private List<Location> locations;
    private List<LatLng> latLngs;
    private Location currentLocation;

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, location.toString(), Toast.LENGTH_SHORT).show();
//        if (location.getAccuracy() <= M_FIX_ACC){
            long diffTime = currentLocation != null?
                    (location.getTime() - currentLocation.getTime()) : 0;

            if (diffTime >= 0 && diffTime <= 6000) {
                locations.add(location);
                latLngs.add(new LatLng(location.getLatitude(), location.getLongitude()));
                currentLocation = location;
            }
//        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public boolean isStop() {
        return currentState == State.STOP;
    }

    enum State {
        START,
        STOP
    }

    public class GPSBinder extends Binder {
        GPSTrackerService getService() {
            return GPSTrackerService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("TAG", "START_COMMAND");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        latLngs = new ArrayList<>();
        locations = new ArrayList<>();
    }

    private boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void toggle() {
        if (currentState == State.STOP) {
            startTracking();
        } else {
            stopTracking();
        }
    }

    private void stopTracking() {
        currentState = State.STOP;
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
            locationManager.removeUpdates(this);
        LocalBroadcastManager.getInstance(this).sendBroadcast(
                new Intent(MainActivity.REQUEST_PERMISSION));
    }

    private void startTracking() {
        locations.clear();
        latLngs.clear();

        currentState = State.START;
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    public List<LatLng> getLatLngs() {
        return latLngs;
    }
}
