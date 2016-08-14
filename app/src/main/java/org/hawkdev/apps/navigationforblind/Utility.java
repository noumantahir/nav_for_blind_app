package org.hawkdev.apps.navigationforblind;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.maps.*;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

/**
 * Created by nomo on 8/12/16.
 */
public class Utility {
    private static final String TAG = "UTILITY";
    private static final GeoApiContext GEOCODE_CONTEXT = new GeoApiContext().setApiKey("AIzaSyBAKY4lG_NDhSbAgV1ibLGeJvRpO6ZuW6I");


    public static boolean haveLocationAccess(Context context) {
        return (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                &&
                (ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }


    public static GeocodingResult[] reverseGeocode(LatLng location) throws Exception {
        return GeocodingApi.reverseGeocode(GEOCODE_CONTEXT, location).await();
    }

    public static JSONObject urlGetRequest(Uri uri) {
        Log.d(TAG, "Starting post request for url: " + uri);

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String response = null;
        JSONObject responseJSON = null;


        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast

            URL url = new URL(uri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestMethod("GET");
//            urlConnection.setDoInput(true);
//            urlConnection.setDoOutput(true);
            urlConnection.connect();

//            // Send POST output.
//            printout = new DataOutputStream(urlConnection.getOutputStream ());
//            printout.write(URLEncoder.encode(postJson.toString(),"UTF-8"));
//            printout.flush ();
//            printout.close ();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
//                setLocationStatus(getContext(), LOCATION_STATUS_SERVER_DOWN);
                Log.w(TAG, "No Data Received, Server Down");
                return null;
            }

            response = buffer.toString();
            responseJSON = new JSONObject(response);

        } catch (IOException e) {
            Log.e(TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
//            setLocationStatus(getContext(), LOCATION_STATUS_SERVER_DOWN);
            Log.w(TAG, "Server Down");

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            e.printStackTrace();
//            setLocationStatus(getContext(), LOCATION_STATUS_SERVER_INVALID);
            Log.w(TAG, "Invalid Json Data");
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
        }
        return responseJSON;
    }
}
