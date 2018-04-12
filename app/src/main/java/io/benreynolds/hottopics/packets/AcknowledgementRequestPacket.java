package io.benreynolds.hottopics.packets;

public class AcknowledgementRequestPacket extends Packet {

    /** Attempts to store the '{@code AcknowledgementRequestPacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(AcknowledgementRequestPacket.class, null);

    public AcknowledgementRequestPacket() {
        mId = ID;
    }

    /**
     * Returns {@code true} if the {@code AcknowledgementRequestPacket} contains a valid ID.
     * @return {@code true} if the {@code AcknowledgementRequestPacket} contains a valid ID.
     */
    @Override
    public boolean isValid() {
        return mId != null;
    }

}
