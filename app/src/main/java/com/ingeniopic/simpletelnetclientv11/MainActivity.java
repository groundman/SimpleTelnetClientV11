package com.ingeniopic.simpletelnetclientv11;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.view.WindowManager;
import android.os.AsyncTask;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    String  host="192.168.4.1",mensaje;
    int port=23;




    private final String TAG = getClass().getSimpleName();
    private Toast fastToast;

    // AsyncTask object that manages the connection in a separate thread
    WiFiSocketTask wifiTask = null;

    // UI elements

    EditText editSend;
    Button buttonSend;
    ToggleButton   condesc;
    private static TextView inputStreamTextView;
    private  static TextView outputStreamTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);                         //orientacion horizontal
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);    //deshabilita teclado al iniciar aplicacion


        inputStreamTextView = (TextView) findViewById(R.id.inputStreamTextView);
        inputStreamTextView.setMovementMethod(new ScrollingMovementMethod());
        outputStreamTextView = (TextView)findViewById(R.id.outputStreamTextView);
        outputStreamTextView.setMovementMethod(new ScrollingMovementMethod());
        editSend = (EditText)findViewById(R.id.editSend);
        buttonSend = (Button)findViewById(R.id.buttonSend);
        condesc = (ToggleButton)findViewById(R.id.condesc);

        fastToast = Toast.makeText(this,"", Toast.LENGTH_SHORT);
    }


    void toastFast(String str) {

        fastToast.setText(str);
        fastToast.show();
    }



    /**
     * Helper function, print a status to both the UI and program log.
     */
    void setStatus(String s) {
        Log.v(TAG, s);

        //textStatus.setText(s);
    }



    public void ToggleButtonConectOnClick(View view){


        if(condesc.isChecked()){
            conectar();
        }else{
            desconectar();
        }

    }



    /**
     * Try to start a connection with the specified remote host.
     */
    public void conectar() {

        if(wifiTask != null) {
            setStatus("ya esta Conectado!");
            toastFast("ya esta Conectado!");
            return;
        }

        try {
            // Get the remote host from the UI and start the thread
            /// String host = editTextAddress.getText().toString();
            // int port = Integer.parseInt(editTextPort.getText().toString());

            // Start the asyncronous task thread
            setStatus("Conectando...");
            wifiTask = new WiFiSocketTask(host, port);
            wifiTask.execute();

        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Puerto o direccion invalidas!");
            toastFast("Puerto o direccion invalidas!");
        }
    }

    /**
     * Disconnect from the connection.
     */
    public void desconectar() {

        if(wifiTask == null) {
            setStatus("Desconectado!");
            toastFast("Desconectado!");
            return;
        }

        wifiTask.disconnect();
        setStatus("Desconectando...");
        toastFast("Desconectando...");
    }

    /**
     * Invoked by the AsyncTask when the connection is successfully established.
     */
    private void connected() {
        setStatus("Conectado.");
        toastFast("Conectado.");
        buttonSend.setEnabled(true);
    }

    /**
     * Invoked by the AsyncTask when the connection ends..
     */
    private void disconnected() {
        setStatus("Desconectado.");
        toastFast("Desconectado.");
        //buttonSend.setEnabled(false);
        // inputStream.setText("");

        // textTX.setText("");
        wifiTask = null;
    }


    private void colorea(){

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            inputStreamTextView.append(Html.fromHtml(mensaje,Html.FROM_HTML_MODE_LEGACY));
        } else {
            inputStreamTextView.append(Html.fromHtml(mensaje));
        }
        inputStreamTextView.append(""+'\n');

    }

    /**
     * Invocado AsyncTask cuando se recibe una nueva linea.
     */
    private void gotMessage(String msg) {

        inputStreamTextView.append(msg+'\n');
        Log.v(TAG, "[RX] "+msg);
    }

    /**
     * Send the message typed in the input field using the AsyncTask.
     */
    @SuppressLint("ResourceAsColor")
    public void sendButtonPressed(View v) {

        if(wifiTask == null) return;
        String msg = editSend.getText().toString();
        if(msg.length() == 0) return;
        wifiTask.sendMessage(msg);
        outputStreamTextView.append(msg+'\n');

        mensaje="<font color=#0000FF>TramaSaliente --> </font>"+msg;                    //colorear en azul
        colorea();;
        Log.v(TAG, "[TX] " );
    }

    /**
     * AsyncTask that connects to a remote host over WiFi and reads/writes the connection
     * using a socket. The read loop of the AsyncTask happens in a separate thread, so the
     * main UI thread is not blocked. However, the AsyncTask has a way of sending data back
     * to the UI thread. Under the hood, it is using Threads and Handlers.
     */
    public class WiFiSocketTask extends AsyncTask<Void, String, Void> {

        // Location of the remote host
        String address;
        int port;

        // Special messages denoting connection status
        private static final String PING_MSG = "SOCKET_PING";
        private static final String CONNECTED_MSG = "SOCKET_CONNECTED";
        private static final String DISCONNECTED_MSG = "SOCKET_DISCONNECTED";

        Socket socket = null;
        BufferedReader inStream = null;
        OutputStream outStream = null;

        // Signal to disconnect from the socket
        private boolean disconnectSignal = false;

        // Socket timeout - close if no messages received (ms)
        private int timeout = 5000;

        // Constructor
        WiFiSocketTask(String address, int port) {
            this.address = address;
            this.port = port;
        }

        /**
         * Main method of AsyncTask, opens a socket and continuously reads from it
         */
        @Override
        protected Void doInBackground(Void... arg) {

            try {

                // Open the socket and connect to it
                socket = new Socket();
                socket.connect(new InetSocketAddress(address, port), timeout);

                // Get the input and output streams
                inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outStream = socket.getOutputStream();

                // Confirm that the socket opened
                if(socket.isConnected()) {

                    // Make sure the input stream becomes ready, or timeout
                    long start = System.currentTimeMillis();
                    while(!inStream.ready()) {
                        long now = System.currentTimeMillis();
                        if(now - start > timeout) {
                            Log.e(TAG, "Input stream timeout, disconnecting!");
                            disconnectSignal = true;
                            break;
                        }
                    }
                } else {
                    Log.e(TAG, "Socket did not connect!");
                    disconnectSignal = true;
                }

                // Read messages in a loop until disconnected
                while(!disconnectSignal) {

                    // Parse a message with a newline character
                    String msg = inStream.readLine();

                    // Send it to the UI thread
                    publishProgress(msg);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error in socket thread!");
            }

            // Send a disconnect message
            publishProgress(DISCONNECTED_MSG);

            // Once disconnected, try to close the streams
            try {
                if (socket != null) socket.close();
                if (inStream != null) inStream.close();
                if (outStream != null) outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * This function runs in the UI thread but receives data from the
         * doInBackground() function running in a separate thread when
         * publishProgress() is called.
         */
        @Override
        protected void onProgressUpdate(String... values) {

            String msg = values[0];
            if(msg == null) return;

            // Handle meta-messages
            if(msg.equals(CONNECTED_MSG)) {
                connected();
            } else if(msg.equals(DISCONNECTED_MSG))
                disconnected();
            else if(msg.equals(PING_MSG))
            {}

            // Invoke the gotMessage callback for all other messages
            else
                gotMessage(msg);

            super.onProgressUpdate(values);
        }

        /**
         * Write a message to the connection. Runs in UI thread.
         */
        public void sendMessage(String data) {

            try {
                outStream.write(data.getBytes());
                outStream.write(0x0d);
                outStream.write(0x0a);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Set a flag to disconnect from the socket.
         */
        public void disconnect() {
            disconnectSignal = true;
        }
    }




}
