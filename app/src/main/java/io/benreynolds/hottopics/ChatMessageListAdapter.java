package io.benreynolds.hottopics;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import io.benreynolds.hottopics.packets.ReceiveMessagePacket;

public class ChatMessageListAdapter extends ArrayAdapter<ReceiveMessagePacket> {

    ChatMessageListAdapter(Context context, ArrayList<ReceiveMessagePacket> receiveMessagePackets) {
        super(context, 0, receiveMessagePackets);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ReceiveMessagePacket receiveMessagePacket = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.chat_message_row,
                    parent, false);
        }

        TextView userName = convertView.findViewById(R.id.textView_userName);
        TextView message = convertView.findViewById(R.id.textView_Message);

        if(receiveMessagePacket == null || !receiveMessagePacket.isValid()) {
            userName.setText(R.string.null_string);
            message.setText(R.string.null_string);
        }
        else {
            userName.setText(receiveMessagePacket.getAuthor());
            message.setText(receiveMessagePacket.getMessage());
        }

        return convertView;
    }

}