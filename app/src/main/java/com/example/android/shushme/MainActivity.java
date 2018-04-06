package com.example.android.shushme;

/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

// Create a Project and Get a key to use Google APIs
// Enable Google Places API for Android from console
// https://console.developers.google.com/flows/enableapi?apiid=placesandroid&reusekey=true

// Google Play Services APIs => https://developers.google.com/android/guides/setup
// Places API => https://developers.google.com/places/android-api/reference
// Location API => https://developer.android.com/reference/android/location/Location.html
// Accessing Google APIs with GoogleApiClient (deprecated)
// https://developers.google.com/android/guides/google-api-client
// New way to use APIs => https://developers.google.com/android/guides/api-client
// https://developer.android.com/training/location/receive-location-updates.html

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Constants
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Switch enableGeofencesSwitch;
    private CheckBox locationPermissionCheckbox;
    GoogleApiClient googleApiClient;

    /**
     * Called when the activity is starting
     *
     * @param savedInstanceState The Bundle that contains the data supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enableGeofencesSwitch = findViewById(R.id.enable_geofences_switch);
        locationPermissionCheckbox = findViewById(R.id.location_permission_checkbox);

        // Set up the recycler view
        mRecyclerView = findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        googleApiClient = new GoogleApiClient.Builder(this)
                //callbacks that will notify when a connection is successful
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                //add the API libraries that you're planning to use
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                //the client will connect/disconnect on its own
                //otherwise you have to call connect and disconnect explicitly
                .enableAutoManage(this, this)
                .build();



    }

    public void onAddPlaceButtonClicked(View view) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.location_permission_not_enabled_message), Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, getString(R.string.location_permission_enabled_message), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermissionCheckbox.setChecked(false);
        } else {
            locationPermissionCheckbox.setChecked(true);
            locationPermissionCheckbox.setEnabled(false);
        }
    }

    public void onLocationPermissionClicked(View view) {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    //Once the client automatically connects to Google Play Services,
    //onConnected method will fire, that is if the connection was successful
    @Override
    public void onConnected(@Nullable Bundle connectionHint) {
        Log.i(TAG, "onConnected: API Client Connection Successful!");
    }

    //If for any reason it wasn’t, onConnectionFailed will fire instead.
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "onConnectionSuspended: API Client Connection Suspended!");

    }

    //onConnectionSuspended as the name suggests fires when the client’s AutoManager
    //decided to suspend the connection.
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: API Client Connection Failed!");
    }
}
