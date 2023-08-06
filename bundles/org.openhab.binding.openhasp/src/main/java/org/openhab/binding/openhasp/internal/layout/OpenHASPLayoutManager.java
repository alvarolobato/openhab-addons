package org.openhab.binding.openhasp.internal.layout;

import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PAGE_LOCATION;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PAGE_LOCATION_RETURN_PAGE;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PAGE_NUM;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PAGE_OBJ_ID;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PAGE_Y;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PREVIOUS_PAGE_NUM;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.getAsInt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.CommandType;
import org.openhab.binding.openhasp.internal.OpenHASPCommunicationManager;
import org.openhab.binding.openhasp.internal.Util;
import org.openhab.binding.openhasp.internal.mapping.ObjItemMapper;
import org.openhab.binding.openhasp.internal.mapping.ObjItemMapping;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.model.sitemap.sitemap.Frame;
import org.openhab.core.model.sitemap.sitemap.Group;
import org.openhab.core.model.sitemap.sitemap.LinkableWidget;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.Slider;
import org.openhab.core.model.sitemap.sitemap.Switch;
import org.openhab.core.model.sitemap.sitemap.Text;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

//know about the items and stuff in openhab and send the layout to OpenHASPLayout to format
public class OpenHASPLayoutManager {
    private static final Logger logger = LoggerFactory.getLogger(OpenHASPLayoutManager.class);
    private final static Gson gson = new Gson();

    String thingId;
    String plateId;
    protected ObjItemMapper objItemMapper;
    protected OpenHASPCommunicationManager comm;
    HashMap<String, String> context;
    OpenHASPLayout layout;
    ArrayList<List<String>> pages;
    ArrayList<String> objectArray;
    ItemRegistry itemRegistry;

    public OpenHASPLayoutManager(String thingId, String plateId, OpenHASPCommunicationManager comm,
            ItemRegistry itemRegistry) {
        objItemMapper = new ObjItemMapper();
        this.thingId = thingId;
        this.plateId = plateId;
        this.comm = comm;
        this.itemRegistry = itemRegistry;
        context = new HashMap<String, String>();
        layout = new OpenHASPLayout(context);
        pages = new ArrayList<List<String>>(0);
        // TODO should disapear and be replaced by pages
        objectArray = new ArrayList<String>(0);
        layout.initiate();
    }

    public void loadFromSiteMap(Sitemap sitemap) {

        String sitemapLabel = sitemap.getLabel();
        // Properties prop=new Properties();
        // prop.load(null);

        objectArray.addAll(Arrays.asList(layout.getInitialPage()));

        context.put(PAGE_LOCATION, !(sitemapLabel.isEmpty()) ? sitemapLabel : sitemap.getName());
        context.put(PAGE_LOCATION_RETURN_PAGE, "");
        context.put("nextGroupPage", "30");

        newPage();

        processWidgetList(sitemap.getChildren());

        logger.trace("OBJECTS:");
        for (String obj : objectArray)
            logger.trace("{}", obj);

        logger.trace("\n\n\n\n\n\n\n\n");

        for (String obj : objectArray) {

            if (obj.trim().isEmpty()) {
                continue;
            }

            int startIndex = 0;
            if (obj.startsWith("{")) {
                startIndex = 1;
            }

            if (obj.endsWith("}")) {
                obj = obj.substring(startIndex, obj.length() - 1);
            } else {
                obj = obj.substring(startIndex);
            }

            String[] attributes = obj.split(",");
            int pageNum = 0;
            for (String attribute : attributes) {

                // String[] fields = attribute.split("=");
                String[] fields = attribute.split(":");
                fields[0] = Util.cleanString(fields[0]);
                if (fields.length >= 2) {
                    fields[1] = Util.cleanString(fields[1]);
                } else {
                    fields = new String[] { fields[0], "" };
                }

                if (fields[0].equalsIgnoreCase("page")) {
                    try {
                        pageNum = Integer.parseInt(fields[1]);
                    } catch (NumberFormatException e) {
                        logger.trace("Invalid page number {} from {}", fields[1], obj);
                    }
                    break;
                }
            }

            ensureSize(pages, pageNum + 1);
            // aaa
            logger.trace("Capacity {}", pages.size());
            List<String> pageList = pages.get(pageNum);
            if (pageList == null) {
                pageList = new ArrayList<String>(0);
                pages.add(pageNum, pageList);
            }
            pageList.add(obj);
        }

        logger.trace("PAGES!!!!!");
        for (int i = 0; i < pages.size(); i++) {
            List<String> pageList = pages.get(i);
            logger.trace("Page {}", i);
            if (pageList != null) {
                for (String objs : pageList) {
                    logger.trace("    {}", objs);
                }
            }
        }
    }

    public static void ensureSize(ArrayList<?> list, int size) {
        // Prevent excessive copying while we're adding
        list.ensureCapacity(size);
        while (list.size() < size) {
            list.add(null);
        }
    }

    private void processWidgetList(EList<Widget> widgetList) {
        for (Widget w : widgetList) {
            // SliderImpl
            // SwitchImpl
            // TextImpl
            // FrameImpl
            // DefaultImpl
            // GroupImpl
            // ListImpl
            logger.trace("Page: {} - {} (Item:{},Label:{})", context.get(PAGE_NUM), w.getClass().getSimpleName(),
                    w.getItem(), w.getLabel());

            context.put("widgetLabel", getWidgetLabel(w));
            context.put("item", w.getItem());

            boolean isGroup = false;
            String component = "";
            if (w instanceof Slider) {
                component = OpenHASPLayout.TPL_COMP_SLIDER;
                // objectArray.addAll(Arrays.asList(layout.addSlider(objItemMapper)));
            } else if (w instanceof Switch) {
                component = OpenHASPLayout.TPL_COMP_BUTTON;
            } else if (w instanceof Frame) {
                component = OpenHASPLayout.TPL_COMP_SECTION;
                // objectArray.addAll(Arrays.asList(layout.addSection()));
            } else if (w instanceof Group | (w instanceof Text && !((Text) w).getChildren().isEmpty())) {
                component = OpenHASPLayout.TPL_COMP_GROUP;
                isGroup = true;
                // Group link item
                // objectArray.addAll(Arrays.asList(layout.addGroup()));
            } else if (w instanceof Text) { // Text without children
                // TODO Implement
            } else {
                component = OpenHASPLayout.TPL_COMP_SECTION;
                context.put("widgetLabel", "*" + getWidgetLabel(w));
                // objectArray.addAll(Arrays.asList(layout.addSection()));
            }

            if (component.equalsIgnoreCase(OpenHASPLayout.TPL_COMP_BUTTON)
                    || component.equalsIgnoreCase(OpenHASPLayout.TPL_COMP_SLIDER)
                    || component.equalsIgnoreCase(OpenHASPLayout.TPL_COMP_SECTION)
                    || component.equalsIgnoreCase(OpenHASPLayout.TPL_COMP_GROUP)) {
                // TODO Remove all the code from the switch and move it here, then remove the if

                // Ensure there's enough height
                // TODO maybe move to separate method
                int y = getAsInt(context, PAGE_Y);
                int height = layout.getHeight(component);
                if (y + height > OpenHASPLayout.MAX_Y) {
                    objectArray.addAll(Arrays.asList(newPage()));
                    y = getAsInt(context, PAGE_Y);
                }

                // Actually add the component
                objectArray.addAll(Arrays.asList(layout.addComponent(component, objItemMapper)));

                if (isGroup) {
                    // TODO review
                    // Prepare to create the group destination pages
                    String currentPage = context.get(PAGE_NUM);
                    String currentY = context.get(PAGE_Y);
                    String currentObjId = context.get(PAGE_OBJ_ID);
                    String previousLocation = context.get(PAGE_LOCATION);
                    String previousReturnPage = context.get(PAGE_LOCATION_RETURN_PAGE);
                    String nextGroupPage = context.getOrDefault("nextGroupPage", "");

                    // context.put(PREVIOUS_PAGE_NUM, currentPage);
                    // context.put(PAGE_NUM, nextGroupPage);
                    // context.put(PAGE_Y, "0");
                    // context.put(PAGE_OBJ_ID, "0");

                    context.put(PAGE_LOCATION, getWidgetLabel(w));
                    context.put(PAGE_LOCATION_RETURN_PAGE, currentPage);

                    objectArray.addAll(Arrays.asList(newPage()));
                    // objectArray.addAll(Arrays.asList(OpenHASPLayout.addSection(context)));

                    // TODO If there is a recursive group we run into problems, should process
                    // groups later
                    int next = Integer.parseInt(nextGroupPage) + 5;
                    context.put("nextGroupPage", Integer.toString(next));
                    processWidgetList(((LinkableWidget) w).getChildren());

                    // int endPage = Integer.parseInt(context.getOrDefault(OpenHASPLayout.PAGE_NUM,
                    // "0")) + 1;
                    // context.put("nextGroupPage", Integer.toString(endPage));

                    context.put(PAGE_NUM, currentPage);
                    context.put(PAGE_Y, currentY);
                    context.put(PAGE_OBJ_ID, currentObjId);
                    context.put(PAGE_LOCATION, previousLocation);
                    context.put(PAGE_LOCATION_RETURN_PAGE, previousReturnPage);
                }

            }

            if (w instanceof LinkableWidget && !isGroup) {
                processWidgetList(((LinkableWidget) w).getChildren());
            }
        }
    }

    public String[] newPage() {
        int currentPage = getAsInt(context, PAGE_NUM);
        context.put(PREVIOUS_PAGE_NUM, Integer.toString(currentPage));
        context.put(PAGE_NUM, Integer.toString(currentPage + 1));
        context.put(PAGE_Y, "0");
        context.put(PAGE_OBJ_ID, "0");
        return layout.addComponent("page", objItemMapper);
    }

    private String getWidgetLabel(Widget w) {
        String label;
        label = w.getLabel();
        if (label == null || label.isEmpty()) {
            Item item = itemRegistry.get(w.getItem());
            if (item != null) {
                label = item.getLabel();
            }
        }
        if (label == null || label.isEmpty()) {
            label = w.getItem();
        }
        return label;
    }

    public void sendPages() {
        // connection.publish(fullCommandTopic + "/LWT", payload, qos, retain);
        logger.info("Sending pages to {}", plateId);

        int LimitDeviceJsonl = 500;
        int LimitObjectJsonl = 230;
        int StartSizeObjectJsonl;
        int currentID = 0;
        int currentPage = 0;
        String currentObj = "";

        String jsonlString = "";
        // var String attribute
        // var z = -1
        // var y = -1

        int objectCount = objectArray.size();

        int pageToClear = -1;

        logger.info("Objects: {}", objectCount);

        // TODO move this to some general setup method
        comm.sendHASPCommand(CommandType.CMD, "config/hasp {\"theme\":5}");

        try {
            for (String object : objectArray) {
                if (object.trim().isEmpty()) {
                    continue;
                }

                int startIndex = 0;
                if (object.startsWith("{")) {
                    startIndex = 1;
                }

                if (object.endsWith("}")) {
                    object = object.substring(startIndex, object.length() - 1);
                } else {
                    object = object.substring(startIndex);
                }

                // while ((z=z+1) <= objectCount -1) {
                // String[] attributes = object.split(";");
                String[] attributes = object.split(",");
                // logger.info("Sending: {}", object);
                // logger.info("Attributes: {}", attributes.length);

                StartSizeObjectJsonl = jsonlString.length();
                jsonlString += "{";
                // y = -1
                // while ((y=y+1) <= attributeCount -1) {
                boolean firstAttribute = true;
                for (String attribute : attributes) {
                    // logger.info("Sending to {} attribute [{}]", plateId, attribute);

                    if (firstAttribute) {
                        firstAttribute = false;
                    } else {
                        jsonlString += ",";
                    }

                    // String[] fields = attribute.split("=");
                    String[] fields = attribute.split(":");
                    fields[0] = Util.cleanString(fields[0]);
                    if (fields.length >= 2) {
                        fields[1] = Util.cleanString(fields[1]);
                    } else {
                        fields = new String[] { fields[0], "" };
                    }

                    if (fields[0].equalsIgnoreCase("page")) {
                        currentPage = Integer.parseInt(fields[1]);
                    }
                    if (fields[0].equalsIgnoreCase("id")) {
                        currentID = Integer.parseInt(fields[1]);
                    }
                    if (fields[0].equalsIgnoreCase("obj")) {
                        currentObj = fields[1];
                    }

                    // Send Clearpage and set bg color when refreshing the lcd

                    if (pageToClear < currentPage) { // < prevents clearing when returning from a groups
                        comm.sendHASPCommand(CommandType.CMD, "clearpage=" + String.valueOf(currentPage));
                        // connection.publish(plateBaseTopic + "/command/clearpage",
                        // String.valueOf(currentPage).getBytes(), 1, true);// .get();

                        // logger.info("[{}] ClearPage: {}", plateId, currentPage);
                        pageToClear = currentPage;
                    }

                    jsonlString += "\"" + fields[0] + "\":";
                    try {
                        int num = Integer.parseInt(fields[1]);
                        jsonlString += num;
                    } catch (NumberFormatException e) {
                        jsonlString += "\"" + fields[1] + "\"";
                    }

                    if (LimitObjectJsonl < (jsonlString.length() - StartSizeObjectJsonl)) {
                        jsonlString += "}\n";
                        StartSizeObjectJsonl = jsonlString.length();
                        jsonlString += "{\"page\":" + currentPage + ",\"id\":" + currentID;
                    }
                }
                jsonlString += "}\n";

                // logger.info("[{}] JSONL Size={}", plateId, jsonlString.length());
                if (LimitDeviceJsonl < jsonlString.length()) {
                    // logger.info("[{}] JSONL={}", jsonlString, jsonlString);

                    comm.sendHASPCommand(CommandType.JSONL, jsonlString);
                    // connection.publish(plateBaseTopic + "/command/jsonl", jsonlString.getBytes(),
                    // 1, true);
                    jsonlString = "";
                }
                // if ("btn".equals(currentObj)) {
                // logger.trace("Button: p{}b{}", currentPage, currentID);
                // }
            }
            if (jsonlString.length() > 0) {
                // logger.info("[{}] JSONL={}", jsonlString, jsonlString);

                comm.sendHASPCommand(CommandType.JSONL, jsonlString);
                // connection.publish(plateBaseTopic + "/command/jsonl", jsonlString.getBytes(),
                // 1, true);

            }
        } catch (RuntimeException e) {
            logger.error("Error sending config", e);
        }

        logger.info("Send page {} DONE", plateId);
    }

    public @Nullable List<ObjItemMapping> getByItem(@NonNull String item) {
        return objItemMapper.getByItem(item);
    }

    public @Nullable ObjItemMapping getByObject(@NonNull String object) {
        return objItemMapper.getByObject(object);
    }

    public void logKeysByObj() {
        objItemMapper.logKeysByObj();
    }
}
