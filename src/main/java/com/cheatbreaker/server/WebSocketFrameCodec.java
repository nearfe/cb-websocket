package com.cheatbreaker.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * Codec that converts between BinaryWebSocketFrame and raw ByteBuf.
 * The CheatBreaker client sends binary frames containing PacketBuffer-serialized data.
 *
 * Inbound: BinaryWebSocketFrame -> ByteBuf (for PacketHandler to read)
 * Outbound: ByteBuf -> BinaryWebSocketFrame (Session writes raw ByteBuf, codec wraps it)
 */
public class WebSocketFrameCodec extends MessageToMessageCodec<BinaryWebSocketFrame, ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        out.add(new BinaryWebSocketFrame(msg.retain()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame frame, List<Object> out) {
        out.add(frame.content().retain());
    }
}