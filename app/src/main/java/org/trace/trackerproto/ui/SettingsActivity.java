package org.trace.trackerproto.ui;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.location.LocationRequest;

import org.trace.trackerproto.R;
import org.trace.trackerproto.settings.SettingsManager;
import org.trace.trackerproto.settings.TrackingProfile;

public class SettingsActivity extends AppCompatActivity {

    private SettingsManager mSettingsManager;
    private TrackingProfile mTrackingProfile;

    //Location Inputs
    private EditText locationIntervalInput, locationFastIntervalInput, locationMinAccuracyInput;
    private SeekBar locationDisplacementSeekbar, locationPrioSeekBar;
    private TextView locationDisplacementLabel, locationPriorityLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSettingsManager = SettingsManager.getInstance(this);
        mTrackingProfile = mSettingsManager.getTrackingProfile();

        //Location
        setupLocationSettings();
    }

    private void setupLocationSettings(){

        //Init
        locationIntervalInput       = (EditText)    findViewById(R.id.locationIntervalInput);
        locationFastIntervalInput   = (EditText)    findViewById(R.id.locationFastIntervalInput);
        locationMinAccuracyInput    = (EditText)    findViewById(R.id.minAccuracyInput);
        locationDisplacementSeekbar = (SeekBar)     findViewById(R.id.displacementSeekBar);
        locationDisplacementLabel   = (TextView)    findViewById(R.id.displacementLabel);
        locationPrioSeekBar         = (SeekBar)     findViewById(R.id.prioritySeekBar);
        locationPriorityLabel       = (TextView)    findViewById(R.id.priorityLabel);

        //Populate
        locationIntervalInput.setText(String.valueOf(mTrackingProfile.getLocationInterval()));
        locationFastIntervalInput.setText(String.valueOf(mTrackingProfile.getLocationFastInterval()));
        //TODO: fazer accuracy para o TrackingProfile
        locationDisplacementSeekbar.setProgress(mTrackingProfile.getLocationDisplacementThreshold());
        locationDisplacementLabel.setText(mTrackingProfile.getLocationDisplacementThreshold() + "m");
        locationPrioSeekBar.setProgress(priorityToValue(mTrackingProfile.getLocationTrackingPriority()));
        locationPriorityLabel.setText(priorityToLabel(mTrackingProfile.getLocationTrackingPriority()));

        //Listener
        locationIntervalInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mTrackingProfile.setLocationInterval(
                            Long.valueOf(locationIntervalInput.getText().toString()));

                    mSettingsManager.saveTrackingProfile(mTrackingProfile);
                }
            }
        });

        locationFastIntervalInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    mTrackingProfile.setLocationFastInterval(
                            Long.valueOf(locationFastIntervalInput.getText().toString()));

                    mSettingsManager.saveTrackingProfile(mTrackingProfile);
                }
            }
        });


        locationDisplacementSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                locationDisplacementLabel.setText(locationDisplacementSeekbar.getProgress() + "m");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mTrackingProfile.setLocationDisplacementThreshold(locationDisplacementSeekbar.getProgress());
                mSettingsManager.saveTrackingProfile(mTrackingProfile);

                locationDisplacementLabel.setText(locationDisplacementSeekbar.getProgress() + "m");
            }
        });


        locationPrioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                locationPriorityLabel.setText(priorityToLabel(valueToPriority(progress)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int prio = locationPrioSeekBar.getProgress();

                mTrackingProfile.setLocationTrackingPriority(valueToPriority(prio));
                mSettingsManager.saveTrackingProfile(mTrackingProfile);

                locationPriorityLabel.setText(priorityToLabel(valueToPriority(prio)));
            }
        });
    }

    private int valueToPriority(int value){
        switch (value){
            case 1:
                return LocationRequest.PRIORITY_HIGH_ACCURACY;
            case 2:
                return LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
            case 3:
                return  LocationRequest.PRIORITY_LOW_POWER;
            case 4:
                return LocationRequest.PRIORITY_NO_POWER;
            default:
                return LocationRequest.PRIORITY_HIGH_ACCURACY;
        }
    }

    private int priorityToValue(int priority){
        switch (priority){
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                return 1;
            case LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY:
                return 2;
            case LocationRequest.PRIORITY_LOW_POWER:
                return 3;
            case LocationRequest.PRIORITY_NO_POWER:
                return 4;
            default:
                return 1;
        }
    }

    private String priorityToLabel(int priority) {
        switch (priority) {
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                return "High Accuracy";
            case LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY:
                return "Balanced";
            case LocationRequest.PRIORITY_LOW_POWER:
                return "Low Power";
            case LocationRequest.PRIORITY_NO_POWER:
                return "No Power";
            default:
                return "High Accuracy";

        }
    }
}
