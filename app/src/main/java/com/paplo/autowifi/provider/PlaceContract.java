package com.paplo.autowifi.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by grajp on 18.03.2018.
 */

public class PlaceContract {
    static final String AUTHORITY = "com.paplo.autowifi";
    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    static final String PATH_PLACES = "places";

    public static final class PlaceEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLACES).build();
        public static final String TABLE_NAME = "places";
        public static final String COLUMN_PLACE_ID = "placeID";
        public static final String COLUMN_PLACE_RADIUS = "placeRadius";
        public static final String COLUMN_PLACE_NAME = "placeName";
        public static final String COLUMN_PLACE_NOTIFICATIONS = "placeNotifications";
        public static final String COLUMN_PLACE_ENTER = "placeEnter";
        public static final String COLUMN_PLACE_EXIT = "placeExit";
        public static final String COLUMN_PLACE_TIME_CONSTRAINTS = "placeTimeConstraints";
        public static final String COLUMN_PLACE_UNDO_AFTER_TIME = "undoAfterTime";
        public static final String COLUMN_PLACE_DAYS = "placeDays";
        public static final String COLUMN_PLACE_START_TIME = "placeStartTime";
        public static final String COLUMN_PLACE_END_TIME = "placeEndTime";



    }
}
