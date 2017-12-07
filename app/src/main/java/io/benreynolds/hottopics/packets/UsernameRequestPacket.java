package io.benreynolds.hottopics.packets;

import com.google.gson.annotations.SerializedName;

/**
 * {code UsernameRequestPacket} sent by client devices when requesting a username.
 */
public class UsernameRequestPacket extends Packet {

    /** Attempts to store a the '{@code UsernameRequestPacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(UsernameRequestPacket.class,
            null);

    /**
     * Regular expression used to validate usernames. Usernames can consist of alphanumeric characters, underscores
     * and periods. Underscores and periods can not be consecutive.
     */
    private static final String REGULAR_EXPRESSION = "^(?=.{8,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$";

    /** Client's requested username. */
    @SerializedName("username")
    private String mUsername;

    /**
     * @param username Client's requested username.
     */
    public UsernameRequestPacket(final String username) {
        mUsername = username;
    }

    /**
     * Returns {@code true} if the {@code UsernameRequestPacket} contains a valid ID and username. Usernames are
     * considered valid if they are non-null and not empty.
     * @return {@code true} if the {@code UsernameRequestPacket} contains a valid ID and username.
     */
    @Override
    public boolean isValid() {
        return mId != null && mUsername != null && mUsername.matches(REGULAR_EXPRESSION);
    }

    /**
     * Return the client's requested username.
     * @return Client's requested username.
     */
    public String getUsername() {
        return mUsername;
    }

}
