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
package org.openhab.binding.openhasp.internal.discovery;

import static org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.DEVICE_ID_FIELD;
import static org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.HASP_DISCOVERY_TOPIC;
import static org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.SUPPORTED_THING_TYPES;
import static org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.THING_TYPE_HASP_PLATE;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mqtt.discovery.AbstractMQTTDiscovery;
import org.openhab.binding.mqtt.discovery.MQTTTopicDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link OpenHASPDiscoveryService}
 *
 * @author Alvaro Lobato - Initial contribution
 */
@Component(service = DiscoveryService.class, configurationPid = "discovery.openhasp")
@NonNullByDefault
public class OpenHASPDiscoveryService extends AbstractMQTTDiscovery {

    private final Logger logger = LoggerFactory.getLogger(OpenHASPDiscoveryService.class);

    protected final MQTTTopicDiscoveryService discoveryService;

    @Activate
    public OpenHASPDiscoveryService(@Reference MQTTTopicDiscoveryService discoveryService) {
        super(SUPPORTED_THING_TYPES, 3, true, HASP_DISCOVERY_TOPIC + "#");
        this.discoveryService = discoveryService;
    }

    @Override
    public void receivedMessage(ThingUID connectionBridge, MqttBrokerConnection connection, String topic,
            byte[] payload) {
        // Check if this is the discovery topic of the device
        if (!topic.contains("/discovery/")) {
            return;
        }

        logger.trace("Received topic: {}", topic);

        JsonObject payloadJson = JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonElement node = payloadJson.get("node");

        if (node != null) {
            logger.info("Found HASP Plate: {}", node);
            String nodeName = node.getAsString();
            Map<String, Object> properties = new HashMap<>();
            properties.put(DEVICE_ID_FIELD, nodeName);

            ThingUID thingUID = new ThingUID(THING_TYPE_HASP_PLATE, connectionBridge, sanitizePlateName(nodeName));

            logger.debug("Discovered Thing: {} ({})({})", thingUID, nodeName, sanitizePlateName(nodeName));
            thingDiscovered(DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withRepresentationProperty(DEVICE_ID_FIELD).withBridge(connectionBridge)
                    .withLabel("OpenHASP Plate " + nodeName).build());
        }
    }

    @Override
    public void topicVanished(ThingUID thingUID, MqttBrokerConnection connection, String topic) {
    }

    @Override
    protected MQTTTopicDiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public static String sanitizePlateName(String name) {
        return name.replaceAll("[^\\w-]", "");
    }
}
