package io.benreynolds.hottopics;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import io.benreynolds.hottopics.packets.ReceiveMessagePacket;
import io.benreynolds.hottopics.packets.SendMessagePacket;

public class ChatroomActivity extends AppCompatActivity {

    private WebSocketCommunicator mWebSocketCommunicator = WebSocketCommunicator.getInstance();

    final List<String> mMessages = new ArrayList<>();
    private ArrayAdapter<String> mMessageListAdapter;

    private Button mBtnSend;
    private EditText mMessage;


    private ListView mMessageList;
    public class UpdateChatFeed implements Runnable {

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                ReceiveMessagePacket receiveMessagePacket = mWebSocketCommunicator.pollPacket(ReceiveMessagePacket.class);
                if(receiveMessagePacket == null) {
                    Thread.yield();
                    continue;
                }

                mMessages.add(receiveMessagePacket.getAuthor() + ": " + receiveMessagePacket.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMessageListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        mMessageListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mMessages);
        mMessageList = findViewById(R.id.lstMessages);
        mMessageList.setAdapter(mMessageListAdapter);

        mMessage = findViewById(R.id.txtMessage);

        mBtnSend = findViewById(R.id.btnSend);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendMessagePacket sendMessagePacket = new SendMessagePacket(mMessage.getText().toString());
                mMessage.setText("");

                mWebSocketCommunicator.sendPacket(sendMessagePacket);
            }
        });

       new Thread(new UpdateChatFeed()).start();


    }

}
