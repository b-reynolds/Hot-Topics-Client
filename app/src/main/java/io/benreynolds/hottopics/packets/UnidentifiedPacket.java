package io.benreynolds.hottopics.packets;

import java.util.Map;
import java.util.Objects;

/**
 * {@code UnidentifiedPacket} instances are created by deserializing JSON data with Google's GSON library.
 * {@code UnidentifiedPacket} provides a {@code getType} method that can be used to identify a '{@code Packet}'s type.
 */
public class UnidentifiedPacket extends Packet {

    /**
     * Attempts to identify and returns the {@code Class} type of the {@code Packet} using {@code PacketIdentifier}.
     * If no matching {@code Class} type can be found, returns {@code null}.
     * @return {@code Class} type of the {@code Packet} using {@code PacketIdentifier}. If no matching {@code Class}
     * type can be found, returns {@code null}.
     */
    public Class<?> getType() {
        for(Map.Entry<Class<? extends Packet>, Integer> entry : PacketIdentifier.PACKET_IDS.entrySet()) {
            if(Objects.equals(entry.getValue(), mId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the {@code UnidentifiedPacket} contains a non-null ID value.
     * @return {@code true} if the {@code UnidentifiedPacket} contains a non-null ID value.
     */
    @Override
    public boolean isValid() {
        return mId != null;
    }

}

