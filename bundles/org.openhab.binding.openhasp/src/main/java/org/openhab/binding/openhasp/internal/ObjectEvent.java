package org.openhab.binding.openhasp.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class ObjectEvent {
    @Nullable
    String event;
    @Nullable
    String val;
    @Nullable
    String text;

    @Override
    public String toString() {
        return "ObjectEvent [event=" + event + ", val=" + val + ", text=" + text + "]";
    }
}
