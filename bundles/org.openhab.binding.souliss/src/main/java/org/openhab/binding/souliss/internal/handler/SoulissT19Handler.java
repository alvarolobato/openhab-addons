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

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.internal.SoulissBindingConstants;
import org.openhab.binding.souliss.internal.SoulissProtocolConstants;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SoulissT19Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */

@NonNullByDefault
public class SoulissT19Handler extends SoulissGenericHandler {
    private final Logger logger = LoggerFactory.getLogger(SoulissT19Handler.class);
    byte t1nRawStateByte0 = 0xF;
    byte t1nRawStateBrigthnessByte1 = 0x00;
    // false until the node has reported a brightness: a REFRESH before that has nothing to publish
    boolean brightnessKnown = false;

    byte xSleepTime = 0;

    public SoulissT19Handler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handle commmand channel: {} command: {} ", channelUID, command);
        if (command instanceof RefreshType) {
            switch (channelUID.getId()) {
                case SoulissBindingConstants.ONOFF_CHANNEL:
                    OnOffType valOnOff = getOhStateOnOffFromSoulissVal(t1nRawStateByte0);
                    if (valOnOff != null) {
                        updateState(channelUID, valOnOff);
                    }
                    break;
                case SoulissBindingConstants.DIMMER_BRIGHTNESS_CHANNEL:
                    if (brightnessKnown) {
                        updateState(SoulissBindingConstants.DIMMER_BRIGHTNESS_CHANNEL,
                                toPercent(t1nRawStateBrigthnessByte1 & 0xFF));
                    }
                    break;
                default:
                    break;
            }
        } else {
            switch (channelUID.getId()) {
                case SoulissBindingConstants.ONOFF_CHANNEL:
                    if (command.equals(OnOffType.ON)) {
                        commandSEND(SoulissProtocolConstants.SOULISS_T1N_ON_CMD);

                    } else if (command.equals(OnOffType.OFF)) {
                        commandSEND(SoulissProtocolConstants.SOULISS_T1N_OFF_CMD);
                    }
                    break;

                case SoulissBindingConstants.DIMMER_BRIGHTNESS_CHANNEL:
                    if (command instanceof PercentType percentCommand) {
                        updateState(SoulissBindingConstants.DIMMER_BRIGHTNESS_CHANNEL, percentCommand);
                        commandSEND(SoulissProtocolConstants.SOULISS_T1N_SET,
                                (byte) Math.round((percentCommand.doubleValue() / 100.00) * 255.00));
                    } else if (command.equals(OnOffType.ON)) {
                        commandSEND(SoulissProtocolConstants.SOULISS_T1N_ON_CMD);

                    } else if (command.equals(OnOffType.OFF)) {
                        commandSEND(SoulissProtocolConstants.SOULISS_T1N_OFF_CMD);
                    }
                    break;

                case SoulissBindingConstants.ROLLER_BRIGHTNESS_CHANNEL:
                    if (command.equals(UpDownType.UP)) {
                        commandSEND(SoulissProtocolConstants.SOULISS_T1N_BRIGHT_UP);
                    } else if (command.equals(UpDownType.DOWN)) {
                        commandSEND(SoulissProtocolConstants.SOULISS_T1N_BRIGHT_DOWN);
                    }
                    break;
                case SoulissBindingConstants.SLEEP_CHANNEL:
                    if (command instanceof OnOffType) {
                        commandSEND((byte) (SoulissProtocolConstants.SOULISS_T1N_TIMED + xSleepTime));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        updateStatus(ThingStatus.UNKNOWN);

        var configurationMap = getThing().getConfiguration();
        if (configurationMap.get(SoulissBindingConstants.SLEEP_CHANNEL) != null) {
            xSleepTime = ((BigDecimal) configurationMap.get(SoulissBindingConstants.SLEEP_CHANNEL)).byteValue();
        }
        // On unless it is switched off: the same default as in thing-types.xml, for a thing whose
        // configuration reaches the handler without it.
        bSecureSend = !Boolean.FALSE.equals(configurationMap.get(SoulissBindingConstants.CONFIG_SECURE_SEND));
    }

    public void setState(@Nullable PrimitiveType state) {
        super.setLastStatusStored();
        if (state != null) {
            updateState(SoulissBindingConstants.SLEEP_CHANNEL, OnOffType.OFF);
            logger.debug("setState - T19, setting state to {}", state.toFullString());
            this.updateState(SoulissBindingConstants.ONOFF_CHANNEL, (OnOffType) state);
        }
    }

    /** A brightness level (0..255) as a percent with two decimals, so that every level maps back to itself. */
    private static PercentType toPercent(int level) {
        return new PercentType(
                BigDecimal.valueOf(level * 100L).divide(BigDecimal.valueOf(255), 2, RoundingMode.HALF_UP));
    }

    public void setRawStateDimmerValue(byte dimmerValue) {
        try {
            // The node sends the brightness as an unsigned byte; as a Java byte, levels above 127 are negative.
            int level = dimmerValue & 0xFF;
            logger.debug("setRawStateDimmerValue - T19, setting raw state to {} current: {}", level,
                    t1nRawStateBrigthnessByte1);
            // A light that is off reports brightness 0: skip it, so the channel keeps the level the light
            // returns to when it is switched on again.
            if (level != 0 || t1nRawStateByte0 != SoulissProtocolConstants.SOULISS_T1N_OFF_COIL) {
                logger.debug("T19, setting dimmer to {} current: {} -  UUID: {} - {}", level,
                        t1nRawStateBrigthnessByte1, this.getThing().getUID().getAsString(), this.getThing().getLabel());
                t1nRawStateBrigthnessByte1 = dimmerValue;
                brightnessKnown = true;
                updateState(SoulissBindingConstants.DIMMER_BRIGHTNESS_CHANNEL, toPercent(level));
                logger.debug("T19, setting dimmer to {} current: {} -  UUID: {} - {}", level,
                        t1nRawStateBrigthnessByte1, this.getThing().getUID().getAsString(), this.getThing().getLabel());
            }
        } catch (Exception ex) {
            logger.warn("UUID: {}, had an update dimmer state error:{}", this.getThing().getUID().getAsString(),
                    ex.getMessage());
        }
    }

    @Override
    public void setRawState(byte rawState) {
        logger.debug("setRawState - T19, setting raw state to {} current: {}", rawState, t1nRawStateByte0);
        // update Last Status stored time
        super.setLastStatusStored();
        // update item state only if it is different from previous
        if (t1nRawStateByte0 != rawState) {
            this.setState(getOhStateOnOffFromSoulissVal(rawState));
            logger.debug("setRawState - setState - T19, done to {} current: {}", rawState, t1nRawStateByte0);
        }
        t1nRawStateByte0 = rawState;
        logger.debug("setRawState - end - done to {} current: {}", rawState, t1nRawStateByte0);
    }

    @Override
    public byte getRawState() {
        return t1nRawStateByte0;
    }

    public byte getRawStateDimmerValue() {
        return t1nRawStateBrigthnessByte1;
    }

    @Override
    public byte getExpectedRawState(byte bCmd) {
        if (bSecureSend) {
            if (bCmd == SoulissProtocolConstants.SOULISS_T1N_ON_CMD) {
                return SoulissProtocolConstants.SOULISS_T1N_ON_COIL;
            } else if (bCmd == SoulissProtocolConstants.SOULISS_T1N_OFF_CMD) {
                return SoulissProtocolConstants.SOULISS_T1N_OFF_COIL;
            }
            // SLEEP is sent once: the node answers it with the good night state, never with ON_COIL, so it
            // could not be confirmed and every resend would restart the countdown.
        }
        return -1;
    }

    @Override
    public boolean keepCommandWhileResending(byte bCmd) {
        // The node reports ON at the first step of the fade in, and only goes on fading while the command
        // stays in its input slot. An OFF is confirmed when its fade out has ended, so it can be dropped.
        return bSecureSend && bCmd == SoulissProtocolConstants.SOULISS_T1N_ON_CMD;
    }
}
