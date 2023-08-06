package org.openhab.binding.openhasp.internal.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjItemMapper {

    final Logger logger = LoggerFactory.getLogger(ObjItemMapper.class);
    private HashMap<String, ObjItemMapping> byObjMapping = new HashMap<String, ObjItemMapping>();
    private HashMap<String, List<ObjItemMapping>> byItemMapping = new HashMap<String, List<ObjItemMapping>>();

    public void mapObj(ObjItemMapping mapping) {
        byObjMapping.put(mapping.objId, mapping);
        if (mapping.item != null) {
            List<ObjItemMapping> list = byItemMapping.get(mapping.item);
            if (list == null) {
                list = new ArrayList<ObjItemMapping>();
                byItemMapping.put(mapping.item, list);
            }
            list.add(mapping);
        }
    }

    public @Nullable List<ObjItemMapping> getByItem(@NonNull String item) {
        return byItemMapping.get(item);
    }

    public @Nullable ObjItemMapping getByObject(@NonNull String object) {
        return byObjMapping.get(object);
    }

    public void logKeysByItem() {
        if (logger.isTraceEnabled()) {
            for (String key : byItemMapping.keySet()) {
                logger.trace("Mapping key {}", key);
            }
        }
    }

    public void logKeysByObj() {
        if (logger.isTraceEnabled()) {
            for (String key : byObjMapping.keySet()) {
                logger.trace("Mapping key {}", key);
            }
        }
    }
}
