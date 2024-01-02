package org.openhab.binding.openhasp.internal.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.ObjectEvent;
import org.openhab.core.items.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ObjItemMapping implements IObjItemMapping {

    public enum ControlType {
        SWITCH,
        SLIDER,
        SELECT
    }

    private Item item;
    private ObjItemMapping.ControlType type; // TODO remove, not used
    private @Nullable String objId;
    private @Nullable String statusLabelId;
    private @Nullable String sliderId;

    public String @Nullable [] positionValues;

    public String @Nullable [] getPositionValues() {
        return positionValues;
    }

    private static final Logger logger = LoggerFactory.getLogger(ObjItemMapping.class);

    public ObjItemMapping(ObjItemMapping.ControlType type, Item item, @Nullable String objId,
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

    @Override
    public List<String> getIds() {
        ArrayList<String> ids = new ArrayList<String>();
        String id = objId;
        if (id != null) {
            ids.add(id);
        }
        id = sliderId;
        if (id != null) {
            ids.add(id);
        }
        return ids;
    }

    @Override
    @NonNull
    public Item getItem() {
        return item;
    }

    @Override
    public void haspEventReceived(ObjectEvent objectEvent) {
        // @Nullable
        // Item item = itemRegistry.get(objectItemMapping.getItem());
        // if (item != null) {
        // // TODO come back to widget type

        // // switch (objectItemMapping.type) {
        // // case SWITCH:
        // if (objectEvent.event != null
        // && (objectEvent.event.contains("changed") && value.contains("text"))) { //
        // Dropdown
        // logger.trace("[Plate {}] DROPDOWN SELECT - topic {}:{}, event: {}", plateId,
        // strippedTopic,
        // value, objectEvent);
        // logger.warn("Item {} item class {}", item, item.getClass().getSimpleName());
        // String strPos = objectEvent.val;
        // if (strPos != null) {
        // if (item instanceof NumberItem) {
        // try {
        // int pos = Integer.parseInt(strPos);
        // NumberItem dItem = (NumberItem) item;

        // String[] positionValues = objectItemMapping.getPositionValues();
        // if (positionValues != null) {
        // if (positionValues.length < pos) {
        // logger.error(
        // "Item {} - {} for object {} could not find position {} available {}",
        // item, item.getClass().getSimpleName(), strippedTopic, pos,
        // positionValues);
        // } else {
        // @Nullable
        // DecimalType command = new DecimalType(positionValues[pos]);
        // dItem.send(command);
        // }
        // } else {
        // @Nullable
        // DecimalType command = new DecimalType(pos);
        // dItem.send(command);
        // }
        // } catch (NumberFormatException e) {
        // logger.warn("Item {} - {} for object {} position value was not numeric",
        // item,
        // item.getClass().getSimpleName(), strippedTopic, strPos);
        // }
        // } else {
        // logger.warn("Item {} - {} for object {} was not type NumberItem {}", item,
        // item.getClass().getSimpleName(), strippedTopic, item);
        // }
        // } else {
        // logger.warn("[Plate {}, Object {}] Event value is null. Event: {}, JsonValue:
        // {}",
        // plateId, strippedTopic, objectEvent, value);
        // }
        // }
        // if (objectEvent.event != null && (objectEvent.event.contains("up") &&
        // !value.contains("val")
        // && !value.contains("text"))) { // Click
        // // button
        // logger.trace("[Plate {}] BUTTON PRESS - topic {}:{}, event: {}", plateId,
        // strippedTopic,
        // value, objectEvent);

        // // TODO StringItem
        // if (item instanceof SwitchItem) {
        // SwitchItem sItem = (SwitchItem) item;
        // OnOffType sState = sItem.getStateAs(OnOffType.class);
        // logger.trace("Current state {}", sState);
        // if (OnOffType.ON.equals(sState)) {
        // logger.trace("SEND OFF COMMAND {}", item);
        // sItem.send(OnOffType.OFF);
        // } else {
        // logger.trace("SEND ON COMMAND {}", item);
        // sItem.send(OnOffType.ON);
        // }
        // } else {
        // logger.warn("Item {} - {} for object {} was not type switch {}", item,
        // item.getClass().getSimpleName(), strippedTopic, item);
        // }
        // }
        // // break;
        // // case SLIDER:
        // if (objectEvent.event != null && (objectEvent.event.contains("up") &&
        // value.contains("val")
        // && !value.contains("text"))) { // Slider
        // // item
        // logger.trace("[Plate {}] SLIDER CHANGE - topic {}:{}, event: {}", plateId,
        // strippedTopic,
        // value, objectEvent);

        // String percentValue = objectEvent.val;
        // if (percentValue != null) {
        // if (item instanceof DimmerItem) {
        // DimmerItem dItem = (DimmerItem) item;
        // @Nullable
        // PercentType command = new PercentType(percentValue);
        // dItem.send(command);
        // } else {
        // logger.warn("Item {} - {} for object {} was not type dimmer {}", item,
        // item.getClass().getSimpleName(), strippedTopic, item);
        // }
        // } else {
        // logger.warn("[Plate {}, Object {}] Event value is null. Event: {}, JsonValue:
        // {}",
        // plateId, strippedTopic, objectEvent, value);
        // }
        // }
        // // break;
        // // }
        // } else {
        // logger.warn("Item for object {} not found, mapping was {}", strippedTopic,
        // objectItemMapping);
        // }
    }

    @Override
    public void updateState() {
        logger.error("Called updateState on old ObjectItemMapping: {}", this);
    }

    // @Override
    // @Nullable
    // public String getStatusLabelId() {
    // return statusLabelId;
    // }
}
