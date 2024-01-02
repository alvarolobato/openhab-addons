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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.CommandType;
import org.openhab.binding.openhasp.internal.layout.OpenHASPLayout;
import org.openhab.binding.openhasp.internal.layout.OpenHASPLayoutManager;
import org.openhab.binding.openhasp.internal.mapping.IObjItemMapping;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEvent;
import org.openhab.core.items.events.ItemStateChangedEvent;
import org.openhab.core.items.events.ItemStateEvent;
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
        layoutManager.sendPages();
        layoutManager.updateAllStates();
        layoutManager.sendPendingCommands();
        // comm.sendHASPCommand(CommandType.CMD, "idle"); // force a check on idle
    }

    /**
     * Callback Processor - Receive from the plate and send to the item
     **/
    public void plateCallback(String strippedTopic, String value) {
        // State
        if (strippedTopic.startsWith(OpenHASPBindingConstants.HASP_STATE_TOPIC)) {
            strippedTopic = strippedTopic.substring(OpenHASPBindingConstants.HASP_STATE_TOPIC.length() + 1);
            logger.trace("Plate {} state {}:{}", plateId, strippedTopic, value);
            // if (OpenHASPBindingConstants.HASP_STATE_IDLE_TOPIC.equals(strippedTopic)) {
            // if (OpenHASPBindingConstants.HASP_STATE_IDLE_OFF_VALUE.equals(value)) {
            // // sendCommand("backlight {\"state\":\"ON\",\"brightness\":" +
            // // config.backlightHigh + "}");
            // } else if (OpenHASPBindingConstants.HASP_STATE_IDLE_SHORT_VALUE.equals(value)) {
            // // sendCommand("backlight {\"state\":\"ON\",\"brightness\":" +
            // // config.backlightMedium + "}");
            // } else if (OpenHASPBindingConstants.HASP_STATE_IDLE_LONG_VALUE.equals(value)) {
            // // sendCommand("backlight {\"state\":\"ON\",\"brightness\":" +
            // // config.backlightLow + "}");
            // }
            // } else
            // match with p0b12 for object events
            if (strippedTopic.matches("^[a-zA-Z]+[0-9]+[a-zA-Z]+[0-9]+$")) {
                // lookup the buttom to item mapping for a press
                logger.trace("EVENT from Plate {} / topic {}: {} ", plateId, strippedTopic, value);
                IObjItemMapping objectItemMapping = layoutManager.getByObject(strippedTopic);
                if (objectItemMapping == null) {
                    logger.info("Could not find item mapping for object {} on plate {}", strippedTopic, plateId);
                    layoutManager.logKeysByObj();
                } else {
                    ObjectEvent objectEvent = gson.fromJson(value, ObjectEvent.class);
                    objectEvent.source = strippedTopic;
                    if (objectEvent != null) {
                        logger.trace("[Plate {}] topic {}: Event type: {}, value: {}, ObjectEvent: {}", plateId,
                                strippedTopic, value, objectItemMapping, value, objectEvent);
                        // TODO Identify the plate that sent the event
                        objectItemMapping.haspEventReceived(objectEvent);
                    } else {
                        logger.trace("[Plate {}] Error processing event value: {}", value);
                    }
                }
            }
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
            // ItemStateEvent stateEvent = (ItemStateEvent) event;
            // No need to process this, we already do it on ItemStateChangedEvent
        } else if (event instanceof ItemStateChangedEvent) { // Called when an Item state changes
            ItemStateChangedEvent changedEvent = (ItemStateChangedEvent) event;
            logger.trace(
                    "ITEM STATE CHANGE EVENT {}, topic: {}, source: {} , itemName: {}, itemState: {}, payload: {}, event:{}",
                    changedEvent.getType(), changedEvent.getTopic(), changedEvent.getSource(),
                    changedEvent.getItemName(), changedEvent.getItemState(), changedEvent.getPayload(), changedEvent);
            layoutManager.processItemStateEvent(event);
            layoutManager.sendPendingCommands();
        } else {
            logger.trace("UNKNOWN EVENT {}, topic: {}, source: {}, payload: {}, event: {}", event.getType(),
                    event.getTopic(), event.getSource(), event.getPayload(), event);
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
                Arrays.asList(new String[] { timeControl + ".text=" + LocalDateTime.now().format(timePattern) }));
    }
}
