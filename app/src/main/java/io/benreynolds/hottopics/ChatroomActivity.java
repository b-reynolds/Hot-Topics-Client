package io.benreynolds.hottopics;

import android.content.Intent;
import android.content.RestrictionEntry;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import io.benreynolds.hottopics.packets.Chatroom;
import io.benreynolds.hottopics.packets.ChatroomUserCountUpdatePacket;
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

    private TextView lblTitle;
    private TextView lblUsers;


    private Chatroom mChatroom;


    private LeaveChatroomResponseObserver leaveChatroomResponseObserver;
    private ReceiveMessageObserver receiveMessageObserver;
    private ChatroomUserCountUpdateObserver chatroomUserCountUpdateObserver;

    @Override
    public void onBackPressed() {
        WEB_SOCKET_COMMUNICATOR.sendPacket(new LeaveChatroomRequestPacket());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        getActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.hot_topics_blue)));
        getActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + getString(R.string.app_name) + "</font>"));

        // Setup the chatroom message list adapter
        mMessageListAdapter = new ChatMessageListAdapter(this, mMessages);
        mMessageList = findViewById(R.id.lstMessages);
        mMessageList.setAdapter(mMessageListAdapter);
        mMessage = findViewById(R.id.txtMessage);

        mChatroom = (Chatroom) getIntent().getSerializableExtra(RoomListActivity.EXTRA_ROOM);

        // Set the chatroom name header text.
        lblTitle = findViewById(R.id.lblTitle);
        lblTitle.setText(mChatroom.getName());

        lblUsers = findViewById(R.id.lblUsers);
        lblUsers.setText(String.format("%s User(s)", mChatroom.getSize() + 1));

        // Add the newly received message to the message list.
        mMessages.addAll(mChatroom.getMessages());

        // Notify the message list adapter that a new message has been added.
        mMessageListAdapter.notifyDataSetChanged();

        // Scroll to message list the latest entry.
        mMessageList.setSelection(mMessageList.getCount() - 1);

        // Assign the send button's OnClick listener.
        findViewById(R.id.btnSend).setOnClickListener(new BtnSendOnClickListener());

        // Instantiate and add observers to the WebSocketCommunicator that handle leaving the chatroom and receiving messages.
        leaveChatroomResponseObserver = new LeaveChatroomResponseObserver();
        WEB_SOCKET_COMMUNICATOR.addObserver(leaveChatroomResponseObserver);

        receiveMessageObserver = new ReceiveMessageObserver();
        WEB_SOCKET_COMMUNICATOR.addObserver(receiveMessageObserver);

        chatroomUserCountUpdateObserver = new ChatroomUserCountUpdateObserver();
        WEB_SOCKET_COMMUNICATOR.addObserver(chatroomUserCountUpdateObserver);
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
            SendMessagePacket sendMessagePacket = new SendMessagePacket(mMessage.getText().toString().trim());

            if(sendMessagePacket.getMessage().isEmpty()) {
                return;
            }

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

            WEB_SOCKET_COMMUNICATOR.removeObserver(receiveMessageObserver);
            WEB_SOCKET_COMMUNICATOR.removeObserver(leaveChatroomResponseObserver);
            WEB_SOCKET_COMMUNICATOR.removeObserver(chatroomUserCountUpdateObserver);

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

    /**
     * {@code ChatroomUserCountUpdateObserver} is responsible for processing '{@code ChatroomUserCountUpdatePacket}'s.
     */
    private class ChatroomUserCountUpdateObserver implements PacketObserver<ChatroomUserCountUpdatePacket> {

        @Override
        public void update(final ChatroomUserCountUpdatePacket packet) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    lblTitle.setText(mChatroom.getName());
                    lblUsers.setText(String.format("%s User(s)", packet.getResponse()));
                }
            });
        }

        @Override
        public Class<ChatroomUserCountUpdatePacket> packetType() {
            return ChatroomUserCountUpdatePacket.class;
        }

    }

}
