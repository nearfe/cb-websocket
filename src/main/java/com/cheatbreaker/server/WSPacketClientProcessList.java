package com.cheatbreaker.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WSPacketClientProcessList extends WSPacket {
    private List<String> processes;

    public WSPacketClientProcessList() {}

    public WSPacketClientProcessList(List<String> processes) {
        this.processes = processes;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeInt(this.processes.size());
        for (String process : this.processes) {
            buf.writeStringToBuffer(process);
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        int count = buf.readInt();
        this.processes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            this.processes.add(buf.readStringFromBuffer(512));
        }
    }

    @Override
    public void handle(Session session) {}

    public List<String> getProcesses() { return processes; }
}
