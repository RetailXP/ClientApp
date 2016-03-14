package com.fydp.retailxp.client;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

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

/**
 * Created by dmok on 11/03/16.
 * Utility class for handling socket communication.
 * Should never be instantiated.
 */
public final class SocketConnection {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    private static final int SERVERPORT = 5005;
    private static final String SERVER_IP = "10.0.0.200";

    // For now just expose the message buffer. Screw "good" practice.
    public static BlockingQueue<String> messageBuffer = new LinkedBlockingQueue<String>();

    public static boolean isLISTENING() { return LISTENING; }

    private static boolean LISTENING = false;

    // Required for the Toast messages
    private static Context mContext;
    public static void setContext(Context c) { mContext = c; }

    private SocketConnection() {
        socket = null;
        out = null;
        in = null;
    }

    /**
     * Start a new socket session if one doesn't already exist.
     */
    public static void startSocketConnection() {
        if (socket == null) {
            try {
                System.out.println("Starting a new SocketTask");
                //new InitializeSocketTask().execute().get();
                new Thread(new InitializeSocketTask()).start();
                while (socket == null) { Thread.yield(); }
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Start listening on the port
                //new SocketListenerTask().execute(in);
                new Thread(new SocketListenerTask(in)).start();
            /*} catch (InterruptedException e) {
                outputErrorMessage(mContext, "Interrupted Exception: Thread was interrupted while waiting");
                e.printStackTrace();
            } catch (ExecutionException e) {
                outputErrorMessage(mContext, "Execution Exception: Computation threw exception");
                e.printStackTrace();*/
            } catch (IOException e) {
                outputErrorMessage(mContext, "IOException: Error getting IO streams");
                e.printStackTrace();
            }
        }
    }

    public static void writeToSocket(String str) {
        if (socket != null) {
            out.println(str);
        }
    }

    /**
     * Worker task to initialize the socket connection
     * Returns the created socket
     */
    //private static class InitializeSocketTask extends AsyncTask<Void, Void, Void> {
    private static class InitializeSocketTask implements Runnable {
        private Exception exception;
        private String errorMsg;

        //protected Void doInBackground(Void... junk) {
        public void run() {
            try {
                System.out.println("Enter SocketTask");
                System.out.println("Trying to initialize socket");
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);
                System.out.println("Socket initialized");
            } catch (UnknownHostException e1) {
                errorMsg = "UnknownHostException: Couldn't connect to server. Socket not initialized.";
                this.exception = e1;
                outputErrorMessage(mContext, errorMsg);
                this.exception.printStackTrace();
            } catch (IOException e2) {
                errorMsg = "IOException: Couldn't connect to server. Socket not initialized.";
                this.exception = e2;
                outputErrorMessage(mContext, errorMsg);
                this.exception.printStackTrace();
            }
            //return null;
        }
        /*
        protected Void onPostExecute() {
            outputErrorMessage(mContext, errorMsg);
            this.exception.printStackTrace();
            return null;
        }
        */
    }

    /**
     * Worker task to listen on a port
     * Waits for incoming messages and adds them to a global message buffer.
     */
    //private static class SocketListenerTask extends AsyncTask<BufferedReader, Void, Void> {
     private static class SocketListenerTask implements Runnable {
        private Exception exception;
        private String errorMsg;
        private BufferedReader in;
        private String msg;

        public SocketListenerTask(BufferedReader in) {
            this.in = in;
        }

        //protected Void doInBackground(BufferedReader... in) {
        @Override
        public void run() {
            try {
                System.out.println("Enter SocketListener");
                System.out.println("Start listening for incoming messages");
                LISTENING = true;
                while (true) {
                    //msg = in[0].readLine();
                    msg = in.readLine();
                    if (msg == null) {
                        break;
                    }
                    System.out.println("Received message: " + msg);
                    messageBuffer.add(msg);
                    System.out.println("Added message to buffer");
                }
            } catch (IOException e) {
                errorMsg = "IOException: Error when listening on socket.";
                this.exception = e;
                outputErrorMessage(mContext, errorMsg);
                this.exception.printStackTrace();
            }
            //return null;
        }
        /*
        protected Void onPostExecute() {
            outputErrorMessage(mContext, errorMsg);
            this.exception.printStackTrace();
            return null;
        }
        */
    }

    private static void outputErrorMessage(Context c, String errorMsg) {
        Toast.makeText(c, errorMsg, Toast.LENGTH_SHORT);
        System.out.println(errorMsg);
    }

}
