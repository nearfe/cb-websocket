package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketClientRequestsStatus extends WSPacket {

    public WSPacketClientRequestsStatus() {}

    @Override
    public void write(PacketBuffer buf) throws IOException {}

    @Override
    public void read(PacketBuffer buf) throws IOException {}

    @Override
    public void handle(Session session) {
        session.sendPacket(new WSPacketBulkFriendRequest(new java.util.HashMap<>()));
    }
}
