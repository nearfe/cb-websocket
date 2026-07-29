package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketForceCrash extends WSPacket {

    public WSPacketForceCrash() {}

    @Override
    public void write(PacketBuffer buf) throws IOException {}

    @Override
    public void read(PacketBuffer buf) throws IOException {}

    @Override
    public void handle(Session session) {}
}
