package io.benreynolds.hottopics.packets;

/**
 * {code JoinChatroomResponsePacket} sent to client devices as a response to {@code JoinChatroomRequestPacket}.
 */
public class JoinChatroomResponsePacket extends BooleanResponsePacket {

    /** Attempts to store the '{@code JoinChatroomResponsePacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(JoinChatroomResponsePacket.class, null);

    /**
     * @param response Server's response.
     */
    public JoinChatroomResponsePacket(final boolean response) {
        super(response);
        mId = ID;
    }

}
