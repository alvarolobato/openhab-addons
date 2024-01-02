package org.openhab.binding.openhasp.internal.layout.components;

import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.WIDGET_LABEL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.layout.TemplateProcessor;
import org.openhab.core.items.Item;
import org.openhab.core.model.sitemap.sitemap.Widget;

@NonNullByDefault
public class SectionWidget extends AbstractComponent {
    String label;

    public SectionWidget(HashMap<String, String> context, @Nullable Widget w, @Nullable Item item) {
        super(context, w, item);
        this.label = getWidgetLabel(w, item != null ? item.getState() : null);
        ;
    }

    public String getComponent() {
        return "section";
    }

    @Override
    public void render(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException {

        context.put(WIDGET_LABEL, label);
        super.render(tplProc, context, objectArray);
    }

    @Override
    public void updateState() {
    }
}
