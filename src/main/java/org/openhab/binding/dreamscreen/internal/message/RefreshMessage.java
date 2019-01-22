package org.openhab.binding.dreamscreen.internal.message;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class RefreshMessage extends DreamScreenMessage {
    static final byte COMMAND_UPPER = 0x01;
    static final byte COMMAND_LOWER = 0x0A;

    protected RefreshMessage(final byte[] data, final int off) {
        super(data, off);
    }

    public RefreshMessage() {
        super((byte) 0xFF, COMMAND_UPPER, COMMAND_LOWER, new byte[0]);
    }

    static boolean matches(final byte[] data, final int off) {
        return matches(data, off, COMMAND_UPPER, COMMAND_LOWER);
    }

    public byte getGroup() {
        return this.payload.get(32);
    }

    public String getName() {
        return new String(this.payload.array(), 0, 16, StandardCharsets.UTF_8).trim();
    }

    public byte getMode() {
        return this.payload.get(33);
    }

    public byte getScene() {
        return this.payload.get(62);
    }

    public byte getRed() {
        return this.payload.get(40);
    }

    public byte getGreen() {
        return this.payload.get(41);
    }

    public byte getBlue() {
        return this.payload.get(42);
    }

    public byte getProductId() {
        return this.payload.get(this.payloadLen - 1);
    }

    @Override
    public DatagramPacket writePacket(InetAddress address, int port) {
        return broadcastReadPacket(address, port);
    }

    @Override
    public String toString() {
        return "Refresh";
    }
}
