package com.cheatbreaker.server;

import java.io.IOException;
import java.util.UUID;

/**
 * Packet ID 39 - Shared: emote broadcast.
 * Client sends: [UUID playerId (16 bytes)][int emoteId]
 * Server relays to the sender AND all friends of the sender.
 *
 * The client uses PacketBuffer.writeUUID/readUUID which writes 2 longs
 * (mostSignificantBits + leastSignificantBits) - NOT a string.
 *
 * The client's playEmote(Emote) only sends the packet to the server.
 * The actual animation is activated when the client RECEIVES the
 * WSPacketEmote back (via handleEmote -> playEmote(AbstractClientPlayer, Emote)).
 * Therefore the server MUST echo the packet back to the sender.
 *
 * Emote IDs (from client EmoteManager):
 *   0=wave, 1=handsup, 2=floss, 3=dab, 4=tpose,
 *   5=shrug, 6=facepalm, 7=narutorun, 8=superfacepalm
 */
public class WSPacketEmote extends WSPacket {
    private String playerId;
    private int emoteId;

    public WSPacketEmote() {}

    public WSPacketEmote(String playerId, int emoteId) {
        this.playerId = playerId;
        this.emoteId = emoteId;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        // Client expects: buf.writeUUID(playerId) -> 2 longs (16 bytes)
        buf.writeUUID(toUUID(this.playerId));
        buf.writeInt(this.emoteId);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        // Client sends: buf.writeUUID(playerId) -> 2 longs (16 bytes)
        UUID uuid = buf.readUUID();
        this.playerId = uuid.toString().replace("-", "");
        this.emoteId = buf.readInt();
    }

    @Override
    public void handle(Session session) {
        WSPacketEmote emotePacket = new WSPacketEmote(session.getPlayerId(), emoteId);
        // Send back to the sender so their client activates the animation locally
        session.sendPacket(emotePacket);
        // Relay to friends so they see the emote too
        session.getSessionManager().broadcastToFriends(session.getPlayerId(), emotePacket);
    }

    /**
     * Converts a player ID string (with or without dashes) to a UUID object.
     */
    private static UUID toUUID(String id) {
        if (id == null) return new UUID(0, 0);
        if (id.contains("-")) return UUID.fromString(id);
        if (id.length() == 32) {
            return UUID.fromString(
                id.substring(0, 8) + "-" + id.substring(8, 12) + "-" +
                id.substring(12, 16) + "-" + id.substring(16, 20) + "-" +
                id.substring(20)
            );
        }
        return UUID.fromString(id);
    }

    public String getPlayerId() { return playerId; }
    public int getEmoteId() { return emoteId; }
}
