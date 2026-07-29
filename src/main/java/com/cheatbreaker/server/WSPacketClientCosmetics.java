package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketClientCosmetics extends WSPacket {
    private String cosmeticsJson;

    public WSPacketClientCosmetics() {}

    public WSPacketClientCosmetics(String cosmeticsJson) {
        this.cosmeticsJson = cosmeticsJson;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.cosmeticsJson);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.cosmeticsJson = buf.readStringFromBuffer(65535);
    }

    @Override
    public void handle(Session session) {
        session.getSessionManager().setPlayerCosmetics(session.getPlayerId(), cosmeticsJson);
        session.getSessionManager().broadcastToFriends(session.getPlayerId(),
            new WSPacketCosmetics(cosmeticsJson));
    }

    public String getCosmeticsJson() { return cosmeticsJson; }
}
