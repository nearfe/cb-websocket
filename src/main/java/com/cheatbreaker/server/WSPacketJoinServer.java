package com.cheatbreaker.server;

import java.io.IOException;
import java.security.PublicKey;

public class WSPacketJoinServer extends WSPacket {
    private PublicKey publicKey;
    private byte[] verifyToken;

    public WSPacketJoinServer() {}

    public WSPacketJoinServer(PublicKey publicKey, byte[] verifyToken) {
        this.publicKey = publicKey;
        this.verifyToken = verifyToken;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        // Client reads exactly: [blob publicKey][blob verifyToken]
        // Do NOT write a string here — the old empty string shifted the payload and broke the RSA handshake.
        byte[] encoded = publicKey.getEncoded();
        buf.writeBlob(encoded);
        buf.writeBlob(verifyToken);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {}

    @Override
    public void handle(Session session) {}
}
