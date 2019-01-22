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
package org.openhab.binding.dreamscreen.internal.handler;

import static org.eclipse.smarthome.core.library.types.OnOffType.*;
import static org.eclipse.smarthome.core.thing.ThingStatus.*;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static org.openhab.binding.dreamscreen.internal.DreamScreenBindingConstants.*;
import static org.openhab.binding.dreamscreen.internal.model.DreamScreenMode.*;
import static org.openhab.binding.dreamscreen.internal.model.DreamScreenScene.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

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
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.dreamscreen.internal.DreamScreenConfiguration;
import org.openhab.binding.dreamscreen.internal.DreamScreenServer;
import org.openhab.binding.dreamscreen.internal.message.AmbientModeTypeMessage;
import org.openhab.binding.dreamscreen.internal.message.ColorMessage;
import org.openhab.binding.dreamscreen.internal.message.DreamScreenMessage;
import org.openhab.binding.dreamscreen.internal.message.ModeMessage;
import org.openhab.binding.dreamscreen.internal.message.RefreshMessage;
import org.openhab.binding.dreamscreen.internal.message.SceneMessage;
import org.openhab.binding.dreamscreen.internal.message.SerialNumberMessage;
import org.openhab.binding.dreamscreen.internal.model.DreamScreenMode;
import org.openhab.binding.dreamscreen.internal.model.DreamScreenScene;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DreamScreenBaseHandler} is responsible for handling DreamScreen commands
 *
 * @author Bruce Brouwer
 */
@NonNullByDefault
public abstract class DreamScreenBaseHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(DreamScreenBaseHandler.class);

    private @Nullable ServiceTracker<DreamScreenServer, DreamScreenServer> serverTracker;
    private @Nullable DreamScreenServer server;

    protected int serialNumber;
    private @Nullable InetAddress address;
    protected byte group = 0;
    // private boolean powerOn;
    private byte mode = 0;
    private DreamScreenMode powerOnMode = VIDEO; // TODO: consider persisting this
    private byte ambientModeType = COLOR.ambientModeType;
    private byte ambientScene = RANDOM_COLOR.ambientScene;
    private @Nullable DreamScreenScene newScene = null;
    private HSBType color = HSBType.WHITE;
    private boolean isOnline = false;

    public DreamScreenBaseHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        DreamScreenConfiguration config = getConfigAs(DreamScreenConfiguration.class);
        updateStatus(UNKNOWN);
        this.serialNumber = Integer.valueOf(config.serialNumber);
        logger.debug("Initializing {}", this.serialNumber);

        DreamScreenServer server = this.server;
        if (server != null) {
            server.addHandler(this);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        switch (channelUID.getId()) {
            case CHANNEL_POWER:
                powerCommand(command);
                break;
            case CHANNEL_MODE:
                modeCommand(command);
                break;
            case CHANNEL_SCENE:
                sceneCommand(command);
                break;
            case CHANNEL_COLOR:
                colorCommand(command);
                break;
        }
    }

    protected void online() {
        if (!this.isOnline) {
            updateStatus(ONLINE);
        }
    }

    public final boolean message(final DreamScreenMessage msg, final InetAddress address) {
        if (msg instanceof SerialNumberMessage) {
            return link(((SerialNumberMessage) msg).getSerialNumber(), address);
        } else if (!address.equals(this.address)) {
            return false;
        }
        return processMsg(msg, address);
    }

    protected boolean processMsg(final DreamScreenMessage msg, final InetAddress address) {
        if (msg instanceof RefreshMessage) {
            return refreshMsg((RefreshMessage) msg);
        } else if (msg instanceof ModeMessage) {
            return modeMsg((ModeMessage) msg);
        } else if (msg instanceof ColorMessage) {
            return colorMsg((ColorMessage) msg);
        } else if (msg instanceof AmbientModeTypeMessage) {
            return ambientModeTypeMsg((AmbientModeTypeMessage) msg);
        } else if (msg instanceof SceneMessage) {
            return ambientSceneMsg((SceneMessage) msg);
        }
        return true;
    }

    public boolean link(final int serialNumber, final InetAddress address) {
        if (this.serialNumber == serialNumber) {
            logger.debug("Linking {} to {}", serialNumber, address);
            this.address = address;

            delayedWrite(new RefreshMessage());
            return true;
        }
        return false;
    }

    protected boolean refreshMsg(final RefreshMessage msg) {
        online();
        this.group = msg.getGroup();
        modeRefresh(msg.getMode());
        colorRefresh(msg.getRed(), msg.getGreen(), msg.getBlue());
        this.ambientScene = msg.getScene(); // ambientSceneRefresh(msg.getScene());
        delayedRead(new AmbientModeTypeMessage(this.group, this.ambientModeType));
        return true;
    }

    private void powerCommand(Command command) {
        if (command instanceof OnOffType) {
            logger.debug("Changing {} power to {}", this.serialNumber, command);
            try {
                write(new ModeMessage(this.group, command == ON ? powerOnMode.deviceMode : 0));
            } catch (IOException e) {
                logger.error("Error changing {} power state", this.serialNumber, e);
                updateStatus(OFFLINE, COMMUNICATION_ERROR, "Cannot send power command");
            }
        } else if (command instanceof RefreshType) {
            updateState(CHANNEL_POWER, this.mode == 0 ? OFF : ON);
        }
    }

    private void modeCommand(Command command) {
        if (command instanceof DecimalType) {
            logger.debug("Changing {} mode to {}", this.serialNumber, command);
            try {
                final DreamScreenMode mode = DreamScreenMode.fromState((DecimalType) command);
                if (this.mode != 0) {
                    write(new ModeMessage(this.group, mode.deviceMode));
                }
            } catch (IOException e) {
                logger.error("Error changing {} mode", this.serialNumber, e);
                updateStatus(OFFLINE, COMMUNICATION_ERROR, "Cannot send mode command");
            }
        } else if (command instanceof RefreshType) {
            updateState(CHANNEL_MODE,
                    this.mode == 0 ? this.powerOnMode.state() : DreamScreenMode.fromDevice(this.mode).state());
        }
    }

    private boolean modeMsg(final ModeMessage msg) {
        online();
        modeRefresh(msg.getMode());
        if (msg.getMode() == AMBIENT.deviceMode) {
            DreamScreenScene updateToScene = this.newScene;
            if (updateToScene != null) {
                delayedWrite(new AmbientModeTypeMessage(this.group, updateToScene.ambientModeType));
            }
        }
        return true;
    }

    private void modeRefresh(final byte newDeviceMode) {
        this.mode = newDeviceMode;

        final DreamScreenMode newMode = DreamScreenMode.fromDevice(newDeviceMode);
        if (newMode == null) {
            updateState(CHANNEL_POWER, OFF);
        } else {
            updateState(CHANNEL_POWER, ON);
            this.powerOnMode = newMode;
            updateState(CHANNEL_MODE, newMode.state());
        }
    }

    private void sceneCommand(Command command) {
        if (command instanceof DecimalType) {
            logger.debug("Changing {} scene to {}", this.serialNumber, command);
            try {
                final DreamScreenScene scene = DreamScreenScene.fromState((DecimalType) command);
                if (this.mode != AMBIENT.deviceMode) {
                    this.newScene = scene;
                    write(new ModeMessage(this.group, AMBIENT.deviceMode));
                } else if (scene.ambientModeType != this.ambientModeType) {
                    this.newScene = scene;
                    write(new AmbientModeTypeMessage(this.group, scene.ambientModeType));
                } else {
                    this.newScene = null;
                    write(new SceneMessage(this.group, scene.ambientScene));
                }
            } catch (IOException e) {
                logger.error("Error changing {} scene", this.serialNumber, e);
                updateStatus(OFFLINE, COMMUNICATION_ERROR, "Cannot send scene command");
            }
        } else if (command instanceof RefreshType) {
            updateState(CHANNEL_SCENE, DreamScreenScene.fromDevice(this.ambientModeType, this.ambientScene).state());
        }
    }

    private boolean ambientModeTypeMsg(final AmbientModeTypeMessage msg) {
        online();
        this.ambientModeType = msg.getAmbientModeType();

        final DreamScreenScene updateToScene = newScene;
        if (updateToScene != null && updateToScene.ambientModeType == msg.getAmbientModeType()) {
            if (msg.getAmbientModeType() == COLOR.ambientModeType) {
                updateState(CHANNEL_SCENE, COLOR.state());
            } else {
                delayedWrite(new SceneMessage(this.group, updateToScene.ambientScene));
            }
        } else {
            updateState(CHANNEL_SCENE,
                    DreamScreenScene.fromDevice(msg.getAmbientModeType(), this.ambientScene).state());
        }
        this.newScene = null;
        return true;
    }

    private boolean ambientSceneMsg(final SceneMessage msg) {
        online();
        DreamScreenScene scene = DreamScreenScene.fromDeviceScene(msg.getScene());
        this.ambientModeType = scene.ambientModeType;
        ambientSceneRefresh(scene.ambientScene);
        this.newScene = null;
        return true;
    }

    private void ambientSceneRefresh(final byte newAmbientScene) {
        this.ambientScene = newAmbientScene;
        updateState(CHANNEL_SCENE, DreamScreenScene.fromDevice(this.ambientModeType, this.ambientScene).state());
    }

    private void colorCommand(Command command) {
        if (command instanceof HSBType) {
            logger.debug("Changing {} color to {}", this.serialNumber, command);
            final HSBType color = (HSBType) command;

            try {
                write(buildColorMsg(color));
                if (this.mode != AMBIENT.deviceMode) {
                    this.newScene = COLOR;
                    this.color = color;
                    delayedWrite(new ModeMessage(this.group, AMBIENT.deviceMode));
                } else if (this.ambientModeType != COLOR.ambientModeType) {
                    this.newScene = COLOR;
                    this.color = color;
                    delayedWrite(new AmbientModeTypeMessage(this.group, COLOR.ambientModeType));
                }
            } catch (IOException e) {
                logger.error("Unable to change {} color", this.serialNumber, e);
                this.newScene = null;
                updateStatus(OFFLINE, COMMUNICATION_ERROR, "Cannot send color command");
            }
        } else if (command instanceof RefreshType) {
            updateState(CHANNEL_COLOR, this.color);
        }
    }

    private ColorMessage buildColorMsg(HSBType color) {
        final PercentType[] rgb = color.toRGB();
        final byte red = colorByte(rgb[0]);
        final byte green = colorByte(rgb[1]);
        final byte blue = colorByte(rgb[2]);
        return new ColorMessage(this.group, red, green, blue);
    }

    private byte colorByte(PercentType percent) {
        return percent.toBigDecimal().multiply(BigDecimal.valueOf(255))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).byteValue();
    }

    private boolean colorMsg(final ColorMessage msg) {
        online();
        colorRefresh(msg.getRed(), msg.getGreen(), msg.getBlue());
        return true;
    }

    private void colorRefresh(final byte red, final byte green, final byte blue) {
        this.color = HSBType.fromRGB(red & 0xFF, green & 0xFF, blue & 0xFF);
        updateState(CHANNEL_COLOR, this.color);
    }

    protected void read(final DreamScreenMessage msg) throws IOException {
        final DreamScreenServer server = this.server;
        final InetAddress address = this.address;
        if (server != null && address != null) {
            server.read(msg, address);
        } else {
            logger.warn("DreamScreen {} is not on-line", this.serialNumber);
        }
    }

    protected void delayedRead(final DreamScreenMessage msg) {
        this.scheduler.schedule(() -> {
            try {
                read(msg);
            } catch (IOException e) {
                logger.error("Unable to send delayed read message {} to {}", msg, this.serialNumber, e);
                updateStatus(OFFLINE, COMMUNICATION_ERROR, "Cannot send messag");
            }
        }, 10, TimeUnit.MILLISECONDS);
    }

    protected void write(final DreamScreenMessage msg) throws IOException {
        final DreamScreenServer server = this.server;
        final InetAddress address = this.address;
        if (server != null && address != null) {
            server.write(msg, address);
        } else {
            logger.warn("DreamScreen {} is not on-line", this.serialNumber);
        }
    }

    protected void delayedWrite(final DreamScreenMessage msg) {
        this.scheduler.schedule(() -> {
            try {
                write(msg);
            } catch (IOException e) {
                logger.error("Unable to send delayed write message {} to {}", msg, this.serialNumber, e);
                updateStatus(OFFLINE, COMMUNICATION_ERROR, "Cannot send messag");
            }
        }, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        this.isOnline = status == ONLINE;
        super.updateStatus(status, statusDetail, description);
    }

    @Override
    public void setBundleContext(final BundleContext bundleContext) {
        final ServiceTracker<DreamScreenServer, DreamScreenServer> tracker;
        tracker = new ServiceTracker<DreamScreenServer, DreamScreenServer>(bundleContext, DreamScreenServer.class,
                null) {
            @Override
            public DreamScreenServer addingService(final @Nullable ServiceReference<DreamScreenServer> reference) {
                final DreamScreenServer server = bundleContext.getService(reference);
                DreamScreenBaseHandler.this.server = server;
                if (DreamScreenBaseHandler.this.serialNumber != 0) {
                    server.addHandler(DreamScreenBaseHandler.this);
                }
                return server;
            }

            @Override
            public void removedService(final @Nullable ServiceReference<DreamScreenServer> reference,
                    final @Nullable DreamScreenServer service) {
                if (service != null && service == DreamScreenBaseHandler.this.server) {
                    service.removeHandler(DreamScreenBaseHandler.this);
                    DreamScreenBaseHandler.this.server = null;
                }
            }
        };
        this.serverTracker = tracker;
        tracker.open();
    }

    @Override
    public void unsetBundleContext(final BundleContext bundleContext) {
        final ServiceTracker<DreamScreenServer, DreamScreenServer> tracker = this.serverTracker;
        if (tracker != null) {
            tracker.close();
        }
    }

    @Override
    public void dispose() {
        final DreamScreenServer server = this.server;
        if (server != null) {
            server.removeHandler(this);
        }
        super.dispose();
    }
}
