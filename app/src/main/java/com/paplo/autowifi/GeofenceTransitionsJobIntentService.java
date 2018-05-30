package com.paplo.autowifi;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.paplo.autowifi.provider.PlaceContract;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;



public class GeofenceTransitionsJobIntentService extends JobIntentService {

    private String placeName;
    private boolean showNotifications;
    private boolean timeConstraints;
    private boolean undoAfterTime;
    private String enterString;
    private String exitString;
    private long startTimeLong;
    private long endTimeLong;
    private String encodedDays;
    private Boolean[] placeDays = new Boolean[7];
    private String geofenceId;


    private static final int JOB_ID = 573;

    private static final String TAG = "GeofenceTransitionsIS";

    private static final String CHANNEL_ID = "channel_01";

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeofenceTransitionsJobIntentService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        Log.d(TAG, "Geofence triggered");
        final SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), MODE_PRIVATE);
        boolean isEnabled = sharedPreferences.getBoolean(this.getString(R.string.enable_app_key), false);
        if (isEnabled) {
            Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;


            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();

            if (geofenceList == null)
                return;

            geofenceId = geofenceList.get(0).getRequestId();
            PlaceListAdapter.activeId = geofenceId;


            Cursor data = getContentResolver().query(
                    uri,
                    null,
                    "placeID='" + geofenceId + "'",
                    null,
                    null);
            if (data != null) {
                if (data.moveToNext()) {
                    placeName = data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_NAME));
                    showNotifications = (data.getInt(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_NOTIFICATIONS)) == 1);
                    timeConstraints = (data.getInt(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_TIME_CONSTRAINTS)) == 1);
                    undoAfterTime = (data.getInt(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_UNDO_AFTER_TIME)) == 1);
                    startTimeLong = data.getLong(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_START_TIME));
                    endTimeLong = data.getLong(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_END_TIME));
                    encodedDays = data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_DAYS));
                    enterString = data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ENTER));
                    exitString = data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_EXIT));


                } else {
                    data.close();
                }
            } else {
                return;
            }

            List<Address> addresses;
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());


            try {

                addresses = geocoder.getFromLocation(geofencingEvent.getTriggeringLocation().getLatitude(), geofencingEvent.getTriggeringLocation().getLongitude(), 1);


                if (placeName == null || placeName.isEmpty()) {

                    MainActivity.name = addresses.get(0).getThoroughfare() + " " + addresses.get(0).getFeatureName();
                    MainActivity.address = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea() + ", " + addresses.get(0).getCountryName();
                } else {
                    MainActivity.address = addresses.get(0).getAddressLine(0);
                    MainActivity.name = placeName;

                }


            } catch (IOException e) {
                e.printStackTrace();
            }


            if (encodedDays != null && !encodedDays.isEmpty()) {
                String[] placeDaysString = encodedDays.split("_,_");
                for (int i = 0; i < placeDaysString.length; i++) {
                    placeDays[i] = Boolean.parseBoolean(placeDaysString[i]);
                }
            }

        /*

        Log.d(TAG, "Final ringer: " + userPlaceRinger);
        Log.d(TAG, "Final notifications: " + showNotifications);
        Log.d(TAG, "Final time constraints: " + timeConstraints);
        Log.d(TAG, "Final start time: " + startTimeLong);
        Log.d(TAG, "Final end time: " + endTimeLong);

        */

            Calendar rightNow = Calendar.getInstance();
            int currentDay = rightNow.get(Calendar.DAY_OF_WEEK) - 1;
            if (currentDay == 0)
                currentDay = 7;
            int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
            int currentMinute = rightNow.get(Calendar.MINUTE);
            long minuteInMillis = TimeUnit.MINUTES.toMillis(currentMinute);
            long hoursInMillis = TimeUnit.HOURS.toMillis(currentHour);
            long currentTimeInMillis = hoursInMillis + minuteInMillis;

            int geofenceTransition = geofencingEvent.getGeofenceTransition();


            if (timeConstraints) {

                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent1 = new Intent(this, autostart.class);
                PendingIntent alarmStartIntent = PendingIntent.getBroadcast(this, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent alarmEndIntent = PendingIntent.getBroadcast(this, 1, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

                int hEnd = (int) ((endTimeLong / 1000) / 3600);
                int mEnd = (int) (((endTimeLong / 1000) / 60) % 60);
                int hStart = (int) ((startTimeLong / 1000) / 3600);
                int mStart = (int) (((startTimeLong / 1000) / 60) % 60);


                Calendar calendarStart = Calendar.getInstance();
                calendarStart.setTimeInMillis(System.currentTimeMillis());
                calendarStart.set(Calendar.HOUR_OF_DAY, hStart);
                calendarStart.set(Calendar.MINUTE, mStart);

                if (alarmManager != null) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendarStart.getTimeInMillis() + TimeUnit.MINUTES.toMillis(1), AlarmManager.INTERVAL_DAY, alarmStartIntent);
                }


                if (undoAfterTime) {
                    Calendar calendarEnd = Calendar.getInstance();
                    calendarEnd.setTimeInMillis(System.currentTimeMillis());
                    calendarEnd.set(Calendar.HOUR_OF_DAY, hEnd);
                    calendarEnd.set(Calendar.MINUTE, mEnd);

                    if (alarmManager != null) {
                        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendarEnd.getTimeInMillis() + TimeUnit.MINUTES.toMillis(1), AlarmManager.INTERVAL_DAY, alarmEndIntent);
                    }


                }

                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {// check if user entered geofence


                    if (placeDays[currentDay - 1]) {
                        if (endTimeLong - startTimeLong > 0) {
                            if (currentTimeInMillis >= startTimeLong && currentTimeInMillis <= endTimeLong) {
                                getGeofenceTransition(geofenceTransition);
                            } else {
                                if (undoAfterTime) {
                                    getGeofenceTransition(Geofence.GEOFENCE_TRANSITION_EXIT);
                                }
                            }
                        } else if (endTimeLong - startTimeLong <= 0) {
                            if (currentTimeInMillis >= startTimeLong || currentTimeInMillis <= endTimeLong) {
                                getGeofenceTransition(geofenceTransition);
                            } else {
                                if (undoAfterTime) {
                                    getGeofenceTransition(Geofence.GEOFENCE_TRANSITION_EXIT);
                                }
                            }
                        }
                    }
                } else {
                    getGeofenceTransition(geofenceTransition);
                }
            } else {
                getGeofenceTransition(geofenceTransition);
            }


            if (geofencingEvent.hasError()) {
                Log.e(TAG, "Error: " + geofencingEvent.getErrorCode());

            }
        }
    }


    private void getGeofenceTransition(int geofenceTransition) {

        final SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), MODE_PRIVATE);
        String activePlaceId = sharedPreferences.getString(getString(R.string.active_place_id), null);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        try {
            MainActivity.getIns().updateLayout(geofenceTransition, geofenceId);
        } catch (Exception e) {
            Log.e(TAG, "exception" + e.toString());
        }

        Log.d(TAG, "active place id: " + activePlaceId);

        Log.d(TAG, "geofence id: " + geofenceId);


        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

            //if (showNotifications && !Objects.equals(activePlaceId, geofenceId)) {
            if (showNotifications) {
                Log.d(TAG, "Notification on");
                sendNotification(geofenceTransition, placeName);
            } else {
                clearNotification();
            }

            editor.putString(getString(R.string.active_place_id), geofenceId);
            setMode(enterString);
            editor.apply();

        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            editor.putString(getString(R.string.active_place_id), geofenceId);
            setMode(enterString);
            editor.apply();
        }


        //else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT && Objects.equals(activePlaceId, geofenceId)){
        else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            if (showNotifications)
                sendNotification(geofenceTransition, placeName);


            editor.putString(getString(R.string.active_place_id), null);
            editor.apply();


            setMode(exitString);
        } else {
            Log.e(TAG, "Unknown transition : &d" + geofenceTransition);
        }


    }

    private void clearNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.cancel(0);
        }

    }

    private void sendNotification(int geofenceTransition, String placeName) {


        Intent notificationIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);


        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_wifi_tethering_black_24dp)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_wifi_tethering_black_24dp));

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {


            if (placeName != null && !placeName.isEmpty()) {
                builder.setContentTitle(getString(R.string.you_entered) + " " + placeName);
            } else {
                builder.setContentTitle(getString(R.string.you_entered_without_name));
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            builder.setContentTitle(getString(R.string.back_to_normal));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }
        builder.setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setContentText(getString(R.string.touch_to_relaunch))
                .setContentIntent(notificationPendingIntent)
                .setAutoCancel(true);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, getString(R.string.channel_label), NotificationManager.IMPORTANCE_LOW);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(notificationChannel);
            }
        }

        if (mNotificationManager != null) {
            mNotificationManager.notify(0, builder.build());
        }

    }


    private void setMode(String mode) {
        char[] modes = mode.toCharArray();
        for (int i = 0; i <= 2; i++) {
            Log.d(TAG, "Mode" + i + " " + modes[i]);
            int state = Character.getNumericValue(modes[i]);
            switch (i) {
                case 0: {
                    switch (state) {
                        case 0: {
                            Log.d(TAG, "Mode wifi on");
                            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            if (wifiManager != null) {
                                wifiManager.setWifiEnabled(true);
                            }
                            break;
                        }
                        case 1: {
                            Log.d(TAG, "Mode wifi off");
                            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            if (wifiManager != null) {
                                wifiManager.setWifiEnabled(false);
                            }
                            break;
                        }
                        case 2: {
                            Log.d(TAG, "Mode wifi disabled");
                            break;
                        }
                    }

                    break;


                }

                case 2: {
                    Log.d(TAG, "bt mode state " + state);
                    switch (state) {
                        case 0: {
                            Log.d(TAG, "Mode bt on");
                            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            mBluetoothAdapter.enable();
                            break;
                        }
                        case 1: {
                            Log.d(TAG, "Mode bt off");
                            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            mBluetoothAdapter.disable();
                            break;
                        }

                        case 2: {
                            Log.d(TAG, "Mode bt disabled");
                            break;
                        }


                    }
                    break;

                }
            }


        }


    }
}

