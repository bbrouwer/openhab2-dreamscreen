package org.openhab.binding.dreamscreen.internal.message;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.dreamscreen.internal.model.DreamScreenMode;

@NonNullByDefault
public class ModeMessage extends DreamScreenMessage {
    static final byte COMMAND_UPPER = 0x03;
    static final byte COMMAND_LOWER = 0x01;

    protected ModeMessage(final byte[] data, final int off) {
        super(data, off);
    }

    public ModeMessage(byte group, byte mode) {
        super(group, COMMAND_UPPER, COMMAND_LOWER, new byte[] { mode });
    }

    static boolean matches(final byte[] data, final int off) {
        return matches(data, off, COMMAND_UPPER, COMMAND_LOWER);
    }

    public byte getMode() {
        return this.payload.get(0);
    }

    @Override
    public String toString() {
        final DreamScreenMode mode = DreamScreenMode.fromDevice(getMode());
        return "Mode " + (mode == null ? "SLEEP" : mode.name());
    }
}
