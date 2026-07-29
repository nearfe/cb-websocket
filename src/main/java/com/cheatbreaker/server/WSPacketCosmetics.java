package com.cheatbreaker.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client: broadcasts a player's cosmetics to their friends.
 * Binary format expected by the client's WSPacketCosmetics.read():
 *   [string playerId][int size][per-cosmetic: long time, float scale, boolean active,
 *    string resourceLocation, string name, string type][string username][boolean join][int color][int color2]
 */
public class WSPacketCosmetics extends WSPacket {
    private String playerId;
    private String username;
    private boolean join;
    private int color;
    private int color2;
    private List<CosmeticData> cosmetics = new ArrayList<>();

    public WSPacketCosmetics() {}

    public WSPacketCosmetics(String playerId, String username, boolean join, int color, int color2, List<CosmeticData> cosmetics) {
        this.playerId = playerId;
        this.username = username;
        this.join = join;
        this.color = color;
        this.color2 = color2;
        this.cosmetics = cosmetics != null ? cosmetics : new ArrayList<>();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.playerId);
        buf.writeInt(this.cosmetics.size());

        for (CosmeticData c : this.cosmetics) {
            buf.writeLong(c.lastUpdate);
            buf.writeFloat(c.scale);
            buf.writeBoolean(c.equipped);
            buf.writeStringToBuffer(c.location);
            buf.writeStringToBuffer(c.name);
            buf.writeStringToBuffer(c.type);
        }

        buf.writeStringToBuffer(this.username);
        buf.writeBoolean(this.join);
        buf.writeInt(this.color);
        buf.writeInt(this.color2);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.playerId = buf.readStringFromBuffer(52);
        int size = buf.readInt();

        this.cosmetics = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            CosmeticData c = new CosmeticData();
            c.lastUpdate = buf.readLong();
            c.scale = buf.readFloat();
            c.equipped = buf.readBoolean();
            c.location = buf.readStringFromBuffer(512);
            c.name = buf.readStringFromBuffer(128);
            c.type = buf.readStringFromBuffer(128);
            this.cosmetics.add(c);
        }

        this.username = buf.readStringFromBuffer(16);
        this.join = buf.readBoolean();
        this.color = buf.readInt();
        this.color2 = buf.readInt();
    }

    @Override
    public void handle(Session session) {}

    public String getPlayerId() { return playerId; }
    public String getUsername() { return username; }
    public boolean isJoin() { return join; }
    public int getColor() { return color; }
    public int getColor2() { return color2; }
    public List<CosmeticData> getCosmetics() { return cosmetics; }

    /**
     * Simple data holder for a single cosmetic entry.
     */
    public static class CosmeticData {
        public long lastUpdate;
        public float scale;
        public boolean equipped;
        public String location;
        public String name;
        public String type;

        public CosmeticData() {}

        public CosmeticData(long lastUpdate, float scale, boolean equipped, String location, String name, String type) {
            this.lastUpdate = lastUpdate;
            this.scale = scale;
            this.equipped = equipped;
            this.location = location;
            this.name = name;
            this.type = type;
        }
    }
}