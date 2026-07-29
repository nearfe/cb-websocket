package com.cheatbreaker.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Intercepts the HTTP upgrade request BEFORE WebSocketServerProtocolHandler consumes it.
 * Extracts custom headers sent by the CheatBreaker client:
 *   username, playerId, HWID, version, gitCommit, branch, buildType, server
 * Stores them as a channel attribute for later use by PacketHandler.
 */
public class HttpHeaderCaptureHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpHeaderCaptureHandler.class);

    public static final AttributeKey<Map<String, String>> CLIENT_HEADERS =
            AttributeKey.valueOf("clientHeaders");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            HttpHeaders headers = req.headers();

            Map<String, String> clientData = new HashMap<>();
            clientData.put("username", headers.get("username"));
            clientData.put("playerId", headers.get("playerId"));
            clientData.put("HWID", headers.get("HWID"));
            clientData.put("version", headers.get("version"));
            clientData.put("gitCommit", headers.get("gitCommit"));
            clientData.put("branch", headers.get("branch"));
            clientData.put("buildType", headers.get("buildType"));
            clientData.put("server", headers.get("server"));

            ctx.channel().attr(CLIENT_HEADERS).set(clientData);
            LOGGER.info("Captured HTTP headers from {}: username={}, playerId={}",
                    ctx.channel().remoteAddress(),
                    clientData.get("username"),
                    clientData.get("playerId"));
        }
        super.channelRead(ctx, msg);
    }
}
