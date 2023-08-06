package org.openhab.binding.openhasp.internal.layout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.openhasp.internal.mapping.ObjItemMapper;
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
    public static final String SLIDE_OBJECT_ID = "slideObjId";
    public static final String STATUS_LABEL_ID = "statusLabelId";

    public static final String TPL_COMP_GROUP = "group";
    public static final String TPL_COMP_BUTTON = "button";
    public static final String TPL_COMP_PAGE = "page";
    public static final String TPL_COMP_SLIDER = "slider";
    public static final String TPL_COMP_SECTION = "section";

    public static final String[] PAGE_INITIAL_TEMPLATE = new String[] { "{id:0,bg_color:#111}",
            "{page:0,id:1,obj:obj,x:0,y:0,h:35,w:320,bg_color:#1c1c1c,text:,radius:0, bg_grad_dir:0,bg_opa:255,border_width:0}",
            "{page:0,id:2,obj:label,x:5,y:5,h:30,w:305,text:2022/10/12  3.13 PM,align:left,text_color:#FFF,text_font:24}",
            "{page:0,id:3,obj:label,x:5,y:5,h:30,w:305,text:1/15,align:right,text_color:#FFF,text_font:24}",

            "{page:0,id:10,obj:btn,action:prev,x:0,y:445,w:107,h:35,bg_color:#1c1c1c,text:,text_color:#FFFFFF,radius:0,border_side:0,text_font:32, bg_grad_dir:0}",
            "{page:0,id:11,obj:btn,action:back,x:107,y:445,w:107,h:35,bg_color:#1c1c1c,text:,text_color:#FFFFFF,radius:0,border_side:0,text_font:32, bg_grad_dir:0}",
            "{page:0,id:12,obj:btn,action:next,x:214,y:445,w:107,h:35,bg_color:#1c1c1c,text:,text_color:#FFFFFF,radius:0,border_side:0,text_font:32,bg_grad_dir:0}" };

    // TODO move this to templates
    public String[] getInitialPage() {
        return PAGE_INITIAL_TEMPLATE;
    }

    // TODO Move to properties
    public static final int MAX_Y = 435;

    private static final Logger logger = LoggerFactory.getLogger(OpenHASPLayout.class);

    private Map<String, String> context;
    private TemplateProcessor tplProc;

    public OpenHASPLayout(Map<String, String> context) {
        this.context = context;
        tplProc = new TemplateProcessor();
    }

    public void initiate() {
    }

    public int getHeight(String component) {
        if (component.equalsIgnoreCase(TPL_COMP_BUTTON)) {
            return 75;
        } else if (component.equalsIgnoreCase(TPL_COMP_PAGE)) {
            return 35;
        } else if (component.equalsIgnoreCase(TPL_COMP_SLIDER)) {
            return 110;
        } else if (component.equalsIgnoreCase(TPL_COMP_SECTION)) {
            return 30; // TODO: This didn't have margin, maybe change how the margin is calculated
        } else if (component.equalsIgnoreCase(TPL_COMP_GROUP)) {
            return 55; // TODO: This didn't have margin, maybe change how the margin is calculated
        }
        return 0;
    }

    public int getVMargin() {
        return 5; // TODO move to properties
    }

    private String getTemplate(String name) {
        return "/templates/" + name + ".json";
    }

    public String[] addComponent(String component, ObjItemMapper objItemMapper) {
        ArrayList<String> result = new ArrayList<String>();
        int y = getAsInt(context, PAGE_Y);

        try {
            result.addAll(Arrays.asList(tplProc.processTemplate(getTemplate(component), context)));
        } catch (IOException e) {
            logger.error("Error processing template", e);
        }
        // String clickObject = builder.getObjectId();

        String clickObject = context.get(CLICK_OBJECT_ID);
        String slideObject = context.get(SLIDE_OBJECT_ID);
        String statusLabel = context.get(STATUS_LABEL_ID);

        String item = context.get(ITEM);
        if (clickObject != null && item != null) {
            objItemMapper.mapObj(new ObjItemMapping(ControlType.SWITCH, item, clickObject, statusLabel));
        }
        if (slideObject != null && item != null) {
            objItemMapper.mapObj(new ObjItemMapping(ControlType.SLIDER, item, slideObject, statusLabel));
        }

        y = y + getHeight(component) + getVMargin();
        context.put(PAGE_Y, Integer.toString(y));

        // reset some values
        context.put(CLICK_OBJECT_ID, null);
        context.put(SLIDE_OBJECT_ID, null);
        context.put(STATUS_LABEL_ID, null);
        context.put(ITEM, null);

        return result.toArray(new String[result.size()]);
    }

    static class ObjectBuilder {
        Map<String, String> context;
        Map<String, String> attributes;
        int pageNum;
        int pageObjId;
        boolean built = false;

        ObjectBuilder(Map<String, String> context) {
            this.context = context;
            pageNum = getAsInt(context, PAGE_NUM);
            pageObjId = getAsInt(context, PAGE_OBJ_ID);
            attributes = new HashMap<String, String>();
        }

        ObjectBuilder withAttr(String key, String value) {
            attributes.put(key, value);
            return this;
        }

        ObjectBuilder withAttr(String key, int value) {
            attributes.put(key, Integer.toString(value));
            return this;
        }

        String build() {
            StringBuilder buffer = new StringBuilder();
            buffer.append("{page:").append(pageNum).append(",id:").append(pageObjId);
            attributes.forEach((k, v) -> buffer.append(",").append(k).append(":").append(v));
            buffer.append("}");

            if (!built) {
                context.put(PAGE_OBJ_ID, Integer.toString(pageObjId + 1));
            }

            built = true;
            return buffer.toString();
        }

        public String getObjectId() {
            return "p" + pageNum + "b" + pageObjId;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }
    }

    public static int getAsInt(Map<String, String> context, String key) {
        String value = context.get(key);
        if (value == null) {
            return 0;
        } else {
            return Integer.parseInt(value);
        }
    }

    public static String getObjectId(String pageNum, String pageObjId) {
        return "p" + pageNum + "b" + pageObjId;
    }
}

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
