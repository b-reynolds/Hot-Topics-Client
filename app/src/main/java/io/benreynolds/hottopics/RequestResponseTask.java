package io.benreynolds.hottopics;

import io.benreynolds.hottopics.packets.Packet;

/**
 * {@code RequestResponseTask} sends a request {@code Packet} to the Hot Topics server and awaits a
 * response.
 * @param <T> Request {@code Packet} type.
 */
public class RequestResponseTask<T extends Packet> implements Runnable {

    /** Singleton instance of the {@code WebSocketCommunicator} used for network communications. */
    private static final WebSocketCommunicator WEB_SOCKET_COMMUNICATOR = WebSocketCommunicator.getInstance();

    private static final double TIMEOUT_PERIOD_DEFAULT = 5.0;

    /** Request {@code Packet} to send to the Hot Topics server. */
    private final T mRequestPacket;

    /** Response {@code Packet} to await. */
    private final Class<? extends Packet> mResponseType;

    /** Amount of time to await a response (in seconds). */
    private double mTimeoutPeriod;

    /** Response {@code Packet}. */
    private Packet mResponse;

    /**
     * @param requestPacket Request {@code Packet} to send to the Hot Topics server.
     * @param responseType Response {@code Packet} to await.
     * @param timeoutPeriod Amount of time to await a response (in seconds).
     */
    RequestResponseTask(final T requestPacket, final Class<? extends Packet> responseType, final double timeoutPeriod) {
        mRequestPacket = requestPacket;
        mResponseType = responseType;
        mTimeoutPeriod = timeoutPeriod;
    }

    /**
     * @param requestPacket Request {@code Packet} to send to the Hot Topics server.
     * @param responseType Response {@code Packet} to await.
     */
    RequestResponseTask(final T requestPacket, final Class<? extends Packet> responseType) {
        mRequestPacket = requestPacket;
        mResponseType = responseType;
        mTimeoutPeriod = TIMEOUT_PERIOD_DEFAULT;
    }

    @Override
    public void run() {
        Timer mTimer = new Timer(mTimeoutPeriod);
        WEB_SOCKET_COMMUNICATOR.sendPacket(mRequestPacket);
        mTimer.start();

        while(!Thread.currentThread().isInterrupted() && !mTimer.hasElapsed()) {
            Packet responsePacket = WEB_SOCKET_COMMUNICATOR.pollPacket(mResponseType);
            if(responsePacket != null) {
                mResponse = responsePacket;
                break;
            }
        }
    }

    /**
     * Returns the response {@code Packet} that was received.
     * @return Response {@code Packet} that was received. If no response was received,
     * returns {@code null}.
     */
    Packet getResponse() {
        return mResponse;
    }

}
