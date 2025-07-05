package org.openhab.binding.openhasp.internal.layout.components;

import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.ICON_OFF;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.ICON_ON;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.OBJECT_ID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.ObjectEvent;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.ui.items.ItemUIRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ButtonWidget extends LabelIconComponent {
    private static final Logger logger = LoggerFactory.getLogger(ButtonWidget.class);

    @Nullable
    String objectIdClick;

    @Nullable
    String objectIdSwitch;

    OnOffType localState;

    public ButtonWidget(HashMap<String, String> context, Widget w, @Nullable Item item,
            @NonNull ItemUIRegistry itemUIRegistry) {
        super(context, w, item, itemUIRegistry);
        localState = OnOffType.OFF;
    }

    public String getComponent() {
        return "button";
    }

    @Override
    public void prepareContext(Map<String, String> context) {
        super.prepareContext(context);
        addToContextSafe(context, OBJECT_ID + "click", objectIdClick);
        addToContextSafe(context, OBJECT_ID + "switch", objectIdSwitch);
        addToContextSafe(context, "widgetVal", OnOffType.ON.equals(localState) ? "1" : "0");
    }

    @Override
    public void readFromContext(Map<String, String> context) {
        super.readFromContext(context);
        objectIdClick = context.get(OBJECT_ID + "click");
        objectIdSwitch = context.get(OBJECT_ID + "switch");
    }

    @Override
    public List<String> getIds() {
        List<String> ids = super.getIds();
        addToListSafe(ids, objectIdClick);
        addToListSafe(ids, objectIdSwitch);
        return ids;
    }

    @Override
    public void haspEventReceived(ObjectEvent objectEvent) {
        if (item != null) {
            if (objectEvent.event != null
                    && (objectEvent.event.contains("up") || objectEvent.event.contains("release"))) { // Click
                // button
                logger.trace("BUTTON PRESS {} - event: {}", item, objectEvent);
                if (item instanceof SwitchItem) {
                    SwitchItem sItem = (SwitchItem) item;
                    OnOffType targetState = null;
                    if (objectEvent.val != null) { // Event comes from the switch
                        if ("0".equals(objectEvent.val)) {
                            targetState = OnOffType.OFF;
                        } else {
                            targetState = OnOffType.ON;
                        }
                        logger.trace("Event from switch sending {}", targetState);
                    } else { // Event comes from the label -> switch to the oposite state
                        OnOffType sState = sItem.getStateAs(OnOffType.class);
                        if (OnOffType.ON.equals(sState)) {
                            targetState = OnOffType.OFF;
                        } else {
                            targetState = OnOffType.ON;
                        }
                        logger.trace("Event from label current state {} sending {}", sState, targetState);
                    }
                    sItem.send(targetState);
                } else {
                    logger.warn("Item {} - {} for object {} was not type switch {}", item,
                            item.getClass().getSimpleName(), objectEvent.source, item);
                }
            }
        } else {
            logger.warn("Item for {} not found, mapping was {}", item, this);
        }
    }

    @Override
    public void updateState() {
        super.updateState();
        Item item = getItem();
        if (item != null) {
            OnOffType sState = item.getStateAs(OnOffType.class);
            logger.trace("Current state {}", sState);
            if (OnOffType.ON.equals(sState)) {
                logger.trace("Update state to ON {}", this);
                localState = OnOffType.ON;
                widgetIcon = resolveIcon(itemIcon + ICON_ON, itemIcon);
            } else {
                logger.trace("Update state to OFF {}", this);
                localState = OnOffType.OFF;
                widgetIcon = resolveIcon(itemIcon + ICON_OFF, itemIcon);
            }
        } else {
            logger.warn("Widget {} tried to update state but Item", this);
        }
    }
}
