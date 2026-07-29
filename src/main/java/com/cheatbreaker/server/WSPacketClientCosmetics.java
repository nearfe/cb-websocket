package com.cheatbreaker.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Client -> Server: client sends its equipped cosmetics in binary format.
 * Binary layout written by the client's WSPacketClientCosmetics.write():
 *   [int size]
 *   per cosmetic:
 *     [long lastUpdate][boolean equipped][string name][string name(type)][float scale][string location]
 *
 * The server reads this, stores it, and broadcasts a WSPacketCosmetics (binary, server->client
 * format) to the sender's friends.
 */
public class WSPacketClientCosmetics extends WSPacket {
    private List<CosmeticEntry> entries = new ArrayList<>();

    public WSPacketClientCosmetics() {}

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeInt(this.entries.size());
        for (CosmeticEntry e : this.entries) {
            buf.writeLong(e.lastUpdate);
            buf.writeBoolean(e.equipped);
            buf.writeStringToBuffer(e.name);
            buf.writeStringToBuffer(e.typeName);
            buf.writeFloat(e.scale);
            buf.writeStringToBuffer(e.location);
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        int size = buf.readInt();
        this.entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            CosmeticEntry e = new CosmeticEntry();
            e.lastUpdate = buf.readLong();
            e.equipped = buf.readBoolean();
            e.name = buf.readStringFromBuffer(128);
            e.typeName = buf.readStringFromBuffer(128);
            e.scale = buf.readFloat();
            e.location = buf.readStringFromBuffer(512);
            this.entries.add(e);
        }
    }

    @Override
    public void handle(Session session) {
        String playerId = session.getPlayerId();
        String username = session.getUsername() != null ? session.getUsername() : "Unknown";

        // Convert client entries to the server->client CosmeticData format
        List<WSPacketCosmetics.CosmeticData> cosmeticsList = new ArrayList<>();
        for (CosmeticEntry e : this.entries) {
            // The client writes name twice; the second field is actually the type name.
            // If typeName looks like a valid CosmeticType name, use it; otherwise default to "cape".
            String type = e.typeName;
            if (!type.equals("cape") && !type.equals("dragon_wings") && !type.equals("emote")) {
                type = "cape";
            }
            cosmeticsList.add(new WSPacketCosmetics.CosmeticData(
                e.lastUpdate, e.scale, e.equipped, e.location, e.name, type
            ));
        }

        // Store cosmetics on the session manager for later (e.g. friend join broadcasts)
        session.getSessionManager().setPlayerCosmetics(playerId, cosmeticsList);

        // Broadcast to friends in the binary format the client expects
        WSPacketCosmetics packet = new WSPacketCosmetics(
            playerId, username, false, 0, 0, cosmeticsList
        );
        session.getSessionManager().broadcastToFriends(playerId, packet);
    }

    public List<CosmeticEntry> getEntries() { return entries; }

    /**
     * Raw entry as received from the client.
     */
    public static class CosmeticEntry {
        public long lastUpdate;
        public boolean equipped;
        public String name;
        public String typeName;
        public float scale;
        public String location;
    }
}