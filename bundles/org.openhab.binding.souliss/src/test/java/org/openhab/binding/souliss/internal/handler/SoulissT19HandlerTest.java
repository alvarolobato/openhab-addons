/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.souliss.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.souliss.internal.SoulissBindingConstants;
import org.openhab.binding.souliss.internal.SoulissProtocolConstants;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.types.RefreshType;

/**
 * Brightness the node reports for a T19 dimmer, as it reaches the brightness channel.
 *
 * @author Alvaro Lobato - Initial contribution
 */
@NonNullByDefault
class SoulissT19HandlerTest {

    private static final ThingUID THING = new ThingUID("souliss:t19:gateway:1-2");

    private final List<String> brightness = new ArrayList<>();
    private final SoulissT19Handler handler = newHandler();

    private SoulissT19Handler newHandler() {
        Thing thing = mock(Thing.class);
        when(thing.getUID()).thenReturn(THING);
        ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        doAnswer(invocation -> {
            ChannelUID channel = invocation.getArgument(0);
            if (SoulissBindingConstants.DIMMER_BRIGHTNESS_CHANNEL.equals(channel.getId())) {
                brightness.add(invocation.getArgument(1).toString());
            }
            return null;
        }).when(callback).stateUpdated(any(), any());
        SoulissT19Handler h = new SoulissT19Handler(thing);
        h.setCallback(callback);
        return h;
    }

    /** One status packet: the decoder hands over the state byte, then the brightness byte. */
    private void report(byte state, int level) {
        handler.setRawState(state);
        handler.setRawStateDimmerValue((byte) level);
    }

    @Test
    void levelsAboveHalfAreReported() {
        report(SoulissProtocolConstants.SOULISS_T1N_ON_COIL, 128);
        report(SoulissProtocolConstants.SOULISS_T1N_ON_COIL, 170);
        report(SoulissProtocolConstants.SOULISS_T1N_ON_COIL, 255);

        assertEquals(List.of("50.20", "66.67", "100.00"), brightness);
    }

    @Test
    void levelsUpToHalfAreStillReported() {
        report(SoulissProtocolConstants.SOULISS_T1N_ON_COIL, 7);
        report(SoulissProtocolConstants.SOULISS_T1N_ON_COIL, 127);

        assertEquals(List.of("2.75", "49.80"), brightness);
    }

    @Test
    void levelEqualToTheStateByteIsReported() {
        // Level 1 with the light on (ON coil is 0x01) used to be dropped.
        report(SoulissProtocolConstants.SOULISS_T1N_ON_COIL, 1);

        assertEquals(List.of("0.39"), brightness);
    }

    @Test
    void lightThatIsOffKeepsItsBrightness() {
        report(SoulissProtocolConstants.SOULISS_T1N_ON_COIL, 170);
        report(SoulissProtocolConstants.SOULISS_T1N_OFF_COIL, 0);

        assertEquals(List.of("66.67"), brightness);
    }

    @Test
    void refreshRepublishesTheLastReportedBrightness() {
        report(SoulissProtocolConstants.SOULISS_T1N_ON_COIL, 170);
        report(SoulissProtocolConstants.SOULISS_T1N_OFF_COIL, 0);
        brightness.clear();

        handler.handleCommand(new ChannelUID(THING, SoulissBindingConstants.DIMMER_BRIGHTNESS_CHANNEL),
                RefreshType.REFRESH);

        assertEquals(List.of("66.67"), brightness);
    }

    @Test
    void refreshBeforeTheNodeHasReportedPublishesNothing() {
        // It used to publish 0 %, overwriting the brightness the item had restored.
        handler.handleCommand(new ChannelUID(THING, SoulissBindingConstants.DIMMER_BRIGHTNESS_CHANNEL),
                RefreshType.REFRESH);

        assertEquals(List.of(), brightness);
    }

    @Test
    void withSecureSendOnAndOffAreResentUntilTheNodeConfirms() {
        Thing thing = mock(Thing.class);
        when(thing.getUID()).thenReturn(THING);
        when(thing.getConfiguration())
                .thenReturn(new Configuration(Map.of("node", 1, "slot", 2, "secureSend", true)));
        SoulissT19Handler secure = new SoulissT19Handler(thing);
        secure.setCallback(mock(ThingHandlerCallback.class));
        secure.initialize();

        assertEquals(SoulissProtocolConstants.SOULISS_T1N_OFF_COIL,
                secure.getExpectedRawState(SoulissProtocolConstants.SOULISS_T1N_OFF_CMD));
        assertEquals(SoulissProtocolConstants.SOULISS_T1N_ON_COIL,
                secure.getExpectedRawState(SoulissProtocolConstants.SOULISS_T1N_ON_CMD));
        // A brightness is not confirmed by the on/off state: sent once.
        assertEquals(-1, secure.getExpectedRawState(SoulissProtocolConstants.SOULISS_T1N_SET));
    }

    @Test
    void secureSendIsOnByDefaultForT19() throws Exception {
        // Without it an OFF is sent once and its byte is zeroed in the queued packet, so a packet resent
        // for another light of the node carries "no command" for this one and stops its fade half way.
        try (InputStream xml = getClass().getResourceAsStream("/OH-INF/thing/thing-types.xml")) {
            var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
            String value = XPathFactory.newInstance().newXPath().evaluate(
                    "//*[local-name()='thing-type'][@id='t19']//*[local-name()='parameter'][@name='secureSend']/*[local-name()='default']",
                    doc);

            assertEquals("true", value);
        }
    }

    @Test
    void zeroWhileTheLightIsOnIsReported() {
        report(SoulissProtocolConstants.SOULISS_T1N_ON_COIL, 0);

        assertEquals(List.of("0.00"), brightness);
    }
}
