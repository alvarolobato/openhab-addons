package org.openhab.binding.openhasp.internal.layout.components;

import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.ICON_OFF;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.ICON_ON;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.OBJECT_ID;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.WIDGET_ICON;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.WIDGET_LABEL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.ObjectEvent;
import org.openhab.binding.openhasp.internal.layout.TemplateProcessor;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ButtonWidget extends AbstractComponent {
    private static final Logger logger = LoggerFactory.getLogger(ButtonWidget.class);

    String label;

    String itemIcon;

    String widgetIcon;

    @Nullable
    String objectIdClick;

    @Nullable
    String objectIdLabel;

    @Nullable
    String objectIdIcon;

    @Nullable
    String objectIdSwitch;

    OnOffType localState;

    public ButtonWidget(HashMap<String, String> context, Widget w, @Nullable Item item) {
        super(context, w, item);
        localState = OnOffType.OFF;
        this.label = getWidgetLabel(w, item != null ? item.getState() : null);
        this.itemIcon = getWidgetIcon(w);
        this.widgetIcon = resolveIcon(itemIcon + ICON_OFF, itemIcon);
    }

    public String getComponent() {
        return "button";
    }

    @Override
    public void render(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException {

        context.put(WIDGET_LABEL, label);
        context.put(WIDGET_ICON, widgetIcon);

        objectArray.addAll(tplProc.processTemplate(getComponentTemplate(getComponent()), context));

        // process mappings
        objectIdClick = context.get(OBJECT_ID + "click");
        objectIdLabel = context.get(OBJECT_ID + "label");
        objectIdIcon = context.get(OBJECT_ID + "icon");
        objectIdSwitch = context.get(OBJECT_ID + "switch");
    }

    @Override
    public List<String> getIds() {
        ArrayList<String> ids = new ArrayList<String>();
        String id = objectIdClick;
        if (id != null) {
            ids.add(id);
        }
        id = objectIdLabel;
        if (id != null) {
            ids.add(id);
        }
        id = objectIdIcon;
        if (id != null) {
            ids.add(id);
        }
        id = objectIdSwitch;
        if (id != null) {
            ids.add(id);
        }
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

    @Override
    public void sendStatusUpdate(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException {

        context.put(WIDGET_LABEL, label);
        context.put(WIDGET_ICON, widgetIcon);

        addToContextSafe(context, OBJECT_ID + "label", objectIdLabel);
        addToContextSafe(context, OBJECT_ID + "icon", objectIdIcon);
        addToContextSafe(context, OBJECT_ID + "click", objectIdClick);
        addToContextSafe(context, OBJECT_ID + "switch", objectIdSwitch);

        objectArray.addAll(
                tplProc.processTemplate(getComponentStatusTemplate(localState.toString().toLowerCase()), context));
    }
}
