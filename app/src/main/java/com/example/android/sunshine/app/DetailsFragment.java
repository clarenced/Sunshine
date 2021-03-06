package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

import org.w3c.dom.Text;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int  DETAIL_LOADER_ID = 1;
    public static final String DATE_KEY = "date";
    public static final String LOCATION_KEY = "location";


    private String forecast;
    private String location;
    private String mDateStr;
    private static final String SHARE_TAG = " #sunshine";
    private static final String LOG_TAG = DetailsFragment.class.getSimpleName();
    private ShareActionProvider shareActionProvider;


    private TextView detail_day_textview;
    private TextView detail_date_textview;
    private TextView detail_high_textview;
    private TextView detail_low_textview;
    private TextView detail_forecast_textview;
    private TextView detail_humidity;
    private TextView detail_wind;
    private TextView detail_pressure;


    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };


    public DetailsFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_details, container, false);
        detail_day_textview = (TextView) rootView.findViewById(R.id.detail_day_textview);
        detail_date_textview = (TextView) rootView.findViewById(R.id.detail_date_textview);
        detail_high_textview = (TextView) rootView.findViewById(R.id.detail_high_textview);
        detail_low_textview = (TextView) rootView.findViewById(R.id.detail_low_textview);
        detail_forecast_textview = (TextView)rootView.findViewById(R.id.detail_forecast_textview);
        detail_humidity = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        detail_wind = (TextView) rootView.findViewById(R.id.detail_wind_textview);
        detail_pressure = (TextView) rootView.findViewById(R.id.detail_pressure_textview);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mDateStr = arguments.getString(DATE_KEY);
        }

        if (savedInstanceState != null) {
            location = savedInstanceState.getString(LOCATION_KEY);
        }

        final Intent intent = getActivity().getIntent();
        if(intent != null && intent.hasExtra(Intent.EXTRA_TEXT)){
            forecast = intent.getStringExtra(Intent.EXTRA_TEXT);
            ((TextView)rootView.findViewById(R.id.detail_forecast_textview)).setText(forecast);
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
        super.onActivityCreated(savedInstanceState);

        if(savedInstanceState != null){
            location = savedInstanceState.getString(LOCATION_KEY);
        }

        final Bundle arguments = getArguments();
        if(arguments != null && arguments.containsKey(DATE_KEY)){
            getLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        final Bundle arguments = getArguments();
        if(arguments != null && arguments.containsKey(DATE_KEY)
                && null != location && !location.equals(Utility.getPreferredLocation(getActivity()))){
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

        final String date = getArguments().getString(DATE_KEY);

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";
        location = Utility.getPreferredLocation(getActivity());

        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                location, date);
        Log.v(LOG_TAG, weatherForLocationUri.toString());



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

        final int weatherId =
                data.getInt(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
        int weatherDrawable = Utility.getArtResourceForWeatherCondition(weatherId);

        String dateString =
                data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATETEXT));
        String friendlyDateString = Utility.getDayName(getActivity(), dateString);
        String dateText = Utility.getFormattedMonthDay(getActivity(),dateString);
        detail_day_textview.setText(friendlyDateString);
        detail_date_textview.setText(dateText);


        String weatherDescription =
                data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC));
        detail_forecast_textview.setText(weatherDescription);

        boolean isMetric = Utility.isMetric(getActivity());

        String high = Utility.formatTemperature(getActivity(),
                data.getDouble(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)),
                isMetric);
        detail_high_textview.setText(high);


        String low = Utility.formatTemperature(getActivity(),
                data.getDouble(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)),
                isMetric);
        detail_low_textview.setText(low);

        final String pressure =
                data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_PRESSURE));
        detail_pressure.setText(pressure);

        final float humidity =
                data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_HUMIDITY));

        final String humidityString = getActivity()
                .getString(R.string.format_humidity, humidity);
        detail_humidity.setText(humidityString);

        float windSpeedStr =
                data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED));
        float windDirStr =
                data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DEGREES));

        final String formattedWind = Utility.getFormattedWind(getActivity(), windSpeedStr, windDirStr);
        detail_wind.setText(formattedWind);

        forecast = String.format("%s - %s - %s/%s",
                dateString, weatherDescription, high, low);

        Log.v(LOG_TAG, "Forecast String: " + forecast);

        ImageView forecastIcon = (ImageView) getView().findViewById(R.id.detail_icon);
        forecastIcon.setImageResource(weatherDrawable);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        getLoaderManager().restartLoader(DETAIL_LOADER_ID, null, this);
    }


    public static DetailsFragment newFragment(String date){
        final DetailsFragment detailsFragment = new DetailsFragment();

        Bundle bundle = new Bundle();
        bundle.putString(DATE_KEY, date);

        detailsFragment.setArguments(bundle);
        return detailsFragment;
    }
}