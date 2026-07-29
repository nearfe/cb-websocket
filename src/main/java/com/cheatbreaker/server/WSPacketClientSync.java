package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketClientSync extends WSPacket {
    private double value;

    public WSPacketClientSync() {}

    public WSPacketClientSync(double value) {
        this.value = value;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeDouble(this.value);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.value = buf.readDouble();
    }

    @Override
    public void handle(Session session) {}

    public double getValue() { return value; }
}
