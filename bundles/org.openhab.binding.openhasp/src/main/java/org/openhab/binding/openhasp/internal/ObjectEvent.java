package org.openhab.binding.openhasp.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class ObjectEvent {
    @Nullable
    public String source;
    @Nullable
    public String event;
    @Nullable
    public String val;
    @Nullable
    public String text;

    @Override
    public String toString() {
        return "ObjectEvent [source=" + source + " event=" + event + ", val=" + val + ", text=" + text + "]";
    }
}
