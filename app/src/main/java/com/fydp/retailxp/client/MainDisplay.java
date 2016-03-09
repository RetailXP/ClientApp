package com.fydp.retailxp.client;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.GridView;

// All of this stuff is basically to do RPi-Android Communication
// http://examples.javacodegeeks.com/android/core/socket-core/android-socket-example/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

public class MainDisplay extends AppCompatActivity {

    public final static String EXTRA_MESSAGE = "com.fydp.retailxp.client.MESSAGE";

    // Socket is static so we don't keep recreating the connection
    // It won't work otherwise
    // private static Socket socket;
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


        // Have to create socket connections on a separate thread
        // Will crash if attempted on the main thread (NetworkMainThreadException)
        // Create a socket connection only if it doesn't already exist
        if (socketConn == null) {
            try {
                System.out.println("Starting a new SocketTask");
                socketConn = new SocketConnection(new InitializeSocketTask().execute().get());
                //socket = new InitializeSocketTask().execute().get();
                // Legacy: From when Runnable was used instead of AsyncTask
                // new Thread(new ClientThread()).start();
            } catch (InterruptedException e) {
                System.out.println("Interrupted Exception: Thread was interrupted while waiting");
                e.printStackTrace();
            } catch (ExecutionException e) {
                System.out.println("Execution Exception: Computation threw exception");
                e.printStackTrace();
            }
        }
        // Make sure that a socket connection is made before continuing
        while (socketConn.getSocket() == null) { Thread.yield(); }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // TODO: Fix issue with the cell stretching
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when a shoe is selected on the main menu.
     * Takes the user to a more detailed product page for the shoe.
     *
     * For now, the individual product details are each their own parameter.
     * However this should be a single object in the future, received from the database.
     */
    public void seeDetailedShoeDisplay() {
        Intent intent = new Intent(this, DetailedShoeDisplay.class);
        String message = "Preliminary Test";
        intent.putExtra(EXTRA_MESSAGE, message);

        /*
        TODO: Preliminary TCP Testing
        For now just send some data to the Raspberry Pi
        */
        //try {
            String str = "test";
            if (socketConn.getSocket() != null) {
                // Define input and output streams
                //PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                //BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Output to socket using PrintWriter
                // Necessary to use println() instead of print()
                // The message won't be properly terminated without an endline
                //out.println(str);
                //PrintWriter out = new PrintWriter(poop.getOutputStream(), true);
                socketConn.writeToSocket(str);
                System.out.println("Sent message: " + str);
            }
        /*} catch (UnknownHostException e1) {
            System.out.println("Error 1: UnknownHostException");
            e1.printStackTrace();
        } catch (IOException e2) {
            System.out.println("Error 2: IOException");
            e2.printStackTrace();
        } catch (Exception e3) {
            System.out.println("Error 3: General Exception");
            e3.printStackTrace();
        }*/

        startActivity(intent);
    }

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

    // Worker task to initialize the socket connection
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
            } finally {
                return this.socket;
            }
        }

        protected Void onPostExecute() {
            this.exception.printStackTrace();
            return null;
        }
    }

    // Worker task to listen on the port
    private class SocketListenerTask extends AsyncTask<BufferedReader, Void, Void> {
        private Exception exception;
        private String msg;

        protected Void doInBackground(BufferedReader... in) {
            try {
                System.out.println("Enter SocketListener");
                System.out.println("Start listening for incoming messages");
                while(true) {
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
            } finally {
                return null;
            }
        }

        protected Void onPostExecute() {
            this.exception.printStackTrace();
            return null;
        }
    }


    /*
    // Legacy: Using a Runnable instead of AsyncTask
    class ClientThread implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("Trying to run thread");
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);
                System.out.println("Thread run success");
            } catch (UnknownHostException e1) {
                System.out.println("Thread Error 1");
                e1.printStackTrace();
            } catch (IOException e2) {
                System.out.println("Thread Error 2");
                e2.printStackTrace();
            }
        }
    }
    */
}
