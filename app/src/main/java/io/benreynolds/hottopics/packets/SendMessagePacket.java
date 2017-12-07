package io.benreynolds.hottopics.packets;

import com.google.gson.annotations.SerializedName;

/**
 * {@code SendMessagePacket} is sent by client devices when they attempt to send a message within a chatroom.
 */
public class SendMessagePacket extends Packet {

    /** Attempts to store a static reference to the '{@code SendMessagePacket}'s ID (as determined by the
     * {@code PacketIdentifier}).
     */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(SendMessagePacket.class, null);

    /** Client's message. */
    @SerializedName("message")
    private String mMessage;

    /**
     * @param message Client's message.
     */
    public SendMessagePacket(final String message) {
        mId = ID;
        mMessage = message;
    }

    /**
     * Returns the client's message.
     * @return Client's message.
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * Returns {@code true} if the {@code SendMessagePacket} contains a valid ID and message. Messages are considered
     * valid if they are non-null and not empty.
     * @return {@code true} if the {@code SendMessagePacket} contains a valid ID and message.
     */
    @Override
    public boolean isValid() {
        return mId != null && mMessage != null && !mMessage.isEmpty();
    }

}
