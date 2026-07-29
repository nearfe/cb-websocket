package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketFriendRequestSent extends WSPacket {
    private String playerId;
    private String name;
    private boolean friend;

    public WSPacketFriendRequestSent() {}

    public WSPacketFriendRequestSent(String playerId, String name, boolean friend) {
        this.playerId = playerId;
        this.name = name;
        this.friend = friend;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.playerId);
        buf.writeStringToBuffer(this.name);
        buf.writeBoolean(this.friend);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.playerId = buf.readStringFromBuffer(52);
        this.name = buf.readStringFromBuffer(32);
        this.friend = buf.readBoolean();
    }

    @Override
    public void handle(Session session) {
        Session target = session.getSessionManager().getSession(playerId);
        if (target != null) {
            target.sendPacket(new WSPacketFriendRequest(
                session.getPlayerId(), session.getUsername(), ""));
        }
    }

    public String getPlayerId() { return playerId; }
    public String getName() { return name; }
    public boolean isFriend() { return friend; }
}
