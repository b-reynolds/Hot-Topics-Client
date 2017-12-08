package io.benreynolds.hottopics.packets;

import com.google.gson.annotations.SerializedName;

/**
 * {code UsernameRequestPacket} sent by client devices when requesting a username.
 */
public class UsernameRequestPacket extends Packet {

    /** Attempts to store a the '{@code UsernameRequestPacket}'s ID (as determined by the {@code PacketIdentifier}). */
    public static final Integer ID = PacketIdentifier.PACKET_IDS.getOrDefault(UsernameRequestPacket.class,
            null);

    /** Regular expression used to validate usernames (must consist of alphanumeric characters). */
    public static final String INVALID_CHARACTER_REGEX = "^.*[^a-zA-Z0-9 ].*$";

    /** Minimum username length. */
    public static final int MIN_LENGTH = 4;

    /** Maximum username length. */
    public static final int MAX_LENGTH = 12;

    /** Client's requested username. */
    @SerializedName("username")
    private String mUsername;

    /**
     * @param username Client's requested username.
     */
    public UsernameRequestPacket(final String username) {
        mId = ID;
        mUsername = username;
    }

    /**
     * Returns {@code true} if the {@code UsernameRequestPacket} contains a valid ID and username. Usernames are
     * considered valid if they are non-null and not empty.
     * @return {@code true} if the {@code UsernameRequestPacket} contains a valid ID and username.
     */
    @Override
    public boolean isValid() {
        return mId != null && mUsername != null && mUsername.length() > MIN_LENGTH &&
                mUsername.length() < MAX_LENGTH && !mUsername.matches(INVALID_CHARACTER_REGEX);
    }

    /**
     * Return the client's requested username.
     * @return Client's requested username.
     */
    public String getUsername() {
        return mUsername;
    }

}
