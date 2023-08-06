package org.openhab.binding.openhasp.internal.layout.handlebars;

import java.util.HashMap;

import org.openhab.binding.openhasp.internal.layout.OpenHASPLayout;

public class HBUtils {
    public static String getObjId(HashMap<Object, Object> realContext) {
        Object pageNum = realContext.get(OpenHASPLayout.PAGE_NUM);
        Object pageObjId = realContext.get(OpenHASPLayout.PAGE_OBJ_ID);
        String objId = OpenHASPLayout.getObjectId(pageNum.toString(), pageObjId.toString());
        return objId;
    }
}
