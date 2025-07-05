package org.openhab.binding.openhasp.internal.layout.components;

import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.OBJECT_ID;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.WIDGET_ICON;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.WIDGET_LABEL;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.WIDGET_LABEL_STATE;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.WIDGET_LABEL_TEXT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.UnDefType;
import org.openhab.core.ui.items.ItemUIRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
abstract public class LabelIconComponent extends AbstractComponent {
    private static final Logger logger = LoggerFactory.getLogger(TextWidget.class);

    String widgetLabel;
    String widgetLabelText;
    String widgetLabelState;
    String itemIcon;
    String widgetIcon;
    String widgetValueColor;

    @Nullable
    String objectIdLabel;
    @Nullable
    String objectIdLabelState;
    @Nullable
    String objectIdIcon;

    public LabelIconComponent(HashMap<String, String> context, @Nullable Widget w, @Nullable Item item,
            @NonNull ItemUIRegistry itemUIRegistry) {
        super(context, w, item, itemUIRegistry);
        widgetLabel = widgetLabelText = widgetLabelState = widgetValueColor = "";
        processWidgetLabel(w, item != null ? item.getState() : null);
        this.itemIcon = getWidgetIcon(w);
        this.widgetIcon = resolveIcon(itemIcon);
    }

    public void prepareContext(Map<String, String> context) {
        addToContextSafe(context, WIDGET_LABEL, widgetLabel);
        addToContextSafe(context, WIDGET_LABEL_TEXT, widgetLabelText);
        addToContextSafe(context, WIDGET_LABEL_STATE, widgetLabelState);
        addToContextSafe(context, WIDGET_ICON, widgetIcon);
        addToContextSafe(context, "widgetValueColor", widgetValueColor);

        addToContextSafe(context, OBJECT_ID + "label", objectIdLabel);
        addToContextSafe(context, OBJECT_ID + "labelState", objectIdLabelState);
        addToContextSafe(context, OBJECT_ID + "icon", objectIdIcon);
    }

    public void readFromContext(Map<String, String> context) {
        // process mappings
        objectIdLabel = context.get(OBJECT_ID + "label");
        objectIdLabelState = context.get(OBJECT_ID + "labelState");
        objectIdIcon = context.get(OBJECT_ID + "icon");
    }

    @Override
    public List<String> getIds() {
        ArrayList<String> ids = new ArrayList<String>();
        addToListSafe(ids, objectIdLabel);
        addToListSafe(ids, objectIdIcon);
        return ids;
    }

    @Override
    public void updateState() {
        Item item = getItem();
        if (item != null) {
            State state = item.getState();
            if (state != null) {
                logger.trace("Current state {}", state);
                processWidgetLabel(w, state);
            }
        } else {
            logger.warn("Widget {} tried to update state but Item is null", this);
        }
    }

    protected void processWidgetLabel(@Nullable Widget w, @Nullable State itemState) {
        String label = "";
        String labelState = "";
        if (w == null) {
            widgetLabelText = "NULL";
            widgetLabelState = "";
            widgetLabel = widgetLabelText;
            return;
        }

        label = w.getLabel();
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
            if (start >= 0) {
                int end = label.indexOf("]");
                if (end > start) {
                    statePattern = label.substring(start + 1, end).trim();
                } else {
                    statePattern = label.substring(start + 1).trim();
                }
                label = label.substring(0, start).trim();
            }

            if ((statePattern == null || statePattern.isBlank()) && item != null) {
                @Nullable
                StateDescription stateDescription = item.getStateDescription();
                if (stateDescription != null) {
                    statePattern = stateDescription.getPattern();
                }
            }

            widgetLabelText = label;

            if (itemState != null && !(itemState instanceof UnDefType)) {
                if (statePattern != null) {
                    logger.trace(
                            "Formating widget {}, itemState {}, itemStateClass {}, label {}, pattern {}, resultado {}",
                            w, itemState, itemState.getClass().getSimpleName(), label, statePattern,
                            itemState.format(statePattern));
                    labelState = itemState.format(statePattern);
                    widgetLabelState = labelState;
                    widgetLabel = widgetLabelText + " " + widgetLabelState;
                } else {
                    labelState = itemState.toFullString();
                    if (labelState != null && !labelState.isBlank()) {
                        widgetLabelState = labelState;
                        widgetLabel = widgetLabelText + " " + widgetLabelState;
                    }
                }
            } else {
                widgetLabel = widgetLabelText;
            }

        }
        widgetValueColor = processValueColor(w);
    }
}
