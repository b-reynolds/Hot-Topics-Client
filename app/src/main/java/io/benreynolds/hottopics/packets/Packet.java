package io.benreynolds.hottopics.packets;

import com.google.gson.annotations.SerializedName;

/**
 * {@code Packet} is the base class extended by all of the Hot Topics {@code Packet} classes ({@code SendMessagePacket},
 * {@code ReceiveMessagePacket}) that are used for WebSocket communications. {@code Packet} classes are intended to be
 * serialized and deserialized by the client and server applications using Google's Gson library.
 */
public abstract class Packet {

    /** Unique ID of the {@code Packet} */
    @SerializedName("id")
    Integer mId = null;

    /**
     * Returns the unique ID of the {@code Packet}. IDs are assigned to {@code Packet} derivatives during the static
     * initialization of {@code PacketIdentifier}.
     * @return Unique ID of the {@code Packet}
     */
    public final Integer getId() {
        return mId;
    }

    /**
     * Returns {@code true} if the {@code Packet} contains the information required to complete its intended action.
     * All classes derived from {@code Packet} should provide a suitable implementation.
     * @return {@code true} if the {@code Packet} contains the information required to complete its intended action.
     */
    public abstract boolean isValid();

}
