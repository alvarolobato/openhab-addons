package org.openhab.binding.openhasp.internal.mapping;

public class ObjItemMapping {

    public enum ControlType {
        SWITCH,
        SLIDER
    }

    public String item;
    public ObjItemMapping.ControlType type;
    public String objId;
    public String statusLabelId;

    public ObjItemMapping(ObjItemMapping.ControlType type, String item, String objId, String statusLabelId) {
        this.type = type;
        this.item = item;
        this.objId = objId;
        this.statusLabelId = statusLabelId;
    }

    @Override
    public String toString() {
        return "ObjectItemMapping [item=" + item + ", type=" + type + ", obj=" + objId + "]";
    }
}
