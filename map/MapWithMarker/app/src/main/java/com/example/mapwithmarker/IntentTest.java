package com.example.mapwithmarker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;

public class IntentTest extends AppCompatActivity {
    private static final String TAG = IntentTest.class.getSimpleName();

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;
    private boolean mAddressRequested;
    private ProgressBar mProgressBar;
    private AddressResultReceiver mResultReceiver;

    /** Called when the activity is first created. */
    EditText nameEditCtrl;
    EditText descriptionEditCtrl;
    EditText timeEditCtrl;
    EditText zipEditCtrl;
    EditText addEditCtrl;
    Button btnCtlr;
    Button locBtnCtlr;
    String name;
    String description;
    String time;
    Double lat;
    Double lng;
    String zipcode = null;
    String address = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        nameEditCtrl = (EditText) findViewById(R.id.editText1);
        descriptionEditCtrl = (EditText) findViewById(R.id.editText2);
        timeEditCtrl = (EditText) findViewById(R.id.editText3);
        zipEditCtrl = (EditText) findViewById(R.id.editText5);
        addEditCtrl = (EditText) findViewById(R.id.editText6);

        zipEditCtrl.setKeyListener(null);
        addEditCtrl.setKeyListener(null);

        btnCtlr = (Button) findViewById(R.id.button1);
        btnCtlr.setOnClickListener(new ButtonClickHandler());
        locBtnCtlr = (Button) findViewById(R.id.button2);
        locBtnCtlr.setOnClickListener(new LocationButtonHandler());

        mLastLocation = new Location("");
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mResultReceiver = new AddressResultReceiver(new Handler());

        mAddressRequested = false;
        updateUIWidgets();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        lat = Double.parseDouble(intent.getStringExtra(LocationMonitoringService.EXTRA_LATITUDE));
                        lng = Double.parseDouble(intent.getStringExtra(LocationMonitoringService.EXTRA_LONGITUDE));
                    }
                }, new IntentFilter(LocationMonitoringService.ACTION_LOCATION_BROADCAST)
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkGooglePlayServices()) {
            checkInternet();
        } else {
            showToast("No google play service");
        }
    }

    public boolean checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(this, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    private void checkInternet() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            showToast("No internet connection");
            return;
        }

        if (checkPermissions()) {
            Intent intent = new Intent(this, LocationMonitoringService.class);
            startService(intent);
        } else {
            requestPermissions();
        }
    }

    /**
     * Go to detail page.
     */
    public class ButtonClickHandler implements View.OnClickListener {
        public void onClick(View view) {
            if (nameEditCtrl != null && nameEditCtrl.getText().length() != 0) {
                name = nameEditCtrl.getText().toString();
            } else {
                showSnackbar("No event name");
                return;
            }
            if (descriptionEditCtrl != null && descriptionEditCtrl.getText().length() != 0) {
                description = descriptionEditCtrl.getText().toString();
            } else {
                showSnackbar("No event description");
                return;
            }
            if (timeEditCtrl != null && timeEditCtrl.getText().length() != 0) {
                time = timeEditCtrl.getText().toString();
            } else {
                showSnackbar("No event time");
                return;
            }
            Intent intObj = new Intent(IntentTest.this,
                    MapsMarkerActivity.class);
            intObj.putExtra("NAME", name);
            intObj.putExtra("DES", description);
            intObj.putExtra("TIME", time);
            intObj.putExtra("LAT", lat);
            intObj.putExtra("LNG", lng);
            intObj.putExtra("ZIPCODE", zipcode);
            intObj.putExtra("ADDRESS", address);
            startActivity(intObj);
        }
    }

    /**
     * Set location.
     */
    public class LocationButtonHandler implements View.OnClickListener {
        public void onClick(View view) {
            stopService(new Intent(IntentTest.this, LocationMonitoringService.class));
            if (lat != null && lng != null) {
                Log.i(TAG, String.format("%f, %f", lat, lng));
                mLastLocation.setLatitude(lat);
                mLastLocation.setLongitude(lng);
                startIntentService();
                mAddressRequested = true;
                updateUIWidgets();
            } else {
                showToast("No location");
            }
            checkInternet();
        }
    }

    private void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    private void showSnackbar(final String text) {
        View container = findViewById(R.id.main_activity_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(IntentTest.this,
                new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION);

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            Log.i(TAG, "Requesting permission");
            startLocationPermissionRequest();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(this, LocationMonitoringService.class);
                startService(intent);
            } else {
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    private void displayAddressOutput() {
        if(address == null) {
            showSnackbar("Invalid location.");
        } else {
            addEditCtrl.setText(address);
        }
        if(zipcode == null) {
            showSnackbar("Invalid location.");
        } else {
            zipEditCtrl.setText(zipcode);
        }
        mAddressRequested = false;
        updateUIWidgets();
        stopService(new Intent(this, FetchAddressIntentService.class));
    }

    private void updateUIWidgets() {
        if (mAddressRequested) {
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
            locBtnCtlr.setEnabled(false);
        } else {
            mProgressBar.setVisibility(ProgressBar.GONE);
            locBtnCtlr.setEnabled(true);
        }
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
                address = resultData.getString(Constants.RESULT_ADDRESS);
                zipcode = resultData.getString(Constants.RESULT_ZIPCODE);
                Log.i("Address", address);
                Log.i("Zipcode", zipcode);
                displayAddressOutput();
            } else {
                showToast("Address not found");
            }
            mAddressRequested = false;
            updateUIWidgets();
        }
    }

    public void onDestroy() {
        stopService(new Intent(this, LocationMonitoringService.class));
        super.onDestroy();
    }
}