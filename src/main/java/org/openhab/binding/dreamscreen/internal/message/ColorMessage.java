package org.openhab.binding.dreamscreen.internal.message;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class ColorMessage extends DreamScreenMessage {
    static final byte COMMAND_UPPER = 0x03;
    static final byte COMMAND_LOWER = 0x05;

    protected ColorMessage(final byte[] data, final int off) {
        super(data, off);
    }

    public ColorMessage(byte group, byte red, byte green, byte blue) {
        super(group, COMMAND_UPPER, COMMAND_LOWER, new byte[] { red, green, blue });
    }

    static boolean matches(final byte[] data, final int off) {
        return matches(data, off, COMMAND_UPPER, COMMAND_LOWER);
    }

    public byte getRed() {
        return this.payload.get(0);
    }

    public byte getGreen() {
        return this.payload.get(1);
    }

    public byte getBlue() {
        return this.payload.get(2);
    }

    @Override
    public String toString() {
        return String.format("Color %02X:%02X:%02X", getRed(), getGreen(), getBlue());
    }
}
