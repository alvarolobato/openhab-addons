package org.openhab.binding.openhasp.internal.layout.components;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.ui.items.ItemUIRegistry;

@NonNullByDefault
public class PageWidget extends AbstractComponent {

    public PageWidget(HashMap<String, String> context, @NonNull ItemUIRegistry itemUIRegistry) {
        super(context, null, null, itemUIRegistry);
    }

    public String getComponent() {
        return "page";
    }

    @Override
    public void updateState() {
    }

    @Override
    protected void prepareContext(Map<String, String> context) {
    }

    @Override
    protected void readFromContext(Map<String, String> context) {
    }
}
