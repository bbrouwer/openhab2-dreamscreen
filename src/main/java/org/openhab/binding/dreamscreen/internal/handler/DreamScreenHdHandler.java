package org.openhab.binding.dreamscreen.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.dreamscreen.internal.message.RefreshTvMessage;

@NonNullByDefault
public class DreamScreenHdHandler extends DreamScreenBaseTvHandler {
    public final static byte PRODUCT_ID = 0x01;

    public DreamScreenHdHandler(Thing thing, DreamScreenInputDescriptionProvider descriptionProvider) {
        super(thing, descriptionProvider);
    }

    @Override
    protected boolean refreshTvMsg(final RefreshTvMessage msg) {
        if (msg.getProductId() == PRODUCT_ID) {
            return super.refreshTvMsg(msg);
        }
        return false;
    }
}
