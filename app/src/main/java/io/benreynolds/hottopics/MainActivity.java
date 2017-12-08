package io.benreynolds.hottopics;

import android.content.Intent;
import android.os.Bundle;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import io.benreynolds.hottopics.packets.UsernameRequestPacket;
import io.benreynolds.hottopics.packets.UsernameResponsePacket;

/**
 * TODO:
 *
 *  1.) Validate Username
 *  2.) Connect To The Server
 *  3.) Send Username Request
 *  4.) Handle Username Response
 *  5.) Rooms

 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private WebSocketCommunicator mWebSocketCommunicator = WebSocketCommunicator.getInstance();

    private EditText txtUsername;
    private Button btnConnect;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtUsername = findViewById(R.id.txtUsername);
        btnConnect = findViewById(R.id.btnConnect);


        //mWebsocketClient.connect();
        btnConnect.setOnClickListener(new BtnConnectListener());

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

    private void setFormEnabled(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtUsername.setEnabled(enabled);
                btnConnect.setEnabled(enabled);
                btnConnect.setClickable(enabled);
            }
        });
    }

    public class ConnectToServer implements Runnable {

        private UsernameRequestPacket mUsernameRequestPacket;

        ConnectToServer(final UsernameRequestPacket usernameRequestPacket) {
            mUsernameRequestPacket = usernameRequestPacket;
            run();
        }

        @Override
        public void run() {

            if (mWebSocketCommunicator.isConnected()) {
                return;
            }

            Log.i(TAG, "Connecting to the Hot Topics server.");

            mWebSocketCommunicator.connect();
            while (mWebSocketCommunicator.isConnecting()) {
                assert true;
            }

            if (!mWebSocketCommunicator.isConnected()) {
                Log.i(TAG, "Failed to connect to the server.");
                showMessageBox(getString(R.string.connection_timeout_error));
                return;
            }

            mWebSocketCommunicator.sendMessage(mUsernameRequestPacket.toString());

            while (!Thread.currentThread().isInterrupted()) {
                UsernameResponsePacket responsePacket = mWebSocketCommunicator.pollPacket(UsernameResponsePacket.class);
                if (responsePacket == null || !responsePacket.isValid()) {
                    continue;
                }

                if (!responsePacket.getResponse()) {
                    showMessageBox(getString(R.string.username_unknown_error));
                    setFormEnabled(true);
                } else {
                    startActivity(new Intent(MainActivity.this, RoomListActivity.class));
                }

                break;
            }
        }
    }
//
//    public class ResponseListener implements Runnable {
//        public void run() {
//            while (!Thread.currentThread().isInterrupted()) {
//                UsernameResponsePacket responsePacket = mWebsocketClient.getPacket(UsernameResponsePacket.class);
//                if(responsePacket == null || !responsePacket.isValid()) {
//                    continue;
//                }
//
//                if(!responsePacket.getResponse()) {
//                    showMessageBox(getString(R.string.username_unknown_error));
//                    setFormEnabled(true);
//                }
//                else {
//                    startActivity(new Intent(MainActivity.this, RoomListActivity.class));
//                }
//
//                break;
//            }
//        }
//    }

    private class BtnConnectListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            // Disable the Activity's controls whilst the desired username is validated.
            setFormEnabled(false);

            // Store the user's desired username, disregarding any trailing or leading spaces.
            String requestedUsername = txtUsername.getText().toString().trim();

            // Ensure that the username consists of only alphanumeric characters.
            if (requestedUsername.matches(UsernameRequestPacket.INVALID_CHARACTER_REGEX)) {
                showMessageBox(getString(R.string.username_character_error));
                setFormEnabled(true);
                return;
            }

            // Ensure that the username is more than MIN_LENGTH characters in length.
            if (requestedUsername.length() < UsernameRequestPacket.MIN_LENGTH) {
                showMessageBox(getString(R.string.username_short_error));
                setFormEnabled(true);
                return;
            }

            // Ensure that the username is less than MAX_LENGTH characters in length.
            if (requestedUsername.length() > UsernameRequestPacket.MAX_LENGTH) {
                showMessageBox(getString(R.string.username_long_error));
                setFormEnabled(true);
                return;
            }

            // Start a thread that listens for responses to the request.
            new Thread(new ConnectToServer(new UsernameRequestPacket(requestedUsername))).start();
        }

    }
}