package com.cheatbreaker.server;

import java.io.IOException;
import java.util.*;

public class WSPacketBulkFriendRequest extends WSPacket {
    private Map<String, String> requests;

    public WSPacketBulkFriendRequest() {}

    public WSPacketBulkFriendRequest(Map<String, String> requests) {
        this.requests = requests;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeInt(requests.size());
        for (Map.Entry<String, String> entry : requests.entrySet()) {
            buf.writeStringToBuffer(entry.getKey());
            buf.writeStringToBuffer(entry.getValue());
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        int count = buf.readInt();
        requests = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String playerId = buf.readStringFromBuffer(52);
            String username = buf.readStringFromBuffer(32);
            requests.put(playerId, username);
        }
    }

    @Override
    public void handle(Session session) {}

    public Map<String, String> getRequests() { return requests; }
}
