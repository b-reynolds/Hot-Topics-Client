package io.benreynolds.hottopics.packets;

public class AcknowledgementResponsePacket extends Packet {

    /** Attempts to store the '{@code AcknowledgementResponsePacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(AcknowledgementResponsePacket.class, null);

    public AcknowledgementResponsePacket() {
        mId = ID;
    }

    /**
     * Returns {@code true} if the {@code AcknowledgementResponsePacket} contains a valid ID.
     * @return {@code true} if the {@code AcknowledgementResponsePacket} contains a valid ID.
     */
    @Override
    public boolean isValid() {
        return mId != null;
    }

}
