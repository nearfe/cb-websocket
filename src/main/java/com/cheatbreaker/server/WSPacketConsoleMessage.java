package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketConsoleMessage extends WSPacket {
    private String message;

    public WSPacketConsoleMessage() {}

    public WSPacketConsoleMessage(String message) {
        this.message = message;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.message);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.message = buf.readStringFromBuffer(512);
    }

    @Override
    public void handle(Session session) {}

    public String getMessage() { return message; }
}
