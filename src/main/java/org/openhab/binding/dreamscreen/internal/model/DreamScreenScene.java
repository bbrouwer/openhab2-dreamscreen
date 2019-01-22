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
package org.openhab.binding.dreamscreen.internal.model;

import org.eclipse.smarthome.core.library.types.DecimalType;

public enum DreamScreenScene {
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

    public final byte ambientModeType;
    public final byte ambientScene;

    private DreamScreenScene(int ambientModeType, int ambientScene) {
        this.ambientModeType = (byte) ambientModeType;
        this.ambientScene = (byte) ambientScene;
    }

    public static DreamScreenScene fromDevice(byte ambientModeType, byte ambientScene) {
        return DreamScreenScene.values()[ambientModeType == 0 ? 0 : ambientScene + 1];
    }

    public static DreamScreenScene fromDeviceScene(byte ambientScene) {
        return DreamScreenScene.values()[ambientScene + 1];
    }

    static DreamScreenScene fromScene(byte ambientScene) {
        return DreamScreenScene.values()[ambientScene + 1];
    }

    public static DreamScreenScene fromState(DecimalType state) {
        return DreamScreenScene.values()[state.intValue()];
    }

    public DecimalType state() {
        return new DecimalType(this.ordinal());
    }
}