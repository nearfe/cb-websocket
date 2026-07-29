package com.cheatbreaker.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Main packet handler for the WebSocket server.
 * Dispatches incoming binary frames to the appropriate packet handler.
 *
 * Protocol: [int packetId][payload...]
 * The packetId maps to the WSPacket.REGISTRY BiMap.
 */
public class PacketHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacketHandler.class);

    private final SessionManager sessionManager;
    private Session session;

    public PacketHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Session is created after the WebSocket handshake completes (see userEventTriggered).
        // Do NOT create it here — HTTP headers haven't arrived yet.
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (session == null) {
            LOGGER.warn("Received packet before handshake from {}", ctx.channel().remoteAddress());
            return;
        }

        if (msg.readableBytes() < 1) {
            LOGGER.warn("Received empty packet from {}", ctx.channel().remoteAddress());
            return;
        }

        // Client uses VarInt for packet ID (Minecraft-style)
        PacketBuffer buffer = new PacketBuffer(msg.retain());
        try {
            int packetId = buffer.readVarInt();
            WSPacket packet = WSPacket.createPacket(packetId);

            if (packet == null) {
                LOGGER.warn("Unknown packet ID {} from {}", packetId, ctx.channel().remoteAddress());
                return;
            }

            packet.read(buffer);
            packet.handle(session);
        } catch (Exception e) {
            LOGGER.error("Error handling packet from {}", ctx.channel().remoteAddress(), e);
        } finally {
            buffer.release();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionManager.removeSession(ctx.channel());
        LOGGER.info("Connection closed: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            // WebSocket upgrade complete — now create the session and read captured HTTP headers
            Map<String, String> headers = ctx.channel().attr(HttpHeaderCaptureHandler.CLIENT_HEADERS).get();

            this.session = sessionManager.createSession(ctx.channel());

            if (headers != null) {
                session.setUsername(headers.get("username"));
                session.setPlayerId(headers.get("playerId"));
                session.setHttpHeaders(headers);
            }

            // Register immediately so commands like "emote add <username>" work
            // without waiting for the client to send WSPacketClientPlayerJoin (id 19)
            if (session.getPlayerId() != null) {
                sessionManager.registerPlayer(session.getPlayerId(), session);
            }

            LOGGER.info("WebSocket handshake complete: {} ({})",
                    session.getUsername(), ctx.channel().remoteAddress());

            // Now safe to send binary frames — initiate auth handshake
            session.sendJoinServer();
        } else if (evt instanceof IdleStateEvent) {
            LOGGER.debug("Closing idle connection: {}", ctx.channel().remoteAddress());
            ctx.close();
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Exception in channel {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
