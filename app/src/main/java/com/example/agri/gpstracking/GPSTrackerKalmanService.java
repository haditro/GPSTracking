package com.example.agri.gpstracking;


import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
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
 * Created by msk-1252 on 11/8/16.
 */

public class GPSTrackerKalmanService extends Service implements LocationListener, GpsStatus.NmeaListener {

    private final IBinder mBinder = new GPSBinder();
    private LocationManager locationManager;
    private State currentState;

    private List<Location> locations;
    private List<LatLng> kalmans;
    private List<LatLng> latLngs;
    private Location currentLocation;
    private final int MIN_ACCURACY = 2;
    private float velocity;
    private long latestTimestamp;
    private double lat;
    private double lng;
    private float variance;

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, location.toString(), Toast.LENGTH_SHORT).show();
//        if (location.getAccuracy() <= M_FIX_ACC){
        long diffTime = currentLocation != null ?
                (location.getTime() - currentLocation.getTime()) : 0;
        if (Double.compare(lat, 0.0) == 0 && Double.compare(lng, 0.0) == 0) {
            lat = location.getLatitude();
            lng = location.getLongitude();
        }
        process(location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                location.getTime());
        kalmans.add(new LatLng(lat, lng));
        if (diffTime >= 0 && diffTime <= 6000) {
            locations.add(location);
            latLngs.add(new LatLng(location.getLatitude(), location.getLongitude()));
            currentLocation = location;
        }
//        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.d("MO", "MO");
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d("MO", "enable");
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.d("MO", "provider disable");
    }

    public boolean isStop() {
        return currentState == State.STOP;
    }

    @Override
    public void onNmeaReceived(long l, String s) {
        Log.d("MO", s + " " + l);
    }

    enum State {
        START,
        STOP
    }

    public class GPSBinder extends Binder {
        GPSTrackerKalmanService getService() {
            return GPSTrackerKalmanService.this;
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
        currentState = State.STOP;
        latLngs = new ArrayList<>();
        locations = new ArrayList<>();
        kalmans = new ArrayList<>();
        variance = 4;
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
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeNmeaListener(this);
            locationManager.removeUpdates(this);
        }
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
            locationManager.addNmeaListener(this);
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    public List<LatLng> getLatLngs() {
        return latLngs;
    }

    public void process(double lat_measurement, double lng_measurement, float accuracy, long currentTimestamp) {
        if (accuracy < MIN_ACCURACY) accuracy = MIN_ACCURACY;
        long timeDiff = currentTimestamp - this.latestTimestamp;
        if (timeDiff > 0) {
            // time has moved on, so the uncertainty in the current position increases
            variance += timeDiff * velocity * velocity / 1000;
            this.latestTimestamp = currentTimestamp;
            // TO DO: USE VELOCITY INFORMATION HERE TO GET A BETTER ESTIMATE OF CURRENT POSITION
        }

        // Kalman gain matrix K = Covarariance * Inverse(Covariance + MeasurementVariance)
        // NB: because K is dimensionless, it doesn't matter that variance has different units to lat and lng
        float K = variance / (variance + accuracy * accuracy);
        // apply K
        lat += K * (lat_measurement - lat);
        lng += K * (lng_measurement - lng);
        // new Covarariance  matrix is (IdentityMatrix - K) * Covarariance
        variance = (1 - K) * variance;
    }
}
