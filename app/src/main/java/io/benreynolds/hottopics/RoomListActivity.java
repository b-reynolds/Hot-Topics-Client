package io.benreynolds.hottopics;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.benreynolds.hottopics.packets.Chatroom;
import io.benreynolds.hottopics.packets.ChatroomsRequestPacket;
import io.benreynolds.hottopics.packets.ChatroomsResponsePacket;
import io.benreynolds.hottopics.packets.JoinChatroomRequestPacket;
import io.benreynolds.hottopics.packets.JoinChatroomResponsePacket;

public class RoomListActivity extends AppCompatActivity {

    private static final String TAG = RoomListActivity.class.getSimpleName();

    private WebSocketCommunicator mWebSocketCommunicator = WebSocketCommunicator.getInstance();

    final List<String> mChatrooms = new ArrayList<>();
    private ArrayAdapter<String> mRoomListAdapter;

    private ListView mRoomList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);

        mRoomListAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, mChatrooms);

        mRoomList = findViewById(R.id.lstRooms);
        mRoomList.setAdapter(mRoomListAdapter);

        new Thread(new PopulateChatroomList()).start();

        mRoomList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                String selectedRoom = (String)mRoomList.getItemAtPosition(position);
                Log.d(TAG, "Selected: " + selectedRoom);
                new Thread(new JoinRoom(selectedRoom)).start();
            }

        });

    }

    public class PopulateChatroomList implements Runnable {

        @Override
        public void run() {
            mWebSocketCommunicator.sendPacket(new ChatroomsRequestPacket());
            while (!Thread.currentThread().isInterrupted()) {
                ChatroomsResponsePacket responsePacket = mWebSocketCommunicator.pollPacket(ChatroomsResponsePacket.class);
                if (responsePacket == null) {
                    continue;
                }

                for(Chatroom chatroom : responsePacket.getChatrooms()) {
                    mChatrooms.add(chatroom.getName());
                }
                mRoomListAdapter.notifyDataSetChanged();
                break;
            }
        }

    }

    public class JoinRoom implements Runnable {

        private String mRoomName;

        public JoinRoom(final String roomName) {
            mRoomName = roomName;
        }

        @Override
        public void run() {
            mWebSocketCommunicator.sendPacket(new JoinChatroomRequestPacket(mRoomName));
            while (!Thread.currentThread().isInterrupted()) {
                JoinChatroomResponsePacket responsePacket = mWebSocketCommunicator.pollPacket(JoinChatroomResponsePacket.class);
                if (responsePacket == null) {
                    continue;
                }

                if(!responsePacket.getResponse()) {
                    break;
                }

                Intent chatRoom = new Intent(RoomListActivity.this, ChatroomActivity.class);
                chatRoom.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(chatRoom);
            }
        }
    }

}
