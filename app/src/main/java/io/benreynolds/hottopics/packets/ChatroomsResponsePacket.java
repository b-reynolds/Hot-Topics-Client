package io.benreynolds.hottopics.packets;

import com.google.gson.annotations.SerializedName;

/**
 * {code ChatroomsResponsePacket} sent to client devices as a response to {@code ChatroomsRequestPacket}.
 */
public class ChatroomsResponsePacket extends Packet {

    /** Attempts to store the '{@code ChatroomsResponsePacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(ChatroomsResponsePacket.class, null);

    /**
     * '{@code Chatroom}'s that are available for clients to join.
     */
    @SerializedName("chatrooms")
    private Chatroom[] mChatrooms;

    /**
     * @param chatrooms '{@code Chatroom}'s that are available for clients to join.
     */
    public ChatroomsResponsePacket(Chatroom[] chatrooms) {
        mId = ID;
        mChatrooms = chatrooms;
    }

    /**
     * Returns {@code true} if the {@code ChatroomsResponsePacket} contains a valid ID and at least one chatroom.
     * @return {@code true} if the {@code ChatroomsResponsePacket} contains a valid ID and at least one chatroom.
     */
    @Override
    public boolean isValid() {
        return mId != null && mChatrooms != null && mChatrooms.length != 0;
    }

    /** Returns the '{@code Chatroom}'s that are available for clients to join.
     * @return '{@code Chatroom}'s that are available for clients to join.
     */
    public Chatroom[] getChatrooms() {
        return mChatrooms;
    }

}
