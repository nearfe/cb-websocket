package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketFriendMessage extends WSPacket {
    private String playerId;
    private String message;

    public WSPacketFriendMessage() {}

    public WSPacketFriendMessage(String playerId, String message) {
        this.playerId = playerId;
        this.message = message;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.playerId);
        buf.writeStringToBuffer(this.message);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.playerId = buf.readStringFromBuffer(52);
        this.message = buf.readStringFromBuffer(512);
    }

    @Override
    public void handle(Session session) {
        Session target = session.getSessionManager().getSession(playerId);
        if (target != null) {
            target.sendPacket(new WSPacketFriendMessage(session.getPlayerId(), message));
        }
    }

    public String getPlayerId() { return playerId; }
    public String getMessage() { return message; }
}
