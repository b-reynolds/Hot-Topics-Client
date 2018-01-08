package io.benreynolds.hottopics.packets;

import com.google.gson.annotations.SerializedName;

/**
 * {@code ReceiveMessagePacket} is sent by the server to client devices when a new message is ready to be received.
 */
public class ReceiveMessagePacket extends Packet {

    /** Attempts to store a the '{@code SendMessagePacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(ReceiveMessagePacket.class,
            null);

    /** Client's name. */
    @SerializedName("author")
    private String mAuthor;

    /** Client's message. */
    @SerializedName("message")
    private String mMessage;

    /**
     * @param author Client's name.
     * @param message Client's message.
     */
    public ReceiveMessagePacket(final String author, final String message) {
        mId = ID;
        mAuthor = author;
        mMessage = message;
    }

    /**
     * Returns the client's name.
     * @return Client's name.
     */
    public String getAuthor() {
        return mAuthor;
    }

    /**
     * Returns the client's message.
     * @return Client's message.
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * Returns {@code true} if the {@code ReceiveMessagePacket} contains a valid ID, author and message. Messages and
     * authors are considered valid if they are non-null and not empty.
     * @return {@code true} if the {@code ReceiveMessagePacket} contains a valid ID, author and message.
     */
    @Override
    public boolean isValid() {
        return mId != null && mAuthor != null && !mAuthor.isEmpty() && mMessage != null && !mMessage.isEmpty();
    }

}
