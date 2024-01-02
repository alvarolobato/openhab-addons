package org.openhab.binding.openhasp.internal.layout.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.ObjectEvent;
import org.openhab.binding.openhasp.internal.layout.OpenHASPLayout;
import org.openhab.binding.openhasp.internal.layout.TemplateProcessor;
import org.openhab.core.items.Item;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public abstract class AbstractComponent implements IComponent {
    private static final Logger logger = LoggerFactory.getLogger(AbstractComponent.class);
    private HashMap<String, String> context;

    @Nullable
    Widget w;

    @Nullable
    Item item;

    protected AbstractComponent(HashMap<String, String> context, @Nullable Widget w, @Nullable Item item) {
        this.context = context;
        this.w = w;
        this.item = item;
    }

    @Override
    public void render(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException {
        objectArray.addAll(tplProc.processTemplate(getComponentTemplate(getComponent()), context));
    }

    public String getComponentStatusTemplate(String status) {
        return getComponentTemplate(getComponent() + "_" + status);
    }

    static public String getComponentTemplate(String component) {
        return component + ".json";
    }

    @Override
    public int getHeight() {
        return OpenHASPLayout.getAsInt(context, getComponent() + OpenHASPLayout.COMP_HEIGHT);
    }

    @Override
    @Nullable
    public Item getItem() {
        return item;
    }

    public String toString() {
        return "Comp: " + getComponent() + "- Item: " + getItem();
    }

    @Override
    public @NonNull List<String> getIds() {
        return new ArrayList<String>();
    }

    @Override
    public void haspEventReceived(ObjectEvent objectEvent) {
    }

    @Override
    public void sendStatusUpdate(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException {
    }
    // protected static int getItemStateIntVal(State itemState) {
    // int intVal = 0;
    // if (itemState != null) {
    // if (itemState instanceof DecimalType) {
    // intVal = ((DecimalType) itemState).intValue();
    // } else if (itemState instanceof QuantityType) {
    // intVal = ((QuantityType<?>) itemState).intValue();
    // } else {
    // logger.trace("Selection item came with unexpected state {}", itemState.getClass().getSimpleName());
    // }
    // }
    // return intVal;
    // }

    // protected static double getItemStateDoubletVal(State itemState) {
    // double doubleVal = 0;
    // if (itemState != null) {
    // if (itemState instanceof DecimalType) {
    // doubleVal = ((DecimalType) itemState).doubleValue();
    // } else if (itemState instanceof QuantityType) {
    // doubleVal = ((QuantityType<?>) itemState).doubleValue();
    // } else {
    // logger.trace("Selection item came with unexpected state {}", itemState.getClass().getSimpleName());
    // }
    // }
    // return doubleVal;
    // }

    protected String getWidgetLabel(@Nullable Widget w, @Nullable State itemState) {
        String label;
        // Item item = itemRegistry.get(w.getItem());

        if (w == null) {
            return "NULL";
        }

        label = w.getLabel();
        // logger.trace("label1:{}", label);
        if (label == null || label.isEmpty()) {
            if (item != null) {
                label = item.getLabel();
            }
        }
        if (label == null || label.isEmpty()) {
            label = w.getItem();
        }

        if (label != null) {
            String statePattern = null;
            int start = label.indexOf("[");

            if (label.indexOf("[") >= 0) {
                int end = label.indexOf("]");
                if (end > start) {
                    statePattern = label.substring(start + 1, end).trim();
                } else {
                    statePattern = label.substring(start + 1).trim();
                }

                label = label.substring(0, start).trim();
            }

            if (statePattern == null && item != null) {
                @Nullable
                StateDescription stateDescription = item.getStateDescription();
                if (stateDescription != null) {
                    statePattern = stateDescription.getPattern();
                }
            }

            if (statePattern != null && itemState != null && !(itemState instanceof UnDefType)) {
                logger.trace("Formating widget {}, itemState {}, itemStateClass {}, label {}, pattern {}, resultado {}",
                        w, itemState, itemState.getClass().getSimpleName(), label, statePattern,
                        itemState.format(statePattern));
                label = label + " " + itemState.format(statePattern);
            }

            return label;
        } else {
            return "NULL";
        }
    }

    protected String getWidgetIcon(@Nullable Widget w) {
        String icon;

        icon = w.getIcon();

        if (icon == null || icon.isEmpty()) {
            if (item != null) {
                icon = item.getCategory();
            }
        }
        if (icon == null) {
            icon = "noicon";
        }

        return icon;
    }

    protected String resolveIcon(String... icons) {
        StringBuffer tagsNotFound = new StringBuffer();
        String result;
        for (String icon : icons) {
            result = context.get("icon." + icon);
            if (result != null && !result.isEmpty()) {
                return result;
            } else {
                tagsNotFound.append(icon).append(" ");
            }
        }

        logger.info("No icon found for any of {}", tagsNotFound.toString());

        result = context.get("icon.noicon");
        if (result != null) {
            return result;
        } else {
            return "";
        }
    }

    protected void addToContextSafe(Map<String, String> context, String id, @Nullable String value) {
        if (value != null) {
            context.put(id, value);
        } else {
            context.remove(id);
        }
    }
}
