package com.example.android.shushme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by figengungor on 4/6/2018.
 */

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    /*Handles the Broadcast message sent when the Geofence Transition is triggered
    Careful here though, this is running on the main thread so make sure you start an AsyncTask for
    anything that takes longer than say 10 second to run*/

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive called");
    }
}
