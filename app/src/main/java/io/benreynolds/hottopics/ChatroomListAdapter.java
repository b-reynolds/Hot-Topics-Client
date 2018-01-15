package io.benreynolds.hottopics;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import io.benreynolds.hottopics.packets.Chatroom;

public class ChatroomListAdapter extends ArrayAdapter<Chatroom> {

    ChatroomListAdapter(Context context, ArrayList<Chatroom> chatrooms) {
        super(context, 0, chatrooms);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Chatroom chatroom = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.chatroom_row, parent,
                    false);
        }

        TextView roomName = convertView.findViewById(R.id.textView_roomName);
        TextView roomSize = convertView.findViewById(R.id.textView_roomSize);

        if(chatroom == null) {
            roomName.setText(R.string.null_string);
            roomSize.setText(R.string.null_string);
        }
        else {
            roomName.setText(chatroom.getName());
            roomSize.setText(String.format("%s User(s)", chatroom.getSize()));
        }

        return convertView;
    }

}