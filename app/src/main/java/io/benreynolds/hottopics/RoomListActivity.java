package io.benreynolds.hottopics;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;

import io.benreynolds.hottopics.packets.Chatroom;
import io.benreynolds.hottopics.packets.ChatroomsRequestPacket;
import io.benreynolds.hottopics.packets.ChatroomsResponsePacket;
import io.benreynolds.hottopics.packets.JoinChatroomRequestPacket;
import io.benreynolds.hottopics.packets.JoinChatroomResponsePacket;

/**
 * {@code RoomListActivity} handles the retrieval of and connection to rooms from the Hot Topics
 * server. Transitions to {@code ChatroomActivity} and {@code LoginActivity}.
 */
public class RoomListActivity extends ConnectedActivity {

    /** String key for the name of the chatroom sent to {@code ChatroomActivity} */
    public static final String EXTRA_ROOM = "ROOM";

    /** Singleton instance of the {@code WebSocketCommunicator} used for network communications. */
    private static final WebSocketCommunicator WEB_SOCKET_COMMUNICATOR = WebSocketCommunicator.getInstance();

    /** List of available chatrooms. */
    final ArrayList<Chatroom> mChatrooms = new ArrayList<>();
    private ChatroomListAdapter mChatroomsAdapter;
    private ListView mChatroomList;

    /** Observer used to handle the receipt of available Chatrooms */
    private ChatroomsResponsePacketHandler chatroomsResponsePacketHandler;

    /** Observer used to handle responses to 'JoinChatroomRequestPacket's */
    private JoinChatroomResponsePacketHandler joinChatroomResponsePacketHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);

        getActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.hot_topics_blue)));
        getActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + getString(R.string.app_name) + "</font>"));

        // Setup the chatroom list adapter
        mChatroomsAdapter = new ChatroomListAdapter(this, mChatrooms);
        mChatroomList = findViewById(R.id.lstRooms);
        mChatroomList.setAdapter(mChatroomsAdapter);

        // Assign the OnClick handler for handling room selection
        mChatroomList.setOnItemClickListener(new ChatroomListItemClickListener());

        // Instantiate and attach the observer that handles chatroom list responses
        chatroomsResponsePacketHandler = new ChatroomsResponsePacketHandler();
        WEB_SOCKET_COMMUNICATOR.addHandler(chatroomsResponsePacketHandler);

        // Send a ChatRoomsRequestPacket to the server
        WEB_SOCKET_COMMUNICATOR.sendPacket(new ChatroomsRequestPacket());
    }

    @Override
    public void onBackPressed() {
        // Remove all observers attached within this activity from the WebSocketCommunicator
        if(chatroomsResponsePacketHandler != null) {
            WEB_SOCKET_COMMUNICATOR.removeHandler(chatroomsResponsePacketHandler);
        }
        if(joinChatroomResponsePacketHandler != null) {
            WEB_SOCKET_COMMUNICATOR.removeHandler(joinChatroomResponsePacketHandler);
        }

        // TODO: Prompt the user to confirm/deny that they would like to disconnect from the server.
        // Disconnect from the Hot Topics server (In turn returning to the LoginActivity (see ConnectedActivity)
        WEB_SOCKET_COMMUNICATOR.disconnect();
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
                mChatroomList.setEnabled(state);
            }
        });
    }

    /**
     * {@code ChatroomListItemClickListener}'s {@code onItemClick} method is executed when an item
     * from the chatroom list is pressed. It sends a request to the Hot Topics server to join the
     * selected room.
     */
    private class ChatroomListItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            // Disable the UI controls
            setActivityState(false);

            // Get the name of the selected room from the chatroom list
            Chatroom selectedRoom = (Chatroom) mChatroomList.getItemAtPosition(position);

            // Instantiate and attach a JoinChatroomResponsePacketHandler to the WebSocketCommunicator to handle ChatroomRequestPacket responses
            joinChatroomResponsePacketHandler = new JoinChatroomResponsePacketHandler(selectedRoom.getName());
            WEB_SOCKET_COMMUNICATOR.addHandler(joinChatroomResponsePacketHandler);

            // Send a JoinChatroomRequestPacket to the Hot Topics server containing the name of the selected chatroom
            WEB_SOCKET_COMMUNICATOR.sendPacket(new JoinChatroomRequestPacket(selectedRoom.getName()));
        }

    }

    /**
     * {@code JoinChatroomResponsePacketHandler} is responsible for processing '{@code JoinChatroomResponse}'s.
     */
    private class JoinChatroomResponsePacketHandler implements PacketHandler<JoinChatroomResponsePacket> {

        /** Name of the chatroom the user wants to join */
        private final String mChatroomName;

        /**
         * @param chatroomName Name of the chatroom the user wants to join
         */
        JoinChatroomResponsePacketHandler(final String chatroomName) {
            mChatroomName = chatroomName;
        }

        @Override
        public void update(JoinChatroomResponsePacket packet) {
            // Remove this observer from the WebSocketCommunicator.
            WEB_SOCKET_COMMUNICATOR.removeHandler(this);

            // TODO: Improve handling of negative responses to 'JoinChatroomRequestPacket's (Are they even required?)
            if(!packet.getResponse()) {
                // Re-enable the UI controls
                setActivityState(true);
                return;
            }

            // Prepare transition to ChatroomActivity, include an extra containing the chatroom's name so that it can be used to display above the message feed.
            Intent chatRoomActivity = new Intent(RoomListActivity.this, ChatroomActivity.class);

            for(Chatroom chatroom : mChatrooms) {
                if(chatroom.getName().equals(mChatroomName)) {
                    chatRoomActivity.putExtra(EXTRA_ROOM, chatroom);
                    break;
                }
            }


            // Transition to the ChatroomActivity
            startActivity(chatRoomActivity);
        }

        @Override
        public Class<JoinChatroomResponsePacket> packetType() {
            return JoinChatroomResponsePacket.class;
        }

    }

    /**
     * {@code ChatroomsResponsePacketHandler} is responsible for processing '{@code ChatroomsResponsePacket}'s.
     */
    private class ChatroomsResponsePacketHandler implements PacketHandler<ChatroomsResponsePacket> {

        @Override
        public void update(ChatroomsResponsePacket packet) {
            // Remove all existing chatrooms from the chatroom list
            mChatrooms.clear();

            // Populate the chatroom list with the newly received data
            mChatrooms.addAll(Arrays.asList(packet.getChatrooms()));

            // Notify the room list adapter that the underlying data has changed and the list requires updating
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mChatroomsAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public Class<ChatroomsResponsePacket> packetType() {
            return ChatroomsResponsePacket.class;
        }

    }

}
