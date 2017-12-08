package io.benreynolds.hottopics.packets;

import com.google.gson.annotations.SerializedName;

/**
 * '{@code Chatroom}'s are named using trend information (see {@code TrendManager}) and contain references to the user's
 * '{@code Session}'s that are present within them.
 */
public class Chatroom {

    /** Name of the {@code Chatroom}. */
    @SerializedName("name")
    private String mName;

    /**
     * @param name Name of the {@code Chatroom}.
     */
    public Chatroom(final String name) {
        mName = name;
    }

    /** Returns the name of the {@code Chatroom}.
     * @return Name of the {@code Chatroom}.
     */
    public String getName() {
        return mName;
    }

}