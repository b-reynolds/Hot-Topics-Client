package io.benreynolds.hottopics.packets;

/**
 * {code ChatroomsRequestPacket} sent by client devices when requesting a list of available '{@code Chatroom}'s.
 */
public class ChatroomsRequestPacket extends Packet {

    /** Attempts to store the '{@code ChatroomsRequestPacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(ChatroomsRequestPacket.class, null);

    public ChatroomsRequestPacket() {
        mId = ID;
    }

    /**
     * This method always returns {@code true} due to the packet requir
     * @return {@code true}.
     */
    @Override
    public boolean isValid() {
        return mId != null;
    }
}
