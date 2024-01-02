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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mqtt.generic.AbstractMQTTThingHandler;
import org.openhab.binding.mqtt.generic.ChannelState;
import org.openhab.binding.mqtt.generic.MqttChannelTypeProvider;
import org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.CommandType;
import org.openhab.binding.openhasp.internal.layout.OpenHASPLayoutManager;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.io.transport.mqtt.MqttConnectionObserver;
import org.openhab.core.io.transport.mqtt.MqttConnectionState;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.model.sitemap.SitemapProvider;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
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
public class OpenHASPThingHandler extends AbstractMQTTThingHandler implements MqttConnectionObserver {
    final Logger logger = LoggerFactory.getLogger(OpenHASPThingHandler.class);

    boolean testValue = true;

    // private @Nullable MqttBrokerConnection connection;
    // private ThingRegistry thingRegistry;
    private ItemRegistry itemRegistry;
    // private @Nullable OpenHASPThingConfiguration config;
    private String plateId = "";
    private String thingId;

    private @Nullable ServiceRegistration<?> eventSubscriberRegistration;
    private List<SitemapProvider> sitemapProviders;

    private @Nullable OpenHASPPlate plate;
    private @Nullable OpenHASPCommunicationManager comm;
    private @Nullable OpenHASPThingConfiguration config;

    private BundleContext bundleContext;

    public OpenHASPThingHandler(Thing thing, ThingRegistry thingRegistry, BundleContext bundleContext,
            MqttChannelTypeProvider channelTypeProvider, ItemRegistry itemRegistry, int deviceTimeout,
            int subscribeTimeout, int attributeReceiveTimeout, List<SitemapProvider> sitemapProviders, Gson gson) {
        super(thing, deviceTimeout);
        this.bundleContext = bundleContext;

        logger.trace("Created handler for {}", thing);

        // this.thingRegistry = thingRegistry;
        this.itemRegistry = itemRegistry;
        this.sitemapProviders = sitemapProviders;
        // this.gson = gson;

        thingId = getThing().getUID().getId();
    }

    // TODO Review all log message levels
    @Override
    protected CompletableFuture<@Nullable Void> start(MqttBrokerConnection connection) {
        logger.debug("Start handler {}:{}", thingId, plateId);
        OpenHASPThingConfiguration config = this.config;
        if (config != null) {
            // Called after it's already initialized
            // if (connection == null) {
            // logger.error("Connection was null {}", this);
            // return;
            // }
            // TODO check what happens when changes are made to configuration, we might need
            // to tear all down and recreate
            OpenHASPLayoutManager layoutManager;

            OpenHASPCommunicationManager comm = new OpenHASPCommunicationManager(plateId, connection);
            this.comm = comm;
            layoutManager = new OpenHASPLayoutManager(thingId, plateId, comm, config, itemRegistry);

            if (plate != null) {
                logger.error("############ There was already a plate object"); // TODO CHECK THIS CASE
            }

            plate = new OpenHASPPlate(thingId, plateId, comm, layoutManager, config, sitemapProviders, itemRegistry);
            comm.setCallbackProcessor(Objects.requireNonNull(plate));
            // TODO: Move to after creating the plate object
            eventSubscriberRegistration = bundleContext.registerService(EventSubscriber.class.getName(), plate, null);

            logger.debug("About to start HASP device {} current status {}", plateId, thing.getStatus());
            if (connection.getQos() != 1) {
                logger.warn(
                        "HASP devices require QoS 1 but Qos 0/2 is configured. Using override. Please check the configuration");
                connection.setQos(1);
            }

            // Check config before starting
            if (config.deviceId.isEmpty()) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device Id missing");
                return CompletableFuture.allOf();
            }

            if (OpenHASPBindingConstants.OPENHASP_CONFIGMODE_MANUAL.equalsIgnoreCase(config.configMode)
                    && config.pages == null) {
                logger.error("Could not send pages to {}, config pages is null", plateId);
                logger.error("Config:\n\tdeviceId:{}\n\tpages:{}", config.deviceId, config.pages);
                updateStatus(thing.getStatusInfo().getStatus(), ThingStatusDetail.CONFIGURATION_ERROR);
            } else {
                if (config.sitemap == null) {
                    logger.error("Could not send pages to {}, config sitemap is null or empty", plateId);
                    logger.error("Config:\n\tdeviceId:{}\n\tsitemap:{}", config.deviceId, config.sitemap);
                    updateStatus(thing.getStatusInfo().getStatus(), ThingStatusDetail.CONFIGURATION_ERROR);
                }
            }

            logger.trace("Adding availability topic {} for {}", comm.getPlateLWTTopic(), thingId);
            addAvailabilityTopic(comm.getPlateLWTTopic(), "online", "offline");

            if (plate != null) {
                plate.start();
                // Check if the broker is connected
                if (connection.connectionState().compareTo(MqttConnectionState.CONNECTED) == 0) {
                    // If it was already online we leave it, it could come from a config save and
                    // resend pages to refresh config
                    if (ThingStatus.ONLINE.equals(thing.getStatus())) {
                        plate.refresh();
                    } else { // In the general case wait for it to come online with the availability topic
                        updateStatus(ThingStatus.OFFLINE);
                    }
                    logger.trace("OpenHASP plate {}/{} {}", thingId, plateId, thing.getStatus());

                    logger.trace("Subscribing to topic {} for plate {}", comm.getPlateStateTopic(), plateId);
                    connection.subscribe(comm.getPlateStateTopic(), comm);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                    logger.trace("OpenHASP plate {}/{} OFFLINE bridge is disconnected", thingId, plateId);
                }
                logger.trace("OpenHASP start {}/{} DONE", thingId, plateId);
            } else {
                logger.error("HASP device {} could not be initialized plate object is NULL, status {}", plateId,
                        thing.getStatus());
            }
            return super.start(connection);
        } else {
            logger.error("Config for thing {} is NULL", thingId);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            return CompletableFuture.allOf();
        }
    }

    @Override
    public void initialize() { // Called after start
        logger.debug("Initializing plate configuration {}", plateId);
        loadConfiguration();
        super.initialize();
    }

    private void loadConfiguration() {
        logger.debug("Loading configuration for {}", thingId);
        logger.trace("Clearing availability topics for {}", thingId);
        clearAllAvailabilityTopics();

        OpenHASPThingConfiguration config = getConfigAs(OpenHASPThingConfiguration.class);
        this.config = config;
        plateId = config.deviceId;

        logger.debug("Configuration loaded for {}", thingId);
    }

    @Override
    public void connectionStateChanged(MqttConnectionState state, @Nullable Throwable error) {
        logger.info("MQTT brokers state changed to:{}", state);
        switch (state) {
            case CONNECTED:
                // updateStatus(ThingStatus.ONLINE);
                break;
            case CONNECTING:
            case DISCONNECTED:
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Bridge (broker) is not connected to your MQTT broker.");
        }
    }

    @Override
    public @Nullable ChannelState getChannelState(ChannelUID channelUID) {
        // TODO Auto-generated method stub
        logger.error("GET CHAMNEL STATE {} {}", plateId, channelUID);
        return null;
    }

    @Override
    public CompletableFuture<Void> unsubscribeAll() {
        if (comm != null) {
            return comm.unsubscribeAll();
        } else {
            return CompletableFuture.allOf();
        }
    }

    @Override
    protected void updateThingStatus(boolean messageReceived, Optional<Boolean> availabilityTopicsSeen) {
        logger.trace("Update plate {} status, messageReceived {}, availabilityTopicsSeen {}", plateId, messageReceived,
                availabilityTopicsSeen);
        if (availabilityTopicsSeen.orElse(messageReceived)) {
            if (plate != null) {
                logger.trace("Plate {} came online, sending pages", plateId);
                plate.refresh();
            } else {
                logger.warn("Plate {} came online, but can't send pages plate object is NULL", plateId);
            }
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
            plate.onLine();
            logger.trace("OpenHASP plate {}/{} ONLINE", thingId, plateId);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE);
            plate.offLine();
            logger.trace("OpenHASP plate {}/{} OFFLINE", thingId, plateId);
        }
    }

    @Override
    public void dispose() {
        if (eventSubscriberRegistration != null) {
            eventSubscriberRegistration.unregister(); // TODO Maybe this is not the best place, when thing is offline
                                                      // and we
                                                      // recrate might be better
        }
        super.dispose();
    }

    // TODO - Review this method
    private void updateConfigurationParameter(String parameter, Object value) {
        Configuration configMap = getConfig();
        configMap.put(parameter, value);
        updateConfiguration(configMap);
        config = configMap.as(OpenHASPThingConfiguration.class);
    }

    // TODO - Review this method
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("Plate {} channel {} received Command {} ({})", plateId, channelUID, command, command.getClass());
        if (OpenHASPBindingConstants.CHANNEL_BACKLIGHT.equals(channelUID.getId())) {
            if (command instanceof PercentType) {
                updateConfigurationParameter("backlightHigh", ((PercentType) command).intValue());
                comm.sendHASPCommand(CommandType.JSON,
                        "backlight {\"state\":\"ON\",\"brightness\":" + config.backlightHigh + "}");
            }
        }
    }
}
