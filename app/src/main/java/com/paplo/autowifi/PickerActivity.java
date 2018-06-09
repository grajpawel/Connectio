package com.paplo.autowifi;

import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.common.primitives.Booleans;
import com.paplo.autowifi.provider.PlaceContract;

import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class PickerActivity extends AppCompatActivity {
    public static String TAG = PickerActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 101;


    public String enterOptionsString = "222";
    public char[] enterOptions = new char[3];
    public char[] exitOptions = new char[3];
    private static final int PLACE_PICKER_REQUEST = 1;
    private static String placeid;
    private static int finalRadius;
    private static String userPlaceName;
    private static boolean placeAdded = false;
    private static boolean showNotifications;
    private static boolean undoAfterTime;
    private LinearLayout addressLinearLayout;
    private long startTimeLong;
    private long endTimeLong;
    private boolean timeConstraints = false;
    private MenuItem menuItem;
    private Boolean[] dayUserArray = new Boolean[7];
    public static long startLong;
    public static long endLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picker);

        for (int i = 0; i < enterOptions.length; i++){
            enterOptions[i] = exitOptions[i] = '2';
        }

        FloatingActionButton fab = findViewById(R.id.accept_place_fab);


        Arrays.fill(dayUserArray, Boolean.FALSE);
        showNotifications = false;
        addressLinearLayout = findViewById(R.id.address_linear_layout);
        addressLinearLayout.setVisibility(View.GONE);

        TextView dayTextView = findViewById(R.id.active_day_text_view);
        TextView startTimeTextView = findViewById(R.id.start_time_text_view);
        TextView endTimeTextView = findViewById(R.id.end_time_text_view);

        dayTextView.setText(getString(R.string.never));



        StringBuilder sb = new StringBuilder(50);
        Formatter f = new Formatter(sb, Locale.getDefault());
        String timeString  = DateUtils.formatDateRange(this, f, 0, 0, DateUtils.FORMAT_SHOW_TIME, TimeZone.getDefault().toString()).toString();

        startTimeTextView.setText(timeString);
        endTimeTextView.setText(timeString);

        userPlaceName = "";
        ActionBar actionBar = this.getSupportActionBar();
        fab.setVisibility(View.INVISIBLE);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        final SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), MODE_PRIVATE);
        int radiusPref = sharedPreferences.getInt(getString(R.string.pref_radius_key), 100);
        finalRadius = radiusPref;
        final EditText radiusEditText = findViewById(R.id.radiusPicker);
        EditText nameEditText = findViewById(R.id.name_edit_text);
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


            }

            @Override
            public void afterTextChanged(Editable s) {
                userPlaceName = s.toString();
                Log.i(TAG, userPlaceName);

            }
        });

        radiusEditText.setText(String.valueOf(radiusPref));
        radiusEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                FloatingActionButton acceptButton = findViewById(R.id.accept_place_fab);

                String radiusString = s.toString();
                if (!radiusString.isEmpty()){
                    try {
                        finalRadius = (int) Float.parseFloat(radiusString);
                        if (finalRadius > 1000 || finalRadius < 50){
                            radiusEditText.setError(getString(R.string.pick_radius_error_text));
                            if (menuItem != null)
                            menuItem.setVisible(false);
                            if (acceptButton != null)
                                acceptButton.setVisibility(View.INVISIBLE);

                        } else {
                            if (placeAdded) {
                                if (menuItem != null)
                                menuItem.setVisible(true);
                                if (acceptButton != null)
                                acceptButton.setVisibility(View.VISIBLE);
                            }
                        }

                    }
                    catch (NumberFormatException nfe){
                        Log.e(TAG, "No int selected" + nfe.toString());
                    }
                } else {
                    radiusEditText.setError(getString(R.string.pick_radius_error_text));
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_picker, menu);
        menuItem = menu.findItem(R.id.action_save);
        menuItem.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save){
            onConfirmFabClicked(null);
        }
        return super.onOptionsItemSelected(item);
    }



    public void onConfirmFabClicked(View view) {
        final SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.pref_radius_key), finalRadius);
        editor.apply();
        Log.d(TAG, "PlacePickerActivityName: " + userPlaceName);

        String convertedString = TextUtils.join("_,_", dayUserArray);
        ContentValues contentValues = new ContentValues();
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeid);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_RADIUS, finalRadius);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_NAME, userPlaceName);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ENTER, String.valueOf(enterOptions));
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_EXIT, String.valueOf(exitOptions));
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_NOTIFICATIONS, showNotifications);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_TIME_CONSTRAINTS, timeConstraints);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_UNDO_AFTER_TIME, undoAfterTime);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_DAYS, convertedString);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_START_TIME, startTimeLong);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_END_TIME, endTimeLong);
        getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, contentValues);

        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.place_added), Toast.LENGTH_SHORT);
        toast.show();


        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("intent", "true");
        startActivity(intent);
    }

    public void showWifiPicker(View view) {

        final View clickedView = view;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DayPickerDialogTheme)
                .setItems(R.array.wifi_modes_array, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TextView textView;

                        if (clickedView.getId() == findViewById(R.id.wifi_options_enter_linearLayout).getId()) {
                            enterOptions[0] = (char) (which + '0');
                            textView = findViewById(R.id.active_wifi_option_enter_textView);
                        } else {
                            textView = findViewById(R.id.active_wifi_option_exit_textView);
                            exitOptions[0] = (char) (which + '0');
                        }
                        textView.setText(getResources().getStringArray(R.array.wifi_modes_array)[which]);
                    }
                })

                .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        if (clickedView.getId() == findViewById(R.id.wifi_options_enter_linearLayout).getId()){
            builder.setTitle(R.string.enter_action);
            builder.setIcon(R.drawable.ic_directions_enter_black_24dp);
        } else {
            builder.setTitle(R.string.exit_action);
            builder.setIcon(R.drawable.ic_directions_exit_black_24px);
        }
        final AlertDialog dialog = builder.create();
        dialog.show();

    }


    public void showBluetoothPicker(View view) {


        final View clickedView = view;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DayPickerDialogTheme)
                .setItems(R.array.bluetooth_modes_array, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TextView textView;
                        Log.d(TAG, enterOptionsString);

                        if (clickedView.getId() == findViewById(R.id.bluetooth_options_enter_linearLayout).getId()) {
                            textView = findViewById(R.id.active_bluetooth_option_enter_textView);
                            enterOptions[2] = (char) (which + '0');

                        } else {
                            textView = findViewById(R.id.active_bluetooth_option_exit_textView);
                            exitOptions[2] = (char) (which + '0');
                        }
                        textView.setText(getResources().getStringArray(R.array.bluetooth_modes_array)[which]);
                    }
                })

                .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        if (clickedView.getId() == findViewById(R.id.bluetooth_options_enter_linearLayout).getId()){
            builder.setTitle(R.string.enter_action);
            builder.setIcon(R.drawable.ic_directions_enter_black_24dp);
        } else {
            builder.setTitle(R.string.exit_action);
            builder.setIcon(R.drawable.ic_directions_exit_black_24px);
        }
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onExitOptionsClicked(View view) {
        LinearLayout linearLayout = findViewById(R.id.exit_details_linearLayout);
        if (linearLayout.getVisibility() == View.VISIBLE){
            linearLayout.setVisibility(View.GONE);
        } else {
            linearLayout.setVisibility(View.VISIBLE);
        }
    }

    public void onEnterOptionsClicked(View view) {
        LinearLayout linearLayout = findViewById(R.id.enter_details_linearLayout);
        if (linearLayout.getVisibility() == View.VISIBLE){
            linearLayout.setVisibility(View.GONE);
        } else {
            linearLayout.setVisibility(View.VISIBLE);
        }

    }


    public void onNotificationCheckBoxClicked(View view) {
        CheckBox notificationCheckBox = findViewById(R.id.notification_checkbox);
        showNotifications = notificationCheckBox.isChecked();
    }


    public void showDayPicker(View view) {
        boolean[] setChecked = Booleans.toArray(Arrays.asList(dayUserArray));


        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this, R.style.DayPickerDialogTheme)

                .setTitle(R.string.active_days)
                .setIcon(R.drawable.ic_date_range_black_24dp)
                .setMultiChoiceItems(R.array.days_array, setChecked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                    }
                })

                .setPositiveButton(R.string.action_accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                        Log.d(TAG, "Final days: " + TextUtils.join("_,_", dayUserArray));
                        StringBuilder dayString = new StringBuilder();
                        String[] dayArray = getResources().getStringArray(R.array.days_array);
                        int run = 0;



                        for (int i = 0; i < dayUserArray.length; i++){
                            if (dayUserArray[i]){
                                dayString.append(dayArray[i].substring(0, 3)).append(", ");
                                run ++;
                            }
                        }

                        if (dayString.length() == 0){
                            dayString = new StringBuilder(getString(R.string.never));
                        } else {
                            if (run == 7){
                                dayString = new StringBuilder(getString(R.string.everyday));
                            } else {
                                dayString = new StringBuilder(dayString.substring(0, dayString.length() - 2));
                            }
                        }
                        TextView dayTextView = findViewById(R.id.active_day_text_view);
                        if (dayTextView != null)
                        dayTextView.setText(dayString.toString());



                    }
                })
                .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })


                .create();



        dialog.getListView().setItemsCanFocus(false);
        dialog.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        View testView = dialog.getListView().getChildAt(2);
        if (testView != null) {
            Log.d(TAG, "testView isn't null");
            CheckedTextView testTextView = (CheckedTextView) testView;
            testTextView.setChecked(true);
        } else {
            Log.d(TAG, "testView is null");

        }
        dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                CheckedTextView checkedTextView = (CheckedTextView) view;
                Log.d(TAG, "Position checked:" + position + " " + checkedTextView.isChecked());
                dayUserArray[position] = checkedTextView.isChecked();

            }
        });

        dialog.show();
    }

    public void showStartTimePicker(View view) {
        DialogFragment startTimePickerFragment = new TimePickerFragment();
        startTimePickerFragment.show(getFragmentManager(), "startTimePicker");
    }

    public void showEndTimePicker(View view) {
        DialogFragment endTimePickerFragment = new TimePickerFragment();
        endTimePickerFragment.show(getFragmentManager(), "endTimePicker");
    }

    public void setTimeTextViews(String tag, String time, long timeInMillis){
        if (tag.equals("endTimePicker")){
            TextView endTimeTextView = findViewById(R.id.end_time_text_view);
            endTimeTextView.setText(time);
            endTimeLong = timeInMillis;
            endLong = timeInMillis;

        } else if (tag.equals("startTimePicker")){
            TextView startTimeTextView = findViewById(R.id.start_time_text_view);
            startTimeTextView.setText(time);
            startTimeLong = timeInMillis;
            startLong = timeInMillis;

        }

    }

    public void onTimeConstraintsCheckBoxClicked(View view) {
        CheckBox TimeConstraintsCheckBox = findViewById(R.id.time_constraints_checkbox);
        timeConstraints = TimeConstraintsCheckBox.isChecked();
        LinearLayout timeConstraintsLinearLayout = findViewById(R.id.time_constraints_detail);
        if (timeConstraintsLinearLayout != null) {
            if (TimeConstraintsCheckBox.isChecked()) {
                timeConstraintsLinearLayout.setVisibility(View.VISIBLE);
            } else {
                timeConstraintsLinearLayout.setVisibility(View.GONE);
            }
        }
    }

    public void onUndoCheckBoxClicked(View view) {
        CheckBox undoCheckBox = findViewById(R.id.undo_checkbox);
        undoAfterTime = undoCheckBox.isChecked();
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = new NetworkInfo[0];
        if (cm != null) {
            netInfo = cm.getAllNetworkInfo();
        }
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    public void onAddLocationButtonClicked(View view) {
        if (ActivityCompat.checkSelfPermission(PickerActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(PickerActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_FINE_LOCATION);
        } else {

            if (!haveNetworkConnection()) {
                Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinatorPicker), R.string.no_internet, Snackbar.LENGTH_SHORT);
                snackbar.show();
            } else {
                try {
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                    Intent i = builder.build(this);
                    startActivityForResult(i, PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    Log.e(TAG, String.format("GooglePlayServices Not Available [%s]", e.getMessage()));
                } catch (Exception e) {
                    Log.e(TAG, String.format("PlacePicker Exception: %s", e.getMessage()));


                }

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSIONS_REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Location permission granted");
                    onAddLocationButtonClicked(null);
                } else {
                    Log.d(TAG, "Location permission not granted");

                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {
            addressLinearLayout.setVisibility(View.VISIBLE);
            FloatingActionButton acceptButton = findViewById(R.id.accept_place_fab);
            Button locationButton = findViewById(R.id.add_location_button);
            locationButton.setText(getString(R.string.change_location_label));

            Place place = PlacePicker.getPlace(this, data);
            if (place == null) {
                menuItem.setVisible(false);
                acceptButton.setVisibility(View.INVISIBLE);
                return;
            }
            Log.d(TAG, "Radius equal to: " + finalRadius);

            if (finalRadius >= 50 && finalRadius <= 1000){

                menuItem.setVisible(true);
                acceptButton.setVisibility(View.VISIBLE);

            }
            placeAdded = true;

            TextView placeNameTextView = findViewById(R.id.name_text_view);
            TextView placeAddressTextView = findViewById(R.id.address_text_view);
            placeid = place.getId();
            String placeName = place.getName().toString();
            String placeAddress = Objects.requireNonNull(place.getAddress()).toString();
            placeNameTextView.setText(placeName);
            placeAddressTextView.setText(placeAddress);


            //ContentValues contentValues = new ContentValues();
            //contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeID);
            //getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, contentValues);


        }
    }
}
