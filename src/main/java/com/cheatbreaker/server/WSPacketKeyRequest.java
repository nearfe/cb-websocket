package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketKeyRequest extends WSPacket {

    public WSPacketKeyRequest() {}

    @Override
    public void write(PacketBuffer buf) throws IOException {}

    @Override
    public void read(PacketBuffer buf) throws IOException {}

    @Override
    public void handle(Session session) {}
}
