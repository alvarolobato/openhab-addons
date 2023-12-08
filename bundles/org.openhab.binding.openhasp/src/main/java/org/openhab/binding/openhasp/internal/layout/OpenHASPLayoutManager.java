package org.openhab.binding.openhasp.internal.layout;

import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PAGE_LOCATION;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PAGE_LOCATION_RETURN_PAGE;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PAGE_NUM;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PAGE_OBJ_ID;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PAGE_Y;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.PREVIOUS_PAGE_NUM;
import static org.openhab.binding.openhasp.internal.layout.OpenHASPLayout.getAsInt;

import java.text.DecimalFormat;
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
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.model.sitemap.sitemap.Chart;
import org.openhab.core.model.sitemap.sitemap.Frame;
import org.openhab.core.model.sitemap.sitemap.Group;
import org.openhab.core.model.sitemap.sitemap.LinkableWidget;
import org.openhab.core.model.sitemap.sitemap.Mapping;
import org.openhab.core.model.sitemap.sitemap.Selection;
import org.openhab.core.model.sitemap.sitemap.Setpoint;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.Slider;
import org.openhab.core.model.sitemap.sitemap.Switch;
import org.openhab.core.model.sitemap.sitemap.Text;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.google.gson.Gson;
//know about the items and stuff in openhab and send the layout to OpenHASPLayout to format
public class OpenHASPLayoutManager {
    private static final Logger logger = LoggerFactory.getLogger(OpenHASPLayoutManager.class);
    private final static DecimalFormat setPointFormat = new DecimalFormat("#0.#");
    // private final static Gson gson = new Gson();
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
        layout = new OpenHASPLayout("default-portrait", context);
        pages = new ArrayList<List<String>>(0);
        // TODO should disapear and be replaced by pages
        objectArray = new ArrayList<String>(0);
        layout.initiate();
    }

    public void loadFromSiteMap(Sitemap sitemap) {
        String sitemapLabel = sitemap.getLabel();
        // Properties prop=new Properties();
        // prop.load(null);

        objectArray.addAll(Arrays.asList(layout.getInit()));

        // TODO move this decision to the properties file
        // Decides if we show the sitemap name on every top level page
        // context.put(PAGE_LOCATION, !(sitemapLabel.isEmpty()) ? sitemapLabel : sitemap.getName());
        context.put(PAGE_LOCATION, "");

        context.put(PAGE_LOCATION_RETURN_PAGE, "");
        context.put("nextGroupPage", "30");

        newPage(objectArray);

        processWidgetList(sitemap.getChildren());

        // logger.trace("OBJECTS:");
        // for (String obj : objectArray)
        // logger.trace("{}", obj);

        // logger.trace("\n\n\n\n\n\n\n\n");

        // TODO REMOVE - PROCESSES OBJECT AND ASIGN TO PAGE
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
            List<String> pageList = pages.get(pageNum);
            if (pageList == null) {
                pageList = new ArrayList<String>(0);
                pages.add(pageNum, pageList);
            }
            pageList.add("{" + obj + "}");
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
        int compMaxY = layout.getAsInt(OpenHASPLayout.COMPONENTS_MAX_Y);

        for (Widget w : widgetList) {
            String currentPage = "";
            String currentY = "";
            String currentObjId = "";
            String previousLocation = "";
            String previousReturnPage = "";
            String nextGroupPage = "";

            boolean isGroup = false;
            String component = "";

            String itemName = w.getItem();

            @Nullable
            Item item = itemRegistry.get(itemName);

            @Nullable
            State itemState = null;
            if (item != null) {
                itemState = item.getState();
            }

            String widgetLabel = getWidgetLabel(w, itemState);

            String[] positionValues = null;
            if (w instanceof Slider) {
                component = OpenHASPLayout.TPL_COMP_SLIDER;
            } else if (w instanceof Switch) {
                component = OpenHASPLayout.TPL_COMP_BUTTON;
            } else if (w instanceof Frame) {
                component = OpenHASPLayout.TPL_COMP_SECTION;
            } else if (w instanceof Group | (w instanceof Text && !((Text) w).getChildren().isEmpty())) {
                logger.trace("GROUP IGNORED!! item: {} itemState {} label:{}", itemName, itemState, widgetLabel);
                continue;
                // component = OpenHASPLayout.TPL_COMP_GROUP;
                // isGroup = false;

                // logger.trace("GROUP!! item: {} itemState {} label:{}", itemName, itemState, widgetLabel);
                //// OLD GROUP CODE

                // component = "group";
                // isGroup = true;

                // // Group link item
                // objectArray.addAll(Arrays.asList(layout.addGroup()));

                // // Prepare to create the group destination pages
                // String currentPage = context.get(PAGE_NUM);
                // String currentY = context.get(PAGE_Y);
                // String currentObjId = context.get(PAGE_OBJ_ID);
                // String previousLocation = context.get(PAGE_LOCATION);
                // String previousReturnPage = context.get(PAGE_LOCATION_RETURN_PAGE);
                // String nextGroupPage = context.getOrDefault("nextGroupPage", "");

                // context.put(PAGE_LOCATION, getWidgetLabel(w));
                // context.put(PAGE_LOCATION_RETURN_PAGE, currentPage);
                // context.put(PREVIOUS_PAGE_NUM, currentPage);
                // context.put(PAGE_NUM, nextGroupPage);

                // objectArray.addAll(Arrays.asList(layout.initPage()));
                // // objectArray.addAll(Arrays.asList(OpenHASPLayout.addSection(context)));

                // // TODO If there is a recursive group we run into problems, should process
                // // groups later
                // int next = Integer.parseInt(nextGroupPage) + 5;
                // context.put("nextGroupPage", Integer.toString(next));
                // processWidgetList(((LinkableWidget) w).getChildren());

                // // int endPage = Integer.parseInt(context.getOrDefault(OpenHASPLayout.PAGE_NUM,
                // // "0")) + 1;
                // // context.put("nextGroupPage", Integer.toString(endPage));

                // context.put(PAGE_NUM, currentPage);
                // context.put(PAGE_Y, currentY);
                // context.put(PAGE_OBJ_ID, currentObjId);
                // context.put(PAGE_LOCATION, previousLocation);
                // context.put(PAGE_LOCATION_RETURN_PAGE, previousReturnPage);

                //// OLD GROUP CODE

            } else if (w instanceof Text) { // Text without children
                component = OpenHASPLayout.TPL_COMP_SECTION;
                // TODO Change to a specific implementation
                // if (itemState != null && !(itemState instanceof UnDefType)) {
                // // logger.trace("Formating widget {}, itemState {}, itemStateClass {},item {}, label {}", w,
                // // itemState,
                // // itemState.getClass().getSimpleName(), item, getWidgetLabel(w));
                // context.put("widgetLabel", "TXTR - " + itemState.format(getWidgetLabel(w)));
                // } else {
                // context.put("widgetLabel", "TXT - " + getWidgetLabel(w));
                // }

                // logger.trace("TEXT - TO BE IMPROVED Page: {} - {} (Item:{},Label:{})", context.get(PAGE_NUM),
                // w.getClass().getSimpleName(), w.getItem(), w.getLabel());

            } else if (w instanceof Selection) {
                component = OpenHASPLayout.TPL_COMP_SELECTION;
                Selection sel = (Selection) w;
                int intVal = 0;
                intVal = getItemStateIntVal(itemState);
                StringBuffer options = new StringBuffer();
                positionValues = new String[sel.getMappings().size()];
                int i = 0;
                for (Mapping mapping : sel.getMappings()) {
                    options.append(mapping.getLabel()).append("-").append(mapping.getCmd()).append("\\n");
                    positionValues[i] = mapping.getCmd();
                    i++;
                }
                context.put("widgetOptions", options.toString()); // options in the dropdown
                context.put("widgetVal", Integer.toString(intVal)); // number of selected option
            } else if (w instanceof Chart) {
                // TODO: Not Implemented
                continue;
            } else if (w instanceof Setpoint) {
                double doubleVal = getItemStateDoubletVal(itemState);

                component = OpenHASPLayout.TPL_COMP_SETPOINT;
                context.put("widgetVal", setPointFormat.format(doubleVal)); // number of selected option
            } else {
                component = OpenHASPLayout.TPL_COMP_SECTION;
                context.put("widgetLabel", "*" + widgetLabel);
                logger.trace("NOT IMPLEMENTED Page: {} - {} (Item:{},Label:{})", context.get(PAGE_NUM),
                        w.getClass().getSimpleName(), w.getItem(), w.getLabel());
            }

            // Ensure there's enough height
            int y = getAsInt(context, PAGE_Y);
            int height = layout.getAsInt(component + OpenHASPLayout.COMP_HEIGHT);
            if (y + height > compMaxY) {
                newPage(objectArray);
                y = getAsInt(context, PAGE_Y);
            }

            context.put("item", itemName);
            context.put("widgetLabel", widgetLabel);
            logger.trace("ItemName:{}", itemName);

            // Actually add the component
            logger.trace("Adding component {}", component);
            ObjItemMapping mapping = layout.addComponent(component, objectArray);

            if (mapping != null) {
                if (positionValues != null) {
                    mapping.positionValues = positionValues;
                }

                objItemMapper.mapObj(mapping);
            }

            if (isGroup) {
                // TODO review
                // Prepare to create the group destination pages
                currentPage = context.get(PAGE_NUM);
                currentY = context.get(PAGE_Y);
                currentObjId = context.get(PAGE_OBJ_ID);
                previousLocation = context.get(PAGE_LOCATION);
                previousReturnPage = context.get(PAGE_LOCATION_RETURN_PAGE);
                nextGroupPage = context.getOrDefault("nextGroupPage", "");

                // context.put(PREVIOUS_PAGE_NUM, currentPage);
                context.put(PAGE_NUM, nextGroupPage);
                // context.put(PAGE_Y, "0");
                // context.put(PAGE_OBJ_ID, "0");
                logger.trace("GROUP!! item: {} label:{}", itemName, widgetLabel);
                context.put(PAGE_LOCATION, widgetLabel);
                context.put(PAGE_LOCATION_RETURN_PAGE, currentPage);

                newPage(objectArray);
                isGroup = true;

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

            if (w instanceof LinkableWidget && !isGroup) { // TODO review this
                processWidgetList(((LinkableWidget) w).getChildren());
            }
        }
    }

    private int getItemStateIntVal(State itemState) {
        int intVal = 0;
        if (itemState != null) {
            if (itemState instanceof DecimalType) {
                intVal = ((DecimalType) itemState).intValue();
            } else if (itemState instanceof QuantityType) {
                intVal = ((QuantityType<?>) itemState).intValue();
            } else {
                logger.trace("Selection item came with unexpected state {}", itemState.getClass().getSimpleName());
            }
        }
        return intVal;
    }

    private double getItemStateDoubletVal(State itemState) {
        double doubleVal = 0;
        if (itemState != null) {
            if (itemState instanceof DecimalType) {
                doubleVal = ((DecimalType) itemState).doubleValue();
            } else if (itemState instanceof QuantityType) {
                doubleVal = ((QuantityType<?>) itemState).doubleValue();
            } else {
                logger.trace("Selection item came with unexpected state {}", itemState.getClass().getSimpleName());
            }
        }
        return doubleVal;
    }

    public void newPage(ArrayList<String> objectArray) {
        int currentPage = getAsInt(context, PAGE_NUM);
        context.put(PREVIOUS_PAGE_NUM, Integer.toString(currentPage));
        context.put(PAGE_NUM, Integer.toString(currentPage + 1));
        context.put(PAGE_Y, "0");
        context.put(PAGE_OBJ_ID, "0");
        ObjItemMapping mapping = layout.addComponent("page", objectArray);
        if (mapping != null) {
            objItemMapper.mapObj(mapping);
        }
    }

    private String getWidgetLabel(Widget w, State itemState) {
        String label;
        Item item = itemRegistry.get(w.getItem());

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

        // logger.info("Objects: {}", objectCount);

        // TODO move this to some general setup method
        // TODO This with real plate, does no work with emulator
        comm.sendHASPCommand(CommandType.CMD, "config/hasp {\"theme\":5}");

        try {
            int pageNum = 0;
            for (List<String> page : pages) {
                // Send Clearpage and set bg color when refreshing the lcd
                comm.sendHASPCommand(CommandType.CMD, "clearpage=" + String.valueOf(pageNum));
                if (page != null) {
                    for (String object : page) {

                        // for (String object : objectArray) {
                        object = object.trim();
                        if (object.isEmpty()) {
                            continue;
                        }

                        // OLD MODE
                        // int startIndex = 0;
                        // if (object.startsWith("{")) {
                        // startIndex = 1;
                        // }

                        // if (object.endsWith("}")) {
                        // object = object.substring(startIndex, object.length() - 1);
                        // } else {
                        // object = object.substring(startIndex);
                        // }

                        // String[] attributes = object.split(",");
                        // // logger.info("Sending: {}", object);
                        // // logger.info("Attributes: {}", attributes.length);

                        // StartSizeObjectJsonl = jsonlString.length();
                        // jsonlString += "{";

                        // boolean firstAttribute = true;
                        // for (String attribute : attributes) {
                        // // logger.info("Sending to {} attribute [{}]", plateId, attribute);

                        // if (firstAttribute) {
                        // firstAttribute = false;
                        // } else {
                        // jsonlString += ",";
                        // }

                        // // String[] fields = attribute.split("=");
                        // String[] fields = attribute.split(":");
                        // fields[0] = Util.cleanString(fields[0]);
                        // if (fields.length >= 2) {
                        // fields[1] = Util.cleanString(fields[1]);
                        // } else {
                        // fields = new String[] { fields[0], "" };
                        // }

                        // if (fields[0].equalsIgnoreCase("page")) {
                        // currentPage = Integer.parseInt(fields[1]);
                        // }
                        // if (fields[0].equalsIgnoreCase("id")) {
                        // currentID = Integer.parseInt(fields[1]);
                        // }
                        // if (fields[0].equalsIgnoreCase("obj")) {
                        // currentObj = fields[1];
                        // }

                        // // Send Clearpage and set bg color when refreshing the lcd

                        // if (pageToClear < currentPage) { // < prevents clearing when returning from a groups
                        // comm.sendHASPCommand(CommandType.CMD, "clearpage=" + String.valueOf(currentPage));
                        // // connection.publish(plateBaseTopic + "/command/clearpage",
                        // // String.valueOf(currentPage).getBytes(), 1, true);// .get();

                        // // logger.info("[{}] ClearPage: {}", plateId, currentPage);
                        // pageToClear = currentPage;
                        // }

                        // jsonlString += "\"" + fields[0] + "\":";
                        // try {
                        // int num = Integer.parseInt(fields[1]);
                        // jsonlString += num;
                        // } catch (NumberFormatException e) {
                        // jsonlString += "\"" + fields[1] + "\"";
                        // }

                        // if (LimitObjectJsonl < (jsonlString.length() - StartSizeObjectJsonl)) {
                        // jsonlString += "}\n";
                        // StartSizeObjectJsonl = jsonlString.length();
                        // jsonlString += "{\"page\":" + currentPage + ",\"id\":" + currentID;
                        // }
                        // }
                        // jsonlString += "}\n";

                        // NEW MODE

                        jsonlString += object;
                        jsonlString += "\n";

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
                    // Flush before changing page
                    if (jsonlString.length() > 0) {
                        // logger.info("[{}] JSONL={}", jsonlString, jsonlString);
                        comm.sendHASPCommand(CommandType.JSONL, jsonlString);
                    }
                }
                pageNum++;
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

    public @NonNull OpenHASPLayout getLayout() {
        return layout;
    }

    public ObjItemMapper getObjItemMapper() {
        return objItemMapper;
    }
}
