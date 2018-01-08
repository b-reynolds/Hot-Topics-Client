package io.benreynolds.hottopics;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.benreynolds.hottopics.packets.Chatroom;
import io.benreynolds.hottopics.packets.ChatroomsRequestPacket;
import io.benreynolds.hottopics.packets.ChatroomsResponsePacket;

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

}
