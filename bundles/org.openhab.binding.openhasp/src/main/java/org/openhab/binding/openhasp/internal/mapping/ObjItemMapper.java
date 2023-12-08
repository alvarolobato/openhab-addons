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
        if (mapping.objId != null) {
            byObjMapping.put(mapping.objId, mapping);
            logger.trace("Mapped objId: {}", mapping.objId);
        }
        if (mapping.sliderId != null) {
            byObjMapping.put(mapping.sliderId, mapping);
            logger.trace("Mapped sliderId: {}", mapping.sliderId);
        }

        List<ObjItemMapping> list = byItemMapping.get(mapping.item);
        if (list == null) {
            list = new ArrayList<ObjItemMapping>();
            byItemMapping.put(mapping.item, list);
        }
        list.add(mapping);
    }

    public @Nullable List<ObjItemMapping> getByItem(@NonNull String item) {
        return byItemMapping.get(item);
    }

    public @Nullable ObjItemMapping getByObject(@NonNull String object) {
        ObjItemMapping res = byObjMapping.get(object);
        if (res == null) {
            logger.trace("Couldn't find object {}", object);
            logKeysByObj();
        }
        return res;
    }

    public HashMap<String, ObjItemMapping> getAllByObject() {
        return byObjMapping;
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
