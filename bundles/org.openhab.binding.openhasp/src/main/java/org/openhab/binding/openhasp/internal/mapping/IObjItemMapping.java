package org.openhab.binding.openhasp.internal.mapping;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.ObjectEvent;
import org.openhab.core.items.Item;

@NonNullByDefault
public interface IObjItemMapping {

    @NonNull
    List<String> getIds();

    @Nullable
    Item getItem();

    /**
     * Called when an event is received from the HASP plate
     * 
     * @param objectEvent
     */
    void haspEventReceived(ObjectEvent objectEvent);

    /**
     * Called when the Item state for this mapping has changed
     */
    void updateState();
}
