package org.openhab.binding.dreamscreen.internal.handler;

import static org.openhab.binding.dreamscreen.internal.DreamScreenBindingConstants.CHANNEL_INPUT;

import java.io.IOException;
import java.net.InetAddress;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.dreamscreen.internal.message.DreamScreenMessage;
import org.openhab.binding.dreamscreen.internal.message.InputMessage;
import org.openhab.binding.dreamscreen.internal.message.RefreshTvMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class DreamScreenBaseTvHandler extends DreamScreenBaseHandler {
    private final Logger logger = LoggerFactory.getLogger(DreamScreenBaseTvHandler.class);
    private final DreamScreenInputDescriptionProvider descriptionProvider;
    private byte input = 0;

    public DreamScreenBaseTvHandler(Thing thing, DreamScreenInputDescriptionProvider descriptionProvider) {
        super(thing);
        this.descriptionProvider = descriptionProvider;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_INPUT.equals(channelUID.getId())) {
            inputCommand(command);
        } else {
            super.handleCommand(channelUID, command);
        }
    }

    @Override
    protected boolean processMsg(final DreamScreenMessage msg, final InetAddress address) {
        if (msg instanceof InputMessage) {
            return inputMsg((InputMessage) msg);
        } else if (msg instanceof RefreshTvMessage) {
            return refreshTvMsg((RefreshTvMessage) msg);
        }
        return super.processMsg(msg, address);
    }

    protected boolean refreshTvMsg(final RefreshTvMessage msg) {
        online();
        inputNamesRefresh(msg);
        inputRefresh(msg.getInput());
        return super.refreshMsg(msg);
    }

    private void inputCommand(Command command) {
        if (command instanceof DecimalType) {
            logger.debug("Changing input to {} of {}", command, this.serialNumber);
            try {
                byte newInput = ((DecimalType) command).byteValue();
                write(new InputMessage(this.group, newInput));
            } catch (IOException e) {
                logger.error("Error changing input of {}", this.serialNumber, e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Cannot send input change command to " + this.serialNumber);
            }
        } else if (command instanceof RefreshType) {
            updateState(CHANNEL_INPUT, new DecimalType(this.input));
        }
    }

    private void inputNamesRefresh(final RefreshTvMessage msg) {
        this.descriptionProvider.setInputDescriptions(msg.getInputName1(), msg.getInputName2(), msg.getInputName3());
    }

    private boolean inputMsg(final InputMessage msg) {
        online();
        inputRefresh(msg.getInput());
        return true;
    }

    private void inputRefresh(final byte newInput) {
        this.input = newInput;
        updateState(CHANNEL_INPUT, new DecimalType(newInput));
    }

}
