package com.admin.cyberman.funwithflags;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    //keys for reading data fom SharedPreferences
    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    private boolean phoneDevice = true; // to check if the device is a phone in order to set the layout for portrait mode
    private boolean preferencesChanged = true; //to check if the preference changed in order to update the UI with the current setting

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //set the default values in the shared preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //register the listener for the SharedPreferences changes
        PreferenceManager.getDefaultSharedPreferences(this).
                registerOnSharedPreferenceChangeListener(preferencesChangeListener);

        //find the screen size
        int screenSize = getResources().getConfiguration().
                screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        //if the device is a tablet, set the phoneDevice variable to false
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
            phoneDevice = false;

        //allow only portrait mode for phone device
        if (phoneDevice)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


    }
    //called after onCreate()
    @Override
    protected void onStart() {
        super.onStart();

        if (preferencesChanged){
            //since the default preferences have been set,
            //initialize the MainActivityFragment and start the quiz
            MainActivityFragment quizFragment = (MainActivityFragment)
                    getSupportFragmentManager().findFragmentById(
                            R.id.quizFragment);
            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferencesChanged = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //get the device orientation
        int orientation = getResources().getConfiguration().orientation;

        //if the device is in portrait orientation, show the menu
        if (orientation == Configuration.ORIENTATION_PORTRAIT){
            final int version = Build.VERSION.SDK_INT; //get the current version of android running on the device
            if (version >= 21) { // for Lollipop Devices and above that support animation
                getMenuInflater().inflate(R.menu.menu_main, menu); //show the menu icon
            }
            else{
                getMenuInflater().inflate(R.menu.menu_settings, menu); //show the overflow menu
            }

            return true;
        }
        else
            return false;
    }

    //launches the SettingsActivity when menu item is tapped
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferncesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferncesIntent);
        return super.onOptionsItemSelected(item);
    }

    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
               //called when the user's preference has changed
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    preferencesChanged = true; //true because the user has changed the settings

                    MainActivityFragment quizFragment = (MainActivityFragment)
                            getSupportFragmentManager().findFragmentById(
                                    R.id.quizFragment);

                    if (key.equals(CHOICES)){ //number of choices to display
                        quizFragment.updateGuessRows(sharedPreferences);
                        quizFragment.resetQuiz();
                    }
                    else if (key.equals(REGIONS)){ //regions to include when changed
                        Set<String> regions = sharedPreferences.getStringSet(REGIONS, null);

                        if (regions != null && regions.size() > 0){
                            quizFragment.updateRegions(sharedPreferences);
                            quizFragment.resetQuiz();
                        }
                        else{
                            //one region must be selected - set Africa as the default
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            regions.add(getString(R.string.default_region));
                            editor.putStringSet(REGIONS, regions);
                            editor.apply();

                            Toast.makeText(MainActivity.this, R.string.default_region_message
                            , Toast.LENGTH_SHORT).show();
                        }
                    }

                    Toast.makeText(MainActivity.this, R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
                }
            };
}
