package io.benreynolds.hottopics;

import io.benreynolds.hottopics.packets.Packet;

public interface PacketObserver<T extends Packet> {

    void update(T packet);
    Class<T> packetType();

}