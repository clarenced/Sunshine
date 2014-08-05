package com.example.android.sunshine.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherDbHelper;

/**
 * Created by formation on 05/08/14.
 */
public class TestDbOld extends AndroidTestCase {

    public static final String LOG_TAG = TestDbOld.class.getSimpleName();


    public void testCreateDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        db.close();
    }

    public void testInsertReadDb(){
        //Test data
        String testName = "North Pole";
        String testLocationSetting = "99705";
        double testLatitude = 64.772;
        double testLongitude = -147.355;

        WeatherDbHelper weatherDbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = weatherDbHelper.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(WeatherContract.LocationEntry.COLUMN_CITY, testName);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTINGS, testLocationSetting);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LATITUDE, testLatitude);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, testLongitude);

        long locationRowId = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, contentValues);
        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New row id:" + locationRowId);

        String[] columns = {WeatherContract.LocationEntry._ID,
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTINGS,
                WeatherContract.LocationEntry.COLUMN_CITY,
                WeatherContract.LocationEntry.COLUMN_COORD_LATITUDE,
                WeatherContract.LocationEntry.COLUMN_COORD_LONG
        };

        Cursor cursor = db.query(WeatherContract.LocationEntry.TABLE_NAME,
                columns, null, null, null,null,null);

        if(cursor.moveToFirst()){

            int locationIndex = cursor.getColumnIndex(WeatherContract.
                    LocationEntry.COLUMN_LOCATION_SETTINGS);
            String location = cursor.getString(locationIndex);

            int nameIndex = cursor.getColumnIndex(WeatherContract.
                    LocationEntry.COLUMN_CITY);
            String cityName = cursor.getString(nameIndex);

            int latitudeIndex = cursor.getColumnIndex(WeatherContract.
                    LocationEntry.COLUMN_COORD_LATITUDE);
            double latitude = cursor.getDouble(latitudeIndex);

            int longitudeIndex = cursor.getColumnIndex(WeatherContract.
                    LocationEntry.COLUMN_COORD_LONG);
            double longitude = cursor.getDouble(longitudeIndex);

            assertEquals(testName, cityName);
            assertEquals(testLocationSetting, location);
            assertEquals(testLatitude, latitude);
            assertEquals(testLongitude, longitude);

        } else {

            fail("No values returned");
        }

        cursor.close();

        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationRowId);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATETEXT, "20141205");
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, 1.1);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, 1.2);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, 1.3);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, 75);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, 65);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, 5.5);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, 321);

        long weatherRowId = db.insert(WeatherContract.WeatherEntry.TABLE_NAME,null,weatherValues);
        assertTrue(weatherRowId != -1);

        String[] weatherColumns = { WeatherContract.WeatherEntry.COLUMN_LOC_KEY,
                WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                WeatherContract.WeatherEntry.COLUMN_DEGREES,
                WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
                WeatherContract.WeatherEntry.COLUMN_PRESSURE,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
                WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
        };

        Cursor weatherCursor = db.query(WeatherContract.WeatherEntry.TABLE_NAME,
                weatherColumns,
                null, null, null, null, null);

        if(weatherCursor.moveToFirst()){

        }

        if (!weatherCursor.moveToFirst()) {
            fail("No weather data returned!");
        }

        assertEquals(weatherCursor.getInt(
                weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_LOC_KEY)), locationRowId);
        assertEquals(weatherCursor.getString(
                weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATETEXT)), "20141205");
        assertEquals(weatherCursor.getDouble(
                weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DEGREES)), 1.1);
        assertEquals(weatherCursor.getDouble(
                weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_HUMIDITY)), 1.2);
        assertEquals(weatherCursor.getDouble(
                weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_PRESSURE)), 1.3);
        assertEquals(weatherCursor.getInt(
                weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)), 75);
        assertEquals(weatherCursor.getInt(
                weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)), 65);
        assertEquals(weatherCursor.getString(
                weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC)), "Asteroids");
        assertEquals(weatherCursor.getDouble(
                weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED)), 5.5);
        assertEquals(weatherCursor.getInt(
                weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID)), 321);

        weatherCursor.close();
        weatherDbHelper.close();
        db.close();
    }
}
