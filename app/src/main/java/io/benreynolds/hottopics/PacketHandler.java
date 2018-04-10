package io.benreynolds.hottopics;

import io.benreynolds.hottopics.packets.Packet;

public interface PacketHandler<T extends Packet> {

    void update(T packet);
    Class<T> packetType();

}