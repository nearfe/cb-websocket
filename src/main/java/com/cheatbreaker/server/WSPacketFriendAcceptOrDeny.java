package com.cheatbreaker.server;

import java.io.IOException;

/**
 * Packet ID 21 - Client -> Server (shared)
 * Accept or deny a friend request.
 */
public class WSPacketFriendAcceptOrDeny extends WSPacket {
    private String playerId;
    private boolean accepted;

    public WSPacketFriendAcceptOrDeny() {}

    public WSPacketFriendAcceptOrDeny(String playerId, boolean accepted) {
        this.playerId = playerId;
        this.accepted = accepted;
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(this.playerId);
        buf.writeBoolean(this.accepted);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.playerId = buf.readStringFromBuffer(52);
        this.accepted = buf.readBoolean();
    }

    @Override
    public void handle(Session session) {
        if (accepted) {
            session.getSessionManager().addFriend(session.getPlayerId(), playerId);
            // Notify the requester that they are now friends
            Session target = session.getSessionManager().getSession(playerId);
            if (target != null) {
                target.sendPacket(new WSPacketFriendRequestSent(
                    session.getPlayerId(), session.getUsername(), true));
            }
        }
    }

    public String getPlayerId() { return playerId; }
    public boolean isAccepted() { return accepted; }
}