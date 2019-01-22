package org.openhab.binding.dreamscreen.internal.message;

import static org.openhab.binding.dreamscreen.internal.model.DreamScreenScene.COLOR;

public class AmbientModeTypeMessage extends DreamScreenMessage {
    static final byte COMMAND_UPPER = 0x03;
    static final byte COMMAND_LOWER = 0x08;

    protected AmbientModeTypeMessage(final byte[] data, final int off) {
        super(data, off);
    }

    public AmbientModeTypeMessage(final byte group, final byte ambientModeType) {
        super(group, COMMAND_UPPER, COMMAND_LOWER, new byte[] { ambientModeType });
    }

    static boolean matches(final byte[] data, final int off) {
        return matches(data, off, COMMAND_UPPER, COMMAND_LOWER);
    }

    public byte getAmbientModeType() {
        return this.payload.get(0);
    }

    @Override
    public String toString() {
        final String type = getAmbientModeType() == COLOR.ambientModeType ? "COLOR" : "SCENE";
        return "AmbientModeType " + type;
    }
}
