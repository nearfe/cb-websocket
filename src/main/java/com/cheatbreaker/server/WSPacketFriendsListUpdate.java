package com.cheatbreaker.server;

import java.io.IOException;
import java.util.*;

public class WSPacketFriendsListUpdate extends WSPacket {
    private Map<String, List<String>> onlineFriends;
    private Map<String, List<String>> offlineFriends;

    public WSPacketFriendsListUpdate() {}

    public WSPacketFriendsListUpdate(Map<String, List<String>> onlineFriends, Map<String, List<String>> offlineFriends) {
        this.onlineFriends = onlineFriends;
        this.offlineFriends = offlineFriends;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeInt(onlineFriends.size());
        for (Map.Entry<String, List<String>> entry : onlineFriends.entrySet()) {
            buf.writeStringToBuffer(entry.getKey());
            buf.writeStringToBuffer(entry.getValue().get(0));
            buf.writeStringToBuffer(entry.getValue().get(1));
        }
        buf.writeInt(offlineFriends.size());
        for (Map.Entry<String, List<String>> entry : offlineFriends.entrySet()) {
            buf.writeStringToBuffer(entry.getKey());
            buf.writeStringToBuffer(entry.getValue().get(0));
            buf.writeLong(Long.parseLong(entry.getValue().get(1)));
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        int onlineCount = buf.readInt();
        onlineFriends = new HashMap<>();
        for (int i = 0; i < onlineCount; i++) {
            String playerId = buf.readStringFromBuffer(52);
            String username = buf.readStringFromBuffer(32);
            String server = buf.readStringFromBuffer(100);
            onlineFriends.put(playerId, Arrays.asList(username, server));
        }
        int offlineCount = buf.readInt();
        offlineFriends = new HashMap<>();
        for (int i = 0; i < offlineCount; i++) {
            String playerId = buf.readStringFromBuffer(52);
            String username = buf.readStringFromBuffer(32);
            long offlineSince = buf.readLong();
            offlineFriends.put(playerId, Arrays.asList(username, String.valueOf(offlineSince)));
        }
    }

    @Override
    public void handle(Session session) {}

    public Map<String, List<String>> getOnlineFriends() { return onlineFriends; }
    public Map<String, List<String>> getOfflineFriends() { return offlineFriends; }
}
