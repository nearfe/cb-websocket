package com.cheatbreaker.server;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all connected client sessions.
 * Tracks players by UUID, handles friend relationships, cosmetics, emotes, and server locations.
 */
public class SessionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionManager.class);

    private final Map<String, Session> sessionsByPlayerId = new ConcurrentHashMap<>();
    private final Map<Channel, Session> sessionsByChannel = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> friendRelations = new ConcurrentHashMap<>();
    private final Map<String, String> playerServers = new ConcurrentHashMap<>();
    private final Map<String, List<WSPacketCosmetics.CosmeticData>> playerCosmetics = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> playerEmotes = new ConcurrentHashMap<>();
    private final Map<String, String> usernameToPlayerId = new ConcurrentHashMap<>();

    public Session createSession(Channel channel) {
        Session session = new Session(channel, this);
        sessionsByChannel.put(channel, session);
        return session;
    }

    public void registerPlayer(String playerId, Session session) {
        sessionsByPlayerId.put(playerId, session);
        session.setPlayerId(playerId);
        if (session.getUsername() != null) {
            usernameToPlayerId.put(session.getUsername().toLowerCase(), playerId);
        }
        LOGGER.info("Player registered: {} ({})", session.getUsername(), playerId);
    }

    public void removeSession(Channel channel) {
        Session session = sessionsByChannel.remove(channel);
        if (session != null && session.getPlayerId() != null) {
            String playerId = session.getPlayerId();
            sessionsByPlayerId.remove(playerId);
            playerServers.remove(playerId);
            if (session.getUsername() != null) {
                usernameToPlayerId.remove(session.getUsername().toLowerCase());
            }
            LOGGER.info("Player disconnected: {}", playerId);
            notifyFriendsOffline(playerId);
        }
    }

    public Session getSession(String playerId) {
        return sessionsByPlayerId.get(playerId);
    }

    public Session getSessionByUsername(String username) {
        String playerId = usernameToPlayerId.get(username.toLowerCase());
        if (playerId == null) return null;
        return sessionsByPlayerId.get(playerId);
    }

    public Session getSessionByChannel(Channel channel) {
        return sessionsByChannel.get(channel);
    }

    public boolean isOnline(String playerId) {
        return sessionsByPlayerId.containsKey(playerId);
    }

    public Collection<Session> getAllSessions() {
        return sessionsByChannel.values();
    }

    public int getOnlineCount() {
        return sessionsByPlayerId.size();
    }

    public List<String> getOnlineUsernames() {
        return sessionsByPlayerId.values().stream()
                .map(s -> s.getUsername() != null ? s.getUsername() : s.getPlayerId())
                .collect(Collectors.toList());
    }

    // --- Friend management ---

    public void addFriend(String playerId, String friendId) {
        friendRelations.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(friendId);
        friendRelations.computeIfAbsent(friendId, k -> ConcurrentHashMap.newKeySet()).add(playerId);
        LOGGER.info("Friendship added: {} <-> {}", playerId, friendId);
    }

    public void removeFriend(String playerId, String friendId) {
        Set<String> friends = friendRelations.get(playerId);
        if (friends != null) friends.remove(friendId);
        Set<String> reverseFriends = friendRelations.get(friendId);
        if (reverseFriends != null) reverseFriends.remove(playerId);
        LOGGER.info("Friendship removed: {} <-> {}", playerId, friendId);
    }

    public Set<String> getFriends(String playerId) {
        return friendRelations.getOrDefault(playerId, Collections.emptySet());
    }

    // --- Server location tracking ---

    public void setPlayerServer(String playerId, String server) {
        playerServers.put(playerId, server);
    }

    public String getPlayerServer(String playerId) {
        return playerServers.getOrDefault(playerId, "unknown");
    }

    // --- Cosmetics (binary format) ---

    public void setPlayerCosmetics(String playerId, List<WSPacketCosmetics.CosmeticData> cosmetics) {
        playerCosmetics.put(playerId, cosmetics);
    }

    public List<WSPacketCosmetics.CosmeticData> getPlayerCosmetics(String playerId) {
        return playerCosmetics.getOrDefault(playerId, Collections.emptyList());
    }

    /**
     * Builds and returns a WSPacketCosmetics for the given player, ready to send to friends.
     */
    public WSPacketCosmetics buildCosmeticsPacket(String playerId) {
        Session session = getSession(playerId);
        String username = (session != null && session.getUsername() != null) ? session.getUsername() : "Unknown";
        return new WSPacketCosmetics(playerId, username, false, 0, 0, getPlayerCosmetics(playerId));
    }

    // --- Emote management ---

    public void addPlayerEmote(String playerId, int emoteId) {
        playerEmotes.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(emoteId);
        LOGGER.info("Emote {} granted to player {}", EmoteRegistry.getName(emoteId), playerId);
    }

    public void removePlayerEmote(String playerId, int emoteId) {
        Set<Integer> emotes = playerEmotes.get(playerId);
        if (emotes != null) {
            emotes.remove(emoteId);
            LOGGER.info("Emote {} removed from player {}", EmoteRegistry.getName(emoteId), playerId);
        }
    }

    public Set<Integer> getPlayerEmotes(String playerId) {
        return playerEmotes.getOrDefault(playerId, Collections.emptySet());
    }

    public boolean hasEmote(String playerId, int emoteId) {
        Set<Integer> emotes = playerEmotes.get(playerId);
        return emotes != null && emotes.contains(emoteId);
    }

    // --- Notifications ---

    public void notifyFriendsOnline(String playerId) {
        Set<String> friends = getFriends(playerId);
        for (String friendId : friends) {
            Session friendSession = getSession(friendId);
            if (friendSession != null) {
                friendSession.sendFriendUpdate(playerId, true, getPlayerServer(playerId));
            }
        }
    }

    public void notifyFriendsOffline(String playerId) {
        Set<String> friends = getFriends(playerId);
        for (String friendId : friends) {
            Session friendSession = getSession(friendId);
            if (friendSession != null) {
                friendSession.sendFriendUpdate(playerId, false, null);
            }
        }
    }

    public void broadcastToFriends(String playerId, WSPacket packet) {
        Set<String> friends = getFriends(playerId);
        for (String friendId : friends) {
            Session friendSession = getSession(friendId);
            if (friendSession != null) {
                friendSession.sendPacket(packet);
            }
        }
    }

    public void broadcastToAll(WSPacket packet) {
        for (Session session : sessionsByChannel.values()) {
            session.sendPacket(packet);
        }
    }
}