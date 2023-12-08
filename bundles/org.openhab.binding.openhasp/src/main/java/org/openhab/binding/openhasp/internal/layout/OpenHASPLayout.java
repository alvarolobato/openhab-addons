package org.openhab.binding.openhasp.internal.layout;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.mapping.ObjItemMapping;
import org.openhab.binding.openhasp.internal.mapping.ObjItemMapping.ControlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Handles sending all the layout to the HASP device
public class OpenHASPLayout {

    public static final String PAGE_NUM = "pageNum";
    public static final String PREVIOUS_PAGE_NUM = "previousPageNum";
    public static final String PAGE_OBJ_ID = "pageObjId";
    public static final String PAGE_LOCATION = "pageLoc"; // Title to show on the page -> breadcrumb
    public static final String PAGE_LOCATION_RETURN_PAGE = "pageLocRePg";
    public static final String PAGE_Y = "y";
    public static final String ITEM = "item";
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

    private static final Logger logger = LoggerFactory.getLogger(OpenHASPLayout.class);

    private Map<String, String> context;
    private TemplateProcessor tplProc;
    String template;

    public OpenHASPLayout(String template, Map<String, String> context) {
        this.template = template;
        this.context = context;
        loadTemplateProperties();
        tplProc = new TemplateProcessor();
    }

    public void initiate() {
    }

    private String getTemplatePath() {
        return "/templates/" + template + "/";
    }

    private void loadTemplateProperties() {
        Properties properties = new Properties();
        String propertiesPath = getTemplatePath() + "template.properties";

        try (final InputStream stream = this.getClass().getResourceAsStream(propertiesPath)) {
            if (stream != null) {
                properties.load(stream);
                context.putAll((Map) properties);
            } else {
                logger.error("Error reading properties from {}", propertiesPath);
            }
        } catch (IOException e) {
            logger.error("Error reading properties from " + propertiesPath, e);
        }
    }

    private String getComponentTemplate(String name) {
        return getTemplatePath() + name + ".json";
    }

    public String[] getInit() {
        // TODO Error handling
        try {
            return tplProc.processTemplate(getComponentTemplate("init"), context);
        } catch (IOException e) {
            logger.error("Error processing template", e);
            return new String[0];
        }
    }

    @Nullable
    public ObjItemMapping addComponent(String component, ArrayList<String> objectArray) {
        ArrayList<String> result = new ArrayList<String>();
        int y = getAsInt(PAGE_Y);

        try {
            result.addAll(Arrays.asList(tplProc.processTemplate(getComponentTemplate(component), context)));
        } catch (IOException e) {
            // TODO Error handling
            logger.error("Error processing template", e);
        }
        // String clickObject = builder.getObjectId();
        ObjItemMapping mapping = null;
        // if (component != "page") {
        String clickObject = context.get(CLICK_OBJECT_ID);
        String slideObject = context.get(SLIDE_OBJECT_ID);
        String statusLabel = context.get(STATUS_LABEL_ID);

        String item = context.get(ITEM);

        if (item != null) {
            // TODO Refactor how to figure out the component type
            ControlType type = null;
            if (component.equals(TPL_COMP_BUTTON)) {
                type = ControlType.SWITCH;
            } else if (component.equals(TPL_COMP_SLIDER)) {
                type = ControlType.SLIDER;
            } else if (component.equals(TPL_COMP_SELECTION)) {
                type = ControlType.SELECT;
            } else {
                logger.warn("Setting default controltype for component {}", component);
                type = ControlType.SLIDER;
            }
            mapping = new ObjItemMapping(type, item, clickObject, statusLabel, slideObject);
        } else {
            logger.error("Could not get item from context");
        }

        // if (slideObject != null && item != null) {
        // objItemMapper.mapObj(new ObjItemMapping(ControlType.SLIDER, item, clickObject, statusLabel,
        // slideObject));
        // }
        // if (clickObject != null && item != null) {
        // objItemMapper.mapObj(new ObjItemMapping(ControlType.SWITCH, item, clickObject, statusLabel, null));
        // }

        y = y + getAsInt(component + COMP_HEIGHT) + getAsInt(COMPONENTS_VMARGIN);
        context.put(PAGE_Y, Integer.toString(y));

        // reset some values
        context.put(CLICK_OBJECT_ID, null);
        context.put(SLIDE_OBJECT_ID, null);
        context.put(STATUS_LABEL_ID, null);
        context.put(ITEM, null);
        // }
        clearOverrides();
        objectArray.addAll(result);
        return mapping;
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

// static class ObjectBuilder {
// Map<String, String> context;
// Map<String, String> attributes;
// int pageNum;
// int pageObjId;
// boolean built = false;

// ObjectBuilder(Map<String, String> context) {
// this.context = context;
// pageNum = getAsInt(context, PAGE_NUM);
// pageObjId = getAsInt(context, PAGE_OBJ_ID);
// attributes = new HashMap<String, String>();
// }

// ObjectBuilder withAttr(String key, String value) {
// attributes.put(key, value);
// return this;
// }

// ObjectBuilder withAttr(String key, int value) {
// attributes.put(key, Integer.toString(value));
// return this;
// }

// String build() {
// StringBuilder buffer = new StringBuilder();
// buffer.append("{page:").append(pageNum).append(",id:").append(pageObjId);
// attributes.forEach((k, v) -> buffer.append(",").append(k).append(":").append(v));
// buffer.append("}");

// if (!built) {
// context.put(PAGE_OBJ_ID, Integer.toString(pageObjId + 1));
// }

// built = true;
// return buffer.toString();
// }

// public String getObjectId() {
// return "p" + pageNum + "b" + pageObjId;
// }

// public Map<String, String> getAttributes() {
// return attributes;
// }
// }

// public static final String[] PAGE_TEMPLATE = new String[] { "{page:1,id:0,bg_color:#111}" };

// public static final String[] BUTTON_TEMPLATE = new String[] {
// "{page:1,id:11,obj:obj,x:5,y:195,w:310,h:75,radius:15,click:true,border_side:0,bg_color:#1c1c1c}",
// "{page:1,id:12,obj:obj,x:15,y:200,w:65,h:65,radius:35,border_side:0,bg_color:#493416,click:false}",
// "{page:1,id:13,obj:label,x:23,y:205,w:65,h:65,text:\uE769, text_font: 48, text_color:#e88c03}",
// "{page:1,id:14,obj:label,x:100,y:205,w:200,h:40,text:${widgetLabel}, text_font: 24}",
// "{page:1,id:15,obj:label,x:100,y:235,w:200,h:40,text:On, text_font: 16, text_color:#9b9b9b}" };

// public String[] initPage() {
// // TODO: use context?

// ArrayList<String> result = new ArrayList<String>();
// int y = 0;
// context.put(PAGE_OBJ_ID, "0");
// String object;

// // logger.trace("Init page {}", context.get(OpenHASPLayout.PAGE_NUM));
// // String result = "{page:" + pageNum + ",id:" + pageObjId +
// // "0,bg_color:#111,bg_grad_dir:0}";

//     // @formatter:off
    //     //Container object
    //     // object=new ObjectBuilder(context)
    //     //     .withAttr("bg_grad_dir","0")
    //     //     .withAttr("bg_color","#111")
    //     //     .withAttr("next","1")
    //     //     .withAttr("click","false")
    //     //     .withAttr("prev",context.getOrDefault(PREVIOUS_PAGE_NUM,""))
    //     //     .build();
    //     // result.add(object);        
    //     y = 35;

    //     // //Return link
    //     // String returnPage=context.getOrDefault(PAGE_LOCATION_RETURN_PAGE,"");

    //     // //Background
    //     // object=new ObjectBuilder(context)
    //     //     .withAttr("obj","obj")
    //     //     .withAttr("x", "0")
    //     //     .withAttr("y",y)
    //     //     .withAttr("w","320")
    //     //     .withAttr("h","35")
    //     //     .withAttr("radius","0")
    //     //     .withAttr("bg_grad_dir","0")
    //     //     .withAttr("bg_opa","255")
    //     //     .withAttr("border_width","0")
    //     //     .withAttr("bg_color","#1c1c1c")
    //     //     .build(); 
    //     // result.add(object);

    //     // if(!returnPage.isEmpty()){
    //     //     object=new ObjectBuilder(context)
    //     //         .withAttr("obj","label")
    //     //         .withAttr("x", "5")
    //     //         .withAttr("y",y)
    //     //         .withAttr("w","35")
    //     //         .withAttr("h","30")
    //     //         .withAttr("text","\uE04D")
    //     //         .withAttr("text_font","24")
    //     //         .withAttr("text_color","#FFF")
    //     //         .withAttr("click","true")
    //     //         .withAttr("action","p"+context.get(PAGE_LOCATION_RETURN_PAGE))
    //     //         .build(); 
    //     //     result.add(object);
    //     // }

    //     // object=new ObjectBuilder(context)
    //     //     .withAttr("obj","label")
    //     //     .withAttr("x", "40")
    //     //     .withAttr("y",y)
    //     //     .withAttr("w","280")
    //     //     .withAttr("h","30")
    //     //     .withAttr("text",context.get(PAGE_LOCATION))
    //     //     .withAttr("text_font","24")
    //     //     .withAttr("mode","scroll")
    //     //     .withAttr("bg_color","#1c1c1c")
    //     //     .build(); 
    //     // result.add(object);
    //     // @formatter:on

// y = y + 40;
// context.put(PAGE_Y, Integer.toString(y));
// return result.toArray(new String[result.size()]);
// }

// public String[] newPage(boolean linkPreviousPage) {
// // TODO use context?
// // TODO move to manager
// int currentPage = getAsInt(context, PAGE_NUM);

// context.put(PREVIOUS_PAGE_NUM, Integer.toString(currentPage));
// context.put(PAGE_NUM, Integer.toString(currentPage + 1));
// context.put(PAGE_Y, "0");
// context.put(PAGE_OBJ_ID, "0");
// return new String[] { "{page:" + currentPage + ",id:0,next:" + (currentPage + 1) + "}",
// "{page:1, id:0, prev:" + (currentPage + 1) + "}" };
// }

// public String[] addButton(ObjItemMapper objItemMapper) {
// ArrayList<String> result = new ArrayList<String>();
// int y = getAsInt(context, PAGE_Y);
// // String object;
// // ObjectBuilder builder;
// // if (y + 75 > MAX_Y) {
// // // TODO This new page decision should probably be in the manager
// // result.addAll(Arrays.asList(newPage(true)));
// // result.addAll(Arrays.asList(initPage()));
// // y = getAsInt(context, PAGE_Y);
// // }

//     // @formatter:off
    //     //Container object
    //     // builder = new ObjectBuilder(context)
    //     //     .withAttr("obj","obj")
    //     //     .withAttr("x", "5")
    //     //     .withAttr("y",y)
    //     //     .withAttr("w","310")
    //     //     .withAttr("h","75")
    //     //     .withAttr("radius","15")
    //     //     .withAttr("click","true")
    //     //     .withAttr("border_side","0")
    //     //     .withAttr("bg_color","#1c1c1c")
    //     //     .withAttr("bg_grad_dir","0");
    //     //result.add(builder.build());

    //     //String clickObject = builder.getObjectId();

    //     //Icon background circle
    //     // object = new ObjectBuilder(context)
    //     //     .withAttr("obj","obj")
    //     //     .withAttr("x","15")
    //     //     .withAttr("y",y+5)
    //     //     .withAttr("w","65")
    //     //     .withAttr("h","65")
    //     //     .withAttr("radius","35")
    //     //     .withAttr("click","false")
    //     //     .withAttr("border_side","0")
    //     //     .withAttr("bg_color","#493416")
    //     //     .build();
    //     // result.add(object);

    //     //Icon
    //     // object = new ObjectBuilder(context)
    //     //     .withAttr("obj","label")
    //     //     .withAttr("x","15")
    //     //     .withAttr("y",y+10)
    //     //     .withAttr("w","65")
    //     //     .withAttr("h","65")
    //     //     .withAttr("text","\uE769")
    //     //     .withAttr("text_font","48")
    //     //     .withAttr("text_color","#e88c03")
    //     //     .withAttr("align","center")
    //     //     .build();
    //     // result.add(object);

    //     //Label name
    //     // object = new ObjectBuilder(context)
    //     //     .withAttr("obj","label")
    //     //     .withAttr("x","100")
    //     //     .withAttr("y", y+10)
    //     //     .withAttr("w","200")
    //     //     .withAttr("h","40")
    //     //     .withAttr("text",context.get("widgetLabel"))
    //     //     .withAttr("mode","scroll")
    //     //     .withAttr("text_font","24")
    //     //     .build();
    //     // result.add(object);

    //     //Label status
    //     // builder = new ObjectBuilder(context)
    //     //     .withAttr("obj","label")
    //     //     .withAttr("x","100")
    //     //     .withAttr("y", y+40)
    //     //     .withAttr("w","200")
    //     //     .withAttr("h","40")
    //     //     .withAttr("text","On")
    //     //     .withAttr("text_font","16")
    //     //     .withAttr("text_color","#9b9b9b");
    //     // result.add(builder.build());
        // String statusLabelId= builder.getObjectId();

    //     try {
    //         result.addAll(Arrays.asList(tplProc.processTemplate("/templates/button.json", context)));
    //     } catch (IOException e) {
    //         logger.error("Error processing template", e);
    //     }
        
    //     // String item=context.get("item");
    //     // if(item!=null){
    //     //     objItemMapper.mapObj(new ObjItemMapping(ControlType.SWITCH, item, clickObject,statusLabelId));
    //     // }else{
    //     //     logger.error("Missing item when mapping {}",context.get("widgetLabel"));
    //     // }
    //  // @formatter:on

// y = y + 80;
// context.put(PAGE_Y, Integer.toString(y));
// return result.toArray(new String[result.size()]);
// }

// public String[] addSlider(ObjItemMapper objItemMapper) {
// // ArrayList<String> result = new ArrayList<String>();
// // int y = getAsInt(context, PAGE_Y);
// // ObjectBuilder builder;
// // String object, objId;
// // if (y + 110 > MAX_Y) {
// // // TODO this should probably be in the manager
// // result.addAll(Arrays.asList(newPage(true)));
// // result.addAll(Arrays.asList(initPage()));
// // y = getAsInt(context, PAGE_Y);
// // }

//     // @formatter:off
    //     //Container object
    //     object=new ObjectBuilder(context)
    //         .withAttr("obj","obj")
    //         .withAttr("x", "5")
    //         .withAttr("y",y)
    //         .withAttr("w","310")
    //         .withAttr("h","110")
    //         .withAttr("radius","15")
    //         .withAttr("click","false")
    //         .withAttr("border_side","0")
    //         .withAttr("bg_color","#1c1c1c")
    //         .withAttr("bg_grad_dir","0")
    //         .build();
    //     result.add(object);

    //     //Container that doesn't include the slider for the click even
    //     builder = new ObjectBuilder(context)
    //         .withAttr("obj","obj")
    //         .withAttr("x", "5")
    //         .withAttr("y",y)
    //         .withAttr("w","310")
    //         .withAttr("h","68")
    //         .withAttr("radius","15")
    //         .withAttr("click","true")
    //         .withAttr("border_side","0")
    //         .withAttr("bg_color","#1c1c1c")
    //         .withAttr("bg_grad_dir","0");
    //     result.add(builder.build());

        
    //     String clickObject = builder.getObjectId();

    //     //Icon background circle
    //     object = new ObjectBuilder(context)
    //         .withAttr("obj","obj")
    //         .withAttr("x","15")
    //         .withAttr("y",y+5)
    //         .withAttr("w","65")
    //         .withAttr("h","65")
    //         .withAttr("radius","35")
    //         .withAttr("click","false")
    //         .withAttr("border_side","0")
    //         .withAttr("bg_color","#493416")
    //         .build();
    //     result.add(object);
        
    //     //Icon
    //     object = new ObjectBuilder(context)
    //         .withAttr("obj","label")
    //         .withAttr("x","15")
    //         .withAttr("y",y+10)
    //         .withAttr("w","65")
    //         .withAttr("h","65")
    //         .withAttr("text","\uE6B5")
    //         .withAttr("text_font","48")
    //         .withAttr("text_color","#e88c03")
    //         .withAttr("align","center")
    //         .build();
    //     result.add(object);

    //     //Label
    //     object = new ObjectBuilder(context)
    //         .withAttr("obj","label")
    //         .withAttr("x","100")
    //         .withAttr("y", y+10)
    //         .withAttr("w","200")
    //         .withAttr("h","40")
    //         .withAttr("text",context.get("widgetLabel"))
    //         .withAttr("text_font","24")
    //         .withAttr("mode","scroll")
    //         .build();
    //     result.add(object);

    //     //Label status
    //     builder = new ObjectBuilder(context)
    //         .withAttr("obj","label")
    //         .withAttr("x","100")
    //         .withAttr("y", y+40)
    //         .withAttr("w","200")
    //         .withAttr("h","40")
    //         .withAttr("text","On")
    //         .withAttr("text_font","16")
    //         .withAttr("text_color","#9b9b9b");
    //     result.add(builder.build());

    //     String item=context.get("item");
    //     String statusLabelId = builder.getObjectId();
    //     if(item!=null){
    //         objItemMapper.mapObj(new ObjItemMapping(ControlType.SWITCH, item, clickObject, statusLabelId));
    //     }else{
    //         logger.error("Missing item when mapping {}",context.get("widgetLabel"));
    //     }

    //     //Slider
    //     builder = new ObjectBuilder(context)
    //         .withAttr("obj","slider")
    //         .withAttr("x","25")
    //         .withAttr("y", y+80)
    //         .withAttr("w","270")
    //         .withAttr("h","15")
    //         .withAttr("min","0")
    //         .withAttr("max","100");
    //     result.add(builder.build());

    //     if(item!=null){
    //         objId = builder.getObjectId();
    //         objItemMapper.mapObj(new ObjItemMapping(ControlType.SLIDER, item, objId, statusLabelId));
    //     }else{
    //         logger.error("Missing item when mapping {}",context.get("widgetLabel"));
    //     }

    //     // @formatter:on

// y = y + 115;
// context.put(PAGE_Y, Integer.toString(y));
// return result.toArray(new String[result.size()]);
// }

// public String[] addSection() {
// ArrayList<String> result = new ArrayList<String>();
// int y = getAsInt(context, PAGE_Y);
// String object;
// if (y + 30 > MAX_Y) {
// // TODO Should probably be in the manager
// result.addAll(Arrays.asList(newPage(true)));
// result.addAll(Arrays.asList(initPage()));
// y = getAsInt(context, PAGE_Y);
// }

//     // @formatter:off
    //     object=new ObjectBuilder(context)
    //         .withAttr("obj","label")
    //         .withAttr("x", "20")
    //         .withAttr("y",y)
    //         .withAttr("w","300")
    //         .withAttr("h","30")
    //         .withAttr("text",context.get("widgetLabel"))
    //         .withAttr("text_font","24")
    //         .withAttr("mode","scroll")
    //         .withAttr("bg_color","#1c1c1c")

    //         .build(); 
    //     result.add(object);
    //  // @formatter:on

// y = y + 30;
// context.put(PAGE_Y, Integer.toString(y));
// return result.toArray(new String[result.size()]);
// }

// public String[] addGroup() {
// ArrayList<String> result = new ArrayList<String>();
// int y = getAsInt(context, PAGE_Y);
// String object;
// if (y + 55 > MAX_Y) {
// // TODO Should porbably be in the manager
// result.addAll(Arrays.asList(newPage(true)));
// result.addAll(Arrays.asList(initPage()));
// y = getAsInt(context, PAGE_Y);
// }

// int nextGroupPage = getAsInt(context, "nextGroupPage");

//     // @formatter:off
    //     //Container object
    //     object=new ObjectBuilder(context)
    //         .withAttr("obj","obj")
    //         .withAttr("x", "5")
    //         .withAttr("y",y)
    //         .withAttr("w","310")
    //         .withAttr("h","50")
    //         .withAttr("radius","15")
    //        // .withAttr("click","false")
    //         .withAttr("border_side","0")
    //         .withAttr("bg_color","#1c1c1c")
    //         .withAttr("action","p"+nextGroupPage)
    //         .withAttr("bg_grad_dir","0")
    //         .build();
    //     result.add(object);

    //     //Icon background circle
    //     object = new ObjectBuilder(context)
    //         .withAttr("obj","obj")
    //         .withAttr("x","15")
    //         .withAttr("y",y+5)
    //         .withAttr("w","40")
    //         .withAttr("h","40")
    //         .withAttr("radius","35")
    //         .withAttr("click","false")
    //         .withAttr("border_side","0")
    //         .withAttr("bg_color","#493416")
    //         .build();
    //     result.add(object);

    //     //Icon
    //     object = new ObjectBuilder(context)
    //         .withAttr("obj","label")
    //         .withAttr("x","15")
    //         .withAttr("y",y+5)
    //         .withAttr("w","40")
    //         .withAttr("h","40")
    //         .withAttr("text","\uE769")
    //         .withAttr("text_font","32")
    //         .withAttr("text_color","#e88c03")
    //         .withAttr("align","center")
    //         .build();
    //     result.add(object);

    //     //Label
    //     object = new ObjectBuilder(context)
    //         .withAttr("obj","label")
    //         .withAttr("x","60")
    //         .withAttr("y", y+10)
    //         .withAttr("w","225")
    //         .withAttr("h","40")
    //         .withAttr("text",context.get("widgetLabel"))
    //         .withAttr("text_font","24")
    //         .withAttr("mode","scroll")
    //         .build();
    //     result.add(object);

    //     //Arrow
    //     object = new ObjectBuilder(context)
    //         .withAttr("obj","label")
    //         .withAttr("x","280")
    //         .withAttr("y", y+5)
    //         .withAttr("w","20")
    //         .withAttr("h","40")
    //         .withAttr("text","\uE142")
    //         .withAttr("text_font","32")
    //         .withAttr("text_color","#e88c03")
    //         .build();
    //     result.add(object);
    //  // @formatter:on

// y = y + 55;
// context.put(PAGE_Y, Integer.toString(y));
// return result.toArray(new String[result.size()]);
// }
