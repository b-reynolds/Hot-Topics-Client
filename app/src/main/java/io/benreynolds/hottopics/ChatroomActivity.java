package io.benreynolds.hottopics;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.benreynolds.hottopics.packets.Chatroom;
import io.benreynolds.hottopics.packets.ChatroomUserCountUpdatePacket;
import io.benreynolds.hottopics.packets.LeaveChatroomRequestPacket;
import io.benreynolds.hottopics.packets.LeaveChatroomResponsePacket;
import io.benreynolds.hottopics.packets.ReceiveMessagePacket;
import io.benreynolds.hottopics.packets.SendMessagePacket;

/**
 * {@code ChatroomActivity} allows users to send and receive messages within a {@code Chatroom}.
 * It is navigated to by {@code RoomListActivity} when a user selects a chatroom.
 */
public class ChatroomActivity extends ConnectedActivity {

    /** TAG used in Logcat logs outputted by {@code ChatroomActivity}. */
    private static final String TAG = ChatroomActivity.class.getSimpleName();

    /** Singleton instance of the {@code WebSocketCommunicator} used for network communications. */
    private static final WebSocketCommunicator WEB_SOCKET_COMMUNICATOR = WebSocketCommunicator.getInstance();

    /** Alpha value used to fade out the chatroom's controls whilst waiting for a LeaveChatroomResponse (see onBackPressed) */
    private static final float ALPHA_FADE_OUT = 0.33f;

    /** Stores handlers that are attached to the WebSocketCommunicator throughout this activity (for mass removal upon cleanup) */
    private Set<PacketHandler> mPacketHandlers = new HashSet<>();

    /** Currently active chatroom */
    private Chatroom mActiveChatroom;

    /** Input field that users's use to type messages */
    private EditText etMessageBox;
    /** Text that displays the active chatroom's name */
    private TextView tvChatroomName;
    /** Text that displays the amount of users present in the active chatroom */
    private TextView tvUsersInChatroom;

    /** Progress bar that is displayed whilst leaving the active chatroom */
    private ProgressBar pbLeavingChatroom;
    /** Constraint layout that contains all of the activities' views other than the progress bar (used to fade alpha whilst leaving the active chatroom) */
    private ConstraintLayout clChatroom;

    /** List view and related objects used to display chat messages. */
    final ArrayList<ReceiveMessagePacket> mMessages = new ArrayList<>();
    private ChatMessageListAdapter mMessageListAdapter;
    private ListView lvMessageFeed;

    @Override
    public void onBackPressed() {
        // Fade the alpha value of the chatroom views and make them non-interactive.
        clChatroom.setAlpha(ALPHA_FADE_OUT);
        for(int i = 0; i < clChatroom.getChildCount(); i++) {
            clChatroom.getChildAt(i).setEnabled(false);
        }

        // Display the progress bar
        pbLeavingChatroom.setVisibility(ProgressBar.VISIBLE);

        // Send a request to the server to be removed from the active chatroom (see LeaveChatroomResponseHandler).
        WEB_SOCKET_COMMUNICATOR.sendPacket(new LeaveChatroomRequestPacket());

        // TODO: What happens if no LeaveChatroomResponsePacket is received? (Implement a Runnable with a timeout period?)
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        // Set the action bar background and text colour.
        if(getActionBar() != null) {
            getActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.hot_topics_blue)));
            getActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + getString(R.string.app_name) + "</font>"));
        }

        // Retrieve the active chatroom from the serialized extra stored upon room selection.
        mActiveChatroom = (Chatroom) getIntent().getSerializableExtra(RoomListActivity.EXTRA_ROOM);

        // Display the active chatroom's name.
        tvChatroomName = findViewById(R.id.tvChatroomName);
        tvChatroomName.setText(mActiveChatroom.getName());

        // Display the amount of users present within the active chatroom (incremented to account for this user joining).
        tvUsersInChatroom = findViewById(R.id.tvUsersInChatroom);
        tvUsersInChatroom.setText(String.format("%s User(s)", mActiveChatroom.getSize() + 1));

        // Setup the chat message feed and related objects.
        mMessageListAdapter = new ChatMessageListAdapter(this, mMessages);
        lvMessageFeed = findViewById(R.id.lvMessageFeed);
        lvMessageFeed.setAdapter(mMessageListAdapter);

        // Populate the chat message feed with any messages that have been cached for the active chatroom.
        mMessages.addAll(mActiveChatroom.getMessages());

        // Notify the message list adapter that new messages have been added and scroll to message feed the latest entry.
        mMessageListAdapter.notifyDataSetChanged();
        lvMessageFeed.setSelection(lvMessageFeed.getCount() - 1);

        // Obtain a reference to the chat message box widget.
        etMessageBox = findViewById(R.id.etMessageBox);
        // Obtain a reference to the progress bar widget.
        pbLeavingChatroom = findViewById(R.id.pbLeavingChatroom);
        // Obtain a reference to the constraint layout widget.
        clChatroom = findViewById(R.id.clChatroom);

        // Assign the send button's OnClick listener to handle the sending of messages.
        findViewById(R.id.btnSend).setOnClickListener(new BtnSendOnClickListener());

        // Add a ReceiveMessagePacketHandler to the WebSocketCommunicator that updates the chat feed when messages are received.
        ReceiveMessagePacketHandler receiveMessagePacketHandler = new ReceiveMessagePacketHandler();
        mPacketHandlers.add(receiveMessagePacketHandler);
        WEB_SOCKET_COMMUNICATOR.addHandler(receiveMessagePacketHandler);

        // Add a ChatroomUserCountUpdateHandler to the WebSocketCommunicator that updates the text displaying how many users are present within the active chatroom.
        ChatroomUserCountUpdateHandler chatroomUserCountUpdateHandler = new ChatroomUserCountUpdateHandler();
        mPacketHandlers.add(chatroomUserCountUpdateHandler);
        WEB_SOCKET_COMMUNICATOR.addHandler(chatroomUserCountUpdateHandler);

        // Add a ChatroomUserCountUpdateHandler to the WebSocketCommunicator that handles users requests to leave the active chatroom (see onBackButtonPressed()).
        LeaveChatroomResponsePacketHandler leaveChatroomResponsePacketHandler = new LeaveChatroomResponsePacketHandler();
        mPacketHandlers.add(leaveChatroomResponsePacketHandler);
        WEB_SOCKET_COMMUNICATOR.addHandler(leaveChatroomResponsePacketHandler);
    }

    /**
     * {@code BtnSendOnClickListener}'s {@code onClick} method is executed when '{@code ChatroomActivity}'s send button is pressed.
     * It sends the text written within the message box to the Hot Topics server and clears the message box.
     */
    private class BtnSendOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            // Obtain the user's message and trim any leading or trailing spaces.
            String messageToSend = etMessageBox.getText().toString().trim();

            // Construct a SendMessagePacket and, if valid, send it to the server.
            SendMessagePacket sendMessagePacket = new SendMessagePacket(messageToSend);
            if(sendMessagePacket.isValid()) {
                WEB_SOCKET_COMMUNICATOR.sendPacket(sendMessagePacket);
            }

            // Clear the message box.
            etMessageBox.setText("");
        }

    }

    /**
     * {@code LeaveChatroomResponsePacketHandler} is responsible for handling responses to '{@code LeaveChatroomRequestPacket}'s.
     */
    private class LeaveChatroomResponsePacketHandler implements PacketHandler<LeaveChatroomResponsePacket> {

        @Override
        public void update(LeaveChatroomResponsePacket packet) {
            // If the LeaveChatroomResponsePacket contains a 'false' value, an unexpected error has occurred and the connection to server is broken.
            if(!packet.getResponse()) {
                WEB_SOCKET_COMMUNICATOR.disconnect();
                return;
            }

            // Detach all of the 'PacketHandler's used in this activity from the WebSocketCommunicator.
            for(PacketHandler packetHandler : mPacketHandlers) {
                WEB_SOCKET_COMMUNICATOR.removeHandler(packetHandler);
            }

            // Start the chatroom list activity.
            startActivity(new Intent(ChatroomActivity.this, RoomListActivity.class));
        }

        @Override
        public Class<LeaveChatroomResponsePacket> packetType() {
            return LeaveChatroomResponsePacket.class;
        }

    }

    /**
     * {@code ReceiveMessagePacketHandler} is responsible for processing '{@code ReceiveMessagePacket}'s.
     * It adds messages to the chat feed when they are received.
     */
    private class ReceiveMessagePacketHandler implements PacketHandler<ReceiveMessagePacket> {

        @Override
        public void update(ReceiveMessagePacket packet) {
            // Add the message to the message feed.
            mMessages.add(packet);

            // Notify the message list adapter that a new message has been added and scroll the message feed to the latest entry.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessageListAdapter.notifyDataSetChanged();
                    lvMessageFeed.setSelection(lvMessageFeed.getCount() - 1);
                }
            });
        }

        @Override
        public Class<ReceiveMessagePacket> packetType() {
            return ReceiveMessagePacket.class;
        }

    }

    /**
     * {@code ChatroomUserCountUpdateHandler} is responsible for processing '{@code ChatroomUserCountUpdatePacket}'s.
     * It updates the text that displays the amount of users present within the room.
     */
    private class ChatroomUserCountUpdateHandler implements PacketHandler<ChatroomUserCountUpdatePacket> {

        @Override
        public void update(final ChatroomUserCountUpdatePacket packet) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvChatroomName.setText(mActiveChatroom.getName());
                    tvUsersInChatroom.setText(String.format("%s User(s)", packet.getResponse()));
                }
            });
        }

        @Override
        public Class<ChatroomUserCountUpdatePacket> packetType() {
            return ChatroomUserCountUpdatePacket.class;
        }

    }

}
