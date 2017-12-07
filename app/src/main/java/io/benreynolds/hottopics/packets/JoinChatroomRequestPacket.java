package io.benreynolds.hottopics.packets;

import com.google.gson.annotations.SerializedName;

/**
 * {code ChatroomsRequestPacket} sent by client devices when attempting to join a {@code Chatroom}.
 */
public class JoinChatroomRequestPacket extends Packet {

    /** Attempts to store the '{@code JoinChatroomRequestPacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(JoinChatroomRequestPacket.class,
            null);

    /** Name of the {@code Chatroom} to join. */
    @SerializedName("chatroom_name")
    private String mChatroomName;

    /**
     * @param chatroomName Name of the {@code Chatroom} to join.
     */
    public JoinChatroomRequestPacket(final String chatroomName) {
        mId = ID;
        mChatroomName = chatroomName;
    }

    /**
     * Returns {@code true} if the {@code JoinChatroomRequestPacket} contains a valid ID and chatroom name. Chatroom
     * names are considered valid if they are non-null and not empty.
     * @return {@code true} if the {@code JoinChatroomRequestPacket} contains a valid ID and chatroom name.
     */
    @Override
    public boolean isValid() {
        return mId != null && mChatroomName != null && !mChatroomName.isEmpty();
    }

    /**
     * @return Name of the {@code Chatroom} to join.
     */
    public String getChatroomName() {
        return mChatroomName;
    }

}
