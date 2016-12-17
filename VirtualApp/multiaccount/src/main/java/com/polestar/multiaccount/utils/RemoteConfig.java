package com.polestar.multiaccount.utils;

import android.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.polestar.ad.AdConfig;
import com.polestar.multiaccount.BuildConfig;
import com.polestar.multiaccount.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by guojia on 2016/12/17.
 */

public class RemoteConfig {

    private static FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    private static String TAG = "RemoteConfig";

    public static void init () {
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        int cacheTime = BuildConfig.DEBUG ? 0 : 8*60*60;
        mFirebaseRemoteConfig.setDefaults(R.xml.default_remote_config);
        mFirebaseRemoteConfig.fetch(cacheTime).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                MLogs.d(TAG, "Fetch Succeeded");
                mFirebaseRemoteConfig.activateFetched();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                MLogs.d(TAG, "Fetch failed");
            }
        });
        mFirebaseRemoteConfig.activateFetched();
    }

    public static boolean getBoolean(String key) {
        return mFirebaseRemoteConfig.getBoolean(key);
    }

    public static long getLong(String key) {
        return mFirebaseRemoteConfig.getLong(key);
    }

    public static String getString(String key) {
        return mFirebaseRemoteConfig.getString(key);
    }

    //fb:adfdf:-1;ab:sdff:-2;
    public static List<AdConfig> getAdConfigList(String placement) {
        String config = getString(placement);
        if (TextUtils.isEmpty(config)) {
            return new ArrayList<>();
        }
        List<AdConfig> configList = new ArrayList<>();
        String[] sources = config.split(";");
        for (String s: sources) {
            String[] configs = s.split(":");
            if (configs == null || configs.length < 2) {
                MLogs.e("Wrong config: " + config);
                continue;
            }
            int cachTime = 0;
            if (configs.length == 3) {
                try {
                   cachTime = Integer.valueOf(configs[2]);
                } catch (Exception e){
                    MLogs.e("Wrong config: " + config);
                }
            }
            configList.add(new AdConfig(configs[0], configs[1], cachTime));
        }
        return configList;
    }
}
