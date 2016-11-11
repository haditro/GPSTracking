package com.example.agri.gpstracking;


import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.agri.gpstracking.lib.KalmanLocationManager;
import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by msk-1252 on 11/8/16.
 */

public class GPSTrackerKalmanService extends Service implements LocationListener,
        GpsStatus.Listener {

    private static final long FILTER_TIME = 200;
    private static final long GPS_TIME = 1000;
    private static final long NET_TIME = 5000;
    private static final int MIN_SATELLITE = 2;
    private final IBinder mBinder = new GPSBinder();
    private LocationManager locationManager;
    private State currentState;

    private List<Location> locations;
    private List<LatLng> kalmans;
    private List<LatLng> latLngs;
    private List<LatLng> kalmanManagers;
    private Location currentLocation;
    private Location currentKalmanManagerLocation;
    private KalmanLocationManager mKalmanLocationManager;
    private final int MIN_NOISE = 1;
    private float velocity;
    private long latestTimestamp;
    private double kalmanLatitude;
    private double kalmanLongitude;
    private float variance;
    private StringBuilder builder;
    private int knownSatellite;
    private int usedInLastSatellite;

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, location.toString(), Toast.LENGTH_SHORT).show();
//        if (location.getAccuracy() <= M_FIX_ACC){
        if (knownSatellite >= MIN_SATELLITE) {
            long diffTime = currentLocation != null ?
                    (location.getTime() - currentLocation.getTime()) : 0;
            if (Double.compare(kalmanLatitude, 0.0) == 0 &&
                     Double.compare(kalmanLongitude, 0.0) == 0) {
                kalmanLatitude = location.getLatitude();
                kalmanLongitude = location.getLongitude();
                velocity = location.getSpeed();
            }
            process(location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getSpeed(),
                    location.getTime());
            kalmans.add(new LatLng(kalmanLatitude, kalmanLongitude));
            if (diffTime >= 0 && diffTime <= 6000) {
                locations.add(location);
                latLngs.add(new LatLng(location.getLatitude(), location.getLongitude()));
                currentLocation = location;
            }
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

//    @Override
//    public void onNmeaReceived(long l, String s) {
//        // TODO preprocess NMEA to create better location
//        if (s.contains("GSV")) {
//            Location location = processNMEA(s);
//        }
//        builder.append(s).append(" ").append(l).append("\n");
//        Log.d("MO", s + " " + l);
//    }

    private Location processNMEA(String s) {
        return null;
    }

    public List<LatLng> getKalmans() {
        return kalmans;
    }

    public List<LatLng> getKalmanManagers() { return kalmanManagers; }

    @Override
    public void onGpsStatusChanged(int i) {
        GpsStatus gpsStatus = locationManager.getGpsStatus(null);
        if(gpsStatus != null) {
            Iterable<GpsSatellite>satellites = gpsStatus.getSatellites();
            Iterator<GpsSatellite> sat = satellites.iterator();
            String lSatellites = null;
            int known = 0;
            int used = 0;
            while (sat.hasNext()) {
                GpsSatellite satellite = sat.next();
                known++;
                if (satellite.usedInFix())
                    used++;
            }
            knownSatellite = known;
            usedInLastSatellite = used;
        }
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
        kalmanManagers = new ArrayList<>();
        builder =  new StringBuilder();
        variance = 1;
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
        writeToFile(builder.toString());

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
//            locationManager.removeNmeaListener(this);
            locationManager.removeUpdates(this);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(
                new Intent(MainActivity.REQUEST_PERMISSION));
    }

    private void writeToFile(String s) {
        File logFile = new File(Environment.getExternalStorageDirectory(), "log.txt");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                buf.append(s);
                buf.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void startTracking() {
        locations.clear();
        latLngs.clear();

        currentState = State.START;
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mKalmanLocationManager.requestLocationUpdates(
                    KalmanLocationManager.UseProvider.GPS_AND_NET, FILTER_TIME, GPS_TIME, NET_TIME,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            if (knownSatellite >= MIN_SATELLITE) {
                                long diffTime = currentKalmanManagerLocation != null ?
                                        (location.getTime() - currentKalmanManagerLocation.getTime()) : 0;
//                                if (Double.compare(kalmanLatitude, 0.0) == 0 &&
//                                        Double.compare(kalmanLongitude, 0.0) == 0) {
//                                    kalmanLatitude = location.getLatitude();
//                                    kalmanLongitude = location.getLongitude();
//                                    velocity = location.getSpeed();
//                                }
//                                process(location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getSpeed(),
//                                        location.getTime());
//                                kalmans.add(new LatLng(kalmanLatitude, kalmanLongitude));
                                if (diffTime >= 0 && diffTime <= 6000) {
                                    locations.add(location);
                                    kalmanManagers.add(new LatLng(location.getLatitude(), location.getLongitude()));
                                    currentKalmanManagerLocation = location;
                                }
                            }
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
                    }, true);
            locationManager.addGpsStatusListener(this);
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    public List<LatLng> getLatLngs() {
        return latLngs;
    }

    public void process(double currentLat, double currentLng, float noise, float currentV, long currentTimestamp) {
        long timeDiff = currentTimestamp - this.latestTimestamp;
        if (timeDiff > 0) {
            variance += timeDiff * velocity * velocity / 1000;
            this.latestTimestamp = currentTimestamp;
            // TO DO: USE VELOCITY INFORMATION HERE TO GET A BETTER ESTIMATE OF CURRENT POSITION
        }
        float K = variance / (variance + noise * noise);
        kalmanLatitude += K * (currentLat - kalmanLatitude);
        kalmanLongitude += K * (currentLng - kalmanLongitude);
        velocity += K * (currentV - velocity);
        variance = (1 - K) * variance;
    }
}
