/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.binding.openhasp.internal;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.CommandType;
import org.openhab.binding.openhasp.internal.layout.OpenHASPLayout;
import org.openhab.binding.openhasp.internal.layout.OpenHASPLayoutManager;
import org.openhab.binding.openhasp.internal.mapping.ObjItemMapping;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEvent;
import org.openhab.core.items.events.ItemStateChangedEvent;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.model.sitemap.SitemapProvider;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link OpenHASPThingHandler} is responsible for handling commands, which
 * are
 * sent to one of the channels.
 *
 * @author Alvaro Lobato - Initial contribution
 */
@NonNullByDefault
public class OpenHASPPlate implements EventSubscriber, OpenHASPCallbackProcessor, OpenHASPPlateListener {
    final Logger logger = LoggerFactory.getLogger(OpenHASPPlate.class);
    private final static Gson gson = new Gson();
    private String timeControl;
    private String thingId;
    private String plateId = "";
    private OpenHASPCommunicationManager comm;
    private OpenHASPLayoutManager layoutManager;
    private OpenHASPThingConfiguration config;
    private List<SitemapProvider> sitemapProviders;
    private ItemRegistry itemRegistry;

    public OpenHASPPlate(String thingId, String plateId, OpenHASPCommunicationManager comm,
            OpenHASPLayoutManager layoutManager, OpenHASPThingConfiguration config,
            List<SitemapProvider> sitemapProviders, ItemRegistry itemRegistry) {
        this.thingId = thingId;
        this.plateId = plateId;
        this.comm = comm;
        this.layoutManager = layoutManager;
        this.sitemapProviders = sitemapProviders;
        this.itemRegistry = itemRegistry;
        this.config = config;
        eventFilter = event -> {
            return event instanceof ItemEvent ? layoutManager.getByItem(((ItemEvent) event).getItemName()) != null
                    : false;
        };
        timeControl = layoutManager.getLayout().getAsString(OpenHASPLayout.TIME_CONTROL_ID);
    }

    public void start() {
        logger.info("Initializing plate {}/{}", thingId, plateId);
        if (OpenHASPBindingConstants.OPENHASP_CONFIGMODE_MANUAL.equalsIgnoreCase(config.configMode)) {
            if (config.pages != null) {
                // TODO
                // objectArray = config.pages;
            }
        } else {
            if (config.sitemap != null) {
                // TODO ----------- Sitemap subscription
                String sitemapName = config.sitemap.trim();
                @Nullable
                Sitemap sitemap = null;
                logger.trace("Finding |{}| and |{}|", sitemapName);
                sitemap = findSitemap(sitemapName);
                if (sitemap != null) {
                    logger.trace("[Thing:{}, Plate:{}] Found sitemap: {}", thingId, plateId, sitemap);
                    layoutManager.loadFromSiteMap(sitemap);
                } else {
                    logger.warn("[Thing:{}, Plate:{}] Could not find sitemap {}", thingId, plateId, sitemapName);
                }
            }
        }
    }

    @Nullable
    private Sitemap findSitemap(String sitemapToFind) {
        @Nullable
        Sitemap sitemap = null;
        String sitemapToFindUIComponents = "uicomponents_" + sitemapToFind;
        for (SitemapProvider provider : sitemapProviders) {
            logger.trace("processing sitemap provider {}", provider);
            sitemap = provider.getSitemap(sitemapToFind);
            if (sitemap != null)
                break;

            sitemap = provider.getSitemap(sitemapToFindUIComponents);
            if (sitemap != null)
                break;
        }
        return sitemap;
    }

    public void refresh() { // Called when thing comes back online
        // TODO - Maybe use at the start too?
        // TODO - maybe read the sitemap here tooo
        layoutManager.sendPages();
        updateAllStates();
        comm.sendHASPCommand(CommandType.CMD, "idle"); // force a check on idle
    }

    // ---------------------------------------------
    // Callback Processor - Receive from the plate and send to the item
    // ---------------------------------------------
    public void plateCallback(String strippedTopic, String value) {
        // State
        if (strippedTopic.startsWith(OpenHASPBindingConstants.HASP_STATE_TOPIC)) {
            strippedTopic = strippedTopic.substring(OpenHASPBindingConstants.HASP_STATE_TOPIC.length() + 1);
            logger.trace("Plate {} state {}:{}", plateId, strippedTopic, value);
            if (OpenHASPBindingConstants.HASP_STATE_IDLE_TOPIC.equals(strippedTopic)) {
                if (OpenHASPBindingConstants.HASP_STATE_IDLE_OFF_VALUE.equals(value)) {
                    // sendCommand("backlight {\"state\":\"ON\",\"brightness\":" +
                    // config.backlightHigh + "}");
                } else if (OpenHASPBindingConstants.HASP_STATE_IDLE_SHORT_VALUE.equals(value)) {
                    // sendCommand("backlight {\"state\":\"ON\",\"brightness\":" +
                    // config.backlightMedium + "}");
                } else if (OpenHASPBindingConstants.HASP_STATE_IDLE_LONG_VALUE.equals(value)) {
                    // sendCommand("backlight {\"state\":\"ON\",\"brightness\":" +
                    // config.backlightLow + "}");
                }
            } else if (strippedTopic.matches("^[a-zA-Z]+[0-9]+[a-zA-Z]+[0-9]+$")) { // match with p0b12 for object
                                                                                    // events
                // lookup the buttom to item mapping for a press
                logger.trace("EVENT from Plate {} / topic {}: {} ", plateId, strippedTopic, value);
                ObjItemMapping objectItemMapping = layoutManager.getByObject(strippedTopic);
                if (objectItemMapping == null) {
                    logger.info("Could not find item mapping for object {} on plate {}", strippedTopic, plateId);
                    // layoutManager.logKeysByObj();
                } else {
                    ObjectEvent objectEvent = gson.fromJson(value, ObjectEvent.class);
                    logger.trace("[Plate {}] Event type: {}, value: {}, ObjectEvent: ", plateId, objectItemMapping,
                            value, objectEvent);

                    @Nullable
                    Item item = itemRegistry.get(objectItemMapping.item);
                    if (item != null) {
                        // TODO come back to widget type

                        // switch (objectItemMapping.type) {
                        // case SWITCH:
                        if (objectEvent.event != null
                                && (objectEvent.event.contains("changed") && value.contains("text"))) { // Dropdown
                            logger.trace("[Plate {}] DROPDOWN SELECT - topic {}:{}, event: {}", plateId, strippedTopic,
                                    value, objectEvent);
                            logger.warn("Item {} item class {}", item, item.getClass().getSimpleName());
                            String strPos = objectEvent.val;
                            if (strPos != null) {
                                if (item instanceof NumberItem) {
                                    try {
                                        int pos = Integer.parseInt(strPos);
                                        NumberItem dItem = (NumberItem) item;

                                        if (objectItemMapping.positionValues != null) {
                                            if (objectItemMapping.positionValues.length < pos) {
                                                logger.error(
                                                        "Item {} - {} for object {} could not find position {} available {}",
                                                        item, item.getClass().getSimpleName(), strippedTopic, pos,
                                                        objectItemMapping.positionValues);
                                            } else {
                                                @Nullable
                                                DecimalType command = new DecimalType(
                                                        objectItemMapping.positionValues[pos]);
                                                dItem.send(command);
                                            }
                                        } else {
                                            @Nullable
                                            DecimalType command = new DecimalType(pos);
                                            dItem.send(command);
                                        }
                                    } catch (NumberFormatException e) {
                                        logger.warn("Item {} - {} for object {} position value was not numeric", item,
                                                item.getClass().getSimpleName(), strippedTopic, strPos);
                                    }
                                } else {
                                    logger.warn("Item {} - {} for object {} was not type NumberItem {}", item,
                                            item.getClass().getSimpleName(), strippedTopic, item);
                                }
                            } else {
                                logger.warn("[Plate {}, Object {}] Event value is null. Event: {}, JsonValue: {}",
                                        plateId, strippedTopic, objectEvent, value);
                            }
                        }
                        if (objectEvent.event != null && (objectEvent.event.contains("up") && !value.contains("val")
                                && !value.contains("text"))) { // Click
                            // button
                            logger.trace("[Plate {}] BUTTON PRESS - topic {}:{}, event: {}", plateId, strippedTopic,
                                    value, objectEvent);

                            // TODO StringItem
                            if (item instanceof SwitchItem) {
                                SwitchItem sItem = (SwitchItem) item;
                                OnOffType sState = sItem.getStateAs(OnOffType.class);
                                logger.trace("Current state {}", sState);
                                if (OnOffType.ON.equals(sState)) {
                                    logger.trace("SEND OFF COMMAND {}", item);
                                    sItem.send(OnOffType.OFF);
                                } else {
                                    logger.trace("SEND ON COMMAND {}", item);
                                    sItem.send(OnOffType.ON);
                                }
                            } else {
                                logger.warn("Item {} - {} for object {} was not type switch {}", item,
                                        item.getClass().getSimpleName(), strippedTopic, item);
                            }
                        }
                        // break;
                        // case SLIDER:
                        if (objectEvent.event != null && (objectEvent.event.contains("up") && value.contains("val")
                                && !value.contains("text"))) { // Slider
                            // item
                            logger.trace("[Plate {}] SLIDER CHANGE - topic {}:{}, event: {}", plateId, strippedTopic,
                                    value, objectEvent);

                            String percentValue = objectEvent.val;
                            if (percentValue != null) {
                                if (item instanceof DimmerItem) {
                                    DimmerItem dItem = (DimmerItem) item;
                                    @Nullable
                                    PercentType command = new PercentType(percentValue);
                                    dItem.send(command);
                                } else {
                                    logger.warn("Item {} - {} for object {} was not type dimmer {}", item,
                                            item.getClass().getSimpleName(), strippedTopic, item);
                                }
                            } else {
                                logger.warn("[Plate {}, Object {}] Event value is null. Event: {}, JsonValue: {}",
                                        plateId, strippedTopic, objectEvent, value);
                            }
                        }
                        // break;
                        // }
                    } else {
                        logger.warn("Item for object {} not found, mapping was {}", strippedTopic, objectItemMapping);
                    }
                }
            }
            // if ("p1b11".equals(strippedTopic) && value.contains("up")) {
            // logger.trace("BUTTON PRESS - Plate {} topic {}:{}", plateId, topic, value);
            // for (Channel channel : thing.getChannels()) {

            // logger.trace("channel: {}", channel.getUID());

            // // if (channel.getUID().toString().contains("P0button1")) {
            // if (channel.getUID().toString().contains("p1b11")
            // || channel.getUID().toString().contains("haspbutton")) {

            // triggerChannel(channel.getUID(), CommonTriggerEvents.PRESSED);

            // // triggerChannel(SerialButtonBindingConstants.TRIGGER_CHANNEL,
            // CommonTriggerEvents.PRESSED);
            // logger.trace("Sending PRESSED to channel: {}", channel.getUID());
            // /*
            // * if (testValue) {
            // *
            // * postChannelCommand(channel.getUID(), OnOffType.ON);
            // * logger.trace("Sending ON to channel: {}", channel.getUID());
            // * } else {
            // * postChannelCommand(channel.getUID(), OnOffType.OFF);
            // * logger.trace("Sending OFF to channel: {}", channel.getUID());
            // *
            // * }
            // */
            // testValue = !testValue;

            // }
            // }
            // }
        } else {
            logger.trace("Plate {} topic {}:{}", plateId, strippedTopic, value);
        }
    }

    // ---------------------------------------------
    // Event listener -> receive from the Item and send to the plate
    // ---------------------------------------------
    private static final Set<String> SUBSCRIBED_EVENT_TYPES = Set.of(ItemStateEvent.TYPE, ItemStateChangedEvent.TYPE);
    private @Nullable EventFilter eventFilter;
    private static final DateTimeFormatter timePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public Set<String> getSubscribedEventTypes() {
        return SUBSCRIBED_EVENT_TYPES;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return eventFilter;
    }

    @Override
    public void receive(Event event) {

        /* Method sed to process the events coming from OpenHab in order to update the plate controls */

        if (event instanceof ItemStateEvent) { // Called on ItemStateEvent even if the state didn't change
            ItemStateEvent stateEvent = (ItemStateEvent) event;
            // No need to process this, we already do it on ItemStateChangedEvent
            // logger.trace(
            // "ITEM STATE EVENT {}, topic: {}, source: {} , itemName: {}, itemState: {}, payload: {}, event:{}",
            // stateEvent.getType(), stateEvent.getTopic(), stateEvent.getSource(), stateEvent.getItemName(),
            // stateEvent.getItemState(), stateEvent.getPayload(), stateEvent);

        } else if (event instanceof ItemStateChangedEvent) { // Called when an Item state changes

            ItemStateChangedEvent changedEvent = (ItemStateChangedEvent) event;
            logger.trace(
                    "ITEM STATE CHANGE EVENT {}, topic: {}, source: {} , itemName: {}, itemState: {}, payload: {}, event:{}",
                    changedEvent.getType(), changedEvent.getTopic(), changedEvent.getSource(),
                    changedEvent.getItemName(), changedEvent.getItemState(), changedEvent.getPayload(), changedEvent);

            processItemStateEvent(event);
        } else {
            logger.trace("UNKNOWN EVENT {}, topic: {}, source: {}, payload: {}, event: {}", event.getType(),
                    event.getTopic(), event.getSource(), event.getPayload(), event);
        }
    }

    private void updateAllStates() {
        HashMap<String, ObjItemMapping> allByObject = this.layoutManager.getObjItemMapper().getAllByObject();

        Set<Entry<String, ObjItemMapping>> entrySet = allByObject.entrySet();

        for (Entry<String, ObjItemMapping> entry : entrySet) {

            ObjItemMapping mapping = entry.getValue();

            @Nullable
            Item item = itemRegistry.get(mapping.item);
            if (item != null) {
                logger.trace("Updating HASP {}, item {}, itemType {}", entry.getKey(), item,
                        item.getClass().getSimpleName());
                // TODO do we need to call all of them or skip when a match happens
                if (item instanceof DimmerItem) {
                    updateFromDimmerItem(mapping, (DimmerItem) item);
                }
                if (item instanceof SwitchItem) {
                    updateFromSwitchItem(mapping, (SwitchItem) item);
                }
                if (item instanceof NumberItem) {
                    updateFromNumberItem(mapping, (NumberItem) item);
                }
            } else {
                logger.warn("Couldn't find item from mapping object {} HASP ", mapping, entry.getKey());
            }
        }
    }

    // private void updateDimmerItem()
    private void processItemStateEvent(Event event) {
        List<ObjItemMapping> mappingList = layoutManager.getByItem(((ItemEvent) event).getItemName());
        if (mappingList != null) {
            for (ObjItemMapping mapping : mappingList) {
                @Nullable
                Item item = itemRegistry.get(mapping.item);
                if (item != null) {
                    // switch (objectItemMapping.type) {
                    // case SLIDER:
                    if (mapping.sliderId != null) {
                        if (item instanceof DimmerItem) {
                            updateFromDimmerItem(mapping, (DimmerItem) item);
                        } else {
                            logger.warn("Item {} - {} for object {} was not type DimmerItem", item,
                                    item.getClass().getSimpleName(), mapping.objId);
                        }
                    } else {
                        logger.warn("Slider is null for Item {}", item);
                    }
                    // break; //Don't break, to slso update the switch part
                    // case SWITCH:
                    if (item instanceof SwitchItem) {
                        updateFromSwitchItem(mapping, (SwitchItem) item);
                    } else if (item instanceof NumberItem) {
                        updateFromNumberItem(mapping, (NumberItem) item);
                    } else {
                        logger.warn("Item {} - {} for object {} was not type Switch", item,
                                item.getClass().getSimpleName(), mapping.objId);
                    }
                    // break;
                    // }
                } else {
                    logger.warn("Item for object {} not found, mapping was {}", mapping.objId, mapping);
                }
            }
        }
    }

    private void updateFromSwitchItem(ObjItemMapping mapping, SwitchItem item) {
        OnOffType sState = item.getStateAs(OnOffType.class);
        if (sState != null) {
            logger.trace("Item {} current state {}", item, sState);
            if (OnOffType.ON.equals(sState)) {
                logger.trace("SEND text update ON to {}", mapping.statusLabelId);
                comm.sendHASPCommand(CommandType.JSON, new String[] { mapping.statusLabelId + ".text=On" });
                // TODO change color too - , "p1b13.text_color=#e88c03",
                // "p1b12.bg_color=#493416"
            } else {
                logger.trace("SEND text update OFF to {}", mapping.statusLabelId);
                comm.sendHASPCommand(CommandType.CMD, new String[] { mapping.statusLabelId + ".text=Off" });
                // TODO change color too ,"p1b13.text_color=#9b9b9b", "p1b12.bg_color=#9b9b9b"
            }
        } else {
            logger.trace("SKIPPED SEND update {} to {}", sState, mapping.statusLabelId);
        }
    }

    private void updateFromDimmerItem(ObjItemMapping mapping, DimmerItem item) {
        PercentType percent = item.getStateAs(PercentType.class);
        if (percent != null) {
            logger.trace("SEND percent update {} to {}", percent, mapping.sliderId);
            comm.sendHASPCommand(CommandType.JSON, new String[] { mapping.sliderId + ".val=" + percent });
        } else {
            logger.trace("SKIPPED SEND percent update {} to {}", percent, mapping.sliderId);
        }
    }

    private void updateFromNumberItem(ObjItemMapping mapping, NumberItem item) {
        switch (mapping.type) {
            case SELECT:
                DecimalType decimal = item.getStateAs(DecimalType.class);
                if (decimal != null) {
                    int position = mapping.findValuePosition(decimal.toString());

                    logger.trace("SEND decimal update value {} position {} to {}", decimal, position, mapping.sliderId);
                    comm.sendHASPCommand(CommandType.JSON, new String[] { mapping.sliderId + ".val=" + position });
                } else {
                    logger.trace("SKIPPED SEND decimal update value {} position {} to {}", decimal, mapping.sliderId);
                }
                break;
            case SLIDER:
                logger.warn("Slider update not implemented for NumberItem. {}", item);
                break;
            case SWITCH:
                logger.warn("Switch update not implemented for NumberItem. {}", item);
                break;
            default:
                logger.warn("Unknown mapping type {} for NumberItem. {}", mapping.type, item);
                break;
        }
    }

    public void onLine() {
        OpenHaspService.instance().registerListener(this);
    }

    public void offLine() {
        OpenHaspService.instance().unregisterListener(this);
    }

    @Override
    public void onTimerEvent() {
        // Update clock
        comm.sendHASPCommand(CommandType.JSON,
                new String[] { timeControl + ".text=" + LocalDateTime.now().format(timePattern) });
    }
}
