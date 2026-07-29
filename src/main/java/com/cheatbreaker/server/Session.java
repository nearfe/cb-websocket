package com.cheatbreaker.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.*;
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
        // Send cosmetics (including owned emotes) in binary format
        sendUpdatedCosmetics();

        // Send friends list
        sendFriendsList();

        LOGGER.info("Initial data sent to player {} ({})", username, playerId);
    }

    /**
     * Sends the player's full cosmetics list (normal cosmetics + emotes) in binary format.
     * The client's WSPacketCosmetics.read() expects:
     *   [string playerId][int size][per-cosmetic: long time, float scale, boolean active,
     *    string resourceLocation, string name, string type][string username][boolean join][int color][int color2]
     *
     * For emotes, the client does:
     *   if (type.getTypeName().equals("emote")) {
     *       this.cosmetics.add(new Cosmetic(this.playerId, Integer.parseInt(name), type));
     *   }
     * So "name" must be the emote ID as a string.
     */
    public void sendUpdatedCosmetics() {
        String uname = username != null ? username : "Unknown";

        // Start with any normal cosmetics the player has sent previously
        List<WSPacketCosmetics.CosmeticData> cosmeticsList = new ArrayList<>(
            sessionManager.getPlayerCosmetics(playerId)
        );

        // Add emotes as cosmetic entries with type "emote"
        Set<Integer> emotes = sessionManager.getPlayerEmotes(playerId);
        for (int emoteId : emotes) {
            cosmeticsList.add(new WSPacketCosmetics.CosmeticData(
                0L,                         // lastUpdate (unused for emotes)
                0.0f,                       // scale (unused for emotes)
                false,                      // equipped (unused for emotes)
                "",                         // location (unused for emotes)
                String.valueOf(emoteId),    // name = emote ID as string (client does Integer.parseInt)
                "emote"                     // type
            ));
        }

        // Client calls UUID.fromString(playerId) which requires dashed format
        WSPacketCosmetics packet = new WSPacketCosmetics(
            formatUuid(playerId), uname, false, 0, 0, cosmeticsList
        );
        sendPacket(packet);
    }

    /**
     * Converts a 32-char hex UUID (no dashes) to standard dashed format.
     * e.g. "90badd70e6734b69a5ecc8d8618a4e0b" -> "90badd70-e673-4b69-a5ec-c8d8618a4e0b"
     * If already dashed or invalid, returns as-is.
     */
    private static String formatUuid(String id) {
        if (id == null) return null;
        if (id.contains("-")) return id;
        if (id.length() != 32) return id;
        return id.substring(0, 8) + "-" + id.substring(8, 12) + "-"
             + id.substring(12, 16) + "-" + id.substring(16, 20) + "-"
             + id.substring(20);
    }

    public void sendFriendsList() {
        if (playerId == null) return;
        Set<String> friends = sessionManager.getFriends(playerId);

        Map<String, List<String>> onlineFriends = new HashMap<>();
        Map<String, List<String>> offlineFriends = new HashMap<>();

        for (String friendId : friends) {
            if (sessionManager.isOnline(friendId)) {
                Session friendSession = sessionManager.getSession(friendId);
                String server = sessionManager.getPlayerServer(friendId);
                onlineFriends.put(friendId, Arrays.asList(
                    friendSession != null ? friendSession.getUsername() : "Unknown",
                    server
                ));
            } else {
                offlineFriends.put(friendId, Arrays.asList(
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