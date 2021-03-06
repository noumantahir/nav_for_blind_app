package org.hawkdev.apps.navigationforblind;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.maps.model.AddressComponent;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

import java.util.List;

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

    private ProgressBar progressBar;

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Updated Location: " + location.toString());
            tvLocation.setText(location.toString() + "-----------" + data_id++);

            if(!isGPSFix){
                mLastLocation = location;
                mLastLocationMillis = SystemClock.elapsedRealtime();
            }

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

            mLastLocation = location;
            mLastLocationMillis = SystemClock.elapsedRealtime();
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
    private ShakeListener mShakeListener;


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
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        //initialize location fields
        requestLocationUpdates();
        setLastKnownLocation();

        //initilize narrator
        mSpeechProcessor = new SpeechProcessor(this);
        mSpeechProcessor.setOnTtsInitListener(new SpeechProcessor.OnTtsInitListener() {
            @Override
            public void onInit() {
                //remove progress bar
                progressBar.setVisibility(View.INVISIBLE);

                //narrate note
                mSpeechProcessor.narrateText(getString(R.string.welcome));
                mSpeechProcessor.narrateText(getString(R.string.instructions));
                mSpeechProcessor.narrateText(getString(R.string.post_instructions));
            }
        });

        //init shake listener
        mShakeListener = new ShakeListener(this);
        mShakeListener.setOnShakeListener(new ShakeListener.OnShakeListener() {
            @Override
            public void onShake() {
                speakLocation();
            }
        });

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        mSpeechProcessor.narrateText(getString(R.string.welcome));
        mSpeechProcessor.narrateText(getString(R.string.instructions));
    }

    /**
     * callback for location permission granted or rejected
     *
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

        //check if location accessis granted
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
        if(fastLocation != null) {
            Log.d(TAG, fastLocation.toString());
            tvLocation.setText(fastLocation.toString() + "-----------" + data_id++);

            //set last known location
            if(!isGPSFix)
                mLastLocation = fastLocation;
        }

        //set last known fine location
        Location fastFineLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(fastFineLocation != null) {
            Log.d(TAG, fastFineLocation.toString());
            tvFineLocation.setText(fastFineLocation.toString() + "-----------" + data_id_fine++);
            //set last known location
            if(isGPSFix)
                mLastLocation = fastFineLocation;
        }

        mLastLocationMillis = SystemClock.elapsedRealtime();

    }

    /**
     * GPS status change listener
     *
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

    public void speakLocation(View view) {
        speakLocation();

//        double lati = mLastLocation.getLatitude();
//        double longi = mLastLocation.getLongitude();
//
//        String text = "Your Location is " + lati + " Latitude and " + longi + " longitude";
//
//        mSpeechProcessor.narrateText(text);

    }


    //32.086401, 72.661327 - City Road Sargodha
    //32.077125, 72.674166 - Club Road Sargodha
    //32.088151, 72.661553 - Girl College Rd
    //32.088545, 72.659975 - 16 Block
    public void speakLocation() {
        mSpeechProcessor.narrateText("Getting Location");

        AsyncTask speakLocationTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {

                LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                String addressString = Utility.reverseGeocode(MainActivity.this , latLng);

                if(addressString != null)
                    mSpeechProcessor.narrateText("You are at " + addressString);
                else
                    mSpeechProcessor.narrateText("Could not get location");

                return null;

//                try {
//                    GeocodingResult[] geoCodeResponse = Utility.reverseGeocode(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
//
//                    if (geoCodeResponse.length > 0) {
//                        String location = geoCodeResponse[0].formattedAddress;
//
//
//                        GeocodingResult bestMatch = geoCodeResponse[0];
//                        AddressComponent addressNumber = bestMatch.addressComponents[0];
//                        AddressComponent addressStreet = bestMatch.addressComponents[1];
//
//                        mSpeechProcessor.narrateText("You are at "
//                                + addressNumber.longName + " " + addressStreet.longName);
//
//                    }
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
        };

        speakLocationTask.execute();

    }

    public void repeatInstructions(View view){
        mSpeechProcessor.narrateText(getString(R.string.instructions));
    }

    public void openProject(View view){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.collaborizm.com/project/ByP-RLfF"));
        startActivity(browserIntent);
    }

    /**
     * send location to rest end point
     * example:
     *
     * @param view
     */
    public void sendLocation(View view) {

        //TODO refer to speakLocation method for now

//        AsyncTask sendLocationTask = new AsyncTask() {
//            @Override
//            protected Object doInBackground(Object[] params) {
//
//                try {
//                    GeocodingResult[] geoCodeResponse = Utility.reverseGeocode(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
//
//                    if (geoCodeResponse.length > 0) {
//                        GeocodingResult bestMatch = geoCodeResponse[0];
//                        AddressComponent addressNumber = bestMatch.addressComponents[0];
//                        AddressComponent addressStreet = bestMatch.addressComponents[1];
//
//                        mSpeechProcessor.narrateText(addressNumber.longName + " " + addressStreet.longName);
//
//                    }

/*                    String lati = mLastLocation.getLatitude() + "";

                    String longi = mLastLocation.getLongitude() + "";

                    Uri uri = Uri.parse(getString(R.string.local_server));

                    uri.buildUpon()
                            .appendQueryParameter(getString(R.string.query_latitude), lati)
                            .appendQueryParameter(getString(R.string.query_longitude), longi)
                            .build();

                    JSONObject responseJSON = Utility.urlGetRequest(uri);


                    if (responseJSON != null) {
                        //TODO update location view

                        try {
                            String textToSpeak = responseJSON.getString("say");
                            mSpeechProcessor.narrateText(textToSpeak);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else
                        mSpeechProcessor.narrateText("Server did not respond");

                    return null;
                    */

//                } catch (Exception e) {
//                    e.printStackTrace();
//
//                }
//                return null;
//            }
//        };
//
//        sendLocationTask.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mShakeListener.resume();
        if(mLocationManager != null) {
            mLocationManager.addGpsStatusListener(this);
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        mShakeListener.pause();
        if(mLocationManager != null) {
            mLocationManager.removeGpsStatusListener(this);
        }
    }


}
