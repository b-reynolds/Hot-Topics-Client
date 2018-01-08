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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private WebSocketCommunicator mWebSocketCommunicator = WebSocketCommunicator.getInstance();

    private Thread tEstablishConnection;

    private TextView lblStatus;
    private EditText txtUsername;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lblStatus = findViewById(R.id.lblStatus);
        txtUsername = findViewById(R.id.txtUsername);
        btnConnect = findViewById(R.id.btnConnect);

        btnConnect.setOnClickListener(new BtnConnectListener());

        setStatus(getString(R.string.status_idle));

        // If the WebSocketCommunicator has an active connection to the server then disconnect it.
        if(mWebSocketCommunicator.isConnected()) {
            mWebSocketCommunicator.disconnect();
        }
    }

    private void showMessageBox(final String message) {
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

    private void setStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lblStatus.setText(status);
            }
        });
    }

    public class EstablishConnection implements Runnable {

        @Override
        public void run() {
            Log.w(TAG, "Attempting to establish a connection to the server.");

            // Ensure that the WebSocketCommunicator is not already connected or establishing a connection.
            if(mWebSocketCommunicator.isConnected() || mWebSocketCommunicator.isConnecting()) {
                Log.w(TAG, "Attempted to establish a connection to the server whilst already connecting/connected.");
                return;
            }

            // Attempt to connect to the server.
            mWebSocketCommunicator.connect();
            setStatus(getString(R.string.status_connecting));
            while (mWebSocketCommunicator.isConnecting()) {
                Thread.yield();
            }

            if(!mWebSocketCommunicator.isConnected()) {
                setStatus(getString(R.string.status_connection_failed));
                Log.w(TAG, "Failed to establish a connection to the server.");
                return;
            }

            setStatus(getString(R.string.status_connected
            ));
            Log.d(TAG, "Connected to the server.");
        }

    }

    public void setFormState(final boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtUsername.setEnabled(state);
                btnConnect.setClickable(state);
                btnConnect.setEnabled(state);
            }
        });
    }

    public class RequestUsername implements Runnable {

        private final String mUsername;

        RequestUsername(final String username) {
            mUsername = username;
        }

        @Override
        public void run() {

            // If the WebSocketCommunicator is not connected to the server, attempt to establish a connection.
            if(tEstablishConnection == null || !tEstablishConnection.isAlive()) {
                tEstablishConnection = new Thread(new EstablishConnection());
                tEstablishConnection.start();
            }

            while (tEstablishConnection.isAlive()) {
                Thread.yield();
            }

            if (!mWebSocketCommunicator.isConnected()) {
                showMessageBox("Failed to connect to the server");
                setFormState(true);
                return;
            }

            UsernameRequestPacket usernameRequestPacket = new UsernameRequestPacket(mUsername);
            if(!usernameRequestPacket.isValid()) {
                setFormState(true);
                return;
            }

            mWebSocketCommunicator.sendPacket(usernameRequestPacket);

            while (!Thread.currentThread().isInterrupted()) {
                UsernameResponsePacket responsePacket = mWebSocketCommunicator.pollPacket(UsernameResponsePacket.class);
                if (responsePacket == null) {
                    continue;
                }

                if (!responsePacket.getResponse()) {
                    showMessageBox(getString(R.string.username_unknown_error));
                    setFormState(true);
                    return;
                }

                Intent roomList = new Intent(MainActivity.this, RoomListActivity.class);
                roomList.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(roomList);
                break;
            }
        }

    }

    private class BtnConnectListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            // Store the user's desired username, disregarding any trailing or leading spaces.
            String requestedUsername = txtUsername.getText().toString().trim();

            // Disable the form while validating the username.
            setFormState(false);

            // Ensure that the username consists of only alphanumeric characters.
            if (requestedUsername.matches(UsernameRequestPacket.INVALID_CHARACTER_REGEX)) {
                showMessageBox(getString(R.string.username_character_error));
                setFormState(true);
                return;
            }

            // Ensure that the username is more than MIN_LENGTH characters in length.
            if (requestedUsername.length() < UsernameRequestPacket.MIN_LENGTH) {
                showMessageBox(getString(R.string.username_short_error));
                setFormState(true);
                return;
            }

            // Ensure that the username is less than MAX_LENGTH characters in length.
            if (requestedUsername.length() > UsernameRequestPacket.MAX_LENGTH) {
                showMessageBox(getString(R.string.username_long_error));
                setFormState(true);
                return;
            }

            // Connect to the server and request the specified username.
            new Thread(new RequestUsername(requestedUsername)).start();
        }

    }
}