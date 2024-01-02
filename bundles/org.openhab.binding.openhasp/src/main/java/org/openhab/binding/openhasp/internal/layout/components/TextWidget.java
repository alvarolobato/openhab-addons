package org.openhab.binding.openhasp.internal.layout.components;

import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.OBJECT_ID;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.WIDGET_LABEL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.layout.TemplateProcessor;
import org.openhab.core.items.Item;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class TextWidget extends AbstractComponent {
    private static final Logger logger = LoggerFactory.getLogger(TextWidget.class);

    String label;

    @Nullable
    String objectIdLabel;

    public TextWidget(HashMap<String, String> context, Widget w, @Nullable Item item) {
        super(context, w, item);
        this.label = getWidgetLabel(w, item != null ? item.getState() : null);
    }

    public String getComponent() {
        return "text";
    }

    @Override
    public void render(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException {

        context.put(WIDGET_LABEL, label);

        super.render(tplProc, context, objectArray);
        objectIdLabel = context.get(OBJECT_ID + "label");
    }

    @Override
    public @NonNull List<String> getIds() {
        ArrayList<String> ids = new ArrayList<String>();
        String id = objectIdLabel;
        if (id != null) {
            ids.add(id);
        }
        return ids;
    }

    @Override
    public void updateState() {
        Item item = getItem();
        if (item != null) {
            State state = item.getState();
            if (state != null) {
                logger.trace("Current state {}", state);
                label = getWidgetLabel(w, state);
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

        id = objectIdLabel;
        if (id != null) {
            context.put(OBJECT_ID + "label", id);
        }

        objectArray.addAll(tplProc.processTemplate(getComponentStatusTemplate("val"), context));
    }
}
