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
package org.openhab.binding.souliss.internal.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.binding.souliss.internal.SoulissProtocolConstants;
import org.openhab.binding.souliss.internal.SoulissUDPConstants;
import org.openhab.binding.souliss.internal.config.GatewayConfig;
import org.openhab.binding.souliss.internal.handler.SoulissGatewayHandler;
import org.openhab.binding.souliss.internal.handler.SoulissGenericHandler;
import org.openhab.binding.souliss.internal.handler.SoulissT11Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT19Handler;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What a packet still carries, and when it leaves the queue, as the node confirms its commands.
 *
 * @author Alvaro Lobato - Initial contribution
 */
@NonNullByDefault
class SendDispatcherRunnableTest {

    private static final int NODE = 7;
    private static final byte ON = SoulissProtocolConstants.SOULISS_T1N_ON_CMD;
    private static final byte OFF = SoulissProtocolConstants.SOULISS_T1N_OFF_CMD;
    private static final byte IS_ON = SoulissProtocolConstants.SOULISS_T1N_ON_COIL;
    private static final byte IS_OFF = SoulissProtocolConstants.SOULISS_T1N_OFF_COIL;

    private final Logger logger = LoggerFactory.getLogger(SendDispatcherRunnableTest.class);
    private final List<Thing> things = new ArrayList<>();
    private final Bridge bridge = mock(Bridge.class);
    private SendDispatcherRunnable dispatcher = new SendDispatcherRunnable(bridge);

    @BeforeEach
    void setUp() {
        SendDispatcherRunnable.packetsList.clear();
        GatewayConfig config = new GatewayConfig();
        config.timeoutToRequeue = 5000;
        config.timeoutToRemovePacket = 20000;
        SoulissGatewayHandler gateway = mock(SoulissGatewayHandler.class);
        when(gateway.getGwConfig()).thenReturn(config);
        when(gateway.getThing()).thenReturn(bridge);
        when(bridge.getHandler()).thenReturn(gateway);
        when(bridge.getThings()).thenReturn(things);
        dispatcher = new SendDispatcherRunnable(bridge);
    }

    @AfterEach
    void tearDown() {
        SendDispatcherRunnable.packetsList.clear();
    }

    private <T extends SoulissGenericHandler> T typical(String type, int slot,
            java.util.function.Function<Thing, T> create) {
        Thing thing = mock(Thing.class);
        when(thing.getUID()).thenReturn(new ThingUID("souliss:" + type + ":gateway:" + NODE + "-" + slot));
        when(thing.getThingTypeUID()).thenReturn(new ThingTypeUID("souliss", type));
        when(thing.getStatus()).thenReturn(ThingStatus.ONLINE);
        when(thing.getConfiguration())
                .thenReturn(new Configuration(Map.of("node", NODE, "slot", slot, "secureSend", true)));
        T handler = create.apply(thing);
        handler.setCallback(mock(ThingHandlerCallback.class));
        handler.initialize();
        when(thing.getHandler()).thenReturn(handler);
        things.add(thing);
        return handler;
    }

    private SoulissT19Handler dimmer(int slot, byte state) {
        SoulissT19Handler handler = typical("t19", slot, SoulissT19Handler::new);
        handler.setRawState(state);
        return handler;
    }

    private SoulissT11Handler light(int slot, byte state) {
        SoulissT11Handler handler = typical("t11", slot, SoulissT11Handler::new);
        handler.setRawState(state);
        return handler;
    }

    /** Queue one force frame for the node, with the given command at each slot (slot, command, slot, ...). */
    private byte[] send(int... slotAndCommand) {
        int last = 0;
        for (int i = 0; i < slotAndCommand.length; i += 2) {
            last = Math.max(last, slotAndCommand[i]);
        }
        byte[] frame = new byte[12 + last + 1];
        frame[7] = SoulissUDPConstants.SOULISS_UDP_FUNCTION_FORCE;
        frame[10] = NODE;
        for (int i = 0; i < slotAndCommand.length; i += 2) {
            frame[12 + slotAndCommand[i]] = (byte) slotAndCommand[i + 1];
        }
        SendDispatcherRunnable.put(new DatagramPacket(frame, frame.length), logger);
        return frame;
    }

    /** One dispatcher cycle after the packet went out: check what the node has confirmed. */
    private void cycle() {
        for (PacketStruct packet : SendDispatcherRunnable.packetsList) {
            packet.setSent(true);
            packet.setTime(System.currentTimeMillis());
        }
        dispatcher.safeSendCheck();
    }

    private static boolean queued() {
        return !SendDispatcherRunnable.packetsList.isEmpty();
    }

    @Test
    void dimmerSwitchedOnStaysInThePacketWhileAnotherOneFadesOut() {
        SoulissT19Handler ambient = dimmer(27, IS_OFF);
        SoulissT19Handler strip = dimmer(33, IS_ON);
        byte[] frame = send(27, ON, 33, OFF);

        cycle();
        assertEquals(ON, frame[12 + 27]);
        assertEquals(OFF, frame[12 + 33]);

        // The node reports ON at the first step of the fade in. A zero at slot 27 in the resends for the
        // strip would stop that fade a few percent in.
        ambient.setRawState(IS_ON);
        cycle();
        assertEquals(ON, frame[12 + 27], "the confirmed ON is still sent while the packet is resent");
        assertEquals(OFF, frame[12 + 33]);
        assertTrue(queued());
        assertFalse(SendDispatcherRunnable.packetsList.get(0).getSent(), "queued for another send");

        strip.setRawState(IS_OFF);
        cycle();
        assertFalse(queued(), "nothing waits for a confirmation any more");
    }

    @Test
    void dimmerSwitchedOnAloneLeavesTheQueueWhenConfirmed() {
        SoulissT19Handler ambient = dimmer(27, IS_OFF);
        send(27, ON);

        cycle();
        assertTrue(queued());

        ambient.setRawState(IS_ON);
        cycle();
        assertFalse(queued(), "a kept command does not keep the packet alive by itself");
    }

    @Test
    void dimmerSwitchedOffIsResentUntilItsFadeOutEnds() {
        SoulissT19Handler strip = dimmer(43, IS_ON);
        SoulissT11Handler spot = light(5, IS_ON);
        byte[] frame = send(5, OFF, 43, OFF);

        // The spot confirms at once; the strip reports OFF only when its fade out has ended.
        spot.setRawState(IS_OFF);
        cycle();
        assertEquals(0, frame[12 + 5], "a confirmed T11 command is dropped as before");
        assertEquals(OFF, frame[12 + 43], "the resends still carry the OFF of the strip");
        assertTrue(queued());

        strip.setRawState(IS_OFF);
        cycle();
        assertFalse(queued());
    }

    @Test
    void wallSwitchDuringTheResendsIsTakenBackToTheCommandedState() {
        SoulissT19Handler ambient = dimmer(27, IS_ON);
        dimmer(33, IS_ON);
        byte[] frame = send(27, ON, 33, OFF);

        cycle();
        ambient.setRawState(IS_OFF);
        cycle();

        assertEquals(ON, frame[12 + 27]);
        assertTrue(queued(), "the ON waits for its confirmation again");
    }
}
