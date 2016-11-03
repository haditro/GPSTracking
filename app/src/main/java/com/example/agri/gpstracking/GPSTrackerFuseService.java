package com.example.agri.gpstracking;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by msk-1196 on 11/1/16.
 */

public class GPSTrackerFuseService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<LocationSettingsResult>, LocationListener {

    private static final int M_FIX_ACC = 3;
    private Location currenLocation;

    protected GoogleApiClient mGoogleApiClient;
    protected LocationSettingsRequest mLocationSettingsRequest;
    protected LocationRequest mLocationRequest;
    private static final String TAG = "OIW";
    private boolean GPSEnabled = false;
    private boolean isGPlayConnected = false;
    private boolean isTrackRequested = false;
    private State currentState;

    private List<Location> locations;
    private List<LatLng> latLngs;

    enum State {
        START,
        STOP
    }

    public void setGPSEnabled(boolean GPSEnabled) {
        this.GPSEnabled = GPSEnabled;

        if (GPSEnabled && isTrackRequested) {
            startTracking();
        }
    }

    public boolean isStop() {
        return currentState == State.STOP;
    }

    public class GPSBinder extends Binder {
        GPSTrackerFuseService getService() {
            return GPSTrackerFuseService.this;
        }
    }

    private final IBinder mBinder = new GPSBinder();


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e("TAG", "BINDER");
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
        Log.e("CREATE", "OK");
        locations = new ArrayList<>();
        latLngs = new ArrayList<>();
        currentState = State.STOP;
        if (mGoogleApiClient == null) {
            buildGoogleApiClient();
        }
        createLocationRequest();
        mGoogleApiClient.connect();
    }

    public List<Location> getLocations() {
        return locations;
    }

    public List<LatLng> getLatLngs() {
        return latLngs;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(6000);
        mLocationRequest.setFastestInterval(3000);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    protected void checkLocationSettings() {
        Log.e("HI", "CHECK");
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e("API", "CONNECTED");
        isGPlayConnected = true;
        buildLocationSettingsRequest();
        checkLocationSettings();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("API", "SUSPENDED");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("API", "FAILED");
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {

        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.e(TAG, "All location settings are satisfied.");
                GPSEnabled = true;
                if (isTrackRequested)
                    startTracking();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.e(TAG, "Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings ");

                LocalBroadcastManager.getInstance(this).sendBroadcast(
                        new Intent(MainActivity.GPSCHECK).putExtra("status", status));
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.e(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                break;
        }
    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
        isGPlayConnected = false;
        isTrackRequested = false;
        super.onDestroy();
    }

    public void toggle() {
        isTrackRequested = true;
        if (isGPlayConnected && GPSEnabled) {
            if (currentState == State.STOP) {
                startTracking();
            } else {
                stopTracking();
            }
        } else if (isGPlayConnected) {
            mGoogleApiClient.connect();
        } else {
            checkLocationSettings();
        }
    }

    private void stopTracking() {
        currentState = State.STOP;
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
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
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, location.toString(), Toast.LENGTH_SHORT).show();
        if (location.getAccuracy() <= M_FIX_ACC){
            long diffTime = currenLocation != null?
                    (location.getTime() - currenLocation.getTime()) : 0;

            if (diffTime >= 0 && diffTime <= 6000) {
                locations.add(location);
                latLngs.add(new LatLng(location.getLatitude(), location.getLongitude()));
                currenLocation = location;
            }
        }
    }
}
