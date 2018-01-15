package io.benreynolds.hottopics;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.benreynolds.hottopics.packets.LeaveChatroomRequestPacket;
import io.benreynolds.hottopics.packets.LeaveChatroomResponsePacket;
import io.benreynolds.hottopics.packets.ReceiveMessagePacket;
import io.benreynolds.hottopics.packets.SendMessagePacket;

public class ChatroomActivity extends AppCompatActivity {

    /** TAG used in Logcat messages outputted by {@code ChatroomActivity}. */
    private static final String TAG = ChatroomActivity.class.getSimpleName();

    /** Singleton instance of the {@code WebSocketCommunicator} used for network communications. */
    private static final WebSocketCommunicator WEB_SOCKET_COMMUNICATOR =
            WebSocketCommunicator.getInstance();

    /** List of chat messages. */
    final List<String> mMessages = new ArrayList<>();
    private ArrayAdapter<String> mMessageListAdapter;
    private ListView mMessageList;

    /** Chat message field. */
    private EditText mMessage;

    /** Thread used to leave the current chatroom. */
    private Thread tLeaveChatroom;

    /** Thread used to update the chat message list with new messages. */
    private Thread tUpdateChatFeed;

    /** Thread used to monitor connection status. */
    private Thread tCheckConnectionStatus;

    @Override
    public void onBackPressed() {
        if(tLeaveChatroom == null || !tLeaveChatroom.isAlive()) {
            tLeaveChatroom = new Thread(new LeaveChatroomTask());
            tLeaveChatroom.start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        // Setup the chatroom message list adapter
        mMessageListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mMessages);
        mMessageList = findViewById(R.id.lstMessages);
        mMessageList.setAdapter(mMessageListAdapter);
        mMessage = findViewById(R.id.txtMessage);

        // Set the chatroom name header text.
        TextView lblTitle = findViewById(R.id.lblTitle);
        lblTitle.setText(getIntent().getStringExtra(RoomListActivity.ROOM_NAME_EXTRA));

        // Assign the send button's OnClick listener.
        findViewById(R.id.btnSend).setOnClickListener(new BtnSendOnClickListener());

        // Begin monitoring the status of the Hot Topics server connection.
        tCheckConnectionStatus = new Thread(new CheckConnectionStatus());
        tCheckConnectionStatus.start();

        // Begin checking for new messages and updating the chat message feed.
        tUpdateChatFeed = new Thread(new UpdateChatMessagesTask());
        tUpdateChatFeed.start();
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
     * {@code LeaveChatroomTask} sends a {@code LeaveChatroomRequestPacket} to the Hot Topics server
     * and awaits a {@code LeaveChatroomResponsePacket} in return. Transitions to
     * {@code RoomListActivity} if the response yields true.
     */
    public class LeaveChatroomTask implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, String.format("Thread [%s] started... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));

            RequestResponseTask<LeaveChatroomRequestPacket> leaveChatroomTask =
                    new RequestResponseTask<>(new LeaveChatroomRequestPacket(),
                            LeaveChatroomResponsePacket.class);
            Thread tLeaveChatroomRequest = new Thread(leaveChatroomTask);
            tLeaveChatroomRequest.start();

            while(tLeaveChatroomRequest.isAlive()) {
                Thread.yield();
            }

            if(!((LeaveChatroomResponsePacket)leaveChatroomTask.getResponse()).getResponse()) {
                WEB_SOCKET_COMMUNICATOR.disconnect();
                Log.d(TAG, String.format("Thread [%s] finished... (%d).",
                        getClass().getSimpleName(), Thread.currentThread().getId()));
                return;
            }

            interruptThread(tUpdateChatFeed);
            interruptThread(tCheckConnectionStatus);

            Intent mainActivity = new Intent(ChatroomActivity.this, RoomListActivity.class);
            startActivity(mainActivity);

            Log.d(TAG, String.format("Thread [%s] finished... (%d).", getClass().getSimpleName(),
                    Thread.currentThread().getId()));
        }

    }

    /**
     * {@code UpdateChatMessagesTask} polls {@code WebSocketCommunicator} for new messages (
     * '{@code ReceiveMessagePacket}'s) and adds them to the message list.
     */
    public class UpdateChatMessagesTask implements Runnable {

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                ReceiveMessagePacket receiveMessagePacket = WEB_SOCKET_COMMUNICATOR.pollPacket(ReceiveMessagePacket.class);
                if(receiveMessagePacket == null) {
                    continue;
                }

                // Add the new message to the message list
                mMessages.add(receiveMessagePacket.getAuthor() + ": " + receiveMessagePacket.getMessage());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Notify the message list adapter that new data has been added.
                        mMessageListAdapter.notifyDataSetChanged();
                        // Scroll to the latest entry.
                        mMessageList.setSelection(mMessageList.getCount() - 1);
                    }
                });
            }
        }

    }

    /**
     * {@code CheckConnectionStatus} checks whether the {@code WebSocketCommunicator} has an active
     * connection to the server. If no active connection is found, the application transitions to
     * {@code LoginActivity}.
     */
    public class CheckConnectionStatus implements Runnable {

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                if (!WEB_SOCKET_COMMUNICATOR.isConnected()) {
                    Log.w(TAG, "Connection Lost Unexpectedly.");
                    Intent mainActivity = new Intent(ChatroomActivity.this,
                            LoginActivity.class);
                    startActivity(mainActivity);
                    break;
                }
                Thread.yield();
            }
        }

    }

    /**
     * {@code BtnSendOnClickListener}'s {@code onClick} method is executed when
     * '{@code ChatroomActivity}'s send button is pressed. It sends the text within the message
     * field to the Hot Topics server.
     */
    private class BtnSendOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            // Create a SendMessagePacket from the users message.
            SendMessagePacket sendMessagePacket = new SendMessagePacket(mMessage.getText().toString());
            // Clear the message field.
            mMessage.setText("");
            // Send the Packet.
            WEB_SOCKET_COMMUNICATOR.sendPacket(sendMessagePacket);
        }

    }

}
