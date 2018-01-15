package io.benreynolds.hottopics;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.benreynolds.hottopics.packets.Packet;
import io.benreynolds.hottopics.packets.PacketIdentifier;
import io.benreynolds.hottopics.packets.UnidentifiedPacket;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * {@code WebSocketCommunicator}
 */
public class WebSocketCommunicator extends WebSocketListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    /** Indicates a normal closure, meaning that the purpose for which the connection was
     *  established has been fulfilled. */
    private static final int CLOSURE_NORMAL = 1000;

    /** Address of the Hot Topics server. */
    private static final String SERVER_ADDRESS = "ws://35.189.116.222:8025/hottopics/chat";

    /** Stores a references the singleton instance of {@code WebSocketCommunicator}. */
    private static volatile WebSocketCommunicator mInstance;

    /** Used to store the '{@code Packet}'s that the {@code WebSocketCommunicator} has received from
     *  the server. */
    private final Queue<Packet> mReceivedPackets = new ConcurrentLinkedQueue<>();

    /** WebSocket. */
    private WebSocket mWebSocket;

    /** {@code true} if the {@code WebSocketCommunicator} is currently connecting to the server. */
    private boolean mConnecting;

    /** {@code true} if the {@code WebSocketCommunicator} has a connection to the server. */
    private boolean mConnected;

    /**
     * Connects the {@code WebSocketCommunicator} to the server.
     */
    public void connect() {
        if(mConnected) {
            mWebSocket.close(CLOSURE_NORMAL, null);
        }

        mConnecting = true;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0,  TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(SERVER_ADDRESS)
                .build();

        mWebSocket = okHttpClient.newWebSocket(request, this);
        okHttpClient.dispatcher().executorService().shutdown();
    }

    /**
     * Disconnects the {@code WebSocketCommunicator} from the server.
     */
    public void disconnect() {
        if(mWebSocket != null) {
            mWebSocket.close(CLOSURE_NORMAL, null);
        }
        mConnected = false;
    }

    /** Private constructor used during {@code WebSocketCommunicator}'s singleton initialization
     * (see {@code getInstance()}. */
    private WebSocketCommunicator() {}

    /**
     * Invoked when a WebSocket has been accepted by the remote peer and may begin transmitting
     * messages.
     * @param webSocket Open WebSocket connection.
     * @param response HTTP response.
     */
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.d(TAG, String.format("onOpen() { WebSocket: \"%s\"," + " Response: \"%s\".", webSocket.toString(), response != null ? response.toString() : null));
        mConnecting = false;
        mConnected = true;
    }

    /**
     * Invoked when a text message has been received.
     * @param webSocket Open WebSocket connection.
     * @param message Received message.
     */
    @Override
    public void onMessage(WebSocket webSocket, String message) {
        UnidentifiedPacket unidentifiedPacket = PacketIdentifier.convertToPacket(message, UnidentifiedPacket.class);
        if (unidentifiedPacket == null || !unidentifiedPacket.isValid() || !PacketIdentifier.PACKET_IDS.containsValue(unidentifiedPacket.getId())) {
            Log.w(TAG, String.format("Unknown Packet Received: \"%s\".", message));
            return;
        }

        // TODO: ONLY ADD VALID PACKETS!!
        for (Map.Entry<Class<? extends Packet>, Integer> entry : PacketIdentifier.PACKET_IDS.entrySet()) {
            if (Objects.equals(unidentifiedPacket.getId(), entry.getValue())) {
                Packet convertedPacket = new Gson().fromJson(message, entry.getKey());
                if(!convertedPacket.isValid()) {
                    Log.w(TAG, String.format("Invalid Packet Received: \"%s\".", message));
                    break;
                }

                mReceivedPackets.add(convertedPacket);
                Log.i(TAG, String.format("Packet Received: \"%s\".", convertedPacket.toString()));
                break;
            }
        }
    }

    /**
     * Invoked when the remote peer has indicated that no more incoming messages will be transmitted.
     * @param webSocket Closing WebSocket connection.
     * @param code Status code as defined by Section 7.4 of RFC 6455.
     * @param reason Reason for shutting down.
     */
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        Log.d(TAG, String.format("onClosing() { WebSocket: \"%s\"," +
                " Code: \"%s\", Reason: \"%s\".", webSocket.toString(), code, reason));
    }

    /**
     * Invoked when both peers have indicated that no more messages will be transmitted and the
     * connection has been successfully released. No further calls to this listener will be made.
     * @param webSocket Closed WebSocket connection.
     * @param code Status code as defined by Section 7.4 of RFC 6455.
     * @param reason Reason for shutting down..
     */
    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        Log.d(TAG, String.format("onClosed() { WebSocket: \"%s\"," +
                " Code: \"%s\", Reason: \"%s\".", webSocket.toString(), code, reason));
        mConnected = false;
    }

    /**
     * Invoked when a web socket has been closed due to an error reading from or writing to the
     * network. Both outgoing and incoming messages may have been lost. No further calls to this
     * listener will be made.
     * @param webSocket Closed WebSocket connection.
     * @param throwable Thrown exception.
     * @param response HTTP response.
     */
    @Override
    public void onFailure(WebSocket webSocket, Throwable throwable, @Nullable Response response) {
        Log.e(TAG, String.format("onFailure() { WebSocket: \"%s\"," +
                "Throwable: \"%s\", Response: \"%s\".", webSocket.toString(), throwable,
                response != null ? response.toString() : null));
        mConnecting = false;
        mConnected = false;
    }

    /**
     * Sends a {@code Packet} to the server.
     * @param packet {@code Packet} to send.
     */
    public void sendPacket(final Packet packet) {
        if(packet == null) {
            Log.w(TAG, "Attempted to send a null Packet.");
            return;
        }
        if(mWebSocket != null && mConnected) {
            //Log.d(TAG, String.format("Sending Packet: \"%s\".", packet.toString()));
            mWebSocket.send(packet.toString());
        }
    }

    // TODO: Redesign to avoid the need for performing an unchecked type cast.
    public <T extends Packet> T pollPacket(Class<T> packetType) {
        for(Packet receivedPacket : mReceivedPackets) {
            if(receivedPacket.getId().equals(PacketIdentifier.PACKET_IDS.get(packetType))) {
                mReceivedPackets.remove(receivedPacket);
                if(receivedPacket.getClass().equals(packetType)) {
                    T packet = (T)receivedPacket;
                    if(packet.isValid()) {
                        return packet;
                    }
                }
                return null;
            }
        }
        return null;
    }

    /** Returns the singleton instance of {@code WebSocketCommunicator}.
     * @return Singleton instance of {@code WebSocketCommunicator}.
     */
    public static WebSocketCommunicator getInstance() {
        if (mInstance == null) {
            synchronized (WebSocketCommunicator.class) {
                if (mInstance == null) {
                    mInstance = new WebSocketCommunicator();
                }
            }
        }
        return mInstance;
    }

    /**
     * Returns a {@code Queue} containing the '{@code Packet}'s that the
     * {@code WebSocketCommunicator} has received from the server.
     * @return {@code Queue} containing the '{@code Packet}'s that the
     * {@code WebSocketCommunicator} has received from the server.
     */
    public Queue<Packet> getReceivedPackets() {
        return mReceivedPackets;
    }

    /**
     * Returns {@code true} if the {@code WebSocketCommunicator} is currently connecting to the
     * server.
     * @return {@code true} if the {@code WebSocketCommunicator} is currently connecting to the
     * server.
     */
    public boolean isConnecting() {
        return mConnecting;
    }

    /**
     * Returns {@code true} if the {@code WebSocketCommunicator} has a connection to the server.
     * @return {@code true} if the {@code WebSocketCommunicator} has a connection to the server.
     */
    public boolean isConnected() {
        return mConnected;
    }

}
