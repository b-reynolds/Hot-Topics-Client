package io.benreynolds.hottopics;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import io.benreynolds.hottopics.packets.Chatroom;
import io.benreynolds.hottopics.packets.ChatroomsRequestPacket;
import io.benreynolds.hottopics.packets.ChatroomsResponsePacket;
import io.benreynolds.hottopics.packets.JoinChatroomRequestPacket;
import io.benreynolds.hottopics.packets.JoinChatroomResponsePacket;

/**
 * {@code RoomListActivity} handles the retrieval of and connection to rooms from the Hot Topics
 * server. Transitions to {@code ChatroomActivity} and {@code LoginActivity}.
 */
public class RoomListActivity extends AppCompatActivity {

    /** String key for the name of the chatroom sent to {@code ChatroomActivity} */
    public static final String ROOM_NAME_EXTRA = "ROOM_NAME";

    /** TAG used in Logcat messages outputted by {@code LoginActivity}. */
    private static final String TAG = RoomListActivity.class.getSimpleName();

    /** Singleton instance of the {@code WebSocketCommunicator} used for network communications. */
    private static final WebSocketCommunicator WEB_SOCKET_COMMUNICATOR =
            WebSocketCommunicator.getInstance();

    /** List of available chatrooms. */
    final List<String> mChatrooms = new ArrayList<>();
    private ArrayAdapter<String> mRoomListAdapter;
    private ListView mRoomList;

    /** Thread used to monitor connection status. */
    private Thread tCheckConnectionStatus;

    /** Thread used to populate the chatroom list. */
    private Thread tRetrieveChatrooms;

    /** Thread used to join a chatroom. */
    private Thread tJoinChatroom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);

        // Setup the chatroom list adapter
        mRoomListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                mChatrooms);
        mRoomList = findViewById(R.id.lstRooms);
        mRoomList.setAdapter(mRoomListAdapter);

        // Assign the OnClick handler for the chatroom list
        mRoomList.setOnItemClickListener(new ChatroomListItemClickListener());

        // Begin monitoring the status of the Hot Topics server connection.
        tCheckConnectionStatus = new Thread(new CheckConnectionStatus());
        tCheckConnectionStatus.start();

        // Request a list of chatrooms from the Hot Topics server.
        tRetrieveChatrooms = new Thread(new RetrieveChatroomsTask());
        tRetrieveChatrooms.start();
    }

    @Override
    public void onBackPressed() {
        // Interrupt all active threads.
        interruptThread(tCheckConnectionStatus);
        interruptThread(tRetrieveChatrooms);
        interruptThread(tJoinChatroom);

        // Transition to the LoginActivity.
        Intent loginActivity = new Intent(RoomListActivity.this, LoginActivity.class);
        startActivity(loginActivity);
    }

    /**
     * Updates '{@code RoomListActivity}'s UI state. Used to prevent user interaction whilst
     * awaiting connections and responses to requests.
     * @param state desired activity state.
     */
    public void setActivityState(final boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRoomList.setEnabled(state);
            }
        });
    }

    /**
     * Ask the specified thread to stop execution.
     * @param thread thread.
     */
    private void interruptThread(final Thread thread) {
        if(thread != null) {
            thread.interrupt();
        }
    }

    /**
     * {@code RetrieveChatroomsTask} sends a {@code ChatroomsRequestPacket} to the Hot Topics server
     * and awaits a {@code ChatroomsResponsePacket} in return, populating the chatroom list with its
     * contents.
     */
    public class RetrieveChatroomsTask implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, String.format("Thread [%s] started... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));

            // Create a RequestResponseTask for sending/awaiting ChatroomRequest/Response 'Packet's.
            RequestResponseTask<ChatroomsRequestPacket> requestResponseTask =
                    new RequestResponseTask<>(new ChatroomsRequestPacket(),
                            ChatroomsResponsePacket.class);

            // Create a Thread for the RequestResponseTask and run it.
            Thread tRequestResponse = new Thread(requestResponseTask);
            tRequestResponse.start();

            // Wait for the RequestResponseTask to complete.
            while (tRequestResponse.isAlive()) {
                Thread.yield();
            }

            // Cast the response packet into a ChatroomsResponsePacket
            ChatroomsResponsePacket responsePacket = (ChatroomsResponsePacket)
                    requestResponseTask.getResponse();

            // If no response was received, disconnect and return to the LoginActivity.
            if(responsePacket == null) {
                Log.e(TAG,"Error: ChatroomsResponsePacket was not received in time.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBackPressed();
                    }
                });
                Log.d(TAG, String.format("Thread [%s] finished... (%d).", getClass().getSimpleName(),
                        Thread.currentThread().getId()));
                return;
            }

            // Clear and populate the chatroom list.
            mChatrooms.clear();
            for(Chatroom chatroom : responsePacket.getChatrooms()) {
                mChatrooms.add(chatroom.getName());
            }

            // Notify the room list adapter that the data has changed and the list needs updating.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRoomListAdapter.notifyDataSetChanged();
                }
            });

            Log.d(TAG, String.format("Thread [%s] finished... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));
        }

    }

    /**
     * {@code JoinChatroomTask} sends a {@code JoinChatroomRequestPacket} to the Hot Topics server
     * and awaits a {@code JoinChatroomResponsePacket} in return. Transitions to
     * {@code ChatroomActivity} if the response yields true.
     */
    public class JoinChatroomTask implements Runnable {

        private final String mChatroomName;

        JoinChatroomTask(final String chatroomName) {
            mChatroomName = chatroomName;
        }

        @Override
        public void run() {
            Log.d(TAG, String.format("Thread [%s] started... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));

            // Disable the UI.
            setActivityState(false);

            RequestResponseTask<JoinChatroomRequestPacket> requestResponseTask =
                    new RequestResponseTask<>(new JoinChatroomRequestPacket(mChatroomName),
                            JoinChatroomResponsePacket.class);

            Thread tRequestResponse = new Thread(requestResponseTask);
            tRequestResponse.start();

            while (tRequestResponse.isAlive()) {
                Thread.yield();
            }

            // Enable the UI.
            setActivityState(true);

            if (!((JoinChatroomResponsePacket) requestResponseTask.getResponse()).getResponse()) {
                Log.d(TAG, String.format("Thread [%s] finished... (%d).", getClass().getSimpleName(),
                        Thread.currentThread().getId()));
                return;
            }

            interruptThread(tCheckConnectionStatus);
            interruptThread(tRetrieveChatrooms);

            Intent chatRoom = new Intent(RoomListActivity.this,
                    ChatroomActivity.class);
            chatRoom.putExtra(ROOM_NAME_EXTRA, mChatroomName);
            startActivity(chatRoom);
            Log.d(TAG, String.format("Thread [%s] finished... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));
        }

    }

    /**
     * {@code CheckConnectionStatus} checks whether the {@code WebSocketCommunicator} has an active
     * connection to the Hot Topics server. If no such connection is found, the application
     * transitions back to {@code LoginActivity}.
     */
    public class CheckConnectionStatus implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, String.format("Thread [%s] started... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));

            while(!Thread.currentThread().isInterrupted()) {
                if (!WEB_SOCKET_COMMUNICATOR.isConnected()) {
                    Log.e(TAG, "Error: Connection lost.");
                    Intent mainActivity = new Intent(RoomListActivity.this, LoginActivity.class);
                    startActivity(mainActivity);
                    break;
                }
            }

            Log.d(TAG, String.format("Thread [%s] finished... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));
        }

    }

    /**
     * {@code ChatroomListItemClickListener}'s {@code onItemClick} method is executed when an item
     * from the chatroom list is pressed. It sends a request to the Hot Topics server to join the
     * selected room.
     */
    private class ChatroomListItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            String selectedRoom = (String)mRoomList.getItemAtPosition(position);
            Log.d(TAG, String.format("Selected Room: \"%s\"", selectedRoom));

            interruptThread(tJoinChatroom);
            tJoinChatroom = new Thread(new JoinChatroomTask(selectedRoom));
            tJoinChatroom.start();
        }

    }

}
