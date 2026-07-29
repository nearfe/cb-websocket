package com.cheatbreaker.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Server-side equivalent of Minecraft's PacketBuffer.
 * Wraps a Netty ByteBuf and provides the same read/write methods
 * the CheatBreaker client uses for WebSocket packet serialization.
 *
 * String format: [VarInt length][UTF-8 bytes]  (matches Minecraft 1.7.10 PacketBuffer)
 * Blob format: [short length][raw bytes]
 * UUID format: [long mostSigBits][long leastSigBits]  (matches Minecraft 1.7.10 PacketBuffer)
 */
public class PacketBuffer {

    private final ByteBuf buf;

    public PacketBuffer(ByteBuf buf) {
        this.buf = buf;
    }

    public PacketBuffer() {
        this.buf = Unpooled.buffer();
    }

    public ByteBuf getByteBuf() {
        return this.buf;
    }

    public void writeStringToBuffer(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        this.writeVarInt(bytes.length);
        this.buf.writeBytes(bytes);
    }

    public String readStringFromBuffer(int maxLength) throws IOException {
        int length = this.readVarInt();
        if (length < 0) {
            throw new IOException("String length was negative: " + length);
        }
        if (length > maxLength * 4) {
            throw new IOException("String length " + length + " exceeds max " + (maxLength * 4));
        }
        byte[] bytes = new byte[length];
        this.buf.readBytes(bytes);
        String result = new String(bytes, StandardCharsets.UTF_8);
        if (result.length() > maxLength) {
            throw new IOException("String exceeds max length " + maxLength);
        }
        return result;
    }

    public void writeInt(int value) { this.buf.writeInt(value); }
    public int readInt() { return this.buf.readInt(); }

    public void writeShort(int value) { this.buf.writeShort(value); }
    public short readShort() { return this.buf.readShort(); }

    public void writeLong(long value) { this.buf.writeLong(value); }
    public long readLong() { return this.buf.readLong(); }

    public void writeDouble(double value) { this.buf.writeDouble(value); }
    public double readDouble() { return this.buf.readDouble(); }

    public void writeFloat(float value) { this.buf.writeFloat(value); }
    public float readFloat() { return this.buf.readFloat(); }

    public void writeBoolean(boolean value) { this.buf.writeBoolean(value); }
    public boolean readBoolean() { return this.buf.readBoolean(); }

    public void writeByte(int value) { this.buf.writeByte(value); }
    public byte readByte() { return this.buf.readByte(); }

    public void writeBlob(byte[] data) {
        this.buf.writeShort(data.length);
        this.buf.writeBytes(data);
    }

    public byte[] readBlob() {
        short length = this.buf.readShort();
        if (length < 0) return new byte[0];
        byte[] data = new byte[length];
        this.buf.readBytes(data);
        return data;
    }

    public void writeBytes(byte[] data) { this.buf.writeBytes(data); }
    public void readBytes(byte[] dest) { this.buf.readBytes(dest); }

    // --- UUID (matches Minecraft 1.7.10 PacketBuffer.writeUUID/readUUID) ---

    public void writeUUID(UUID uuid) {
        this.buf.writeLong(uuid.getMostSignificantBits());
        this.buf.writeLong(uuid.getLeastSignificantBits());
    }

    public UUID readUUID() {
        long most = this.buf.readLong();
        long least = this.buf.readLong();
        return new UUID(most, least);
    }

    // --- VarInt (Minecraft-style, matches client's writeVarIntToBuffer/readVarIntFromBuffer) ---

    public void writeVarInt(int value) {
        while ((value & 0xFFFFFF80) != 0) {
            this.buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        this.buf.writeByte(value);
    }

    public int readVarInt() {
        int result = 0;
        int shift = 0;
        byte b;
        do {
            b = this.buf.readByte();
            result |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return result;
    }

    public int readableBytes() { return this.buf.readableBytes(); }
    public boolean isReadable() { return this.buf.isReadable(); }
    public void release() { this.buf.release(); }

    public PacketBuffer retain() {
        this.buf.retain();
        return this;
    }
}
