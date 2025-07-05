package org.openhab.binding.openhasp.internal.layout.components;

import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.ui.items.ItemUIRegistry;

@NonNullByDefault
public class TextWidget extends LabelIconComponent {

    public TextWidget(HashMap<String, String> context, Widget w, @Nullable Item item,
            @NonNull ItemUIRegistry itemUIRegistry) {
        super(context, w, item, itemUIRegistry);
    }

    public String getComponent() {
        return "text";
    }
}
