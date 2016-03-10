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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

public class MainDisplay extends AppCompatActivity implements AdminLoginDialog.AdminLoginDialogListener {

    public final static String EXTRA_MESSAGE = "com.fydp.retailxp.client.MESSAGE";

    private static Boolean adminMode = false;

    // Socket is static so we don't keep recreating the connection
    // It won't work otherwise
    private static SocketConnection socketConn;
    private static final int SERVERPORT = 5005;
    private static final String SERVER_IP = "10.0.0.200";

    public static BlockingQueue<String> messageBuffer = new LinkedBlockingQueue<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_display);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //toolbar.setNavigationIcon(R.drawable.ic_menu_white_18dp);

        // Have to create socket connections on a separate thread
        // Will crash if attempted on the main thread (NetworkMainThreadException)
        // Create a socket connection only if it doesn't already exist
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
        // Make sure that a socket connection is made before continuing
        // while (socketConn.getSocket() == null) { Thread.yield(); }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // TODO: Fix issue with the cell stretching
        // TODO: Call server to grab list of shoe barcodes
        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new ShoeAdapter(this));

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                //Toast.makeText(MainDisplay.this, "" + parent.getAdapter().getItem(position).toString(), Toast.LENGTH_SHORT).show();
                seeDetailedShoeDisplay();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_display, menu);
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
            if (adminMode) {
                item.setChecked(false);
                adminMode = false;
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
                            byte[] passworddigest = md.digest();
                            StringBuffer sb = new StringBuffer();
                            for (byte b : passworddigest) {
                                sb.append(String.format("%02x", b & 0xff));
                            }
                            System.out.println("PW Hash: " + getString(R.string.admin_pw_hash));
                            System.out.println("Entered Hash: " + sb.toString());
                            if (sb.toString().equals(getString(R.string.admin_pw_hash))) {
                                System.out.println("Authenticating");
                                if (item != null) {
                                    item.setChecked(true);
                                    adminMode = true;
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
    public void seeDetailedShoeDisplay() {
        Intent intent = new Intent(this, DetailedShoeDisplay.class);
        String message = "Preliminary Test";
        intent.putExtra(EXTRA_MESSAGE, message);

        /*
        TODO: Get detailed shoe data (JSON format), wrap in object, pass to new activity
        */

        String str = "test";
        if (socketConn != null) {
            if (socketConn.getSocket() != null) {
                socketConn.writeToSocket(str);
                System.out.println("Sent message: " + str);
            }
        }

        startActivity(intent);
    }

    @Override
    public void onAdminLoginDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
    }

    @Override
    public void onAdminLoginDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button
    }

    /**
     * Object that contains the socket and its input and output streams.
     * Once instantiated the socket automatically begins listening on its port.
     * Only one of these should be created (port 5005).
     */
    private class SocketConnection {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public SocketConnection(Socket socket) {
            try {
                // Initialize members
                this.socket = socket;
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Start listening on the port
                new SocketListenerTask().execute(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public Socket getSocket() { return this.socket; }
        public void writeToSocket(String str) {
            // Necessary to use println() instead of print()
            // The message won't be properly terminated without an endline
            out.println(str);
        }
    }

    /**
     * Worker task to initialize the socket connection
     * Returns the created socket
     */
    private class InitializeSocketTask extends AsyncTask<Void, Void, Socket> {
        private Exception exception;
        private Socket socket = null;

        protected Socket doInBackground(Void... junk) {
            try {
                System.out.println("Enter SocketTask");
                System.out.println("Trying to initialize socket");
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                this.socket = new Socket(serverAddr, SERVERPORT);
                System.out.println("Socket initialized");
            } catch (UnknownHostException e1) {
                System.out.println("UnknownHostException");
                this.exception = e1;
            } catch (IOException e2) {
                System.out.println("IOException");
                this.exception = e2;
            }
            return this.socket;
        }

        protected Void onPostExecute() {
            this.exception.printStackTrace();
            return null;
        }
    }

    /**
     * Worker task to listen on a port
     * Waits for incoming messages and adds them to a global message buffer.
     */
    private class SocketListenerTask extends AsyncTask<BufferedReader, Void, Void> {
        private Exception exception;
        private String msg;

        protected Void doInBackground(BufferedReader... in) {
            try {
                System.out.println("Enter SocketListener");
                System.out.println("Start listening for incoming messages");
                while (true) {
                    msg = in[0].readLine();
                    if (msg == null) {
                        break;
                    }
                    messageBuffer.add(msg);
                    System.out.println("Received message: " + msg);
                }
            } catch (IOException e) {
                System.out.println("IOException");
                this.exception = e;
            }
            return null;
        }

        protected Void onPostExecute() {
            this.exception.printStackTrace();
            return null;
        }
    }
}
