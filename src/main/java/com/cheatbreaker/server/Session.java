package com.cheatbreaker.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a single connected client session.
 * Handles authentication handshake, packet I/O, and player state.
 *
 * The client sends these HTTP headers during the WebSocket upgrade:
 *   username, playerId, HWID, version, gitCommit, branch, buildType, server
 */
public class Session {

    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);

    private final Channel channel;
    private final SessionManager sessionManager;
    private final AtomicBoolean authenticated = new AtomicBoolean(false);

    private String playerId;
    private String username;
    private KeyPair serverKeyPair;
    private byte[] verifyToken;
    private Map<String, String> httpHeaders;

    public Session(Channel channel, SessionManager sessionManager) {
        this.channel = channel;
        this.sessionManager = sessionManager;
        generateKeyPair();
    }

    private void generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            this.serverKeyPair = generator.generateKeyPair();
            this.verifyToken = new byte[4];
            new java.security.SecureRandom().nextBytes(this.verifyToken);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to generate RSA key pair", e);
        }
    }

    // --- Packet I/O ---

    public void sendPacket(WSPacket packet) {
        if (!channel.isActive()) return;
        try {
            PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            int packetId = WSPacket.getPacketId(packet);
            buf.writeVarInt(packetId);
            packet.write(buf);
            // Write raw ByteBuf - the WebSocketFrameCodec wraps it into a BinaryWebSocketFrame
            channel.writeAndFlush(buf.getByteBuf());
        } catch (Exception e) {
            LOGGER.error("Failed to send packet {} to {}", packet.getClass().getSimpleName(), playerId, e);
        }
    }

    public void sendRawBytes(byte[] data) {
        if (!channel.isActive()) return;
        channel.writeAndFlush(Unpooled.wrappedBuffer(data));
    }

    // --- Authentication ---

    public void sendJoinServer() {
        WSPacketJoinServer packet = new WSPacketJoinServer(
            serverKeyPair.getPublic(),
            verifyToken
        );
        sendPacket(packet);
    }

    public void handleJoinServerResponse(byte[] encryptedSecretKey, byte[] encryptedVerifyToken) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, serverKeyPair.getPrivate());
            byte[] decryptedToken = cipher.doFinal(encryptedVerifyToken);

            if (!java.util.Arrays.equals(decryptedToken, verifyToken)) {
                LOGGER.warn("Verify token mismatch from {}", channel.remoteAddress());
                channel.close();
                return;
            }

            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, serverKeyPair.getPrivate());
            byte[] secretKeyBytes = cipher.doFinal(encryptedSecretKey);
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(secretKeyBytes, "AES");

            LOGGER.info("Client {} authenticated successfully", channel.remoteAddress());
            authenticated.set(true);

            sendInitialData();
        } catch (Exception e) {
            LOGGER.error("Authentication failed for {}", channel.remoteAddress(), e);
            channel.close();
        }
    }

    private void sendInitialData() {
        // Send cosmetics (including owned emotes)
        sendUpdatedCosmetics();

        // Send friends list
        sendFriendsList();

        LOGGER.info("Initial data sent to player {} ({})", username, playerId);
    }

    public void sendUpdatedCosmetics() {
        java.util.Set<Integer> emotes = sessionManager.getPlayerEmotes(playerId);
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (int emoteId : emotes) {
            if (!first) json.append(",");
            json.append("{\"playerId\":\"").append(playerId)
                .append("\",\"emoteId\":").append(emoteId)
                .append(",\"type\":\"emote\"}");
            first = false;
        }
        json.append("]");
        sendPacket(new WSPacketCosmetics(json.toString()));
    }

    public void sendFriendsList() {
        if (playerId == null) return;
        java.util.Set<String> friends = sessionManager.getFriends(playerId);

        java.util.Map<String, java.util.List<String>> onlineFriends = new java.util.HashMap<>();
        java.util.Map<String, java.util.List<String>> offlineFriends = new java.util.HashMap<>();

        for (String friendId : friends) {
            if (sessionManager.isOnline(friendId)) {
                Session friendSession = sessionManager.getSession(friendId);
                String server = sessionManager.getPlayerServer(friendId);
                onlineFriends.put(friendId, java.util.Arrays.asList(
                    friendSession != null ? friendSession.getUsername() : "Unknown",
                    server
                ));
            } else {
                offlineFriends.put(friendId, java.util.Arrays.asList(
                    "Unknown",
                    String.valueOf(System.currentTimeMillis())
                ));
            }
        }

        sendPacket(new WSPacketFriendsListUpdate(onlineFriends, offlineFriends));
    }

    public void sendFriendUpdate(String friendPlayerId, boolean online, String server) {
        sendPacket(new WSPacketFriendUpdate(friendPlayerId, online, server));
    }

    public void sendNotification(String title, String message) {
        sendPacket(new WSPacketNotification(title, message));
    }

    public void sendConsoleMessage(String message) {
        sendPacket(new WSPacketConsoleMessage(message));
    }

    public void sendEmote(String playerId, int emoteId) {
        sendPacket(new WSPacketEmote(playerId, emoteId));
    }

    // --- HTTP Headers ---

    public void setHttpHeaders(Map<String, String> headers) {
        this.httpHeaders = headers;
    }

    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }

    public String getHwid() {
        return httpHeaders != null ? httpHeaders.get("HWID") : null;
    }

    public String getClientVersion() {
        return httpHeaders != null ? httpHeaders.get("version") : null;
    }

    // --- Getters/Setters ---

    public Channel getChannel() { return channel; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isAuthenticated() { return authenticated.get(); }
    public SessionManager getSessionManager() { return sessionManager; }
    public KeyPair getServerKeyPair() { return serverKeyPair; }
    public byte[] getVerifyToken() { return verifyToken; }

    public void disconnect() {
        channel.close();
    }
}
