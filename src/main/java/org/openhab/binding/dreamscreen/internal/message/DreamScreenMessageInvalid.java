package org.openhab.binding.dreamscreen.internal.message;

import java.io.IOException;

public class DreamScreenMessageInvalid extends IOException {
    private static final long serialVersionUID = 1L;

    public DreamScreenMessageInvalid(String message) {
        super(message);
    }
}
