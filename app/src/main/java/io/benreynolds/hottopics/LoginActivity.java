package io.benreynolds.hottopics;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Objects;

import io.benreynolds.hottopics.packets.UsernameRequestPacket;
import io.benreynolds.hottopics.packets.UsernameResponsePacket;

/**
 * {@code LoginActivity} allows users to connect to the server and assign themselves a username.
 * Transitions to {@code RoomListActivity}.
 */
public class LoginActivity extends Activity {

    /** TAG used in Logcat messages outputted by {@code LoginActivity}. */
    private static final String TAG = LoginActivity.class.getSimpleName();

    /** Singleton instance of the {@code WebSocketCommunicator} used for network communications. */
    private static final WebSocketCommunicator WEB_SOCKET_COMMUNICATOR =
            WebSocketCommunicator.getInstance();

    /** Thread used to establish connections to the Hot Topics Server (see
     * {@code EstablishConnectionTask}). */
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

    private ProgressBar progressBar;

    private void setStatusText(final boolean visible) {
        setStatusText(false, null, null);
    }

    private void setStatusText(final boolean visible, final String text, final Integer colour) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lblStatus.setVisibility(visible ? TextView.VISIBLE : TextView.INVISIBLE);
                lblStatus.setTextColor(colour);
                lblStatus.setText(text);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Lookup and assign activity controls to their respective variables.
        lblStatus = findViewById(R.id.lblStatus);

        txtUsername = findViewById(R.id.txtUsername);
        btnConnect = findViewById(R.id.btnConnect);
        progressBar = findViewById(R.id.progressBar);

//        // Set the title text to be the app name and version number
//        ((TextView)findViewById(R.id.lblTitle)).setText(String.format("%s (v%s)",
//                getString(R.string.app_name), BuildConfig.VERSION_NAME));

        // Assign the connect button's OnClick listener.
        btnConnect.setOnClickListener(new BtnConnectOnClickListener());

        // If for some reason the WebSocketCommunicator currently has an active connection to the
        // server, disconnect it.
        if(WEB_SOCKET_COMMUNICATOR.isConnected()) {
            WEB_SOCKET_COMMUNICATOR.disconnect();
        }

        // Set the default activity status.
        setActivityState(true);

        txtUsername.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            private void setConnectButtonState(boolean enabled) {
                btnConnect.setEnabled(enabled);
                btnConnect.setBackgroundColor(enabled ? getColor(R.color.hot_topics_blue) : getColor(R.color.hot_topics_blue_disabled));
            }

            @Override
            public void afterTextChanged(Editable editable) {

                // Store the user's desired username, disregarding any trailing or leading spaces.
                String requestedUsername = txtUsername.getText().toString().trim();

                if(requestedUsername.isEmpty()) {
                    lblStatus.setVisibility(TextView.INVISIBLE);
                    setConnectButtonState(false);
                    return;
                }

                String issueStatus = null;

                // Ensure that the username consists of only alphanumeric characters.
                if (requestedUsername.matches(UsernameRequestPacket.INVALID_CHARACTER_REGEX)) {
                    issueStatus = getString(R.string.username_character_error);
                }
                // Ensure that the username is more than MIN_LENGTH characters in length.
                else if (requestedUsername.length() < UsernameRequestPacket.MIN_LENGTH) {
                    issueStatus = getString(R.string.username_short_error);
                }
                // Ensure that the username is less than MAX_LENGTH characters in length.
                else if (requestedUsername.length() > UsernameRequestPacket.MAX_LENGTH) {
                    issueStatus = getString(R.string.username_long_error);
                }

                if(issueStatus != null) {
                    setStatusText(true, issueStatus, getColor(R.color.hot_topics_error_text));
                    setConnectButtonState(false);
                }
                else {
                    setStatusText(true, getString(R.string.username_valid), getColor(R.color.hot_topics_blue));
                    setConnectButtonState(true);
                }
            }

        });

    }

    @Override
    public void onBackPressed() {
        // Close the application
        this.finishAffinity();
    }

    /**
     * Updates '{@code LoginActivity}'s UI state. Used to prevent user interaction whilst awaiting
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
     * {@code EstablishConnectionTask} attempts to establish a connection to the Hot Topics server
     * using the {@code WebSocketCommunicator}. A single attempt at establishing a connection is
     * made before the thread dies (see '{@code WebSocketCommunicator}'s connection timeout period).
     */
    public class EstablishConnectionTask implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, String.format("Thread [%s] started... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));

            // Ensure that the WebSocketCommunicator is not already connected or establishing a
            // connection.
            if(WEB_SOCKET_COMMUNICATOR.isConnected() || WEB_SOCKET_COMMUNICATOR.isConnecting()) {
                Log.w(TAG, "Attempted to establish a connection to the server whilst already" +
                        " connected/connecting.");
                Log.d(TAG, String.format("Thread [%s] finished... (%d).", getClass().getSimpleName(),
                        Thread.currentThread().getId()));
                return;
            }

            // Attempt to connect to the Hot Topics server.
            Log.i(TAG, "Attempting to establish a connection to the Hot Topics server...");
            WEB_SOCKET_COMMUNICATOR.connect();
            while (WEB_SOCKET_COMMUNICATOR.isConnecting()) {
                Thread.yield();
            }

            if(!WEB_SOCKET_COMMUNICATOR.isConnected()) {
                Log.w(TAG, "Failed to establish a connection to the Hot Topics server.");
                Log.d(TAG, String.format("Thread [%s] finished... (%d).", getClass().getSimpleName(),
                        Thread.currentThread().getId()));
                return;
            }

            Log.i(TAG, "Connected to the Hot Topics Server server.");
            Log.d(TAG, String.format("Thread [%s] finished... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));
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
            Log.d(TAG, String.format("Thread [%s] started... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));

            // Disable the form whilst connecting and requesting the username.
            setActivityState(false);

            setStatusText(true, getString(R.string.status_connecting), getColor(R.color.hot_topics_blue));


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                }
            });

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
                    setStatusText(true, getString(R.string.status_connection_failed), getColor(R.color.hot_topics_error_text));
                    setActivityState(true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(ProgressBar.GONE);
                        }
                    });
                    Log.d(TAG, String.format("Thread [%s] finished... (%d).",
                            getClass().getSimpleName(), Thread.currentThread().getId()));
                    return;
                }

                // A connection was established, update the form status to reflect this.
                //setStatus(getString(R.string.status_connected), false);
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
            UsernameResponsePacket responsePacket = (UsernameResponsePacket)requestUsernameTask.getResponse();
            if(responsePacket == null || !responsePacket.getResponse()) {
                setStatusText(true, getString(R.string.username_taken_error), getColor(R.color.hot_topics_error_text));
                setActivityState(true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(ProgressBar.GONE);
                    }
                });
                Log.d(TAG, String.format("Thread [%s] finished... (%d).",
                        getClass().getSimpleName(), Thread.currentThread().getId()));
                return;
            }

            // The username was accepted and assigned to the connection by the server, transition to
            // the RoomListActivity}.

            Intent roomList = new Intent(LoginActivity.this, RoomListActivity.class);
            startActivity(roomList);
            Log.d(TAG, String.format("Thread [%s] finished... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));
        }

    }

    /**
     * {@code BtnConnectOnClickListener}'s {@code onClick} method is executed when
     * '{@code LoginActivity}'s connect button is pressed. It performs validation checks on the
     * requested username before connecting to the server and requesting it (see {@code LoginTask}).
     */
    private class BtnConnectOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            // Store the user's desired username, disregarding any trailing or leading spaces.
            String requestedUsername = txtUsername.getText().toString().trim();

            // Disable the form whilst validating the username.
            setActivityState(false);

            // Connect to the server and request the specified username.
            new Thread(new LoginTask(requestedUsername)).start();
        }

    }

}