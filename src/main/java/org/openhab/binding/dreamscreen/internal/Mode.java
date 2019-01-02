package org.openhab.binding.dreamscreen.internal;

import org.eclipse.smarthome.core.library.types.DecimalType;

enum Mode {
    VIDEO(1),
    MUSIC(2),
    AMBIENT(3);

    final byte deviceMode;

    private Mode(int deviceMode) {
        this.deviceMode = (byte) deviceMode;
    }

    public static Mode fromDevice(byte value) {
        return value > 0 ? Mode.values()[value - 1] : null;
    }

    public static Mode fromState(DecimalType command) {
        return Mode.values()[command.intValue()];
    }

    public DecimalType state() {
        return new DecimalType(this.ordinal());
    }
}