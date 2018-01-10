package io.benreynolds.hottopics;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import io.benreynolds.hottopics.packets.UsernameRequestPacket;
import io.benreynolds.hottopics.packets.UsernameResponsePacket;

/**
 * {@code MainActivity} allows users to connect to the server and assign themselves a username.
 * Transitions to {@code RoomListActivity}.
 */
public class MainActivity extends AppCompatActivity {

    /** TAG used in Logcat messages outputted by {@code MainActivity}. */
    private static final String TAG = MainActivity.class.getSimpleName();

    /** Singleton instance of the {@code WebSocketCommunicator} used for network communications. */
    private static final WebSocketCommunicator WEB_SOCKET_COMMUNICATOR =
            WebSocketCommunicator.getInstance();

    /** Thread used to establish connections to the Hot Topics Server
     *  (see {@code EstablishConnectionTask}). */
    private Thread tEstablishConnection;

    /** Thread used to send a {@code UsernameRequestPacket} to the Hot Topics server and await a
     *  response. */
    private Thread tRequestUsername;

    /** Status label. */
    private TextView lblStatus;

    /** Username text field. */
    private EditText txtUsername;

    /** Connect button. */
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lookup and assign activity controls to their respective variables.
        lblStatus = findViewById(R.id.lblStatus);
        txtUsername = findViewById(R.id.txtUsername);
        btnConnect = findViewById(R.id.btnConnect);

        // Assign the connect button's OnClick listener.
        btnConnect.setOnClickListener(new BtnConnectOnClickListener());

        // Set the default activity status.
        setStatus(getString(R.string.status_idle));

        // If for some reason the WebSocketCommunicator currently has an active connection to the
        // server, disconnect it.
        if(WEB_SOCKET_COMMUNICATOR.isConnected()) {
            WEB_SOCKET_COMMUNICATOR.disconnect();
        }
    }

    /**
     * Opens an alert dialog displaying the specified message.
     * @param message message.
     */
    private void showAlertDialog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setCancelable(true)
                        .setPositiveButton(R.string.message_box_button_ok, null);
                alertBuilder.create().show();
            }
        });
    }

    /**
     * Updates '@code MainActivity'}s status text.
     * @param status activity status.
     */
    private void setStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lblStatus.setText(status);
            }
        });
    }

    /**
     * Updates '{@code MainActivity}'s UI state. Used to prevent user interaction whilst awaiting
     * connections and responses to requests.
     * @param state desired activity state.
     */
    public void setActivityState(final boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtUsername.setEnabled(state);
                btnConnect.setClickable(state);
                btnConnect.setEnabled(state);
            }
        });
    }

    /**
     * {@code BtnConnectOnClickListener}'s {@code onClick} method is executed when
     * '{@code MainActivity}'s connect button is pressed. It performs validation checks on the
     * requested username before connecting to the server and requesting it (see {@code LoginTask}).
     */
    private class BtnConnectOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            // Store the user's desired username, disregarding any trailing or leading spaces.
            String requestedUsername = txtUsername.getText().toString().trim();

            // Disable the form whilst validating the username.
            setActivityState(false);

            // Ensure that the username consists of only alphanumeric characters.
            if (requestedUsername.matches(UsernameRequestPacket.INVALID_CHARACTER_REGEX)) {
                showAlertDialog(getString(R.string.username_character_error));
                setActivityState(true);
                return;
            }

            // Ensure that the username is more than MIN_LENGTH characters in length.
            if (requestedUsername.length() < UsernameRequestPacket.MIN_LENGTH) {
                showAlertDialog(getString(R.string.username_short_error));
                setActivityState(true);
                return;
            }

            // Ensure that the username is less than MAX_LENGTH characters in length.
            if (requestedUsername.length() > UsernameRequestPacket.MAX_LENGTH) {
                showAlertDialog(getString(R.string.username_long_error));
                setActivityState(true);
                return;
            }

            // Connect to the server and request the specified username.
            new Thread(new LoginTask(requestedUsername)).start();
        }

    }

    /**
     * {@code EstablishConnectionTask} attempts to establish a connection to the Hot Topics server
     * using the {@code WebSocketCommunicator}. A single attempt at establishing a connection is
     * made before the thread dies (see '{@code WebSocketCommunicator}'s connection timeout period).
     */
    public class EstablishConnectionTask implements Runnable {

        @Override
        public void run() {
            // Ensure that the WebSocketCommunicator is not already connected or establishing a
            // connection.
            if(WEB_SOCKET_COMMUNICATOR.isConnected() || WEB_SOCKET_COMMUNICATOR.isConnecting()) {
                Log.w(TAG, "Attempted to establish a connection to the server whilst already" +
                        " connected/connecting.");
                return;
            }

            // Attempt to connect to the Hot Topics server.
            Log.i(TAG, "Attempting to establish a connection to the Hot Topics server...");
            setStatus(getString(R.string.status_connecting));
            WEB_SOCKET_COMMUNICATOR.connect();
            while (WEB_SOCKET_COMMUNICATOR.isConnecting()) {
                Thread.yield();
            }

            if(!WEB_SOCKET_COMMUNICATOR.isConnected()) {
                Log.w(TAG, "Failed to establish a connection to the Hot Topics server.");
                setStatus(getString(R.string.status_connection_failed));
                return;
            }

            Log.i(TAG, "Connected to the Hot Topics Server server.");
            setStatus(getString(R.string.status_connected));
        }

    }

    /**
     * {@code LoginTask} uses {@code EstablishConnectionTask} and {@code RequestResponseTask} to
     * establish a connection to the Hot Topics server and request the specified username.
     */
    public class LoginTask implements Runnable {

        /** Requested username */
        private String mUsername;

        /**
         * @param username Requested username
         */
        LoginTask(final String username) {
            mUsername = username;
        }

        @Override
        public void run() {

            // Disable the form whilst connecting and requesting the username.
            setActivityState(false);

            // If a connection to the Hot Topics server is not established then establish one.
            if(!WEB_SOCKET_COMMUNICATOR.isConnected()) {
                // If an instance of the tEstablishConnection thread exits, interrupt it before
                // creating a new one.
                if (tEstablishConnection != null && tEstablishConnection.isAlive()) {
                    tEstablishConnection.interrupt();
                }

                // Create and run a new instance of the EstablishConnectionTask
                tEstablishConnection = new Thread(new EstablishConnectionTask());
                tEstablishConnection.start();

                // Await the completion of the EstablishConnectionTask
                while (tEstablishConnection.isAlive()) {
                    Thread.yield();
                }

                // If a connection could not be established, update its status bar to reflect this
                // and re-enable the form before killing the thread.
                if (!WEB_SOCKET_COMMUNICATOR.isConnected()) {
                    setStatus("Failed to connect.");
                    setActivityState(true);
                    return;
                }

                // A connection was established, update the form status to reflect this.
                setStatus("Connected.");
            }

            // If an instance of the tRequestUsername thread exists, interrupt it and construct
            // a new one.
            if(tRequestUsername != null && tRequestUsername.isAlive()) {
                tRequestUsername.interrupt();
            }

            // Create and run a new instance of the username RequestResponseTask
            UsernameRequestPacket usernameRequestPacket = new UsernameRequestPacket(mUsername);
            RequestResponseTask<UsernameRequestPacket> requestUsernameTask =
                    new RequestResponseTask<>(usernameRequestPacket, UsernameResponsePacket.class);
            tRequestUsername = new Thread(requestUsernameTask);
            tRequestUsername.start();

            // Await the completion of the username RequestResponseTask
            while(tRequestUsername.isAlive()) {
                Thread.yield();
            }

            // If the server rejected the username, update its status bar to reflect this and
            // re-enable the form before killing the thread.
            if(!((UsernameResponsePacket)requestUsernameTask.getResponse()).getResponse()){
                setStatus("Username in use");
                setActivityState(true);
                return;
            }

            // The username was accepted and assigned to the connection by the server, transition to
            // the RoomListActivity}.
            Intent roomList = new Intent(MainActivity.this, RoomListActivity.class);
            roomList.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(roomList);
        }

    }

}