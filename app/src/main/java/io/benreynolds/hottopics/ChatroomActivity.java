package io.benreynolds.hottopics;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

    private static final String TAG = ChatroomActivity.class.getSimpleName();

    private WebSocketCommunicator mWebSocketCommunicator = WebSocketCommunicator.getInstance();

    final List<String> mMessages = new ArrayList<>();
    private ArrayAdapter<String> mMessageListAdapter;

    private Button mBtnSend;
    private EditText mMessage;

    private ListView mMessageList;

    private Thread tLeaveChatroom;
    private Thread tUpdateChatFeed;
    private Thread tCheckConnectionStatus;

    public class UpdateChatFeed implements Runnable {

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                ReceiveMessagePacket receiveMessagePacket = mWebSocketCommunicator.pollPacket(ReceiveMessagePacket.class);
                if(receiveMessagePacket == null) {
                    continue;
                }

                mMessages.add(receiveMessagePacket.getAuthor() + ": " + receiveMessagePacket.getMessage());
                Log.i(TAG, "Added Packet message!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMessageListAdapter.notifyDataSetChanged();
                        mMessageList.setSelection(mMessageList.getCount() - 1);
                    }
                });

            }
        }

    }

    @Override
    public void onBackPressed() {
        if(tLeaveChatroom != null && tLeaveChatroom.isAlive()) {
            tUpdateChatFeed.interrupt();
            tCheckConnectionStatus.interrupt();
            Intent mainActivity = new Intent(ChatroomActivity.this, RoomListActivity.class);
            startActivity(mainActivity);
            return;
        }

        tLeaveChatroom = new Thread(new LeaveChatroomTask());
        tLeaveChatroom.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        mMessageListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mMessages);
        mMessageList = findViewById(R.id.lstMessages);
        mMessageList.setAdapter(mMessageListAdapter);

        mMessage = findViewById(R.id.txtMessage);

        TextView lblTitle = findViewById(R.id.lblTitle);
        lblTitle.setText(getIntent().getStringExtra(RoomListActivity.ROOM_NAME_EXTRA));

        mBtnSend = findViewById(R.id.btnSend);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendMessagePacket sendMessagePacket = new SendMessagePacket(mMessage.getText().toString());
                mMessage.setText("");

                mWebSocketCommunicator.sendPacket(sendMessagePacket);
            }
        });

        tCheckConnectionStatus = new Thread(new CheckConnectionStatus());
        tCheckConnectionStatus.start();

        tUpdateChatFeed = new Thread(new UpdateChatFeed());
        tUpdateChatFeed.start();
    }

    public class LeaveChatroomTask implements Runnable {

        @Override
        public void run() {
            mWebSocketCommunicator.sendPacket(new LeaveChatroomRequestPacket());
            while(!Thread.currentThread().isInterrupted()) {
                LeaveChatroomResponsePacket responsePacket = mWebSocketCommunicator.pollPacket(LeaveChatroomResponsePacket.class);
                if(responsePacket == null) {
                    continue;
                }

                if(responsePacket.getResponse()) {
                   runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           onBackPressed();
                       }
                   });
                }
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
                if (!mWebSocketCommunicator.isConnected()) {
                    Log.w(TAG, "Connection Lost Unexpectedly.");
                    Intent mainActivity = new Intent(ChatroomActivity.this, LoginActivity.class);
                    startActivity(mainActivity);
                    break;
                }
                Thread.yield();
            }
        }

    }

}
