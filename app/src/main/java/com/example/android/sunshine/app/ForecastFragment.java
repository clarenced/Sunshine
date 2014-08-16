package com.example.android.sunshine.app;

/**
 * Created by formation on 01/08/14.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.sunshine.app.data.WeatherContract;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private static int FORECAST_LOADER = 0;
    private ForecastAdapter mForecastAdapter;

    private String mLocation;

    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTINGS,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;


    private int mPosition = -1;
    private boolean mUseTodayLayout;

    private static final String CURRENT_POSITION = "CURRENT_POSITION";
    private ListView listView;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        listView = (ListView) rootView.findViewById(R.id.listview_forecast);

        // The SimpleCursorAdapter will take data from the database through the
        // Loader and use it to populate the ListView it's attached to.
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        //mForecastAdapter.setUseOfToday(mUseOfToday);
        if(savedInstanceState != null && savedInstanceState.containsKey(CURRENT_POSITION)){
            mPosition = savedInstanceState.getInt(CURRENT_POSITION);
        }

       /* mForecastAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                boolean isMetric = Utility.isMetric(getActivity());
                switch (columnIndex){
                    case COL_WEATHER_MAX_TEMP:
                    case COL_WEATHER_MIN_TEMP:
                        ((TextView)view).setText(Utility.formatTemperature(cursor.getDouble(columnIndex), isMetric));
                        return true;
                    case COL_WEATHER_DATE: {
                        String dateString = cursor.getString(columnIndex);
                        TextView dateView = (TextView) view;
                        dateView.setText(Utility.formatDate(dateString));
                        return true;
                    }
                }
                return false;
            }
        });*/


        listView.setAdapter(mForecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Adapter adapter = parent.getAdapter();
                final ForecastAdapter cursorAdapter = (ForecastAdapter) adapter;
                final Cursor cursor = cursorAdapter.getCursor();

                if(null != cursor && cursor.moveToPosition(position)){
                    /*Intent intent = new Intent(getActivity(), DetailsActivity.class);
                    intent.putExtra(DetailsFragment.DATE_KEY, cursor.getString(COL_WEATHER_DATE));
                    //intent.putExtra(Intent.EXTRA_TEXT, forecast);
                    startActivity(intent);*/
                    mPosition = position;
                    ((Callback)getActivity()).onItemSelected(cursor.getString(COL_WEATHER_DATE));
                }
            }
        });
        return rootView;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mPosition != ListView.INVALID_POSITION){
            outState.putInt(CURRENT_POSITION, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //Retrieving the default location from the preference.
        SharedPreferences defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = defaultSharedPreferences.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default_value));

        //Build the Uri
        Uri uri = Uri.parse("geo:0,0").
                buildUpon().appendQueryParameter("q", location).build();

        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        } else if(id == R.id.show_preferred_location){

            //Start an intent
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            if(intent.resolveActivity(getActivity().getPackageManager()) != null){
                startActivity(intent);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private void updateWeather(){
        final FetchWeatherTask fetchWeatherTask = new FetchWeatherTask(getActivity());
        final SharedPreferences defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = defaultSharedPreferences.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default_value));
        fetchWeatherTask.execute(location);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // To only show current and future dates, get the String representation for today,
        // and filter the query to return weather only for dates after or including today.
        // Only return data after today.
        String startDate = WeatherContract.getDbDateString(new Date());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

        final SharedPreferences defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        mLocation =
                defaultSharedPreferences.getString(getString(R.string.pref_location_key),
                        getString(R.string.pref_location_default_value));

        //mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                mLocation, startDate);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        mForecastAdapter.swapCursor(data);
        if(mPosition != ListView.INVALID_POSITION){
            listView.smoothScrollToPosition(mPosition);
        }

    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null) {
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }


    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String date);
    }
}