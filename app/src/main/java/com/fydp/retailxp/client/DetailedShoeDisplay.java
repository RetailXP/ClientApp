package com.fydp.retailxp.client;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DetailedShoeDisplay extends AppCompatActivity {
    private String selection;
    private String currentBarcode;
    private String currentSize;
    private String currentCountry;
    private int currentCountryIndex; // Bad because it relies on the ordering of countries, but w/e
    private String currentSex;

    private ArrayList<ShoeInfo> availDetails = new ArrayList<>();
    private Handler pollHandler;
    private Timer pollingTimer;
    private Thread pollingTask;
    private final int POLLING_PERIOD = 5000;

    // Creating one global object to represent the availability text view
    // Ideally this would be handled by data binding but no time
    private TextView availabilityText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_shoe_display);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Retrieve basic shoe information from MainDisplay
        Intent intent = getIntent();

        selection = intent.getStringExtra(MainDisplay.SHOE_SELECTION);

        TextView nameView = (TextView) findViewById(R.id.nameDetail);
        nameView.setText(intent.getStringExtra(MainDisplay.SHOE_NAME));

        TextView priceView = (TextView) findViewById(R.id.priceDetail);
        DecimalFormat df = new DecimalFormat("#.00");
        priceView.setText("$" + String.valueOf(df.format(intent.getDoubleExtra(MainDisplay.SHOE_PRICE, 0.00))));

        ImageView shoeImageView = (ImageView) findViewById(R.id.imageDetail);
        shoeImageView.setImageResource(intent.getIntExtra(MainDisplay.SHOE_IMG, 0));

        availabilityText = (TextView) findViewById(R.id.availabilityCount);
        availabilityText.setText("0");

        // Set up the spinner objects
        spinnerSetup();

        // Handle administrator mode
        Button orderButton = (Button) findViewById(R.id.orderButton);
        orderButton.setText(MainDisplay.ADMIN_MODE ? "Restock" : "Order");

        // Build JSON request
        JSONObject jsonrequest = new JSONObject();
        try {
            jsonrequest.put("Request", "DetailedShoeDisplay");
            jsonrequest.put("Selection", selection);
        } catch (JSONException e) {
            System.out.println("JSONException: Error constructing DetailedShoeDisplay JSON");
            e.printStackTrace();
        }
        // Hopefully this doesn't result in several threads being created and never disposed of...
        final String detailedShoeDisplay_request = jsonrequest.toString();
        pollHandler = new Handler();
        pollingTimer = new Timer();
        pollingTask = new Thread(new GetDetailedShoeInfoTask(detailedShoeDisplay_request));
        pollingTask.setDaemon(true);
        TimerTask poller = new TimerTask() {
            @Override
            public void run() {
                //pollHandler.post(new Thread(new GetDetailedShoeInfoTask(detailedShoeDisplay_request)));
                pollHandler.post(pollingTask);
            }
        };
        pollingTimer.schedule(poller, 0, POLLING_PERIOD);
    }

    @Override
    protected void onPause() {
        // kill polling timer task
        pollingTimer.purge();
        pollingTimer.cancel();
        super.onPause();
    }

    /**
     * Set up and load the spinner objects on the screen.
     * Originally I only wanted to load the available options, but keeping track of the
     * various combinations might be difficult.
     * TODO: I probably won't implement the above since it's not high priority
     * One way it could be potentially implemented is to listen for when the user changes a
     * spinner option. When that happens, we change up the other spinners to only populate with
     * the stuff that goes with that option.
     * However that might just be too intrusive anyways. The fact that the options can keep
     * changing around could be confusing.
     *
     * TODO: Need to update the current barcode every time the size is changed
     * TODO: Need to implement having 0 availability for unavailable size combos
     */
    private void spinnerSetup() {
        // Sex Spinner
        Spinner sexSpinner = (Spinner) findViewById(R.id.sexSpinner);
        ArrayAdapter<CharSequence> sexAdapter = ArrayAdapter.createFromResource(this,
                R.array.sexes, android.R.layout.simple_spinner_item);
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sexSpinner.setAdapter(sexAdapter);
        sexSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSex = parent.getItemAtPosition(position).toString();
                Toast.makeText(DetailedShoeDisplay.this, "Sex: " + currentSex, Toast.LENGTH_SHORT).show();
                availabilityUpdate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Country Spinner
        Spinner countrySpinner = (Spinner) findViewById(R.id.countrySpinner);
        ArrayAdapter<CharSequence> countryAdapter = ArrayAdapter.createFromResource(this,
                R.array.countries, android.R.layout.simple_spinner_item);
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(countryAdapter);
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCountry = parent.getItemAtPosition(position).toString();
                currentCountryIndex = position;
                Toast.makeText(DetailedShoeDisplay.this, "Country: " + currentCountry, Toast.LENGTH_SHORT).show();
                availabilityUpdate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Size Spinner
        Spinner sizeSpinner = (Spinner) findViewById(R.id.sizeSpinner);
        ArrayAdapter<CharSequence> sizeAdapter = ArrayAdapter.createFromResource(this,
                R.array.sizes, android.R.layout.simple_spinner_item);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(sizeAdapter);
        sizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSize = parent.getItemAtPosition(position).toString();
                Toast.makeText(DetailedShoeDisplay.this, "Size: " + currentSize, Toast.LENGTH_SHORT).show();
                availabilityUpdate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    // For the current size configuration, check if there's a shoe in the list that matches.
    // Also sets the current bar code
    private int checkShoeInList() {
        for (int i = 0; i < availDetails.size(); i++) {
            // compare current configuration with the current availDetails item
            ShoeInfo si = availDetails.get(i);
            String selectCountrySize = (si.getSize())[currentCountryIndex];
            if (si.getSex().equals(currentSex) && selectCountrySize.equals(currentSize)) {
                currentBarcode = si.getBarcode();
                return i;
            }
        }
        currentBarcode = "";
        return -1;
    }

    private void availabilityUpdate() {
        // Only need to send an update if the current shoe is in the list
        // Otherwise it's obvious that there's no availability and we can set it to 0 immediately
        if (checkShoeInList() != -1) {
            JSONObject jsonrequest = new JSONObject();
            try {
                System.out.println("==========================================");
                System.out.println("There's an available shoe to request!!!");
                System.out.println("==========================================");
                jsonrequest.put("Request", "AvailabilityUpdate");
                jsonrequest.put("Barcode", currentBarcode);
                SocketConnection.writeToSocket(jsonrequest.toString());
                String availability = SocketConnection.messageBuffer.take();
                // Forget receiving JSON for this function, just straight up return the number of available shoes.
                System.out.println("Availability: " + availability);
                availabilityText.setText(availability);
            } catch (JSONException e) {
                System.out.println("JSONException: Error building AvailabilityUpdate JSON.");
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("InterruptedException: Error getting availability update.");
                e.printStackTrace();
            }
        } else {
            availabilityText.setText(String.valueOf(0));
        }
    }

    // Send request to order a shoe (restock if in admin mode)
    public void OrderButtonClick(View v) {
        // Get latest availability
        availabilityUpdate();
        // If we're ordering and the current shoe is unavailable, don't send order request
        if (!MainDisplay.ADMIN_MODE && availabilityText.getText().equals("0")) { return; }
        // If we're restocking and the current shoe has no barcode, don't send restock request
        if (MainDisplay.ADMIN_MODE && currentBarcode.equals("")) { return; }

        JSONObject jsonrequest = new JSONObject();
        try {
            jsonrequest.put("Request", MainDisplay.ADMIN_MODE ? "Restock" : "Order");
            jsonrequest.put("Barcode", currentBarcode);
        } catch (JSONException e) {
            System.out.println("JSONException: Error constructing Order JSON");
            e.printStackTrace();
        }
        SocketConnection.writeToSocket(jsonrequest.toString());
    }

    private class GetDetailedShoeInfoTask implements Runnable {
        private String request;
        public GetDetailedShoeInfoTask(String request) {
            this.request = request;
        }

        public void run() {
            SocketConnection.writeToSocket(request);
            try {
                String jsonDetailedShoeDisplay = SocketConnection.messageBuffer.take();
                System.out.println("Detailed JSON: " + jsonDetailedShoeDisplay);
                // TODO: Fill up the availDetails arraylist
                JSONObject msg = (JSONObject) new JSONTokener(jsonDetailedShoeDisplay).nextValue();
                String msgType = msg.getString("Type");
                if (msgType.equals("DetailedShoeDisplay")) {
                    JSONArray jsonshoeList = msg.getJSONArray("Shoes");
                    availDetails.clear();
                    for (int i = 0; i < jsonshoeList.length(); i++) {
                        JSONObject jsonshoe = jsonshoeList.getJSONObject(i);
                        String jsonbarcode = jsonshoe.getString("Barcode");
                        JSONArray jsonsizes = jsonshoe.getJSONArray("Size");
                        // Expecting 3 sizes. Not the greatest design.
                        String[] sizes = new String[3];
                        sizes[0] = jsonsizes.get(0).toString();
                        sizes[1] = jsonsizes.get(1).toString();
                        sizes[2] = jsonsizes.get(2).toString();
                        String jsonsex = jsonshoe.getString("Sex");
                        ShoeInfo si = new ShoeInfo(jsonbarcode, sizes, jsonsex);
                        availDetails.add(i, si);
                    }
                }

                // TODO: Send out availability update and handle it
                // I think the best way to handle the requests (for now) is do everything sequentially
                // That way we sort of guarantee a certain order of data arrivals
                // TODO: Check value of availDetails here
                availabilityUpdate();
            } catch (InterruptedException e) {
                System.out.println("InterruptedException: Error retrieving DetailedShoeDisplay JSON from buffer");
                e.printStackTrace();
            } catch (JSONException e) {
                System.out.println("JSONException: Error handling DetailedShoeDisplay JSON");
                e.printStackTrace();
            }
        }
    }

    // Container for the sizing information for each shoe that's available for the current selection
    private class ShoeInfo {
        private String barcode;
        // 0 - US, 1 - UK, 2 - EURO
        private String[] sizes;
        private String sex;

        public ShoeInfo() {
            barcode = "";
            sizes = new String[3];
            sex = "";
        }

        public ShoeInfo(String barcode, String[] sizes, String sex) {
            this.barcode = barcode;
            this.sizes = sizes;
            this.sex = sex;
        }

        public String getBarcode() { return this.barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }

        public String[] getSize() { return this.sizes; }
        public void setSize(String[] sizes) { this.sizes = sizes; }

        public String getSex() { return this.sex; }
        public void setSex(String sex) { this.sex = sex; }
    }
}
