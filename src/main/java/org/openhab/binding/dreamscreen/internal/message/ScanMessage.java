package org.openhab.binding.dreamscreen.internal.message;

public class ScanMessage extends DreamScreenMessage {
    private static final byte COMMAND_UPPER = 0x01;
    private static final byte COMMAND_LOWER = 0x03;

    public ScanMessage() {
        super((byte) 0xFF, COMMAND_UPPER, COMMAND_LOWER, new byte[0]);
    }

    @Override
    public String toString() {
        return "Scan";
    }
}
