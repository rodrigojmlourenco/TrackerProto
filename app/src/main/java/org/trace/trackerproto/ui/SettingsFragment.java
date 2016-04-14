package org.trace.trackerproto.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;

import org.trace.trackerproto.R;
import org.trace.tracking.tracker.TRACETracker;
import org.trace.tracking.tracker.settings.TrackingProfile;

/**
 * Created by Rodrigo Lourenço on 07/03/2016.
 */
public class SettingsFragment extends Fragment {

    //private SettingsManager mSettingsManager;
    private TrackingProfile mTrackingProfile;

    //
    private Button saveSettingsBtn;

    //Location Inputs
    private EditText locationIntervalInput, locationFastIntervalInput, locationMinAccuracyInput, locationSpeedInput;
    private SeekBar locationDisplacementSeekbar, locationPrioSeekBar;
    private TextView locationDisplacementLabel, locationPriorityLabel;
    private Switch removeOutliersSwitch;

    //Activity Recognition Inputs
    private EditText activityIntervalInput;
    private SeekBar activityConfidenceSeekbar;
    private TextView activityConfidenceLabel;

    //Uploading Inputs
    private CheckBox wifiOnlyBox, onDemandUploadOnlyBox;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //mSettingsManager = SettingsManager.getInstance(getActivity());
        mTrackingProfile = TRACETracker.Client.getCurrentTrackingProfile(getActivity());

        //Location
        setupLocationSettings();

    }

    private void setupLocationSettings(){

        saveSettingsBtn = (Button) getView().findViewById(R.id.saveSettingsBtn);

        //Init
        //////Location Inputs
        locationIntervalInput       = (EditText)    getView().findViewById(R.id.locationIntervalInput);
        locationFastIntervalInput   = (EditText)    getView().findViewById(R.id.locationFastIntervalInput);
        locationMinAccuracyInput    = (EditText)    getView().findViewById(R.id.minAccuracyInput);
        locationDisplacementSeekbar = (SeekBar)     getView().findViewById(R.id.displacementSeekBar);
        locationDisplacementLabel   = (TextView)    getView().findViewById(R.id.displacementLabel);
        locationPrioSeekBar         = (SeekBar)     getView().findViewById(R.id.prioritySeekBar);
        locationPriorityLabel       = (TextView)    getView().findViewById(R.id.priorityLabel);
        locationSpeedInput          = (EditText)    getView().findViewById(R.id.locationSpeedInput);
        removeOutliersSwitch        = (Switch)      getView().findViewById(R.id.removeOutliersSwitch);
        ////// Activity Recognition Inputs
        activityIntervalInput       = (EditText)    getView().findViewById(R.id.activityRecogIntervalInput);
        activityConfidenceSeekbar   = (SeekBar)     getView().findViewById(R.id.activityConfidenceSeekBar);
        activityConfidenceLabel     = (TextView)    getView().findViewById(R.id.activityConfidenceLabel);
        ////// Uploading Inputs
        wifiOnlyBox                 = (CheckBox)    getView().findViewById(R.id.wifiOnlyCheckBox);
        onDemandUploadOnlyBox       = (CheckBox)    getView().findViewById(R.id.onDemandOnlyCheckBox);

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
        removeOutliersSwitch.setChecked(mTrackingProfile.isActiveOutlierRemoval());
        ////// Activity Recognition Inputs
        activityIntervalInput.setText(String.valueOf(mTrackingProfile.getActivityInterval()));
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
                        onDemandOnly = onDemandUploadOnlyBox.isChecked(),
                        removeOutliers = removeOutliersSwitch.isChecked();


                ////// Location
                mTrackingProfile.setLocationInterval(locationInterval);
                mTrackingProfile.setLocationFastInterval(locationFastInterval);
                mTrackingProfile.setLocationDisplacementThreshold(displacementThreshold);
                mTrackingProfile.setLocationMinimumAccuracy(minAcc);
                mTrackingProfile.setLocationTrackingPriority(priority);
                mTrackingProfile.setLocationMaximumSpeed(kmh2ms(maxSpeed));
                mTrackingProfile.activateOutlierRemoval(removeOutliers);
                ////// Activity Recognition
                mTrackingProfile.setActivityRecognitionInterval(activityInterval);
                mTrackingProfile.setActivityMinimumConfidence(activityConfidence);
                ////// Uploading
                mTrackingProfile.setWifiOnly(wifiOnly);
                mTrackingProfile.setOnDemandOnly(onDemandOnly);

                //mSettingsManager.saveTrackingProfile(mTrackingProfile);
                TRACETracker.Client.updateTrackingProfile(getActivity(), mTrackingProfile);

                String message = "Settings saved. They will take effect on the next tracking session.";
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
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
