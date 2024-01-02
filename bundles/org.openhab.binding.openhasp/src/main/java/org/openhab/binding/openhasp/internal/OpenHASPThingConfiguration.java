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

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link OpenHASPThingConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Alvaro Lobato - Initial contribution
 */
@NonNullByDefault
public class OpenHASPThingConfiguration {

    public String deviceId = "";
    public String templatePathType = "";
    public String templatePath = "";
    public ArrayList<String> pages = new ArrayList<>();
    public @Nullable String hostname;
    public @Nullable String password;
    public int backlightHigh;
    public int backlightMedium;
    public int backlightLow;
    public String configMode = "sitemap";
    public String sitemap = "";
}
