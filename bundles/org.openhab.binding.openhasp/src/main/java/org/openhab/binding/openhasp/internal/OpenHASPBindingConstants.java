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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link OpenHASPBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Alvaro Lobato - Initial contribution
 */
@NonNullByDefault
public class OpenHASPBindingConstants {
    public static enum CommandType {
        CMD,
        JSON,
        JSONL
    }

    public static final String HASP_BASE_TOPIC = "hasp/";
    public static final String HASP_DISCOVERY_TOPIC = HASP_BASE_TOPIC + "discovery/";
    public static final String BINDING_ID = "mqtt";
    public static final String DEVICE_ID_FIELD = "deviceId";
    public static final String HASP_LWT_TOPIC = "LWT";
    public static final String HASP_STATE_TOPIC = "state";
    public static final String HASP_STATE_IDLE_TOPIC = "idle";
    public static final String HASP_STATE_IDLE_SHORT_VALUE = "short";
    public static final String HASP_STATE_IDLE_LONG_VALUE = "long";
    public static final String HASP_STATE_IDLE_OFF_VALUE = "off";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_HASP_PLATE = new ThingTypeUID(BINDING_ID, "openhasp_plate");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_HASP_PLATE);

    // List of all Channel ids
    public static final String CHANNEL_BACKLIGHT = "backlight";
    public static final String CHANNEL_LWT = "LWT";

    // Timeouts
    public static final int OPENHASP_DEVICE_TIMEOUT_MS = 30000;
    public static final int OPENHASP_SUBSCRIBE_TIMEOUT_MS = 500;
    public static final int OPENHASP_ATTRIBUTE_TIMEOUT_MS = 200;

    // Default bigtness
    public static final int OPENHASP_PLATE_HIGH_BRIGHTNESS = 100;
    public static final int OPENHASP_PLATE_MEDIUM_BRIGHTNESS = 50;
    public static final int OPENHASP_PLATE_LOW_BRIGHTNESS = 20;

    public static final String OPENHASP_CONFIGMODE_MANUAL = "manual";
    public static final String OPENHASP_CONFIGMODE_SITEMAP = "sitemap";
}
