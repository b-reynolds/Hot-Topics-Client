package io.benreynolds.hottopics;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

import io.benreynolds.hottopics.packets.Chatroom;

import static android.support.v4.content.ContextCompat.startActivity;

public class ChatroomListAdapter extends ArrayAdapter<Chatroom> {

    private Context mContext;

    ChatroomListAdapter(Context context, ArrayList<Chatroom> chatrooms) {
        super(context, 0, chatrooms);
        mContext = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Chatroom chatroom = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.chatroom_row, parent,
                    false);
        }

        TextView tvChatroomName = convertView.findViewById(R.id.tvChatroomName);
        TextView tvUsersInChatroom = convertView.findViewById(R.id.tvUsersInChatroom);
        Button ibSearch = convertView.findViewById(R.id.ibSearch);
        ibSearch.setFocusable(false);



        if(chatroom == null) {
            tvChatroomName.setText(R.string.null_string);
            tvUsersInChatroom.setText(R.string.null_string);
        }
        else {
            tvChatroomName.setText(chatroom.getName());
            tvUsersInChatroom.setText(String.format("%s User(s)", chatroom.getSize()));
            ibSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                    intent.putExtra(SearchManager.QUERY, chatroom.getName()); // query contains search string
                    mContext.startActivity(intent);
                }
            });
        }



        return convertView;
    }

}