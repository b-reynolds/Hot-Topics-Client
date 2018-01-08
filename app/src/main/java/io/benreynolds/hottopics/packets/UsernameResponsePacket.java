package io.benreynolds.hottopics.packets;

/**
 * {code UsernameResponsePacket} sent to client devices as a response to {@code UsernameRequestPacket}.
 */
public class UsernameResponsePacket extends BooleanResponsePacket {

    /** Attempts to store a the '{@code UsernameResponsePacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(UsernameResponsePacket.class,
            null);

    /**
     * @param response Server's response.
     */
    public UsernameResponsePacket(final boolean response) {
        super(response);
        mId = ID;
    }

}
