package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketClientKeyResponse extends WSPacket {
    private byte[] data;

    public WSPacketClientKeyResponse() {}

    public WSPacketClientKeyResponse(byte[] data) {
        this.data = data;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeBlob(this.data);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.data = buf.readBlob();
    }

    @Override
    public void handle(Session session) {}

    public byte[] getData() { return data; }
}
