package com.cheatbreaker.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * Sets up the Netty pipeline for WebSocket connections.
 *
 * Pipeline:
 *   HTTP codec -> aggregator -> header-capture -> chunked write -> WS protocol handler
 *   -> idle state (ping/pong keepalive) -> binary frame handler -> packet handler
 */
public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    private static final String WEBSOCKET_PATH = "/";

    private final SessionManager sessionManager;

    public WebSocketServerInitializer(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // HTTP upgrade handshake
        pipeline.addLast("http-codec", new HttpServerCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
        pipeline.addLast("header-capture", new HttpHeaderCaptureHandler());
        pipeline.addLast("chunked-write", new ChunkedWriteHandler());
        pipeline.addLast("ws-protocol", new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true, 65536));

        // Keepalive: close idle connections after 60s with no reads
        pipeline.addLast("idle-state", new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));

        // Binary WebSocket frame <-> ByteBuf codec
        pipeline.addLast("frame-codec", new WebSocketFrameCodec());

        // Application-level packet dispatch
        pipeline.addLast("packet-handler", new PacketHandler(sessionManager));
    }
}
