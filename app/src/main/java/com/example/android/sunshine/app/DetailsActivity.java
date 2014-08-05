package com.example.android.sunshine.app;

import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DetailsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailsFragment())
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
    public static class DetailsFragment extends Fragment {

        private String forecast;
        private static final String SHARE_TAG = " #sunshine";
        private static final String LOG_TAG = DetailsFragment.class.getSimpleName();
        private ShareActionProvider shareActionProvider;

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
                final TextView textView = (TextView)rootView.findViewById(R.id.forecast_detail);
                textView.setText(forecast);
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

        private Intent createForShareIntent() {
            Intent shareIntent  = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.putExtra(Intent.EXTRA_TEXT, forecast + SHARE_TAG);
            return shareIntent;
        }
    }
}
