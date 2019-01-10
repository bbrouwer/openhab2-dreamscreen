/**
 * Copyright (c) 2018-2019 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.dreamscreen.internal;

import org.eclipse.smarthome.core.library.types.DecimalType;

enum DreamScreenScene {
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

    final byte ambientModeType;
    final byte ambientScene;

    private DreamScreenScene(int deviceAmbientSceneType, int deviceAmbientScene) {
        this.ambientModeType = (byte) deviceAmbientSceneType;
        this.ambientScene = (byte) deviceAmbientScene;
    }

    static DreamScreenScene fromDevice(byte deviceAmbientSceneType, byte deviceAmbientScene) {
        return DreamScreenScene.values()[deviceAmbientSceneType == 0 ? 0 : deviceAmbientScene + 1];
    }

    static DreamScreenScene fromState(DecimalType state) {
        return DreamScreenScene.values()[state.intValue()];
    }

    public DecimalType state() {
        return new DecimalType(this.ordinal());
    }
}