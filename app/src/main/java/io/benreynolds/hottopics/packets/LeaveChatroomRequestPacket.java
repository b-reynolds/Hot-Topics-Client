package io.benreynolds.hottopics.packets;

/**
 * {code LeaveChatroomRequestPacket} sent by client devices when attempting to leave a {@code Chatroom}.
 */
public class LeaveChatroomRequestPacket extends Packet {

    /** Attempts to store the '{@code LeaveChatroomRequestPacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(LeaveChatroomRequestPacket.class,
            null);

    public LeaveChatroomRequestPacket() {
        mId = ID;
    }

    /**
     * Returns {@code true} if the {@code LeaveChatroomRequestPacket} contains a valid ID.
     * @return {@code true} if the {@code LeaveChatroomRequestPacket} contains a valid ID.
     */
    @Override
    public boolean isValid() {
        return mId != null;
    }

}
