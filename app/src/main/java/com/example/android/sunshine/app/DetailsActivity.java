package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

public class DetailsActivity extends ActionBarActivity {
    private static final int  DETAIL_LOADER_ID = 1;
    public static final String DATE_KEY = "date";
    public static final String LOCATION_KEY = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.weather_detail_container, new DetailsFragment())
                    .commit();
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.details, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {



        private String forecast;
        private String location;
        private static final String SHARE_TAG = " #sunshine";
        private static final String LOG_TAG = DetailsFragment.class.getSimpleName();
        private ShareActionProvider shareActionProvider;

        private static final String[] FORECAST_COLUMNS = {
                WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
                WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
        };

        public DetailsFragment() {
            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_details, container, false);
            final Intent intent = getActivity().getIntent();
            if(intent != null && intent.hasExtra(Intent.EXTRA_TEXT)){
                forecast = intent.getStringExtra(Intent.EXTRA_TEXT);
                ((TextView)rootView.findViewById(R.id.detail_forecast_textview)).setText(forecast);
                //final TextView textView = (TextView)rootView.findViewById(R.id.forecast_detail);
                //textView.setText(forecast);
            }
            return rootView;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.fragmentdetails, menu);
            MenuItem item = menu.findItem(R.id.share_action);
            shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
            shareActionProvider.setShareIntent(createForShareIntent());

            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            getLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
            if(savedInstanceState != null){
                location = savedInstanceState.getString(LOCATION_KEY);
            }
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();
            if(null != location && !location.equals(Utility.getPreferredLocation(getActivity()))){
                getLoaderManager().restartLoader(DETAIL_LOADER_ID, null, this);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString(LOCATION_KEY, location);
            super.onSaveInstanceState(outState);
        }

        private Intent createForShareIntent() {
            Intent shareIntent  = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.putExtra(Intent.EXTRA_TEXT, forecast + SHARE_TAG);
            return shareIntent;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.v(LOG_TAG, "In onCreateLoader");

            Intent intent = getActivity().getIntent();
            if (intent == null || !intent.hasExtra(DATE_KEY)) {
                return null;
            }
            String forecastDate = intent.getStringExtra(DATE_KEY);

            // Sort order:  Ascending, by date.
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    Utility.getPreferredLocation(getActivity()), forecastDate);
            Log.v(LOG_TAG, weatherForLocationUri.toString());

            location = Utility.getPreferredLocation(getActivity());

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
             Log.v(LOG_TAG, "In onLoadFinished");
            if (!data.moveToFirst()) { return; }

            String dateString = Utility.formatDate(
                    data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATETEXT)));
            String weatherDescription =
                    data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC));

            boolean isMetric = Utility.isMetric(getActivity());
            String high = Utility.formatTemperature(
                    data.getDouble(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)), isMetric);
            String low = Utility.formatTemperature(
                    data.getDouble(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)), isMetric);

            forecast = String.format("%s - %s - %s/%s",
                    dateString, weatherDescription, high, low);

            Log.v(LOG_TAG, "Forecast String: " + forecast);

            ((TextView) getView().findViewById(R.id.detail_low_textview)).setText(low);
            ((TextView) getView().findViewById(R.id.detail_high_textview)).setText(low);
            ((TextView) getView().findViewById(R.id.detail_date_textview)).setText(dateString);
            ((TextView) getView().findViewById(R.id.detail_forecast_textview)).setText(weatherDescription);

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            getLoaderManager().restartLoader(DETAIL_LOADER_ID, null, this);
        }
    }
}
