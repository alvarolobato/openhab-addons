package org.openhab.binding.openhasp.internal;

import static org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.HASP_BASE_TOPIC;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openhasp.internal.OpenHASPBindingConstants.CommandType;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.io.transport.mqtt.MqttMessageSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
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

    private @Nullable OpenHASPCallbackProcessor callbackProcessor;

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
        sendHASPCommand(type, Arrays.asList(new String[] { command }));
    }

    public void sendHASPCommand(CommandType type, List<String> commands) {
        // if (ThingStatus.ONLINE.equals(thing.getStatus())) {
        int jsonLimit = 500;

        switch (type) {
            case JSON:
                StringBuffer jsonCommand = new StringBuffer();
                boolean first = true;
                for (int i = 0; i < commands.size(); i++) {
                    String command = commands.get(i);

                    if ((jsonCommand.length() + command.length() + 5) >= jsonLimit) {
                        // do send if we get over the size
                        jsonCommand.append("']");
                        String formattedCmd = jsonCommand.toString();
                        logger.trace("Send Full Command list to plate {}, command was {}", plateId, formattedCmd);
                        connection.publish(plateJSONCmdTopic, formattedCmd.getBytes(), 1, true);
                        first = true;
                    }

                    if (first) {
                        jsonCommand.append("['");
                        first = false;
                    } else {
                        jsonCommand.append("','");
                    }

                    logger.trace("Send Command to plate {}, command {}", plateId, command);
                    jsonCommand.append(command);
                }

                if (!first) {
                    jsonCommand.append("']");
                    String formattedCmd = jsonCommand.toString();
                    logger.trace("Send Full Command list to plate {}, command was {}", plateId, formattedCmd);
                    connection.publish(plateJSONCmdTopic, formattedCmd.getBytes(), 1, true);
                }
                break;
            case JSONL:
                StringBuffer jsonLCommand = new StringBuffer();
                for (String command : commands) {
                    jsonLCommand.append(command).append("\n");
                }
                String formattedCmd = jsonLCommand.toString();
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

        if (callbackProcessor != null && strippedTopic != null) {
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
