package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketClientJoinServerResponse extends WSPacket {
    private byte[] secretKey;
    private byte[] verifyToken;

    public WSPacketClientJoinServerResponse() {}

    @Override
    public void write(PacketBuffer buf) throws IOException {}

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.secretKey = buf.readBlob();
        this.verifyToken = buf.readBlob();
    }

    @Override
    public void handle(Session session) {
        session.handleJoinServerResponse(secretKey, verifyToken);
    }
}
