package org.hawkdev.apps.navigationforblind;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GpsStatus.Listener {

    private static final float LOCATION_REFRESH_DISTANCE = 1.0f;
    private static final long LOCATION_REFRESH_TIME = 2000;
    private static final int PERMISSIONS_REQUEST_LOCATION = 111;
    private static final String TAG = "MAIN_ACTIVITY";

    private int data_id = 0;
    private int data_id_fine = 0;

    private LocationManager mLocationManager;

    private TextView tvLocation;
    private TextView tvFineLocation;
    private TextView tvGpsStatus;

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Updated Location: " + location.toString());
            tvLocation.setText(location.toString() + "-----------" + data_id++);

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private long mLastLocationMillis;
    private final LocationListener mFineLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Fine Updated Location: " + location.toString());
            tvFineLocation.setText(location.toString() + "-----------" + data_id_fine++);

            mLastLocationMillis = SystemClock.elapsedRealtime();
            mLastLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };


    private boolean isGPSFix = false;
    private Location mLastLocation;
    private SpeechProcessor mSpeechProcessor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //initilize view fields
        tvLocation = (TextView) findViewById(R.id.tv_location);
        tvFineLocation = (TextView) findViewById(R.id.tv_fine_location);
        tvGpsStatus = (TextView) findViewById(R.id.tv_gps_status);

        //initialize location fields
        requestLocationUpdates();
        setLastKnownLocation();

        //initilize narrator
        mSpeechProcessor = new SpeechProcessor(this);
    }


    /**
     * callback for location permission granted or rejected
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    requestLocationUpdates();
            }
        }
    }

    /**
     * register callbacks for location updates
     */
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //request write external storage permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
                return;
            }

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //register network location update listener
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_REFRESH_TIME,
                LOCATION_REFRESH_DISTANCE, mLocationListener);

        //register gps location update listener
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                LOCATION_REFRESH_DISTANCE, mFineLocationListener);

        //add gps status change listener
        mLocationManager.addGpsStatusListener(this);

        Log.d(TAG, "requested location updates");
    }

    /**
     * set location fields to last known locations
     */
    private void setLastKnownLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //request write external storage permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
            return;
        }

        //set last known coarse location
        Location fastLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Log.d(TAG, fastLocation.toString());
        tvLocation.setText(fastLocation.toString() + "-----------" + data_id++);

        //set last known fine location
        Location fastFineLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Log.d(TAG, fastFineLocation.toString());
        tvFineLocation.setText(fastFineLocation.toString() + "-----------" + data_id_fine++);


        mLastLocation = fastFineLocation;
        mLastLocationMillis = SystemClock.elapsedRealtime();
    }

    /**
     * GPS status change listener
     * @param event
     */
    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                if (mLastLocation != null)
                    isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 3000;

                if (isGPSFix) {
                    //sattelite is fix
                    tvGpsStatus.setText("gps fixed");

                } else { // The fix has been lost.
                    // Do something.
                    tvGpsStatus.setText("gps lost");
                }

                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                // Do something.
                isGPSFix = true;

                break;
        }
    }

    public void speakLocation(View view){
        double lati = mLastLocation.getLatitude();
        double longi = mLastLocation.getLongitude();

        String text = "Your Location is " + lati + " Latitube and " + longi + " longitude";

        mSpeechProcessor.narrateText(text);

    }



}
