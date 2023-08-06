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

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.CommandType;
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
import org.openhab.core.library.items.SwitchItem;
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
public class OpenHASPPlate implements EventSubscriber, OpenHASPCallbackProcessor {
    final Logger logger = LoggerFactory.getLogger(OpenHASPPlate.class);
    private final static Gson gson = new Gson();
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
                    layoutManager.logKeysByObj();
                } else {
                    ObjectEvent objectEvent = gson.fromJson(value, ObjectEvent.class);
                    logger.trace("[Plate {}] Event type: {}, value: {}, ObjectEvent: ", plateId, objectItemMapping,
                            value, objectEvent);

                    @Nullable
                    Item item = itemRegistry.get(objectItemMapping.item);
                    if (item != null) {
                        switch (objectItemMapping.type) {
                            case SWITCH:
                                if (objectEvent.event != null && (objectEvent.event.contains("up")
                                        || objectEvent.event.contains("release"))) {
                                    logger.trace("[Plate {}] BUTTON PRESS - topic {}:{}, event: {}", plateId,
                                            strippedTopic, value, objectEvent);

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
                                        logger.warn("Item {} for object {} was not type switch {}", item, strippedTopic,
                                                item);
                                    }
                                }
                                break;
                            case SLIDER:
                                if (objectEvent.event != null && (objectEvent.event.contains("up")
                                        || objectEvent.event.contains("changed"))) {
                                    logger.trace("[Plate {}] SLIDER CHANGE - topic {}:{}, event: {}", plateId,
                                            strippedTopic, value, objectEvent);

                                    String percentValue = objectEvent.val;
                                    if (percentValue != null) {
                                        if (item instanceof DimmerItem) {
                                            DimmerItem dItem = (DimmerItem) item;
                                            @Nullable
                                            PercentType command = new PercentType(percentValue);
                                            dItem.send(command);
                                        } else {
                                            logger.warn("Item {} for object {} was not type dimmer {}", item,
                                                    strippedTopic, item);
                                        }
                                    } else {
                                        logger.warn(
                                                "[Plate {}, Object {}] Event value is null. Event: {}, JsonValue: {}",
                                                plateId, strippedTopic, objectEvent, value);
                                    }
                                }
                                break;
                        }
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
        if (event instanceof ItemStateChangedEvent) {
            ItemStateChangedEvent changedEvent = (ItemStateChangedEvent) event;
            logger.warn(
                    "ITEM STATE CHANGE EVENT {}, topic: {}, source: {} , itemName: {}, itemState: {}, payload: {}, event: {}",
                    changedEvent.getType(), changedEvent.getTopic(), changedEvent.getSource(),
                    changedEvent.getItemName(), changedEvent.getItemState(), changedEvent.getPayload(), changedEvent);

            List<ObjItemMapping> mappingList = layoutManager.getByItem(((ItemEvent) event).getItemName());
            if (mappingList != null) {
                for (ObjItemMapping objectItemMapping : mappingList) {
                    @Nullable
                    Item item = itemRegistry.get(objectItemMapping.item);
                    if (item != null) {
                        switch (objectItemMapping.type) {
                            case SLIDER:
                                if (item instanceof DimmerItem) {
                                    DimmerItem dItem = (DimmerItem) item;
                                    PercentType percent = dItem.getStateAs(PercentType.class);

                                    logger.trace("SEND percent update {} to {}", percent, objectItemMapping.objId);
                                    comm.sendHASPCommand(CommandType.JSON,
                                            new String[] { objectItemMapping.objId + ".val=" + percent });
                                } else {
                                    logger.warn("Item {} for object {} was not type switch", item,
                                            objectItemMapping.objId);
                                }
                                // break; //Don't break, to slso update the switch part
                            case SWITCH:
                                if (item instanceof SwitchItem) {
                                    SwitchItem sItem = (SwitchItem) item;
                                    OnOffType sState = sItem.getStateAs(OnOffType.class);
                                    logger.trace("Item {} current state {}", item, sState);
                                    if (OnOffType.ON.equals(sState)) {
                                        logger.trace("SEND text update ON {}", objectItemMapping.objId);
                                        comm.sendHASPCommand(CommandType.JSON,
                                                new String[] { objectItemMapping.statusLabelId + ".text=On",
                                                        "p1b13.text_color=#e88c03", "p1b12.bg_color=#493416" });
                                    } else {
                                        logger.trace("SEND text update OFF {}", objectItemMapping.objId);
                                        comm.sendHASPCommand(CommandType.CMD,
                                                new String[] { objectItemMapping.statusLabelId + ".text=Off",
                                                        "p1b13.text_color=#9b9b9b", "p1b12.bg_color=#9b9b9b" });
                                    }
                                } else {
                                    logger.warn("Item {} for object {} was not type switch", item,
                                            objectItemMapping.objId);
                                }
                                break;
                        }
                    } else {
                        logger.warn("Item for object {} not found, mapping was {}", objectItemMapping.objId,
                                objectItemMapping);
                    }
                }
            }
        } else if (event instanceof ItemStateEvent) {
            ItemStateEvent stateEvent = (ItemStateEvent) event;
            logger.warn(
                    "ITEM STATE EVENT {}, topic: {}, source: {} , itemName: {}, itemState: {}, payload: {}, event:{}",
                    stateEvent.getType(), stateEvent.getTopic(), stateEvent.getSource(), stateEvent.getItemName(),
                    stateEvent.getItemState(), stateEvent.getPayload(), stateEvent);

        } else {
            logger.warn("EVENT {}, topic: {}, source: {}, payload: {}, event: {}", event.getType(), event.getTopic(),
                    event.getSource(), event.getPayload(), event);
        }
    }
}
