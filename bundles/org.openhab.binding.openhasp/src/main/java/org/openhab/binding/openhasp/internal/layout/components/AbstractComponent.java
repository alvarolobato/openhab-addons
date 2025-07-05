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
import org.openhab.core.ui.items.ItemUIRegistry;
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

    @NonNull
    ItemUIRegistry itemUIRegistry;

    protected AbstractComponent(HashMap<String, String> context, @Nullable Widget w, @Nullable Item item,
            @NonNull ItemUIRegistry itemUIRegistry) {
        this.context = context;
        this.w = w;
        this.item = item;
        this.itemUIRegistry = itemUIRegistry;
    }

    abstract protected void prepareContext(Map<String, String> context);

    abstract protected void readFromContext(Map<String, String> context);

    @Override
    public void render(TemplateProcessor tplProc, Map<String, String> context, ArrayList<String> objectArray)
            throws IOException {
        prepareContext(context);
        objectArray.addAll(tplProc.processTemplate(getComponentTemplate(getComponent()), context));
        readFromContext(context);
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
        prepareContext(context);
        objectArray.addAll(tplProc.processTemplate(getComponentStatusTemplate("val"), context));
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
            if (result != null) {
                return result;
            } else {
                tagsNotFound.append(icon).append(" ");
            }
        }

        logger.info("No icon found for any of {}", tagsNotFound.toString());

        result = context.get("icon.notfound");
        if (result != null) {
            return result;
        } else {
            return "";
        }
    }

    protected String resolveColor(@Nullable String color) {

        if (color == null) {
            return "";
        }

        if (color.trim().startsWith("#")) {
            return color;
        }

        String result;
        result = context.get("color." + color);
        if (result != null) {
            return result;
        } else {
            logger.info("No color found for {}", color);
            return "";
        }
    }

    protected <A, B> void addToContextSafe(Map<A, B> context, A id, @Nullable B value) {
        if (value != null) {
            context.put(id, value);
        } else {
            context.remove(id);
        }
    }

    protected <A> void addToListSafe(List<A> list, @Nullable A value) {
        if (value != null) {
            list.add(value);
        }
    }

    protected String processValueColor(Widget w) {
        return resolveColor(itemUIRegistry.getValueColor(w));
    }
}
