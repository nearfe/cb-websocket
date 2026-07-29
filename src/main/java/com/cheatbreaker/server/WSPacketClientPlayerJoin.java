package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketClientPlayerJoin extends WSPacket {
    private String playerId;

    public WSPacketClientPlayerJoin() {}

    public WSPacketClientPlayerJoin(String playerId) {
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
        session.getSessionManager().registerPlayer(playerId, session);
        session.getSessionManager().notifyFriendsOnline(playerId);
    }

    public String getPlayerId() { return playerId; }
}
