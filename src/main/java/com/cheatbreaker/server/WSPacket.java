package com.cheatbreaker.server;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.IOException;

/**
 * Server-side packet base class.
 * Mirrors the client's WSPacket registry so packet IDs match exactly.
 */
public abstract class WSPacket {

    public static final BiMap<Class<? extends WSPacket>, Integer> REGISTRY = HashBiMap.create();

    public abstract void write(PacketBuffer buf) throws IOException;

    public abstract void read(PacketBuffer buf) throws IOException;

    public abstract void handle(Session session);

    static {
        // Server -> Client packets
        REGISTRY.put(WSPacketJoinServer.class, 0);
        REGISTRY.put(WSPacketConsoleMessage.class, 2);
        REGISTRY.put(WSPacketNotification.class, 3);
        REGISTRY.put(WSPacketFriendsListUpdate.class, 4);
        REGISTRY.put(WSPacketFriendMessage.class, 5);
        REGISTRY.put(WSPacketServerUpdate.class, 6);
        REGISTRY.put(WSPacketBulkFriendRequest.class, 7);
        REGISTRY.put(WSPacketCosmetics.class, 8);
        REGISTRY.put(WSPacketFriendRequest.class, 9);
        REGISTRY.put(WSPacketFriendRequestSent.class, 16);
        REGISTRY.put(WSPacketFriendUpdate.class, 18);
        REGISTRY.put(WSPacketKeyRequest.class, 32);
        REGISTRY.put(WSPacketForceCrash.class, 33);
        REGISTRY.put(WSPacketRequestProcessList.class, 35);

        // Client -> Server packets
        REGISTRY.put(WSPacketClientJoinServerResponse.class, 1);
        REGISTRY.put(WSPacketClientFriendRemove.class, 17);
        REGISTRY.put(WSPacketClientPlayerJoin.class, 19);
        REGISTRY.put(WSPacketClientCosmetics.class, 20);
        REGISTRY.put(WSPacketFriendAcceptOrDeny.class, 21);
        REGISTRY.put(WSPacketClientRequestsStatus.class, 22);
        REGISTRY.put(WSPacketClientCrashReport.class, 23);
        REGISTRY.put(WSPacketClientSync.class, 24);
        REGISTRY.put(WSPacketClientKeyResponse.class, 25);
        REGISTRY.put(WSPacketClientProfilesExist.class, 34);
        REGISTRY.put(WSPacketClientProcessList.class, 36);
        REGISTRY.put(WSPacketClientKeySync.class, 37);
        REGISTRY.put(WSPacketEmote.class, 39);
    }

    public static WSPacket createPacket(int id) {
        Class<? extends WSPacket> clazz = REGISTRY.inverse().get(id);
        if (clazz == null) return null;
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    public static int getPacketId(WSPacket packet) {
        Integer id = REGISTRY.get(packet.getClass());
        return id != null ? id : -1;
    }
}
