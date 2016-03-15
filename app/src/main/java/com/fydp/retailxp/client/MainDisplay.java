package com.fydp.retailxp.client;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;

import android.widget.EditText;
import android.widget.GridView;
import android.widget.TableLayout;
import android.widget.Toast;

// All of this stuff is basically to do RPi-Android Communication
// http://examples.javacodegeeks.com/android/core/socket-core/android-socket-example/
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MainDisplay extends AppCompatActivity {

    public final static String SHOE_NAME = "com.fydp.retailxp.client.SHOE_NAME";
    public final static String SHOE_PRICE = "com.fydp.retailxp.client.SHOE_PRICE";
    public final static String SHOE_IMG = "com.fydp.retailxp.client.SHOE_IMG";
    public final static String SHOE_SELECTION = "com.fydp.retailxp.client.SHOE_SELECTION";

    public static boolean ADMIN_MODE = false;

    // Socket is static so we don't keep recreating the connection
    // It won't work otherwise
    /*
    private static SocketConnection socketConn;
    private static final int SERVERPORT = 5005;
    private static final String SERVER_IP = "10.0.0.200";

    public static BlockingQueue<String> messageBuffer = new LinkedBlockingQueue<String>();
    private static boolean LISTENING = false;
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_display);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Have to create socket connections on a separate thread
        // Will crash if attempted on the main thread (NetworkMainThreadException)
        // Create a socket connection only if it doesn't already exist

        SocketConnection.setContext(getApplicationContext());
        SocketConnection.startSocketConnection();

        /*
        if (socketConn == null) {
            try {
                System.out.println("Starting a new SocketTask");
                Socket socket = new InitializeSocketTask().execute().get();
                if (socket != null) {
                    socketConn = new SocketConnection(socket);
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted Exception: Thread was interrupted while waiting");
                e.printStackTrace();
            } catch (ExecutionException e) {
                System.out.println("Execution Exception: Computation threw exception");
                e.printStackTrace();
            }
        }
        */
        // Make sure that a socket connection is made before continuing
        // while (socketConn.getSocket() == null) { Thread.yield(); }
        // while (SocketConnection.isLISTENING() == false) { Thread.yield(); }

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

        // TODO: Fix issue with the cell stretching

        // GridView Setup for MainDisplay
        JSONObject jsonrequest = new JSONObject();
        try {
            jsonrequest.put("Request", "MainDisplay");
        } catch (JSONException e) {
            System.out.println("JSONException: Error creating MainDisplay request JSON");
            e.printStackTrace();
        }
        SocketConnection.writeToSocket(jsonrequest.toString());
        /*
        if (socketConn != null) {
            if (socketConn.getSocket() != null) {
                socketConn.writeToSocket(getMainDisplayData);
                System.out.println("Sent message: " + getMainDisplayData);
            }
        }
        */

        try {
            // Wait for 10 seconds to get data from the server
            //String setMainDisplayData = messageBuffer.poll(5, TimeUnit.SECONDS);
            //while (messageBuffer.peek() == null) { Thread.yield(); }
            // TODO: Consider emptying the message buffer before taking since we're blindly assuming that order is being maintained
            //SocketConnection.messageBuffer.clear();
            String jsonMainDisplay = SocketConnection.messageBuffer.take();
            System.out.println("MainDisplay JSON: " + jsonMainDisplay);

            // Initialize GridView
            GridView gridview = (GridView) findViewById(R.id.gridview);
            gridview.setAdapter(new ShoeAdapter(this, jsonMainDisplay));
            gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v,
                                        int position, long id) {
                    //Toast.makeText(MainDisplay.this, "" + parent.getAdapter().getItem(position).toString(), Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainDisplay.this, position + " " + ((Shoe)parent.getAdapter().getItem(position)).getName(), Toast.LENGTH_SHORT).show();

                    String name = ((Shoe)parent.getAdapter().getItem(position)).getName();
                    double price = ((Shoe)parent.getAdapter().getItem(position)).getPrice();
                    Integer imgID = ((Shoe)parent.getAdapter().getItem(position)).getImageRes();
                    String selection = ((Shoe)parent.getAdapter().getItem(position)).getSelection();
                    seeDetailedShoeDisplay(name, price, imgID, selection);
                }
            });

        } catch (InterruptedException e) {
            System.out.println("InterruptedException: GridView data couldn't be retrieved");
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_display, menu);
        if (ADMIN_MODE) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getItemId() == R.id.action_admin) {
                    item.setChecked(true);
                }
            }
        }

        return true;
    }

    @Override
    // Needed to declare MenuItem item as final so I could reference it for the AlertDialog function
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_admin) {
            if (ADMIN_MODE) {
                item.setChecked(false);
                ADMIN_MODE = false;
                Toast.makeText(MainDisplay.this,"Administrator mode off.",Toast.LENGTH_SHORT).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                LayoutInflater inflater = this.getLayoutInflater();
                builder.setMessage(R.string.admin_dialog_text);
                builder.setView(inflater.inflate(R.layout.admin_dialog, null));
                builder.setPositiveButton(R.string.admin_dialog_enter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // process text and enable admin mode
                        try {
                            System.out.println("Password Checking");
                            // Need to find view within the dialog, otherwise it can't find the reference and returns null
                            EditText passwordField = (EditText) ((Dialog) dialog).findViewById(R.id.admin_pw);
                            MessageDigest md = MessageDigest.getInstance("MD5");
                            String password = passwordField.getText().toString();
                            md.update(password.getBytes());
                            byte[] passwordDigest = md.digest();
                            StringBuffer sb = new StringBuffer();
                            for (byte b : passwordDigest) {
                                sb.append(String.format("%02x", b & 0xff));
                            }
                            if (sb.toString().equals(getString(R.string.admin_pw_hash))) {
                                if (item != null) {
                                    item.setChecked(true);
                                    ADMIN_MODE = true;
                                    Toast.makeText(MainDisplay.this,"Administrator mode on.",Toast.LENGTH_SHORT).show();
                                } else {
                                    System.out.println("Menu item is null for some reason.");
                                }
                            } else {
                                Toast.makeText(MainDisplay.this,"Incorrect password.",Toast.LENGTH_SHORT).show();
                            }
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                    }
                });
                builder.setNegativeButton(R.string.admin_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Nothing really happens
                    }
                });
                AlertDialog adminPopup = builder.create();
                adminPopup.show();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    // **************************** Custom Functions ****************************

    /**
     * Takes the user to a more detailed product page for the shoe.
     * Called when a shoe is selected on the main menu.
     *
     * For now, the individual product details are each their own parameter.
     * However this should be a single object in the future, received from the database.
     */
    public void seeDetailedShoeDisplay(String name, double price, Integer imageRes, String selection) {
        Intent intent = new Intent(this, DetailedShoeDisplay.class);
        intent.putExtra(SHOE_NAME, name);
        intent.putExtra(SHOE_PRICE, price);
        intent.putExtra(SHOE_IMG, imageRes);
        intent.putExtra(SHOE_SELECTION, selection);

        startActivity(intent);
    }
}
