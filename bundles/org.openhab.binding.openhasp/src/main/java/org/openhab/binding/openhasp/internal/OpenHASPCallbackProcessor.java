package org.openhab.binding.openhasp.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface OpenHASPCallbackProcessor {
    void plateCallback(String strippedTopic, String value);
}
