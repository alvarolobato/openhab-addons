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
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.CommandType;
import org.openhab.binding.openhasp.internal.OpenHASPCommunicationManager;
import org.openhab.binding.openhasp.internal.OpenHASPThingConfiguration;
import org.openhab.binding.openhasp.internal.Util;
import org.openhab.binding.openhasp.internal.layout.components.ButtonWidget;
import org.openhab.binding.openhasp.internal.layout.components.IComponent;
import org.openhab.binding.openhasp.internal.layout.components.PageWidget;
import org.openhab.binding.openhasp.internal.layout.components.SectionWidget;
import org.openhab.binding.openhasp.internal.layout.components.SelectionWidget;
import org.openhab.binding.openhasp.internal.layout.components.SliderWidget;
import org.openhab.binding.openhasp.internal.layout.components.TextWidget;
import org.openhab.binding.openhasp.internal.mapping.IObjItemMapping;
import org.openhab.binding.openhasp.internal.mapping.ObjItemMapper;
import org.openhab.binding.openhasp.internal.mapping.ObjItemMapping;
import org.openhab.core.events.Event;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEvent;
import org.openhab.core.model.sitemap.sitemap.Chart;
import org.openhab.core.model.sitemap.sitemap.Frame;
import org.openhab.core.model.sitemap.sitemap.Group;
import org.openhab.core.model.sitemap.sitemap.LinkableWidget;
import org.openhab.core.model.sitemap.sitemap.Selection;
import org.openhab.core.model.sitemap.sitemap.Setpoint;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.Slider;
import org.openhab.core.model.sitemap.sitemap.Switch;
import org.openhab.core.model.sitemap.sitemap.Text;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.google.gson.Gson;
//know about the items and stuff in openhab and send the layout to OpenHASPLayout to format
@NonNullByDefault
public class OpenHASPLayoutManager {
    private static final Logger logger = LoggerFactory.getLogger(OpenHASPLayoutManager.class);
    private final static DecimalFormat setPointFormat = new DecimalFormat("#0.#");
    // private final static Gson gson = new Gson();
    String thingId;
    String plateId;
    protected ObjItemMapper objItemMapper;
    protected OpenHASPCommunicationManager comm;
    protected OpenHASPThingConfiguration config;
    HashMap<String, String> context;
    OpenHASPLayout layout;

    ArrayList<List<String>> pages;
    ArrayList<String> objectArray;
    ArrayList<String> pendingCommands;
    ItemRegistry itemRegistry;

    String templatePath;
    boolean templatePathFileType = false; // if true will load from file, otherwhise from classpath

    public OpenHASPLayoutManager(String thingId, String plateId, OpenHASPCommunicationManager comm,
            OpenHASPThingConfiguration config, ItemRegistry itemRegistry) {
        objItemMapper = new ObjItemMapper();
        this.thingId = thingId;
        this.plateId = plateId;
        this.comm = comm;
        this.config = config;
        this.itemRegistry = itemRegistry;

        templatePathFileType = "file".equalsIgnoreCase(config.templatePathType);
        templatePath = config.templatePath;
        if (templatePath == null || templatePath.isEmpty()) {
            templatePath = "/templates/default-portrait/";
        } else {
            if (!templatePath.endsWith("/")) {
                templatePath = templatePath + "/";
            }
        }

        context = new HashMap<String, String>();
        layout = new OpenHASPLayout(templatePath, templatePathFileType, context);
        pages = new ArrayList<List<String>>(0);
        // TODO should disapear and be replaced by pages
        objectArray = new ArrayList<String>(0);
        pendingCommands = new ArrayList<String>(0);
        layout.initiate();
    }

    // TODO Multi-sitemap support
    public void loadFromSiteMap(Sitemap sitemap) {
        String sitemapLabel = sitemap.getLabel();
        // Properties prop=new Properties();
        // prop.load(null);

        objectArray.addAll(layout.getInit());

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
            // String component = "";

            String itemName = w.getItem();

            Item item = itemRegistry.get(itemName);

            @Nullable
            State itemState = null;
            if (item != null) {
                itemState = item.getState();
            }
            // else {
            // logger.error("Item for {}:{} was NULL", w, itemName);
            // }

            // String widgetLabel = getWidgetLabel(w, itemState);

            IComponent comp = null;
            if (w instanceof Slider) {
                // component = OpenHASPLayout.TPL_COMP_SLIDER;
                comp = new SliderWidget(context, w, item);
                // TODO set position
            } else if (w instanceof Switch) {
                comp = new ButtonWidget(context, w, item);
                // component = OpenHASPLayout.TPL_COMP_BUTTON;
            } else if (w instanceof Frame) {
                // component = OpenHASPLayout.TPL_COMP_SECTION;
                comp = new SectionWidget(context, w, item);
            } else if (w instanceof Group | (w instanceof Text && !((Text) w).getChildren().isEmpty())) {
                logger.trace("GROUP IGNORED!! item: {} itemState {}", itemName, itemState);
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
                // component = OpenHASPLayout.TPL_COMP_TEXT;
                comp = new TextWidget(context, w, item);
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
                // component = OpenHASPLayout.TPL_COMP_SELECTION;
                Selection sel = (Selection) w;
                comp = new SelectionWidget(context, sel, item);
            } else if (w instanceof Chart) {
                // TODO: Not Implemented
                continue;
            } else if (w instanceof Setpoint) {
                // double doubleVal = getItemStateDoubletVal(itemState);

                // component = OpenHASPLayout.TPL_COMP_SETPOINT;
                // context.put("widgetVal", setPointFormat.format(doubleVal)); // number of selected option
            } else {
                comp = new TextWidget(context, w, item);
                // component = OpenHASPLayout.TPL_COMP_SECTION;
                // context.put("widgetLabel", "*" + widgetLabel);
                // logger.trace("NOT IMPLEMENTED Page: {} - {} (Item:{},Label:{})", context.get(PAGE_NUM),
                // w.getClass().getSimpleName(), w.getItem(), w.getLabel());
            }

            if (comp != null) {
                // Ensure there's enough height
                int y = getAsInt(context, PAGE_Y);
                int height = comp.getHeight();
                if (y + height > compMaxY
                        // also if it is a section
                        || comp.getComponent() == OpenHASPLayout.TPL_COMP_SECTION
                                && !"1".equals(context.get("isBlankPage"))) {

                    newPage(objectArray);
                    y = getAsInt(context, PAGE_Y);
                }

                // int y = getAsInt(context, PAGE_Y);
                // int height = layout.getAsInt(component + OpenHASPLayout.COMP_HEIGHT);
                // if (y + height > compMaxY
                // // also if it is a section
                // || component == OpenHASPLayout.TPL_COMP_SECTION && !"1".equals(context.get("isBlankPage"))) {

                // newPage(objectArray);
                // y = getAsInt(context, PAGE_Y);
                // }

                // if (comp == null) {
                // context.put("item", itemName);
                // context.put("widgetLabel", widgetLabel);
                // }

                // Actually add the component
                ObjItemMapping mapping;
                logger.trace("Adding component {}", comp);
                // if (comp != null) {

                layout.addComponent(comp, objectArray);
                objItemMapper.mapObj(comp);

                // if (mapping != null) {
                // if (positionValues != null) {
                // mapping.positionValues = positionValues;
                // }
                // objItemMapper.mapObj(mapping);
                // }
                // } else {
                // mapping = layout.addComponent(component, objectArray);
                // if (mapping != null) {
                // if (positionValues != null) {
                // mapping.positionValues = positionValues;
                // }
                // objItemMapper.mapObj(mapping);
                // }
                // }

                context.put("isBlankPage", "0");

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
                    String widgetLabel;
                    widgetLabel = "Group";
                    // = getWidgetLabel(w, itemState);
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
            }

            if (w instanceof LinkableWidget && !isGroup) { // TODO review this
                processWidgetList(((LinkableWidget) w).getChildren());
            }
        }
    }

    public void newPage(ArrayList<String> objectArray) {
        int currentPage = getAsInt(context, PAGE_NUM);
        context.put(PREVIOUS_PAGE_NUM, Integer.toString(currentPage));
        context.put(PAGE_NUM, Integer.toString(currentPage + 1));
        context.put(PAGE_Y, "0");
        context.put(PAGE_OBJ_ID, "0");
        PageWidget page = new PageWidget(context); // TODO maybne change to something like getInit()

        layout.addComponent(page, objectArray);
        // ObjItemMapping mapping = layout.addComponent(page, objectArray);
        // if (mapping != null) {
        // objItemMapper.mapObj(mapping);
        // }
        context.put("isBlankPage", "1");
    }

    public void sendPages() {
        // connection.publish(fullCommandTopic + "/LWT", payload, qos, retain);
        logger.info("Sending pages to {}", plateId);

        int LimitDeviceJsonl = 500;
        String jsonlString = "";

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
                        object = object.trim();
                        if (object.isEmpty()) {
                            continue;
                        }
                        // NEW MODE
                        jsonlString += object;
                        jsonlString += "\n";

                        if (LimitDeviceJsonl < jsonlString.length()) {
                            comm.sendHASPCommand(CommandType.JSONL, jsonlString);
                            jsonlString = "";
                        }
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

    public @Nullable List<IObjItemMapping> getByItem(@NonNull String item) {
        return objItemMapper.getByItem(item);
    }

    public @Nullable IObjItemMapping getByObject(@NonNull String object) {
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

    /**
     * Updates the state of all the widgets in this plate
     * This only changes the state of the widget internally, it won't send it to the plate.
     */
    public void updateAllStates() {
        HashMap<String, IObjItemMapping> allByObject = getObjItemMapper().getAllByObject();
        Set<Entry<String, IObjItemMapping>> entrySet = allByObject.entrySet();
        for (Entry<String, IObjItemMapping> entry : entrySet) {
            IObjItemMapping mapping = entry.getValue();
            refreshMapping(mapping);
        }
    }

    /**
     * Updates the state of all the widgets mapped to the item.
     * This only changes the state of the widget internally, it won't send it to the plate.
     */
    public void processItemStateEvent(Event event) {
        List<IObjItemMapping> mappingList = getByItem(((ItemEvent) event).getItemName());
        if (mappingList != null) {
            for (IObjItemMapping mapping : mappingList) {
                refreshMapping(mapping);
            }
        }
    }

    public void refreshMapping(IObjItemMapping mapping) {
        logger.trace("Updating HASP {}", mapping);
        mapping.updateState();
        if (mapping instanceof IComponent) {
            IComponent comp = (IComponent) mapping;
            layout.sendStatusUpdate(comp, pendingCommands);
        }
    }

    public void sendPendingCommands() {
        // connection.publish(fullCommandTopic + "/LWT", payload, qos, retain);
        logger.info("Sending {} pending commands to {}", pendingCommands.size(), plateId);

        comm.sendHASPCommand(CommandType.JSON, pendingCommands);
        pendingCommands.clear();

        /*
         * int LimitDeviceJsonl = 500;
         * String jsonlString = "";
         * 
         * try {
         * for (String cmd : pendingCommands) {
         * cmd = cmd.trim();
         * if (cmd.isEmpty()) {
         * continue;
         * }
         * jsonlString += cmd;
         * jsonlString += "\n";
         * 
         * if (LimitDeviceJsonl < jsonlString.length()) {
         * logger.info("{}", jsonlString);
         * comm.sendHASPCommand(CommandType.JSON, jsonlString);
         * jsonlString = "";
         * }
         * }
         * if (jsonlString.length() > 0) {
         * logger.info("{}", jsonlString);
         * comm.sendHASPCommand(CommandType.JSON, jsonlString);
         * }
         * } catch (RuntimeException e) {
         * logger.error("Error sending pending commands", e);
         * }
         */
        logger.info("Send pending commands {} DONE", plateId);
    }
}
