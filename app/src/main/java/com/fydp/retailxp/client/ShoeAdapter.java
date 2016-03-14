package com.fydp.retailxp.client;

import android.content.Context;
import android.util.JsonReader;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dmok on 20/01/16.
 */
public class ShoeAdapter extends BaseAdapter {
    private Context mContext;

    public ShoeAdapter(Context c, String json) {
        mContext = c;

        // Populate shoe adapter from JSON retrieved from the server.
        try {
            JSONObject msg = (JSONObject) new JSONTokener(json).nextValue();
            String msgType = msg.getString("Type");
            // Check if the message type is correct
            if (msgType.equals("MainDisplay")) {
                // Read in the JSON
                JSONArray jsonshoeList = msg.getJSONArray("Shoes");
                // Clear current shoe list and start filling it up with new shoes
                mShoeList = new ArrayList<>();
                for (int i = 0; i < jsonshoeList.length(); i++) {
                    JSONObject jsonshoe = jsonshoeList.getJSONObject(i);
                    String name = jsonshoe.getString("Name");
                    double price = jsonshoe.getDouble("Price");
                    mShoeList.add(i, new Shoe(name, price, mThumbIds[i]));
                }
            } else {
                System.out.println("Was expecting a MainDisplay type message");
            }
        } catch (JSONException e) {
            System.out.println("JSON: Error reading GridView JSON data");
            e.printStackTrace();
        }
    }

    // Important. This tells the Adapter how many views to load into view.
    // App crashes if we try to grab more items from the list than we actually have.
    public int getCount() {
        return mShoeList.size();
    }

    public Object getItem(int position) { return mShoeList.get(position); }

    public long getItemId(int position) {
        return 0;
    }

    public ArrayList<Shoe> getmShoeList(){
        return mShoeList;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        /*
        * TODO: Create a LinearLayout Viewgroup and populate it with the crap we want.
        * Instead of returning just an ImageView, we return an entire LinearLayout.
        * */

        // Get the shoe
        System.out.println(position);
        Shoe shoe = mShoeList.get(position);

        // Display view objects
        // Overall shoe display object
        LinearLayout shoeDisplayInfo;
        // Picture of the shoe
        ImageView shoeImage;
        // Name of the shoe
        TextView shoeName;
        // Price of the shoe
        TextView shoePrice;

        if (convertView == null) {
            // if it's not recycled, initialize some attributes

            //imageView = new ImageView(mContext);
            shoeDisplayInfo = new LinearLayout(mContext);
            shoeDisplayInfo.setOrientation(LinearLayout.VERTICAL);

            // Set up shoe image
            shoeImage = new ImageView(mContext);
            //imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
            shoeImage.setAdjustViewBounds(true);
            shoeImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            shoeImage.setPadding(8, 8, 8, 8);
            //shoeImage.setImageResource(mThumbIds[position]);
            shoeImage.setImageResource(shoe.getImageRes());

            // Set up shoe name
            shoeName = new TextView(mContext);
            /*
            TODO: Need to actually get the proper shoe name
            The employee should be able to enter a name... and an image...
            But I'm not sure if I can actually load these resources on the fly like that...
            I guess it should load from an URL resource in the end
             */
            //shoeName.setText(mShoeNames[position]);
            shoeName.setText(shoe.getName());
            shoeName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);

            // Set up shoe price
            shoePrice = new TextView(mContext);
            //shoePrice.setText("$100");
            shoePrice.setText(String.valueOf(shoe.getPrice()));
            shoeName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);

            shoeDisplayInfo.addView(shoeImage);
            shoeDisplayInfo.addView(shoeName);
            shoeDisplayInfo.addView(shoePrice);
        } else {
            //imageView = (ImageView) convertView;
            shoeDisplayInfo = (LinearLayout) convertView;
        }
        //imageView.setImageResource(mThumbIds[position]);


        //return imageView;
        return shoeDisplayInfo;
    }

    // **************************** Custom Functions ****************************

    private ArrayList<Shoe> mShoeList;

    // references to our images
    private Integer[] mThumbIds = {
            R.drawable.adidas_3_series_mens, R.drawable.adidas_originals_zx_flux_print_mens,
            R.drawable.jordan_eclipse_mens, R.drawable.jordan_flight_23_mens,
            R.drawable.jordan_phase_23_ii_mens, R.drawable.nike_air_force_1_low_mens,
            R.drawable.nike_air_force_1_mid_mens, R.drawable.nike_prestige_iv_mens,
            R.drawable.nike_tri_fusion_run_mens, R.drawable.timberland_6_waterproof_premium_boots_mens
    };

    private String[] mShoeNames = {
            "Adidas 3 Series", "Adidas Originals ZX Flux Print",
            "Jordan Eclipse", "Jordan Flight 23",
            "Jordan Phase 23 II", "Nike Air Force 1 Low",
            "Nike Air Force 1 Mid", "Nike Prestige IV",
            "Nike Tri-Fusion Run", "Timberland 6 Waterproof Premium Boots"
    };
}

