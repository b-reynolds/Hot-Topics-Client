package io.benreynolds.hottopics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import io.benreynolds.hottopics.packets.LeaveChatroomRequestPacket;
import io.benreynolds.hottopics.packets.LeaveChatroomResponsePacket;
import io.benreynolds.hottopics.packets.ReceiveMessagePacket;
import io.benreynolds.hottopics.packets.SendMessagePacket;

public class ChatroomActivity extends ConnectedActivity {

    /** TAG used in Logcat messages outputted by {@code ChatroomActivity}. */
    private static final String TAG = ChatroomActivity.class.getSimpleName();

    /** Singleton instance of the {@code WebSocketCommunicator} used for network communications. */
    private static final WebSocketCommunicator WEB_SOCKET_COMMUNICATOR = WebSocketCommunicator.getInstance();

    /** List of chat messages. */
    final ArrayList<ReceiveMessagePacket> mMessages = new ArrayList<>();
    private ChatMessageListAdapter mMessageListAdapter;
    private ListView mMessageList;

    /** Chat message field. */
    private EditText mMessage;

    @Override
    public void onBackPressed() {
        WEB_SOCKET_COMMUNICATOR.sendPacket(new LeaveChatroomRequestPacket());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        // Setup the chatroom message list adapter
        mMessageListAdapter = new ChatMessageListAdapter(this, mMessages);
        mMessageList = findViewById(R.id.lstMessages);
        mMessageList.setAdapter(mMessageListAdapter);
        mMessage = findViewById(R.id.txtMessage);

        // Set the chatroom name header text.
        TextView lblTitle = findViewById(R.id.lblTitle);
        lblTitle.setText(getIntent().getStringExtra(RoomListActivity.ROOM_NAME_EXTRA));

        // Assign the send button's OnClick listener.
        findViewById(R.id.btnSend).setOnClickListener(new BtnSendOnClickListener());

        // Instantiate and add observers to the WebSocketCommunicator that handle leaving the chatroom and receiving messages.
        LeaveChatroomResponseObserver leaveChatroomResponseObserver = new LeaveChatroomResponseObserver();
        WEB_SOCKET_COMMUNICATOR.addObserver(leaveChatroomResponseObserver);

        ReceiveMessageObserver receiveMessageObserver = new ReceiveMessageObserver();
        WEB_SOCKET_COMMUNICATOR.addObserver(receiveMessageObserver);
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

    /**
     * {@code LeaveChatroomResponseObserver} is responsible for handling responses to '{@code LeaveChatroomRequestPacket}'s.
     */
    private class LeaveChatroomResponseObserver implements PacketObserver<LeaveChatroomResponsePacket> {

        @Override
        public void update(LeaveChatroomResponsePacket packet) {
            // If the response received contained a 'false' value, an unknown error has occurred and the connection to
            // the Hot Topics server is broken.
            if(!packet.getResponse()) {
                WEB_SOCKET_COMMUNICATOR.disconnect();
                return;
            }

            // Take the user back to the RoomList activity.
            Intent roomListActivity = new Intent(ChatroomActivity.this, RoomListActivity.class);
            startActivity(roomListActivity);
        }

        @Override
        public Class<LeaveChatroomResponsePacket> packetType() {
            return LeaveChatroomResponsePacket.class;
        }

    }

    /**
     * {@code ReceiveMessageObserver} is responsible for processing '{@code ReceiveMessagePacket}'s.
     */
    private class ReceiveMessageObserver implements PacketObserver<ReceiveMessagePacket> {

        @Override
        public void update(ReceiveMessagePacket packet) {
            // Add the newly received message to the message list.
            mMessages.add(packet);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Notify the message list adapter that a new message has been added.
                    mMessageListAdapter.notifyDataSetChanged();
                    // Scroll to message list the latest entry.
                    mMessageList.setSelection(mMessageList.getCount() - 1);
                }
            });
        }

        @Override
        public Class<ReceiveMessagePacket> packetType() {
            return ReceiveMessagePacket.class;
        }

    }

}
