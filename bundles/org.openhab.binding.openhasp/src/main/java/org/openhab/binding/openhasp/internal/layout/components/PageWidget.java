package org.openhab.binding.openhasp.internal.layout.components;

import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class PageWidget extends AbstractComponent {

    public PageWidget(HashMap<String, String> context) {
        super(context, null, null);
    }

    public String getComponent() {
        return "page";
    }

    @Override
    public void updateState() {
    }
}
