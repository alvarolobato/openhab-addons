package org.openhab.binding.openhasp.internal.layout.components;

import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.ICON_OFF;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.ICON_ON;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.OBJECT_ID;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.ObjectEvent;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.model.sitemap.sitemap.Slider;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.types.StateDescription;
import org.openhab.core.ui.items.ItemUIRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class SliderWidget extends LabelIconComponent {
    private static final Logger logger = LoggerFactory.getLogger(SliderWidget.class);

    @Nullable
    String objectIdSlide;
    private double localValue;
    private double minValue;
    private double maxValue;

    public SliderWidget(HashMap<String, String> context, @Nullable Widget w, @Nullable Item item,
            @NonNull ItemUIRegistry itemUIRegistry) {
        super(context, w, item, itemUIRegistry);
        localValue = 0;
        minValue = 0;
        maxValue = 100;

        if (w != null && w instanceof Slider) {
            Slider sw = (Slider) w;
            BigDecimal val;
            val = sw.getMinValue();
            if (val != null) {
                minValue = val.doubleValue();
            }
            val = sw.getMaxValue();
            if (val != null) {
                maxValue = val.doubleValue();
            }
        } else if (item != null) {
            @Nullable
            StateDescription stateDesc = item.getStateDescription();
            if (stateDesc != null) {
                BigDecimal val;
                val = stateDesc.getMinimum();
                if (val != null) {
                    minValue = val.doubleValue();
                }
                val = stateDesc.getMaximum();
                if (val != null) {
                    maxValue = val.doubleValue();
                }
            }
        }
    }

    public String getComponent() {
        return "slider";
    }

    @Override
    public List<String> getIds() {
        List<String> ids = super.getIds();
        addToListSafe(ids, objectIdSlide);
        return ids;
    }

    @Override
    public void haspEventReceived(ObjectEvent objectEvent) {
        if (objectEvent.event != null && objectEvent.val != null
                && (objectEvent.event.contains("up") || objectEvent.event.contains("changed"))) { // Slider
            logger.trace("SLIDER CHANGE {} - event: {}", item, objectEvent);
            if (item != null) {
                String value = objectEvent.val;
                if (value != null) {
                    if (item instanceof DimmerItem) {
                        DimmerItem dItem = (DimmerItem) item;
                        @Nullable
                        PercentType command = new PercentType(value);
                        dItem.send(command);
                    } else if (item instanceof NumberItem) {
                        NumberItem nItem = (NumberItem) item;
                        @Nullable
                        DecimalType command = new DecimalType(value);
                        nItem.send(command);
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
        super.updateState();
        Item item = getItem();
        if (item != null) {
            DecimalType decimal = null;
            if (item instanceof DimmerItem) {
                decimal = item.getStateAs(PercentType.class);
            } else if (item instanceof NumberItem) {
                decimal = item.getStateAs(DecimalType.class);
            } else {
                logger.warn("Item {} - {} was not type dimmer {}", item, item.getClass().getSimpleName(), item);
            }
            if (decimal != null) {
                logger.trace("SEND percent update {} to {}", decimal, this);
                localValue = decimal.doubleValue();
                if (localValue == 0) {
                    widgetIcon = resolveIcon(itemIcon + ICON_OFF, itemIcon);
                } else {
                    widgetIcon = resolveIcon(itemIcon + ICON_ON, itemIcon);
                }
            } else {
                logger.trace("SKIPPED SEND percent update {} to {}", decimal, this);
            }
        } else {
            logger.warn("Widget {} tried to update state but Item is null", this);
        }
    }

    @Override
    public void prepareContext(Map<String, String> context) {
        super.prepareContext(context);
        addToContextSafe(context, OBJECT_ID + "slider", objectIdSlide);
        context.put("widgetVal", Double.toString(localValue));
        context.put("widgetMinVal", Double.toString(minValue));
        context.put("widgetMaxVal", Double.toString(maxValue));
    }

    @Override
    public void readFromContext(Map<String, String> context) {
        super.readFromContext(context);
        objectIdSlide = context.get(OBJECT_ID + "slider");
    }
}
