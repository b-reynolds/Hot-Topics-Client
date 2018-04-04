package io.benreynolds.hottopics;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ConnectedActivity extends Activity {

    private static int COUNT = 0;

    /** TAG used in Logcat messages outputted by {@code ConnectedActivity}. */
    protected static final String TAG = ConnectedActivity.class.getSimpleName();

    /** Singleton instance of the {@code WebSocketCommunicator} used for network communications. */
    protected static final WebSocketCommunicator WEB_SOCKET_COMMUNICATOR =
            WebSocketCommunicator.getInstance();

    /** Thread used to run the {@code MonitorConnectionTask} runnable that monitors the Hot Topics server
     * connection */
    protected Thread tMonitorConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tMonitorConnection = new Thread(new MonitorConnectionTask());
        tMonitorConnection.start();
    }

    @Override
    protected void onResume() {
        if(tMonitorConnection != null && !tMonitorConnection.isAlive()) {
            tMonitorConnection = new Thread(new MonitorConnectionTask());
            tMonitorConnection.start();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if(tMonitorConnection != null && tMonitorConnection.isAlive()) {
            tMonitorConnection.interrupt();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(tMonitorConnection != null && tMonitorConnection.isAlive()) {
            tMonitorConnection.interrupt();
        }
        super.onDestroy();
    }

    /**
     * {@code MonitorConnectionTask} continuously checks if a connection to the Hot Topics server
     * is established. If no connection is established, it transitions to the {@code LoginActivity}.
     */
    private class MonitorConnectionTask implements Runnable {

        @Override
        public void run() {
            // TODO: Remove all observers from the WebSocketCommunicator?

            Log.d(TAG, String.format("[%s:%s] Started...", getClass().getSimpleName(),
                    Thread.currentThread().getId()));

            while(!Thread.currentThread().isInterrupted()) {
                if(WEB_SOCKET_COMMUNICATOR.isConnected()) {
                    continue;
                }

                Log.w(TAG, "Connection was lost unexpectedly.");
                Intent mainActivity = new Intent(ConnectedActivity.this,
                        LoginActivity.class);
                startActivity(mainActivity);

                break;
            }

            Log.d(TAG, String.format("[%s:%s] Finished...", getClass().getSimpleName(),
                    Thread.currentThread().getId()));
        }

    }

}
