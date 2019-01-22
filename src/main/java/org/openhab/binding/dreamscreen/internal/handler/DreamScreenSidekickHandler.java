package org.openhab.binding.dreamscreen.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.dreamscreen.internal.message.RefreshMessage;

@NonNullByDefault
public class DreamScreenSidekickHandler extends DreamScreenBaseHandler {
    public final static byte PRODUCT_ID = 0x03;

    public DreamScreenSidekickHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected boolean refreshMsg(final RefreshMessage msg) {
        if (msg.getProductId() == PRODUCT_ID) {
            return super.refreshMsg(msg);
        }
        return false;
    }
}
