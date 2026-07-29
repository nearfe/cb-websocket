package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketNotification extends WSPacket {
    private String title;
    private String content;

    public WSPacketNotification() {}

    public WSPacketNotification(String title, String content) {
        this.title = title;
        this.content = content;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.title);
        buf.writeStringToBuffer(this.content);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.title = buf.readStringFromBuffer(128);
        this.content = buf.readStringFromBuffer(512);
    }

    @Override
    public void handle(Session session) {}

    public String getTitle() { return title; }
    public String getContent() { return content; }
}
