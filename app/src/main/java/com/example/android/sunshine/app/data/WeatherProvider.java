package com.example.android.sunshine.app.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
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
    private static final SQLiteQueryBuilder sqliteQueryBuilder;

    static {
        sqliteQueryBuilder = new SQLiteQueryBuilder();
        sqliteQueryBuilder.setTables(WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                WeatherContract.LocationEntry.TABLE_NAME + " ON " + WeatherContract.WeatherEntry.TABLE_NAME  +
                "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " = " + WeatherContract.LocationEntry.TABLE_NAME +
                "."+ WeatherContract.LocationEntry._ID);
    }

    private final String sLocationSettingSelection =
            WeatherContract.LocationEntry.TABLE_NAME + "." +
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTINGS + "= ?";

    private final String sLocationSettingWithStartDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME + "." +
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTINGS + "= ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATETEXT + ">= ?";

    private static final String sLocationSettingAndDaySelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTINGS + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATETEXT + " = ? ";

    private Cursor getWeatherByLocationSettingAndDate(
            Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String date = WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sqliteQueryBuilder.query(weatherDbHelper.getReadableDatabase(),
                projection,
                sLocationSettingAndDaySelection,
                new String[]{locationSetting, date},
                null,
                null,
                sortOrder
        );
    }


    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == null) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selectionArgs = new String[]{locationSetting, startDate};
            selection = sLocationSettingWithStartDateSelection;
        }

        return sqliteQueryBuilder.query(weatherDbHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }
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
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
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
            case WEATHER_WITH_LOCATION :
                cursor = getWeatherByLocationSetting(uri, projection, sortOrder); break;
            case WEATHER_WITH_LOCATION_AND_DATE :
                cursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder); break;

            // "location/*"
            case LOCATION_ID: {
                cursor = database.query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        WeatherContract.LocationEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                        null,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "location"
            case LOCATION: {
                cursor = database.query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
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
        final int match = uriMatcher.match(uri);
        Uri returnUri = null;

        switch(match){
            case WEATHER :
                long id = weatherDbHelper.getWritableDatabase().insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);
                if(id > 0){
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case LOCATION :
                long idLocation = weatherDbHelper.getWritableDatabase()
                        .insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);

                if(idLocation > 0){
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(idLocation);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
        }

        getContext().getContentResolver().notifyChange(returnUri, null);

        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        final int match = uriMatcher.match(uri);
        int numberOfRows = 0;

        switch(match){
            case WEATHER :
                numberOfRows = weatherDbHelper.getWritableDatabase().delete(WeatherContract.WeatherEntry.TABLE_NAME,selection,selectionArgs);
                break;
            case LOCATION :
                numberOfRows = weatherDbHelper.getWritableDatabase()
                        .delete(WeatherContract.LocationEntry.TABLE_NAME, selection,selectionArgs);

        }

        if(null == selection || numberOfRows != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return numberOfRows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = uriMatcher.match(uri);
        int numberOfRows = 0;

        switch(match){
            case WEATHER :
                numberOfRows = weatherDbHelper.getWritableDatabase()
                        .update(WeatherContract.WeatherEntry.TABLE_NAME,
                                values, selection,selectionArgs);
                break;
            case LOCATION :
                numberOfRows = weatherDbHelper.getWritableDatabase()
                        .update(WeatherContract.LocationEntry.TABLE_NAME,
                                values,selection,selectionArgs);

        }

        if(numberOfRows != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return numberOfRows;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = weatherDbHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);
        switch (match) {
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }


}
