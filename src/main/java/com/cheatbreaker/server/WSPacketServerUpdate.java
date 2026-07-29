package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketServerUpdate extends WSPacket {
    private String playerId;
    private String server;

    public WSPacketServerUpdate() {}

    public WSPacketServerUpdate(String playerId, String server) {
        this.playerId = playerId;
        this.server = server;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.playerId);
        buf.writeStringToBuffer(this.server);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.playerId = buf.readStringFromBuffer(52);
        this.server = buf.readStringFromBuffer(100);
    }

    @Override
    public void handle(Session session) {
        session.getSessionManager().setPlayerServer(session.getPlayerId(), server);
        session.getSessionManager().notifyFriendsOnline(session.getPlayerId());
    }

    public String getPlayerId() { return playerId; }
    public String getServer() { return server; }
}
