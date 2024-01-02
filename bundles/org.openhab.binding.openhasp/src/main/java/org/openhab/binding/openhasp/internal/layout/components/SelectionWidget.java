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
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.model.sitemap.sitemap.Mapping;
import org.openhab.core.model.sitemap.sitemap.Selection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class SelectionWidget extends AbstractComponent {
    private static final Logger logger = LoggerFactory.getLogger(SelectionWidget.class);
    private String label;

    @Nullable
    private String objectIdSelect;

    private String[] options;
    private String[] positionValues;
    private int selected;

    public SelectionWidget(HashMap<String, String> context, @Nullable Selection sel, @Nullable Item item) {
        super(context, sel, item);
        this.label = getWidgetLabel(w, item != null ? item.getState() : null);

        options = new String[sel.getMappings().size()];
        positionValues = new String[sel.getMappings().size()];
        int i = 0;
        for (Mapping mapping : sel.getMappings()) {
            options[i] = mapping.getLabel();
            positionValues[i] = mapping.getCmd();
            i++;
        }
        selected = 0;
    }

    public String getComponent() {
        return "selection";
    }

    @Override
    public void render(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException {

        context.put(WIDGET_LABEL, label);
        StringBuffer optionsRendered = new StringBuffer();

        for (String opt : options) {
            optionsRendered.append(opt).append("\\n");
        }

        context.put("widgetOptions", optionsRendered.toString()); // options in the dropdown
        context.put("widgetVal", Integer.toString(selected)); // number of selected option

        objectArray.addAll(tplProc.processTemplate(getComponentTemplate(getComponent()), context));

        // process mappings
        objectIdSelect = context.get(OBJECT_ID + "select");
    }

    @Override
    public List<String> getIds() {
        ArrayList<String> ids = new ArrayList<String>();
        String id = objectIdSelect;
        if (id != null) {
            ids.add(id);
        }
        return ids;
    }

    @Override
    public void haspEventReceived(ObjectEvent objectEvent) {
        if (objectEvent.event != null && objectEvent.event.contains("changed")) { // Dropdown
            logger.trace("DROPDOWN SELECT {} - event: {}", item, objectEvent);
            logger.warn("Item {} item class {}", item, item.getClass().getSimpleName());
            String strPos = objectEvent.val;
            if (strPos != null) {
                if (item != null) {
                    if (item instanceof NumberItem) {
                        try {
                            int pos = Integer.parseInt(strPos);
                            NumberItem dItem = (NumberItem) item;
                            if (positionValues != null) {
                                if (positionValues.length < pos) {
                                    logger.error("Item {} - {} for object {} could not find position {} available {}",
                                            item, item.getClass().getSimpleName(), objectEvent, pos, positionValues);
                                } else {
                                    @Nullable
                                    DecimalType command = new DecimalType(positionValues[pos]);
                                    dItem.send(command);
                                }
                            } else {
                                DecimalType command = new DecimalType(pos);
                                dItem.send(command);
                            }
                        } catch (NumberFormatException e) {
                            logger.warn("Item {} - {} for object {} position value was not numeric", item,
                                    item.getClass().getSimpleName(), objectEvent);
                        }
                    } else {
                        logger.warn("Item for object {} was not type NumberItem {} - {}", objectEvent.source,
                                item.getClass().getSimpleName(), item);
                    }
                } else {
                    logger.warn("Item for object {} not found, mapping was {} event {}", objectEvent.source, this,
                            objectEvent);
                }
            } else {
                logger.warn("[Plate Object {}] Event value is null. Event: {}", objectEvent.source, objectEvent);
            }
        }
    }

    @Override
    public void updateState() {
        Item item = getItem();
        if (item != null) {
            DecimalType decimal = item.getStateAs(DecimalType.class);
            if (decimal != null) {
                selected = findValuePosition(decimal.toString());
                logger.trace("SEND decimal update value {} position {} to {}", decimal, selected, this);
            } else {
                logger.trace("SKIPPED SEND decimal update value {} position {} to {}", decimal, this);
            }
        } else {
            logger.warn("Widget {} tried to update state but Item", this);
        }
    }

    private int findValuePosition(String value) {
        if (positionValues != null) {
            for (int i = 0; i < positionValues.length; i++) {
                if (positionValues[i].equalsIgnoreCase(value)) {
                    return i;
                }
            }
            logger.warn("Couldn't find position value {} for item {}, available values {}", value, item,
                    positionValues);
        }
        return 0;
    }

    @Override
    public void sendStatusUpdate(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException {
        context.put(WIDGET_LABEL, label);
        String id;

        id = objectIdSelect;
        if (id != null) {
            context.put(OBJECT_ID + "select", id);
        }

        context.put("select_value", Integer.toString(selected));
        objectArray.addAll(tplProc.processTemplate(getComponentStatusTemplate("val"), context));
    }
}
