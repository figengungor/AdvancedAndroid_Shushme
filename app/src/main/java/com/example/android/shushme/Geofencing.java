package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by figengungor on 4/6/2018.
 */

public class Geofencing implements ResultCallback {
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60;
    private static final float GEOFENCE_RADIUS = 50;
    private static final String TAG = Geofencing.class.getSimpleName();
    private GoogleApiClient googleApiClient;
    private Context context;
    private PendingIntent geofencePendingIntent;
    private List<Geofence> geofenceList;

    public Geofencing(GoogleApiClient googleApiClient, Context context) {
        this.googleApiClient = googleApiClient;
        this.context = context;
        geofencePendingIntent = null;
        geofenceList = new ArrayList<>();
    }

    public void registerAllGeofences() {
        if (googleApiClient == null || !googleApiClient.isConnected()
                || geofenceList == null || geofenceList.size() == 0)
            return;
        try {
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()).setResultCallback(this);
        } catch (SecurityException securityException) {
            Log.e(TAG, "registerAllGeofences: " + securityException.getMessage());
        }
    }

    public void unregisterAllGeofences() {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        try {
            LocationServices.GeofencingApi.removeGeofences(
                    googleApiClient,
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException securityException) {
            Log.e(TAG, "unregisterAllGeofences: " + securityException.getMessage());
        }
    }

    public void updateGeofenceList(PlaceBuffer places) {
        geofenceList = new ArrayList<>();
        if (places == null || places.getCount() == 0) return;
        for (Place place : places) {
            //Read the place information from PlaceBuffer
            String placeUID = place.getId();
            double placeLat = place.getLatLng().latitude;
            double placeLng = place.getLatLng().longitude;
            //Build a Geofence object
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeUID)
                    //lifetime of geofence
                    //If geofence is not registered after this duration,
                    //the location services will remove this geofence.
                    //Since we're using the 24-hou lifetime, for a production quality style app
                    //You should set up a JobScheduler and need register geofences everyday
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(placeLat, placeLng, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            // Add it to the list
            geofenceList.add(geofence);
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        //This trigger defines what happens in case the device was already inside any of the geofences
        //INITIAL_TRIGGER_ENTER
        //If the device is already inside a geofence at the time of registering,
        //then trigger an entry transition event immediately
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        //Resuse the PnedingIntent if we already have it
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.e(TAG, "onResult: " + result.getStatus().toString());
    }
}
