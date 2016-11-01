package com.example.agri.gpstracking;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.LocationListener;

/**
 * Created by msk-1196 on 11/1/16.
 */

public class GPSTrackerService extends Service  {

    public class GPSBinder extends Binder{
        GPSTrackerService getService(){
            return GPSTrackerService.this;
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
}
