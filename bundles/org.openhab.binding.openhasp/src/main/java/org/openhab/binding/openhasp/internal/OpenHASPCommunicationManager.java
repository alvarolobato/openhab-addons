package org.openhab.binding.openhasp.internal;

import static org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.HASP_BASE_TOPIC;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.CommandType;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.io.transport.mqtt.MqttMessageSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenHASPCommunicationManager implements MqttMessageSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(OpenHASPCommunicationManager.class);

    protected String plateId;
    private String plateBaseTopic = "";
    private String plateStateTopic = "";
    private String plateLWTTopic = "";

    private String plateCmdTopic = "";
    private String plateJSONCmdTopic = "";
    private String plateJSONLCmdTopic = "";
    private MqttBrokerConnection connection;
    @Nullable
    OpenHASPCallbackProcessor callbackProcessor;

    public OpenHASPCommunicationManager(String plateId, MqttBrokerConnection connection) {
        this.plateId = plateId;
        this.connection = connection;
        plateBaseTopic = HASP_BASE_TOPIC + plateId;
        plateStateTopic = plateBaseTopic + "/state/#";
        plateLWTTopic = plateBaseTopic + "/LWT";
        plateCmdTopic = plateBaseTopic + "/command";
        plateJSONCmdTopic = plateCmdTopic + "/json";
        plateJSONLCmdTopic = plateCmdTopic + "/jsonl";
    }

    public void start() {
        logger.trace("Subscribing to topic {} for plate {}", plateStateTopic, plateId);
        connection.subscribe(plateStateTopic, this);
    }

    public void sendHASPCommand(CommandType type, String command) {
        sendHASPCommand(type, new String[] { command });
    }

    public void sendHASPCommand(CommandType type, String[] commands) {
        // if (ThingStatus.ONLINE.equals(thing.getStatus())) {
        String formattedCmd;
        switch (type) {
            case JSON:
                StringBuffer jsonCommand = new StringBuffer();
                boolean first = true;
                jsonCommand.append("['");
                for (String command : commands) {
                    if (first) {
                        first = false;
                    } else {
                        jsonCommand.append("','");
                    }
                    jsonCommand.append(command);
                }
                jsonCommand.append("']");
                formattedCmd = jsonCommand.toString();
                logger.trace("Send Command to plate {}, command was {}", plateId, formattedCmd);
                connection.publish(plateJSONCmdTopic, formattedCmd.getBytes(), 1, true);
                break;
            case JSONL:
                StringBuffer jsonLCommand = new StringBuffer();
                for (String command : commands) {
                    jsonLCommand.append(command).append("\n");
                }
                formattedCmd = jsonLCommand.toString();
                logger.trace("Send Command to plate {}, command was {}", plateId, formattedCmd);
                connection.publish(plateJSONLCmdTopic, formattedCmd.getBytes(), 1, true);
                break;
            case CMD:
                for (String cmd : commands) {
                    connection.publish(plateCmdTopic, cmd.getBytes(), 1, true);
                }
                break;
        }

        // } else {
        // logger.trace("Send Command to plate {} SKIPPED, plate state {}, command was
        // {}", plateId, thing.getStatus(),
        // commands);
        // }
    }

    @Override
    public void processMessage(String topic, byte[] payload) {
        String value = new String(payload, StandardCharsets.UTF_8);
        String strippedTopic = topic.substring(plateBaseTopic.length() + 1);

        logger.trace("MESSAGE Plate {} state {}:{}", plateId, strippedTopic, value);
        if (callbackProcessor != null) {
            callbackProcessor.plateCallback(strippedTopic, value);
        }
    }

    public String getPlateId() {
        return plateId;
    }

    public String getPlateBaseTopic() {
        return plateBaseTopic;
    }

    public String getPlateStateTopic() {
        return plateStateTopic;
    }

    public String getPlateLWTTopic() {
        return plateLWTTopic;
    }

    public String getPlateCmdTopic() {
        return plateCmdTopic;
    }

    public String getPlateJSONCmdTopic() {
        return plateJSONCmdTopic;
    }

    public String getPlateJSONLCmdTopic() {
        return plateJSONLCmdTopic;
    }

    public CompletableFuture<Void> unsubscribeAll() {
        MqttMessageSubscriber subscriber = this;
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                logger.trace("Unsubscribing to topic {} for plate {}", getPlateStateTopic(), plateId);
                connection.unsubscribe(getPlateStateTopic(), subscriber);
            }
        });
    }

    public void setCallbackProcessor(OpenHASPCallbackProcessor callbackProcessor) {
        this.callbackProcessor = callbackProcessor;
    }
}
