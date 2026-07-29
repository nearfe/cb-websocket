package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketFriendRequest extends WSPacket {
    private String playerId;
    private String name;
    private String message;

    public WSPacketFriendRequest() {}

    public WSPacketFriendRequest(String playerId, String name, String message) {
        this.playerId = playerId;
        this.name = name;
        this.message = message;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.playerId);
        buf.writeStringToBuffer(this.name);
        buf.writeStringToBuffer(this.message);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.playerId = buf.readStringFromBuffer(52);
        this.name = buf.readStringFromBuffer(32);
        this.message = buf.readStringFromBuffer(256);
    }

    @Override
    public void handle(Session session) {}

    public String getPlayerId() { return playerId; }
    public String getName() { return name; }
    public String getMessage() { return message; }
}
