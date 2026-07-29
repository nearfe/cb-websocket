package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketCosmetics extends WSPacket {
    private String cosmeticsJson;

    public WSPacketCosmetics() {}

    public WSPacketCosmetics(String cosmeticsJson) {
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
    public void handle(Session session) {}

    public String getCosmeticsJson() { return cosmeticsJson; }
}
