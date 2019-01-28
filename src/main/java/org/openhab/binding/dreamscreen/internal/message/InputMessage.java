package org.openhab.binding.dreamscreen.internal.message;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class InputMessage extends DreamScreenMessage {
    static final byte COMMAND_UPPER = 0x03;
    static final byte COMMAND_LOWER = 0x20;

    protected InputMessage(final byte[] data, final int off) {
        super(data, off);
    }

    public InputMessage(byte group, byte input) {
        super(group, COMMAND_UPPER, COMMAND_LOWER, new byte[] { input });
    }

    static boolean matches(final byte[] data, final int off) {
        return matches(data, off, COMMAND_UPPER, COMMAND_LOWER);
    }

    public byte getInput() {
        return this.payload.get(0);
    }

    @Override
    public String toString() {
        return "Input " + getInput();
    }
}
