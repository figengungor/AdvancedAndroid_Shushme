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
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
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
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.example.android.shushme.provider.PlaceContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.List;

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
    private static final int PLACE_PICKER_REQUEST = 1;

    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Switch geofencesSwitch;
    private CheckBox locationPermissionCheckbox;
    GoogleApiClient googleApiClient;
    Geofencing geofencing;
    boolean isGeofenceEnabled;

    /**
     * Called when the activity is starting
     *
     * @param savedInstanceState The Bundle that contains the data supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationPermissionCheckbox = findViewById(R.id.location_permission_checkbox);

        // Set up the recycler view
        mRecyclerView = findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this, null);
        mRecyclerView.setAdapter(mAdapter);

        // Initialize switch state
        geofencesSwitch = findViewById(R.id.enable_geofences_switch);
        isGeofenceEnabled = getPreferences(MODE_PRIVATE).getBoolean(getString(R.string.geofence_setting_enabled_key), false);
        geofencesSwitch.setChecked(isGeofenceEnabled);
        geofencesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                editor.putBoolean(getString(R.string.geofence_setting_enabled_key), isChecked);
                isGeofenceEnabled = isChecked;
                editor.commit();
                if (isChecked) geofencing.registerAllGeofences();
                else geofencing.unregisterAllGeofences();
            }
        });

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

        geofencing = new Geofencing(googleApiClient, this);
    }

    public void onAddPlaceButtonClicked(View view) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.location_permission_not_enabled_message), Toast.LENGTH_SHORT).show();
            return;
        }

        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        Intent i = null;
        try {
            i = builder.build(this);
        } catch (GooglePlayServicesRepairableException e) {
            Log.e(TAG, String.format("Google Play Services Not Available [%s]", e.getMessage()));
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, String.format("Google Play Services Not Available [%s]", e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, String.format("PlacePicker Exception [%s]", e.getMessage()));
        }
        startActivityForResult(i, PLACE_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);
            if (place == null) {
                Log.i(TAG, "onActivityResult: No Place Selected");
                return;
            }

            //Extract the place information from the API
            String placeName = place.getName().toString();
            String placeAddress = place.getAddress().toString();
            String placeID = place.getId();

            //Insert a new place into DB
            ContentValues contentValues = new ContentValues();
            contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeID);
            getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, contentValues);

            refreshPlacesData();
        }
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
        refreshPlacesData();
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

    public void refreshPlacesData() {
        Cursor data = getContentResolver().query(
                PlaceContract.PlaceEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        if (data == null || data.getCount() == 0) return;

        List<String> guids = new ArrayList<String>();
        while (data.moveToNext()) {
            guids.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
        }

        //PendingResult
        /*Represents a pending result from calling an API method in Google Play services.
          The final result object from a PendingResult is of type R,
          which can be retrieved in one of two ways.
          -via blocking calls to await(), or await(long, TimeUnit), or
          -via a callback by passing in an object implementing interface
          ResultCallback to setResultCallback(ResultCallback).

          https://developers.google.com/android/reference/com/google/android/gms/common/api/PendingResult

          PlaceBuffer is a Places API object that acts as a list of places*/

        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(googleApiClient,
                guids.toArray(new String[guids.size()]));
        //To retrieve the server's response you'll need to set a callback for this pending result
        placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                mAdapter.swapPlaces(places);
                geofencing.updateGeofenceList(places);
                if (isGeofenceEnabled) geofencing.registerAllGeofences();
            }
        });

    }

    public void openGooglePrivacyPolicyLink(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/privacy"));
        startActivity(intent);
    }
}
