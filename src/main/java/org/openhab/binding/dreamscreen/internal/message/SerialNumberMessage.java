package org.openhab.binding.dreamscreen.internal.message;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class SerialNumberMessage extends DreamScreenMessage {
    private static final byte COMMAND_UPPER = 0x01;
    private static final byte COMMAND_LOWER = 0x03;

    protected SerialNumberMessage(final byte[] data, final int off) {
        super(data, off);
    }

    static boolean matches(final byte[] data, final int off) {
        return matches(data, off, COMMAND_UPPER, COMMAND_LOWER);
    }

    public int getSerialNumber() {
        return this.payload.getInt(0);
    }

    @Override
    public DatagramPacket writePacket(InetAddress address, int port) {
        return broadcastReadPacket(address, port);
    }

    @Override
    public String toString() {
        return "Serial Number " + getSerialNumber();
    }
}
