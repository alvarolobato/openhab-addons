package org.openhab.binding.openhasp.internal.mapping;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ObjItemMapping {

    public enum ControlType {
        SWITCH,
        SLIDER,
        SELECT
    }

    public String item;
    public ObjItemMapping.ControlType type; // TODO remove, not used
    public @Nullable String objId;
    public @Nullable String statusLabelId;
    public @Nullable String sliderId;
    public String @Nullable [] positionValues;
    private static final Logger logger = LoggerFactory.getLogger(ObjItemMapping.class);

    public ObjItemMapping(ObjItemMapping.ControlType type, String item, @Nullable String objId,
            @Nullable String statusLabelId, @Nullable String sliderId) {
        this.type = type;
        this.item = item;
        this.objId = objId;
        this.statusLabelId = statusLabelId;
        this.sliderId = sliderId;
        this.positionValues = null;
    }

    @Override
    public String toString() {
        return "ObjectItemMapping [item=" + item + ", type=" + type + ", obj=" + objId + ", status=" + statusLabelId
                + ", slider=" + sliderId + "]";
    }

    public int findValuePosition(String value) {
        if (positionValues != null) {
            for (int i = 0; i < positionValues.length; i++) {
                if (positionValues[i].equalsIgnoreCase(value)) {
                    return i;
                }
            }
            logger.warn("Couldn't find position value {} for item {}, available values {}", value, item,
                    positionValues);
        }
        return 0;
    }
}
