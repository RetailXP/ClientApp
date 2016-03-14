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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DetailedShoeDisplay extends AppCompatActivity {
    private ArrayList<ShoeInfo> availDetails = new ArrayList<>();
    private Handler pollHandler;
    private final int POLLING_PERIOD = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_shoe_display);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();

        // Retrieve basic shoe information from MainDisplay
        TextView nameView = (TextView) findViewById(R.id.nameDetail);
        nameView.setText(intent.getStringExtra(MainDisplay.SHOE_NAME));
        TextView priceView = (TextView) findViewById(R.id.priceDetail);
        priceView.setText(String.valueOf(intent.getDoubleExtra(MainDisplay.SHOE_PRICE, 0.0)));
        ImageView shoeImageView = (ImageView) findViewById(R.id.imageDetail);
        shoeImageView.setImageResource(intent.getIntExtra(MainDisplay.SHOE_IMG, 0));

        // TODO: Get detailed information from server using a background task

        // Build JSON request
        String barcode = "SampleBarCode";
        JSONObject jsonrequest = new JSONObject();
        try {
            jsonrequest.put("Request", "DetailedShoeDisplay");
            jsonrequest.put("Barcode", barcode);
        } catch (JSONException e) {
            System.out.println("JSONException: Error constructing DetailedShoeDisplay JSON");
            e.printStackTrace();
        }
        // Hopefully this doesn't result in several threads being created and never disposed of...
        final String detailedShoeDisplay_request = jsonrequest.toString();
        pollHandler = new Handler();
        Timer timer = new Timer();
        TimerTask poller = new TimerTask() {
            @Override
            public void run() {
                pollHandler.post(new Thread(new GetDetailedShoeInfoTask(detailedShoeDisplay_request)));
                //new Thread(new GetDetailedShoeInfoTask(detailedShoeDisplay_request));
            }
        };
        // Periodically query the server
        timer.schedule(poller, 0, POLLING_PERIOD);

        //pollHandler.post(new GetDetailedShoeInfoTask(detailedShoeDisplay_request));

        // Set up the spinner objects
        spinnerSetup();
    }

    /**
     * Set up and load the spinner objects on the screen.
     * Originally I only wanted to load the available options, but keeping track of the
     * various combinations might be difficult.
     * TODO: I probably won't implement the above since it's not high priority
     */
    private void spinnerSetup() {
        // Sex Spinner
        Spinner sexSpinner = (Spinner) findViewById(R.id.sexSpinner);
        ArrayAdapter<CharSequence> sexAdapter = ArrayAdapter.createFromResource(this,
                R.array.sexes, android.R.layout.simple_spinner_item);
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sexSpinner.setAdapter(sexAdapter);

        // Country Spinner
        Spinner countrySpinner = (Spinner) findViewById(R.id.countrySpinner);
        ArrayAdapter<CharSequence> countryAdapter = ArrayAdapter.createFromResource(this,
                R.array.countries, android.R.layout.simple_spinner_item);
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(countryAdapter);

        // Size Spinner
        Spinner sizeSpinner = (Spinner) findViewById(R.id.sizeSpinner);
        ArrayAdapter<CharSequence> sizeAdapter = ArrayAdapter.createFromResource(this,
                R.array.sizes, android.R.layout.simple_spinner_item);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(sizeAdapter);
    }

    public void OrderButtonClick(View v) {
        JSONObject jsonrequest = new JSONObject();
        try {
            jsonrequest.put("Request", "Order");
            // jsonrequest.put("")
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
            } catch (InterruptedException e) {
                System.out.println("InterruptedException: Error retrieving DetailedShoeDisplay JSON from buffer");
                e.printStackTrace();
            }
        }
    }

    private class ShoeInfo {
        private String barcode;
        private int size;
        private String country;
        private String sex;
        private int availability;

        public ShoeInfo(String barcode, int size, String country, String sex, int availability) {
            this.size = size;
            this.country = country;
            this.sex = sex;
            this.availability = availability;
        }

        public String getBarcode() { return this.barcode; }
        public int getSize() { return this.size; }
        public String getCountry() { return this.country; }
        public String getSex() { return this.sex; }
        public int getAvailability() { return this.availability; }
    }
}
