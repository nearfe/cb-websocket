package com.cheatbreaker.server;

import java.io.IOException;

/**
 * Packet ID 39 - Shared: emote broadcast.
 * Client sends: [string playerId (UUID)][int emoteId]
 * Server relays to all friends of the sender.
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
        buf.writeStringToBuffer(this.playerId);
        buf.writeInt(this.emoteId);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.playerId = buf.readStringFromBuffer(52);
        this.emoteId = buf.readInt();
    }

    @Override
    public void handle(Session session) {
        session.getSessionManager().broadcastToFriends(session.getPlayerId(),
            new WSPacketEmote(session.getPlayerId(), emoteId));
    }

    public String getPlayerId() { return playerId; }
    public int getEmoteId() { return emoteId; }
}
