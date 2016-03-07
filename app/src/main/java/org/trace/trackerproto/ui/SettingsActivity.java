package org.trace.trackerproto.ui;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;

import org.trace.trackerproto.R;
import org.trace.trackerproto.settings.SettingsManager;
import org.trace.trackerproto.settings.TrackingProfile;

public class SettingsActivity extends AppCompatActivity {

    private SettingsManager mSettingsManager;
    private TrackingProfile mTrackingProfile;

    //
    private Button saveSettingsBtn;

    //Location Inputs
    private EditText locationIntervalInput, locationFastIntervalInput, locationMinAccuracyInput, locationSpeedInput;
    private SeekBar locationDisplacementSeekbar, locationPrioSeekBar;
    private TextView locationDisplacementLabel, locationPriorityLabel;

    //Activity Recognition Inputs
    private EditText activityIntervalInput;
    private SeekBar activityConfidenceSeekbar;
    private TextView activityConfidenceLabel;

    //Uploading Inputs
    private CheckBox wifiOnlyBox, onDemandUploadOnlyBox;

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

        saveSettingsBtn = (Button) findViewById(R.id.saveSettingsBtn);

        //Init
        //////Location Inputs
        locationIntervalInput       = (EditText)    findViewById(R.id.locationIntervalInput);
        locationFastIntervalInput   = (EditText)    findViewById(R.id.locationFastIntervalInput);
        locationMinAccuracyInput    = (EditText)    findViewById(R.id.minAccuracyInput);
        locationDisplacementSeekbar = (SeekBar)     findViewById(R.id.displacementSeekBar);
        locationDisplacementLabel   = (TextView)    findViewById(R.id.displacementLabel);
        locationPrioSeekBar         = (SeekBar)     findViewById(R.id.prioritySeekBar);
        locationPriorityLabel       = (TextView)    findViewById(R.id.priorityLabel);
        locationSpeedInput          = (EditText)    findViewById(R.id.locationSpeedInput);
        ////// Activity Recognition Inputs
        activityIntervalInput       = (EditText)    findViewById(R.id.activityRecogIntervalInput);
        activityConfidenceSeekbar   = (SeekBar)     findViewById(R.id.activityConfidenceSeekBar);
        activityConfidenceLabel     = (TextView)    findViewById(R.id.activityConfidenceLabel);
        ////// Uploading Inputs
        wifiOnlyBox                 = (CheckBox)    findViewById(R.id.wifiOnlyCheckBox);
        onDemandUploadOnlyBox       = (CheckBox)    findViewById(R.id.onDemandOnlyCheckBox);

        //Populate
        ////// Location Inputs
        locationIntervalInput.setText(String.valueOf(mTrackingProfile.getLocationInterval()));
        locationFastIntervalInput.setText(String.valueOf(mTrackingProfile.getLocationFastInterval()));
        locationMinAccuracyInput.setText(String.valueOf(mTrackingProfile.getLocationMinimumAccuracy()));
        locationDisplacementSeekbar.setProgress(mTrackingProfile.getLocationDisplacementThreshold());
        locationDisplacementLabel.setText(mTrackingProfile.getLocationDisplacementThreshold() + "m");
        locationPrioSeekBar.setProgress(priorityToValue(mTrackingProfile.getLocationTrackingPriority()));
        locationPriorityLabel.setText(priorityToLabel(mTrackingProfile.getLocationTrackingPriority()));
        locationSpeedInput.setText(String.format("%.2f", ms2kmh(mTrackingProfile.getLocationMaximumSpeed())));
        ////// Activity Recognition Inputs
        activityIntervalInput.setText(String.valueOf(mTrackingProfile.getArInterval()));
        activityConfidenceSeekbar.setProgress(mTrackingProfile.getActivityMinimumConfidence());
        activityConfidenceLabel.setText(mTrackingProfile.getActivityMinimumConfidence()+"%");
        ///// Uploading Inputs
        wifiOnlyBox.setChecked(mTrackingProfile.isWifiOnly());
        onDemandUploadOnlyBox.setChecked(false);


        //Listeners
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
                locationPriorityLabel.setText(priorityToLabel(valueToPriority(prio)));
            }
        });

        activityConfidenceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                activityConfidenceLabel.setText(progress+"%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                activityConfidenceLabel.setText(seekBar.getProgress()+"%");
            }
        });

        saveSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                long locationInterval = Long.valueOf(locationIntervalInput.getText().toString()),
                    locationFastInterval = Long.valueOf(locationFastIntervalInput.getText().toString()),
                    activityInterval = Long.valueOf(activityIntervalInput.getText().toString());

                int displacementThreshold = locationDisplacementSeekbar.getProgress(),
                    priority = valueToPriority(locationPrioSeekBar.getProgress()),
                    activityConfidence = activityConfidenceSeekbar.getProgress();

                float minAcc = Float.valueOf(locationMinAccuracyInput.getText().toString()),
                        maxSpeed = Float.valueOf(locationSpeedInput.getText().toString());


                boolean wifiOnly = wifiOnlyBox.isChecked(),
                        onDemandOnly = onDemandUploadOnlyBox.isChecked();


                ////// Location
                mTrackingProfile.setLocationInterval(locationInterval);
                mTrackingProfile.setLocationFastInterval(locationFastInterval);
                mTrackingProfile.setLocationDisplacementThreshold(displacementThreshold);
                mTrackingProfile.setLocationMinimumAccuracy(minAcc);
                mTrackingProfile.setLocationTrackingPriority(priority);
                mTrackingProfile.setLocationMaximumSpeed(kmh2ms(maxSpeed));
                ////// Activity Recognition
                mTrackingProfile.setActivityRecognitionInterval(activityInterval);
                mTrackingProfile.setActivityMinimumConfidence(activityConfidence);
                ////// Uploading
                mTrackingProfile.setWifiOnly(wifiOnly);
                mTrackingProfile.setOnDemandOnly(onDemandOnly);

                mSettingsManager.saveTrackingProfile(mTrackingProfile);

                String message = "Settings saved. They will take effect on the next tracking session.";
                Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private int valueToPriority(int value){
        switch (value){
            case 0:
                return LocationRequest.PRIORITY_HIGH_ACCURACY;
            case 1:
                return LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
            case 2:
                return  LocationRequest.PRIORITY_LOW_POWER;
            case 3:
                return LocationRequest.PRIORITY_NO_POWER;
            default:
                return LocationRequest.PRIORITY_HIGH_ACCURACY;
        }
    }

    private int priorityToValue(int priority){
        switch (priority){
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                return 0;
            case LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY:
                return 1;
            case LocationRequest.PRIORITY_LOW_POWER:
                return 2;
            case LocationRequest.PRIORITY_NO_POWER:
                return 3;
            default:
                return 0;
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

    private float kmh2ms(float kmh){
        return (kmh * 1000) / 3600;
    }

    private float ms2kmh(float ms){
        return (ms / 1000) * 3600;
    }
}
