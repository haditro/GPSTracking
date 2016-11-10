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

import com.example.agri.gpstracking.lib.KalmanLocationManager;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by msk-1252 on 11/8/16.
 */

public class GPSTrackerKalmanService extends Service implements LocationListener, GpsStatus.NmeaListener {

    private static final long FILTER_TIME = 200;
    private static final long GPS_TIME = 1000;
    private static final long NET_TIME = 5000;
    private final IBinder mBinder = new GPSBinder();
    private LocationManager locationManager;
    private State currentState;

    private List<Location> locations;
    private List<LatLng> kalmans;
    private List<LatLng> latLngs;
    private Location currentLocation;
    private KalmanLocationManager mKalmanLocationManager;
    private final int MIN_NOISE = 1;
    private float velocity;
    private long latestTimestamp;
    private double kalmanLatitude;
    private double kalmanLongitude;
    private float variance;

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, location.toString(), Toast.LENGTH_SHORT).show();
//        if (location.getAccuracy() <= M_FIX_ACC){
        long diffTime = currentLocation != null ?
                (location.getTime() - currentLocation.getTime()) : 0;
        if (Double.compare(kalmanLatitude, 0.0) == 0 && Double.compare(kalmanLongitude, 0.0) == 0) {
            kalmanLatitude = location.getLatitude();
            kalmanLongitude = location.getLongitude();
        }
        process(location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                location.getTime());
        kalmans.add(new LatLng(kalmanLatitude, kalmanLongitude));
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
        // TODO preprocess NMEA to create better location
        Location location = processNMEA(s);
        Log.d("MO", s + " " + l);
    }

    private Location processNMEA(String s) {
        return null;
    }

    public List<LatLng> getKalmans() {
        return kalmans;
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
        mKalmanLocationManager = new KalmanLocationManager(this);
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

//            mKalmanLocationManager.requestLocationUpdates(
//                    KalmanLocationManager.UseProvider.GPS_AND_NET, FILTER_TIME, GPS_TIME, NET_TIME, this, true);
            locationManager.addNmeaListener(this);
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    public List<LatLng> getLatLngs() {
        return latLngs;
    }

    public void process(double currentLat, double currentLng, float noise, long currentTimestamp) {
        long timeDiff = currentTimestamp - this.latestTimestamp;
        if (timeDiff > 0) {
            variance += timeDiff * velocity * velocity / 1000;
            this.latestTimestamp = currentTimestamp;
            // TO DO: USE VELOCITY INFORMATION HERE TO GET A BETTER ESTIMATE OF CURRENT POSITION
        }
        float K = variance / (variance + noise * noise);
        kalmanLatitude += K * (currentLat - kalmanLatitude);
        kalmanLongitude += K * (currentLng - kalmanLongitude);
        variance = (1 - K) * variance;
    }
}
