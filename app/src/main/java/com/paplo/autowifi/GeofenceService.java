package com.paplo.autowifi;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by grajp on 18.03.2018.
 */

public class GeofenceService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(GeofenceService.class.getSimpleName(), "GeofenceService started");
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), MODE_PRIVATE);
        boolean isChecked = sharedPreferences.getBoolean(getString(R.string.enable_app_key), true);
        if (isChecked) {
            startActivity(new Intent(this, GeofenceBroadcastReceiver.class));
        }
        return super.onStartCommand(intent, flags, startId);
    }


}
