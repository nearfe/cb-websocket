package com.cheatbreaker.server;

import java.io.IOException;

public class WSPacketClientCrashReport extends WSPacket {
    private String crashReport;

    public WSPacketClientCrashReport() {}

    public WSPacketClientCrashReport(String crashReport) {
        this.crashReport = crashReport;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.crashReport);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.crashReport = buf.readStringFromBuffer(32767);
    }

    @Override
    public void handle(Session session) {
        org.slf4j.LoggerFactory.getLogger(WSPacketClientCrashReport.class)
            .warn("Crash report from player {}: {}", session.getPlayerId(),
                crashReport.length() > 200 ? crashReport.substring(0, 200) + "..." : crashReport);
    }

    public String getCrashReport() { return crashReport; }
}
