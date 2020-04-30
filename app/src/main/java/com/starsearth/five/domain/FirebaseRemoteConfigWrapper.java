package com.starsearth.five.domain;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.starsearth.five.BuildConfig;
import com.starsearth.five.R;
import com.starsearth.five.application.StarsEarthApplication;

public class FirebaseRemoteConfigWrapper {

    private Context mContext;
    private com.google.firebase.remoteconfig.FirebaseRemoteConfig mFirebaseRemoteConfig;

    public FirebaseRemoteConfigWrapper(Context context) {
        mContext = context;
        mFirebaseRemoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings.Builder configSettingsBuilder = new FirebaseRemoteConfigSettings.Builder();
        if (BuildConfig.DEBUG) {
            configSettingsBuilder.setDeveloperModeEnabled(BuildConfig.DEBUG);
        }
        FirebaseRemoteConfigSettings configSettings = configSettingsBuilder.build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        updateRemoteConfigs();
    }

    public void updateRemoteConfigs() {
        Task<Void> task;
        if (BuildConfig.DEBUG) {
            task = mFirebaseRemoteConfig.fetch(300);
        }
        else {
           task = mFirebaseRemoteConfig.fetch();
        }

        //mFirebaseRemoteConfig.fetch();
        task
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {

                            // After config data is successfully fetched, it must be activated before newly fetched
                            // values are returned.
                            mFirebaseRemoteConfig.activateFetched();
                            ((StarsEarthApplication) mContext).getAnalyticsManager().remoteConfigUpdated();
                        }
                    }
                });
    }

    public String get(String key) {
        return mFirebaseRemoteConfig.getString(key);
    }

    public String getAdsFrequencyModulo() {
        return mFirebaseRemoteConfig.getString("ads_frequency_modulo");
    }

    public String getAds() {
        return mFirebaseRemoteConfig.getString("ads");
    }

    public String getGestureSpamMessage() {
        return mFirebaseRemoteConfig.getString("gesture_spam_message");
    }

    public String getVolunteerNetworkName() {
        return mFirebaseRemoteConfig.getString("volunteer_network_name");
    }

    public String getPaginationLimit() {
        return mFirebaseRemoteConfig.getString("requests_pagination_limit");
    }

    //The top item on the list can always become a red SOS button for emergencies, eg: coronavirus
    //If this function returns a specific text, we will show the button
    public String getSOSButtonText() {
        return mFirebaseRemoteConfig.getString("sos_button_text");
    }
}
