package org.openhab.binding.openhasp.internal.layout.components;

import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.OBJECT_ID;
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
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class SliderWidget extends AbstractComponent {
    private static final Logger logger = LoggerFactory.getLogger(SliderWidget.class);
    String label;

    @Nullable
    String objectIdClick;
    @Nullable
    String objectIdSlide;
    @Nullable
    String objectIdLabel;
    private double localValue;

    public SliderWidget(HashMap<String, String> context, @Nullable Widget w, @Nullable Item item) {
        super(context, w, item);
        this.label = getWidgetLabel(w, item != null ? item.getState() : null);
        localValue = 0;
    }

    public String getComponent() {
        return "slider";
    }

    @Override
    public void render(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException {

        context.put(WIDGET_LABEL, label);

        objectArray.addAll(tplProc.processTemplate(getComponentTemplate(getComponent()), context));

        // process mappings
        objectIdClick = context.get(OBJECT_ID + "click");
        objectIdSlide = context.get(OBJECT_ID + "slide");
        objectIdLabel = context.get(OBJECT_ID + "label");
    }

    @Override
    public List<String> getIds() {
        ArrayList<String> ids = new ArrayList<String>();
        String id = objectIdClick;
        if (id != null) {
            ids.add(id);
        }
        id = objectIdSlide;
        if (id != null) {
            ids.add(id);
        }
        id = objectIdLabel;
        if (id != null) {
            ids.add(id);
        }
        return ids;
    }

    @Override
    public void haspEventReceived(ObjectEvent objectEvent) {
        if (objectEvent.event != null && objectEvent.val != null
                && (objectEvent.event.contains("up") || objectEvent.event.contains("changed"))) { // Slider
            logger.trace("SLIDER CHANGE {} - event: {}", item, objectEvent);
            if (item != null) {
                String percentValue = objectEvent.val;
                if (percentValue != null) {
                    if (item instanceof DimmerItem) {
                        DimmerItem dItem = (DimmerItem) item;
                        @Nullable
                        PercentType command = new PercentType(percentValue);
                        dItem.send(command);
                    } else {
                        logger.warn("Item {} - {} for object {} was not type dimmer {}", item,
                                item.getClass().getSimpleName(), objectEvent.source, item);
                    }
                } else {
                    logger.warn("[Plate Object {}] Event value is null. Event: {}", objectEvent.source, objectEvent);
                }
            } else {
                logger.warn("Item for object {} not found, mapping was {} event {}", objectEvent.source, this,
                        objectEvent);
            }
        }
    }

    @Override
    public void updateState() {
        Item item = getItem();
        if (item != null) {
            PercentType percent = item.getStateAs(PercentType.class);
            if (percent != null) {
                logger.trace("SEND percent update {} to {}", percent, this);
                localValue = percent.doubleValue();
                label = getWidgetLabel(w, percent);
            } else {
                logger.trace("SKIPPED SEND percent update {} to {}", percent, this);
            }
        } else {
            logger.warn("Widget {} tried to update state but Item is null", this);
        }
    }

    @Override
    public void sendStatusUpdate(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException {
        context.put(WIDGET_LABEL, label);
        String id;

        id = objectIdSlide;
        if (id != null) {
            context.put(OBJECT_ID + "slider", id);
        }

        id = objectIdLabel;
        if (id != null) {
            context.put(OBJECT_ID + "label", id);
        }

        context.put("slider_value", Double.toString(localValue));
        objectArray.addAll(tplProc.processTemplate(getComponentStatusTemplate("val"), context));
    }
}
