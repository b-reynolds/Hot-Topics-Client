package io.benreynolds.hottopics.packets;

/**
 * {code LeaveChatroomResponsePacket} sent to client devices as a response to {@code LeaveChatroomRequestPacket}.
 */
public class LeaveChatroomResponsePacket extends BooleanResponsePacket {

    /** Attempts to store the '{@code LeaveChatroomResponsePacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(LeaveChatroomResponsePacket.class, null);

    /**
     * @param response Server's response.
     */
    public LeaveChatroomResponsePacket(final boolean response) {
        super(response);
        mId = ID;
    }

}
