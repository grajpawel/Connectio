package com.paplo.autowifi;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.places.PlaceBuffer;
import com.paplo.autowifi.provider.PlaceContract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


/**
 * Created by grajp on 18.03.2018.
 */

public class PlaceListAdapter extends RecyclerView.Adapter<PlaceListAdapter.PlaceViewHolder> {
    private static String TAG = PlaceListAdapter.class.getSimpleName();
    private Context mContext;
    private PlaceBuffer mPlaces;
    private List<String> placeNames;
    private List<Address> addresses;
    public static String activeId;


    PlaceListAdapter (Context context, PlaceBuffer places){
        this.mContext = context;
        this.mPlaces = places;
    }


    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.item_place_card, parent, false);
        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;

        Cursor data = mContext.getContentResolver().query(
                uri,
                null,
                null,
                null,
                null);
        placeNames = new ArrayList<>();
        if (data != null){
            while (data.moveToNext()){
                placeNames.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_NAME)));
            }
            data.close();

        }

        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final PlaceViewHolder holder, int position) {
        addresses = null;
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(mPlaces.get(position).getLatLng().latitude, mPlaces.get(position).getLatLng().longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String placeId = mPlaces.get(position).getId();

        String[] placeInfo = getPlaceInfo(placeNames.get(position), position);

        holder.nameTextView.setText(placeInfo[0]);
        holder.addressTextView.setText(placeInfo[1]);
        holder.nameTextView.setTag(R.string.tag_key, placeId);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tag = holder.nameTextView.getTag(R.string.tag_key).toString();
                String placeName = holder.nameTextView.getText().toString();
                String placeAddress = holder.addressTextView.getText().toString();

                Intent detailIntent = new Intent(mContext, DetailActivity.class);
                detailIntent.putExtra("placeId", tag);
                detailIntent.putExtra("placeName", placeName);
                detailIntent.putExtra("placeAddress", placeAddress);
                mContext.startActivity(detailIntent);


            }
        });



    }

    private String[] getPlaceInfo(String placeNameFromDb, int position){
        String[] placeInfo = new String[2];
        if (placeNameFromDb != null && !placeNameFromDb.isEmpty()){
            placeInfo[0] = placeNameFromDb;
            placeInfo[1] = Objects.requireNonNull(mPlaces.get(position).getAddress()).toString();
        } else {
            if (addresses != null){
                if (addresses.get(0).getThoroughfare() != null) {
                    placeInfo[0] = addresses.get(0).getThoroughfare() + " " + addresses.get(0).getFeatureName();
                }
                else {
                    placeInfo[0] = addresses.get(0).getFeatureName();
                }

                if (addresses.get(0).getLocality() != null) {
                    placeInfo[1] = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea() + ", " + addresses.get(0).getCountryName();
                } else {
                    placeInfo[1] = addresses.get(0).getAdminArea() + ", " + addresses.get(0).getCountryName();

                }
            } else {
                placeInfo[0] = mPlaces.get(position).getName().toString();
                placeInfo[1] = Objects.requireNonNull(mPlaces.get(position).getAddress()).toString();

            }
        }

        return placeInfo;
    }

    void swapPlaces(PlaceBuffer newPlaces){
        Log.d(TAG, "View Swapped");

        mPlaces = newPlaces;
        if (mPlaces != null){
            this.notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        if(mPlaces==null) return 0;
        return mPlaces.getCount();
    }

    public class PlaceViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView;
        TextView addressTextView;

        PlaceViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.name_text_view);
            addressTextView = itemView.findViewById(R.id.address_text_view);

        }
    }
}
