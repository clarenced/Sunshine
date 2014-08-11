package com.example.android.sunshine.app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Adapter;
import android.widget.ArrayAdapter;

import com.example.android.sunshine.app.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * Created by dimitri on 09/08/14.
 */
public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    private Context context;

    public FetchWeatherTask(Context context){
        this.context = context;
    }

    @Override
    protected String[] doInBackground(String... params) {
        Log.i(LOG_TAG, "Launching background task");
        if (params.length == 0) {
            return null;
        }

        String locationQuery = params[0];

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;
        String format = "json";
        String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are available at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String forecastUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";

            final Uri uri = Uri.parse(forecastUrl).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays)).build();


            URL url = new URL(uri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                forecastJsonStr = null;
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
                //forecastJsonStr = null;
                return null;
            }
            forecastJsonStr = buffer.toString();
            try {
                final String[] weatherDataFromJson = getWeatherDataFromJson(forecastJsonStr,
                        numDays,locationQuery);
                return weatherDataFromJson;
            } catch (JSONException e){
                Log.e(LOG_TAG, "Error ", e);
                e.printStackTrace();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
            //forecastJsonStr = null;
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }
        return null;
    }

    /*@Override
    protected void onPostExecute(String[] strings) {
        if (strings != null) {
            adapter.clear();
            for (String s : strings) {
                adapter.add(s);
            }
        }
        adapter.notifyDataSetChanged();
    }*/

    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays, String locationSetting)
            throws JSONException {

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        final String OWM_COORD_LAT = "lat";
        final String OWM_COORD_LONG = "lon";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";


        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DATETIME = "dt";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
        String cityName = cityJson.getString(OWM_CITY_NAME);
        JSONObject coordJSON = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = coordJSON.getLong(OWM_COORD_LAT);
        double cityLongitude = coordJSON.getLong(OWM_COORD_LONG);

        Log.v(LOG_TAG, cityName + ", with coord: " + cityLatitude + " " + cityLongitude);

        // Insert the location into the database.
        long locationID = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

        // Get and insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {

            long dateTime;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;

            double high;
            double low;

            // For now, using the format "Day, description, hi/low"4
            String day;
            String description;
            String highAndLow;
            int weatherId;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);
            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            dateTime = dayForecast.getLong(OWM_DATETIME);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            high = temperatureObject.getDouble(OWM_MAX);
            low = temperatureObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationID);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                    WeatherContract.getDbDateString(new Date(dateTime * 1000L)));
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

            cVVector.add(weatherValues);
            if(cVVector.size() > 0){
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                final ContentValues[] contentValues = cVVector.toArray(cvArray);
                final int rowsInserted = this.context.getContentResolver().
                        bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, contentValues);
                Log.v(LOG_TAG, "inserted " + rowsInserted + " rows of weather data");
            }

            highAndLow = formatHighLows(high, low);
            day = getReadableDateString(dateTime);


            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }
        return resultStrs;

    }


    /* The date/time conversion code is going to be moved outside the asynctask later,
* so for convenience we're breaking it out into its own method now.
*/
    private String getReadableDateString(long time) {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        final SharedPreferences defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this.context);


        final String unitType = defaultSharedPreferences.getString(this.context.getString(R.string.pref_unit_key),
                this.context.getString(R.string.pref_unit_default_value));

        if (unitType.equals(this.context.getString(R.string.pref_unit_imperial))) {
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        } else if (!unitType.equals(this.context.getString(R.string.pref_unit_metric))) {
            Log.d(LOG_TAG, "Unit type not found");
        }

        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    private long addLocation(String locationSettings, String cityName, double lat, double lon){

        long id = 0;
        final Cursor query = this.context.getContentResolver().
                query(WeatherContract.LocationEntry.CONTENT_URI,
                        new String[]{WeatherContract.LocationEntry._ID},
                        WeatherContract.LocationEntry.COLUMN_LOCATION_SETTINGS + " = ?",
                                 new String[]{locationSettings}, null);

        if(query.moveToFirst()){
            final int columnIndex = query.getColumnIndex(WeatherContract.LocationEntry._ID);
            id = query.getLong(columnIndex);
        } else {
            ContentValues cv = new ContentValues();
            cv.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTINGS, locationSettings);
            cv.put(WeatherContract.LocationEntry.COLUMN_CITY, cityName);
            cv.put(WeatherContract.LocationEntry.COLUMN_COORD_LATITUDE, lat);
            cv.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            final Uri insert = context.getContentResolver()
                    .insert(WeatherContract.LocationEntry.CONTENT_URI, cv);
            id = ContentUris.parseId(insert);
        }

        return id;
    }
}
