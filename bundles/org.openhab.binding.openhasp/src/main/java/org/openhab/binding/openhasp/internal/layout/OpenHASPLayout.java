package org.openhab.binding.openhasp.internal.layout;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.openhasp.internal.layout.components.IComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Handles sending all the layout to the HASP device
@NonNullByDefault
public class OpenHASPLayout {

    public static final String PAGE_NUM = "pageNum";
    public static final String PREVIOUS_PAGE_NUM = "previousPageNum";
    public static final String PAGE_OBJ_ID = "pageObjId";
    public static final String PAGE_LOCATION = "pageLoc"; // Title to show on the page -> breadcrumb
    public static final String PAGE_LOCATION_RETURN_PAGE = "pageLocRePg";
    public static final String PAGE_Y = "y";
    public static final String ITEM = "item";
    public static final String OBJECT_ID = "objId_";

    public static final String ICON_ON = ".on";
    public static final String ICON_OFF = ".off";

    public static final String WIDGET_LABEL = "widgetLabel";
    public static final String WIDGET_ICON = "widgetIcon";

    public static final String CLICK_OBJECT_ID = "clickObjId";
    // TODO slideOBJId is used for slider and dropdown etc, maybe convert to secondary object or something simimlar.
    public static final String SLIDE_OBJECT_ID = "slideObjId";
    public static final String STATUS_LABEL_ID = "statusLabelId";
    public static final String COMPONENTS_VMARGIN = "components_vmargin"; // Vertical margin between components
    public static final String COMPONENTS_MAX_Y = "components_maxy"; // Max usable vertical space for components
    public static final String TIME_CONTROL_ID = "time_control_id";
    public static final String COMP_HEIGHT = "_height";// Height for a specific component
    public static final String OVERRIDE = "_override";// Height for a specific component

    public static final String MAX_Y = "maxy";

    public static final String TPL_COMP_GROUP = "group";
    public static final String TPL_COMP_BUTTON = "button";
    public static final String TPL_COMP_PAGE = "page";
    public static final String TPL_COMP_SLIDER = "slider";
    public static final String TPL_COMP_SETPOINT = "setpoint";
    public static final String TPL_COMP_SECTION = "section";
    public static final String TPL_COMP_SELECTION = "selection";
    public static final String TPL_COMP_TEXT = "text";

    private static final Logger logger = LoggerFactory.getLogger(OpenHASPLayout.class);

    private Map<String, String> context;
    private TemplateProcessor tplProc;
    String templatePath;
    boolean templatePathFileType;

    /**
     * 
     * @param templatePath the path to load from
     * @param templatePathFileType if true will load from file, otherwhise from classpath
     * @param context
     */
    public OpenHASPLayout(String templatePath, boolean templatePathFileType, Map<String, String> context) {
        this.templatePath = templatePath;
        this.templatePathFileType = templatePathFileType;
        this.context = context;
        tplProc = new TemplateProcessor(templatePath, templatePathFileType);
        loadTemplateProperties();
    }

    public void initiate() {
    }

    private void loadTemplateProperties() {
        Properties properties = new Properties();

        // String propertiesPath = getTemplatePath() + "template.properties";
        String propertiesPath = "template.properties";

        // try (final InputStream stream = this.getClass().getResourceAsStream(propertiesPath)) {
        try (final InputStream stream = tplProc.getResourceAsStream(propertiesPath)) {
            if (stream != null) {
                properties.load(stream);
                context.putAll((Map) properties);
            } else {
                logger.error("Error reading properties ", propertiesPath);
            }
        } catch (IOException e) {
            logger.error("Error reading properties from " + propertiesPath, e);
        }
    }

    private String getComponentTemplate(String name) {
        return name + ".json";
    }

    public List<String> getInit() {
        // TODO Error handling
        try {
            return tplProc.processTemplate(getComponentTemplate("init"), context);
        } catch (IOException e) {
            logger.error("Error processing template", e);
            return new ArrayList<String>();
        }
    }

    public void addComponent(IComponent comp, ArrayList<String> objectArray) {
        int y = getAsInt(PAGE_Y);
        try {
            comp.render(tplProc, context, objectArray);
        } catch (IOException e) {
            logger.error("Error processing template", e);
        }
        y = y + comp.getHeight() + getAsInt(COMPONENTS_VMARGIN);
        context.put(PAGE_Y, Integer.toString(y));
        clearOverrides();
    }

    public void sendStatusUpdate(IComponent comp, ArrayList<String> objectArray) {
        // TODO decide if we need more context or if it needs to be shared or local. Starting with local for now.

        // Map<String, String> localContext = new HashMap<String, String>();
        // add additional context here
        try {
            comp.sendStatusUpdate(tplProc, context, objectArray);
        } catch (IOException e) {
            logger.error("Error processing template", e);
        }
    }

    public int getAsInt(String key) {
        return getAsInt(context, key);
    }

    public static int getAsInt(Map<String, String> context, String key) {
        String value = context.get(key + OVERRIDE); // Check if the value was overriden
        boolean overriden = true;

        if (value == null) {
            value = context.get(key);
            overriden = false;
        }

        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.error("Could not parse key: {} in context to integer. Value:{}",
                        overriden ? key + OVERRIDE : key, value);
            }
        } else {
            logger.error("Couln't find key: {} in context", overriden ? key + OVERRIDE : key);
        }
        return 0;
    }

    private void clearOverrides() {
        Iterator<Entry<String, String>> iterator = context.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, String> entry = iterator.next();
            String key = entry.getKey();
            if (key.endsWith(OVERRIDE)) {
                iterator.remove();
                logger.error("Removed key {}", key);
            }
        }
    }

    public static String getObjectId(String pageNum, String pageObjId) {
        return "p" + pageNum + "b" + pageObjId;
    }

    public @NonNull String getAsString(String key) {
        String value = context.get(key + OVERRIDE); // Check if the value was overriden
        boolean overriden = true;

        if (value == null) {
            value = context.get(key);
            overriden = false;
        }

        if (value != null) {
            return value;
        } else {
            logger.error("Couln't find key: {} in context", overriden ? key + OVERRIDE : key);
        }
        return "";
    }
}
