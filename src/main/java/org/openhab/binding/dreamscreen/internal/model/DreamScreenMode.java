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

public enum DreamScreenMode {
    VIDEO(1),
    MUSIC(2),
    AMBIENT(3);

    public final byte deviceMode;

    private DreamScreenMode(int deviceMode) {
        this.deviceMode = (byte) deviceMode;
    }

    public static DreamScreenMode fromDevice(byte value) {
        return value > 0 ? DreamScreenMode.values()[value - 1] : null;
    }

    public static DreamScreenMode fromState(DecimalType command) {
        return DreamScreenMode.values()[command.intValue()];
    }

    public DecimalType state() {
        return new DecimalType(this.ordinal());
    }
}