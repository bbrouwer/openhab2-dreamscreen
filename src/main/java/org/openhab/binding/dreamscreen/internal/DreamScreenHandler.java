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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.smarthome.core.library.types.OnOffType.*;
import static org.openhab.binding.dreamscreen.internal.DreamScreenBindingConstants.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DreamScreenHandler} is responsible for handling DreamScreen commands
 *
 * @author Bruce Brouwer
 */
@NonNullByDefault
public class DreamScreenHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(DreamScreenHandler.class);
    private final DreamScreenDatagramServer server;
    private final @Nullable DreamScreenDynamicStateDescriptionProvider descriptionProvider;
    private long lastRefresh;
    @Nullable
    String name;
    @Nullable
    InetAddress address;
    byte group = 0;
    private byte ambientModeType = -1; // TODO: init this once ambientModeType is part of update msg
    private byte ambientScene = 0;
    private boolean powerOn;
    private DreamScreenMode powerOnMode = DreamScreenMode.VIDEO; // TODO: consider persisting this

    public DreamScreenHandler(Thing thing, DreamScreenDatagramServer server,
            @Nullable DreamScreenDynamicStateDescriptionProvider descriptionProvider) {
        super(thing);
        this.server = server;
        this.descriptionProvider = descriptionProvider;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing DreamScreen device");
        DreamScreenConfiguration config = getConfigAs(DreamScreenConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);
        try {
            this.name = config.name;
            server.initialize(this);
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Cannot initialize DreamScreen at " + this.address);
            return;
        }
    }

    @Override
    public void dispose() {
        final ThingUID thingUID = this.getThing().getUID();
        server.dispose(this);
        super.dispose();
        final DreamScreenDynamicStateDescriptionProvider descProvider = this.descriptionProvider;
        if (descProvider != null) {
            descProvider.removeThingDescriptions(thingUID);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            requestRefresh();
        } else {
            switch (channelUID.getId()) {
                case CHANNEL_POWER:
                    changePower(command);
                    break;
                case CHANNEL_MODE:
                    changeMode(command);
                    break;
                case CHANNEL_SCENE:
                    changeScene(command);
                    break;
                case CHANNEL_INPUT:
                    changeInput(command);
                    break;
                case CHANNEL_COLOR:
                    changeColor(command);
                    break;
            }
        }
    }

    private void requestRefresh() {
        synchronized (this) {
            final long now = System.currentTimeMillis();
            if (now - lastRefresh < 1000) {
                return;
            }
            lastRefresh = now;
        }
        try {
            final InetAddress address = this.address;
            if (address == null) {
                server.broadcast(0xFF, 0x30, 0x01, 0x0A, new byte[0]);
            } else {
                server.send(0xFF, 0x01, 0x0A, new byte[0], address);
            }
        } catch (IOException e) {
            logger.error("Error requesting refresh", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Cannot send power command to " + this.name);
        }
    }

    void processMessage(final byte[] data, final int off, final int len) {
        final int upperCommand = data[off + 4];
        final int lowerCommand = data[off + 5];

        if (upperCommand == 0x01 && lowerCommand == 0x0A) {
            updateState(data, off, len);
        } else if (upperCommand == 0x03 && lowerCommand == 0x01 && len > 6) {
            updateMode(data[off + 6]);
            // } else if (upperCommand == 0x03 && lowerCommand == 0x02 && len > 6) {
            // refreshBrightness(data[off + 6]);
        } else if (upperCommand == 0x03 && lowerCommand == 0x05 && len > 8) {
            updateColor(data[off + 6], data[off + 7], data[off + 8]);
        } else if (upperCommand == 0x03 && lowerCommand == 0x08 && len > 6) {
            updateAmbientModeType(data[off + 6]);
        } else if (upperCommand == 0x03 && lowerCommand == 0x0D && len > 6) {
            updateAmbientScene(data[off + 6]);
        } else if (upperCommand == 0x03 && lowerCommand == 0x20 && len > 6) {
            updateInput(data[off + 6]);
        }
        updateStatus(ThingStatus.ONLINE);
    }

    private void updateState(final byte[] data, final int off, final int len) {
        updateMode(data[off + 39]);
        updateAmbientScene(data[off + 68]);
        updateInputNames(new String(data, off + 81, 16, UTF_8), //
                new String(data, off + 97, 16, UTF_8), //
                new String(data, off + 113, 16, UTF_8));
        updateInput(data[off + 79]);
        updateStatus(ThingStatus.ONLINE);
    }

    private void changePower(Command command) {
        if (command instanceof OnOffType) {
            try {
                send(0x03, 0x01, new byte[] { command == ON ? powerOnMode.deviceMode : 0 });
                // this.powerOn = command == ON;
                // updateState(CHANNEL_POWER, (OnOffType) command);
            } catch (IOException e) {
                logger.error("Error changing power state", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Cannot send power command to " + this.name);
            }
        }
    }

    private void changeMode(Command command) {
        if (command instanceof DecimalType) {
            try {
                final DreamScreenMode mode = DreamScreenMode.fromState((DecimalType) command);
                if (this.powerOn) {
                    send(0x03, 0x01, new byte[] { mode.deviceMode });
                }
                // this.powerOnMode = mode;
                // updateState(CHANNEL_MODE, mode.state());
            } catch (IOException e) {
                logger.error("Error changing mode", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Cannot send mode command to " + this.name);
            }
        }
    }

    private void updateMode(final byte newDeviceMode) {
        final DreamScreenMode mode = DreamScreenMode.fromDevice(newDeviceMode);
        if (mode == null) {
            this.powerOn = false;
            updateState(CHANNEL_POWER, OFF);
        } else {
            this.powerOn = true;
            updateState(CHANNEL_POWER, ON);
            this.powerOnMode = mode;
            updateState(CHANNEL_MODE, mode.state());
        }
    }

    private void changeScene(Command command) {
        if (command instanceof DecimalType) {
            try {
                final DreamScreenScene scene = DreamScreenScene.fromState((DecimalType) command);
                if (this.ambientModeType != scene.ambientModeType) {
                    this.ambientModeType = scene.ambientModeType; // TODO: remove once available from update msg
                    send(0x03, 0x08, new byte[] { scene.ambientModeType });
                }
                if (scene.ambientModeType == 1) {
                    send(0x03, 0x0D, new byte[] { scene.ambientScene });
                }
            } catch (IOException e) {
                logger.error("Error changing scene", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Cannot send scene command to " + this.name);
            }
        }
    }

    private void updateAmbientModeType(final byte newAmbientModeType) {
        this.ambientModeType = newAmbientModeType;
        updateState(CHANNEL_SCENE, DreamScreenScene.fromDevice(newAmbientModeType, this.ambientScene).state());
    }

    private void updateAmbientScene(final byte newAmbientScene) {
        this.ambientScene = newAmbientScene;
        updateState(CHANNEL_SCENE, DreamScreenScene.fromDevice(this.ambientModeType, newAmbientScene).state());
    }

    private void changeInput(Command command) {
        if (command instanceof DecimalType) {
            try {
                send(0x03, 0x20, new byte[] { ((DecimalType) command).byteValue() });
                updateState(CHANNEL_INPUT, (DecimalType) command);
            } catch (IOException e) {
                logger.error("Error changing input", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Cannot send input change command to " + this.name);
            }
        }
    }

    private void updateInputNames(String channel1, String channel2, String channel3) {
        final DreamScreenDynamicStateDescriptionProvider descProvider = this.descriptionProvider;
        if (descProvider != null) {
            final ChannelUID inputChannelUID = new ChannelUID(this.getThing().getUID(), CHANNEL_INPUT);
            final List<StateOption> options = Arrays.asList( //
                    new StateOption("0", channel1.trim()), new StateOption("1", channel2.trim()),
                    new StateOption("2", channel3.trim()));
            final StateDescription description = new StateDescription(BigDecimal.ZERO, BigDecimal.valueOf(2),
                    BigDecimal.ONE, null, false, options);
            descProvider.setChannelDescription(inputChannelUID, description);
        }
    }

    private void updateInput(final byte newInput) {
        updateState(CHANNEL_INPUT, new DecimalType(newInput));
    }

    private void changeColor(Command command) {
        if (command instanceof HSBType) {
            try {
                final HSBType color = (HSBType) command;
                final PercentType[] rgb = color.toRGB();
                send(0x03, 0x05, new byte[] { toColorByte(rgb[0]), toColorByte(rgb[1]), toColorByte(rgb[2]) });
                // updateState(CHANNEL_COLOR, color);
            } catch (IOException e) {
                logger.error("Error changing ambient color", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Cannot send color change command to " + this.name);
            }
        }
    }

    private byte toColorByte(PercentType percent) {
        return percent.toBigDecimal().multiply(BigDecimal.valueOf(255))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).byteValue();
    }

    private void updateColor(byte red, byte green, byte blue) {
        updateState(CHANNEL_COLOR, HSBType.fromRGB(red & 0xFF, green & 0xFF, blue & 0xFF));
    }

    private void send(int commandUpper, int commandLower, byte[] payload) throws IOException {
        if (this.address != null) {
            server.send(this.group, commandUpper, commandLower, payload, this.address);
        } else {
            logger.warn("DreamScreen {} is not on-line", this.name);
        }
    }
}
