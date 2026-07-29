package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketFriendUpdate extends WSPacket {
    private String playerId;
    private boolean online;
    private String server;

    public WSPacketFriendUpdate() {}

    public WSPacketFriendUpdate(String playerId, boolean online, String server) {
        this.playerId = playerId;
        this.online = online;
        this.server = server;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.playerId);
        buf.writeBoolean(this.online);
        if (this.online) {
            buf.writeStringToBuffer(this.server != null ? this.server : "");
        } else {
            buf.writeLong(System.currentTimeMillis());
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.playerId = buf.readStringFromBuffer(52);
        this.online = buf.readBoolean();
        if (this.online) {
            this.server = buf.readStringFromBuffer(100);
        }
    }

    @Override
    public void handle(Session session) {}

    public String getPlayerId() { return playerId; }
    public boolean isOnline() { return online; }
    public String getServer() { return server; }
}
