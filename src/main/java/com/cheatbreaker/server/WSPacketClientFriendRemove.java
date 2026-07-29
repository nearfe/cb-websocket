package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketClientFriendRemove extends WSPacket {
    private String playerId;

    public WSPacketClientFriendRemove() {}

    public WSPacketClientFriendRemove(String playerId) {
        this.playerId = playerId;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.playerId);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.playerId = buf.readStringFromBuffer(52);
    }

    @Override
    public void handle(Session session) {
        session.getSessionManager().removeFriend(session.getPlayerId(), playerId);
        Session target = session.getSessionManager().getSession(playerId);
        if (target != null) {
            target.sendPacket(new WSPacketClientFriendRemove(session.getPlayerId()));
        }
    }

    public String getPlayerId() { return playerId; }
}
