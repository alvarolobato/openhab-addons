package org.openhab.binding.openhasp.internal.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjItemMapper {

    final Logger logger = LoggerFactory.getLogger(ObjItemMapper.class);
    private HashMap<String, IObjItemMapping> byObjMapping = new HashMap<String, IObjItemMapping>();
    private HashMap<String, List<IObjItemMapping>> byItemMapping = new HashMap<String, List<IObjItemMapping>>();

    public void mapObj(IObjItemMapping mapping) {

        for (String id : mapping.getIds()) {
            byObjMapping.put(id, mapping);
            logger.trace("Mapped objId: {}", id);
        }

        Item item = mapping.getItem();
        if (item != null) {
            List<IObjItemMapping> list = byItemMapping.get(item.getName());
            if (list == null) {
                list = new ArrayList<IObjItemMapping>();
                byItemMapping.put(item.getName(), list);
            }
            list.add(mapping);
        }
    }

    public @Nullable List<IObjItemMapping> getByItem(@NonNull String item) {
        return byItemMapping.get(item);
    }

    public @Nullable IObjItemMapping getByObject(@NonNull String object) {
        IObjItemMapping res = byObjMapping.get(object);
        if (res == null) {
            logger.trace("Couldn't find object {}", object);
            logKeysByObj();
        }
        return res;
    }

    public HashMap<String, IObjItemMapping> getAllByObject() {
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
