package io.benreynolds.hottopics.packets;

/**
 * {code ChatroomUserCountUpdatePacket} sent to client devices to update them on the amount of users that are in a chatroom.
 */
public class ChatroomUserCountUpdatePacket extends IntegerResponsePacket {

    /** Attempts to store the '{@code IntegerResponsePacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(ChatroomUserCountUpdatePacket.class, null);

    /**
     * @param response Server's response.
     */
    public ChatroomUserCountUpdatePacket(int response) {
        super(response);
        mId = ID;
    }

}
