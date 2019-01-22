package org.openhab.binding.dreamscreen.internal.message;

import org.openhab.binding.dreamscreen.internal.model.DreamScreenScene;

public class SceneMessage extends DreamScreenMessage {
    static final byte COMMAND_UPPER = 0x03;
    static final byte COMMAND_LOWER = 0x0D;

    protected SceneMessage(final byte[] data, final int off) {
        super(data, off);
    }

    public SceneMessage(byte group, byte ambientScene) {
        super(group, COMMAND_UPPER, COMMAND_LOWER, new byte[] { ambientScene });
    }

    static boolean matches(final byte[] data, final int off) {
        return matches(data, off, COMMAND_UPPER, COMMAND_LOWER);
    }

    public byte getScene() {
        // TODO Auto-generated method stub
        return this.payload.get(0);
    }

    @Override
    public String toString() {
        return "Scene " + DreamScreenScene.fromDeviceScene(getScene()).name();
    }
}
