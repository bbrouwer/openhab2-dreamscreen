package org.openhab.binding.dreamscreen.internal;

import org.eclipse.smarthome.core.library.types.DecimalType;

enum Scene {
    COLOR(0, -1),
    RANDOM_COLOR(1, 0),
    FIRESIDE(1, 1),
    TWINKLE(1, 2),
    OCEAN(1, 3),
    RAINBOW(1, 4),
    JULY_4TH(1, 5),
    HOLIDAY(1, 6),
    POP(1, 7),
    ENCHANTED_FOREST(1, 8);

    final byte deviceAmbientSceneType;
    final byte deviceAmbientScene;

    private Scene(int deviceAmbientSceneType, int deviceAmbientScene) {
        this.deviceAmbientSceneType = (byte) deviceAmbientSceneType;
        this.deviceAmbientScene = (byte) deviceAmbientScene;
    }

    static Scene fromDevice(byte deviceAmbientSceneType, byte deviceAmbientScene) {
        return Scene.values()[deviceAmbientSceneType == 0 ? 0 : deviceAmbientScene + 1];
    }

    static Scene fromState(DecimalType state) {
        return Scene.values()[state.intValue()];
    }

    public DecimalType state() {
        return new DecimalType(this.ordinal());
    }
}