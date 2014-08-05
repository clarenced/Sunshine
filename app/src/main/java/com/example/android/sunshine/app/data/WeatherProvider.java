package com.example.android.sunshine.app.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Created by dimitri on 06/08/14.
 */
public class WeatherProvider extends ContentProvider {

    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCATION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    private static final int LOCATION = 300;
    private static final int LOCATION_ID = 301;


    private static UriMatcher uriMatcher = buildUriMatcher();

    private WeatherDbHelper weatherDbHelper;

    private static UriMatcher buildUriMatcher(){
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String weatherUri = WeatherContract.WEATHER_PATH;
        final String locationUri = WeatherContract.LOCATION_PATH;

        final String authority = WeatherContract.CONTENT_AUTHORITY;

        //Uri matcher for weather
        uriMatcher.addURI(authority,weatherUri, WEATHER);
        uriMatcher.addURI(authority,weatherUri + "/*", WEATHER_WITH_LOCATION);
        uriMatcher.addURI(authority,weatherUri + "/*/*", WEATHER_WITH_LOCATION_AND_DATE);

        //uri matcher for locations.
        uriMatcher.addURI(authority, locationUri, LOCATION);
        uriMatcher.addURI(authority, locationUri + "/#", LOCATION_ID);
        return uriMatcher;
    }


    @Override
    public boolean onCreate() {
        weatherDbHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase database = weatherDbHelper.getReadableDatabase();
        final int match = uriMatcher.match(uri);
        Cursor cursor = null;
        switch (match){
            case WEATHER : cursor = database
                    .query(WeatherContract.WeatherEntry.TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder); break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = uriMatcher.match(uri);
        switch (match){
            case WEATHER_WITH_LOCATION : return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER : return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER_WITH_LOCATION_AND_DATE : return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case LOCATION : return WeatherContract.LocationEntry.CONTENT_TYPE;
            case LOCATION_ID : return WeatherContract.LocationEntry.CONTENT_ITEM_TYPE;
            default: throw  new UnsupportedOperationException("Unknown uri : " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


}
