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

import static org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mqtt.generic.MqttChannelTypeProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.model.sitemap.SitemapProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.google.gson.Gson;

/**
 * The {@link OpenHASPThingHandlerFactory} is responsible for creating things
 * and thing
 * handlers.
 *
 * @author Alvaro Lobato - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.openhasp", service = ThingHandlerFactory.class)
public class OpenHASPThingHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_HASP_PLATE);

    private @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @NonNullByDefault({}) MqttChannelTypeProvider channelTypeProvider;
    private @NonNullByDefault({}) ItemRegistry itemRegistry;

    private final List<SitemapProvider> sitemapProviders = new ArrayList<>();
    private Gson gson = new Gson();

    @Activate
    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
    }

    @Deactivate
    @Override
    protected void deactivate(ComponentContext componentContext) {
        super.deactivate(componentContext);
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (THING_TYPE_HASP_PLATE.equals(thingTypeUID)) {
            return new OpenHASPThingHandler(thing, thingRegistry, bundleContext, channelTypeProvider, itemRegistry,
                    OpenHASPBindingConstants.OPENHASP_DEVICE_TIMEOUT_MS,
                    OpenHASPBindingConstants.OPENHASP_SUBSCRIBE_TIMEOUT_MS,
                    OpenHASPBindingConstants.OPENHASP_ATTRIBUTE_TIMEOUT_MS, sitemapProviders, gson);
        }

        return null;
    }

    public ThingRegistry getThingRegistry() {
        return thingRegistry;
    }

    @Reference
    public void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    public MqttChannelTypeProvider getChannelTypeProvider() {
        return channelTypeProvider;
    }

    @Reference
    public void setChannelTypeProvider(MqttChannelTypeProvider channelTypeProvider) {
        this.channelTypeProvider = channelTypeProvider;
    }

    //// Access to sitemaps
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addSitemapProvider(SitemapProvider provider) {
        sitemapProviders.add(provider);
    }

    public void removeSitemapProvider(SitemapProvider provider) {
        sitemapProviders.remove(provider);
    }

    @Reference
    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }
}
