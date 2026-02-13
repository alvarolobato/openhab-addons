# OpenHASP Binding - Technical Analysis & Task List

**OpenHAB Version:** 4.3.6-SNAPSHOT  
**Branch:** OpenHASP_4.3  
**Status:** Development / Pre-Alpha

---

## PROJECT OVERVIEW

### What is OpenHASP?

OpenHASP (Open Hardware Agnostic Smart Panel) is an open-source firmware that transforms ESP32-based touchscreen displays into smart home control panels. Unlike the original HASwitchPlate which required proprietary Nextion/TJC displays, openHASP uses LVGL (Light and Versatile Graphics Library) to render UI on commodity hardware.

**Key Features:**
- Runs on ESP32/ESP32-S3 with various display sizes (2.4" to 7"+)
- Supports 40+ different display/board combinations
- MQTT-based communication protocol
- LVGL graphics for rich UI (buttons, sliders, gauges, charts, etc.)
- Multi-page support with touch navigation
- Low cost (~$15-50 per panel)
- Active community (947 GitHub stars, 51 contributors)

**Related Projects:**
- Firmware: https://github.com/HASwitchPlate/openHASP
- Documentation: https://www.openhasp.com/0.7.0/
- Current Firmware Version: 0.7.0-rc13 (August 2024)

### What Does This Binding Do?

The OpenHASP Binding for OpenHAB provides bidirectional integration between OpenHAB and openHASP touch panels via MQTT:

**Core Functionality:**
1. **Automatic Layout Generation**: Converts OpenHAB sitemaps into touchscreen layouts
2. **Bidirectional Control**: Panel controls OpenHAB items; OpenHAB updates panel state
3. **Device Discovery**: Auto-discovers panels via MQTT
4. **Template System**: Handlebars-based templates for customizable UI generation
5. **Widget Support**: Buttons, sliders, setpoints, selections, text displays, sections

### Architecture

```
OpenHAB Sitemap → Layout Manager → Handlebars Templates → JSON → MQTT → OpenHASP Device
OpenHASP Device → MQTT → Communication Manager → Event Processor → OpenHAB Items
```

**Key Components:**
- `OpenHASPThingHandler`: Manages thing lifecycle, extends AbstractMQTTThingHandler
- `OpenHASPPlate`: Processes events, implements EventSubscriber
- `OpenHASPCommunicationManager`: Handles all MQTT communication
- `OpenHASPLayoutManager`: Converts sitemaps to layouts, manages object mappings
- `TemplateProcessor`: Renders Handlebars templates to JSON
- `Widget Components`: ButtonWidget, SliderWidget, SetPointWidget, etc.
- `OpenHASPDiscoveryService`: MQTT-based device discovery
- `ObjItemMapper`: Maps panel objects to OpenHAB items

### MQTT Protocol

**Topics:**
- `hasp/{deviceId}/command` - Simple commands
- `hasp/{deviceId}/command/json` - JSON commands (array of commands)
- `hasp/{deviceId}/command/jsonl` - JSONL commands (multiple objects)
- `hasp/{deviceId}/state/{subtopic}` - Device state updates
- `hasp/{deviceId}/LWT` - Last Will Testament (online/offline)
- `hasp/discovery/` - Device discovery announcements

**Object ID Format:** `p{page}b{id}` (e.g., p1b5 = page 1, object 5)

**QoS Requirements:** QoS 1 required (at-least-once delivery)

---

## CURRENT STATUS

### What Works

✅ **Core MQTT Communication**: Can send/receive messages to/from devices  
✅ **Sitemap Processing**: Parses OpenHAB sitemaps and extracts widgets  
✅ **Layout Generation**: Converts sitemap widgets to openHASP JSON objects  
✅ **Template System**: Handlebars templates render correctly  
✅ **Event Flow (OpenHAB → Panel)**: Item state changes update panel display  
✅ **Command Flow (Panel → OpenHAB)**: Touch events send commands to items  
✅ **Discovery**: Devices appear in inbox when they announce via MQTT  
✅ **Basic Widgets**: Switch, Slider, Text, Setpoint, Selection work  
✅ **Multi-page Support**: Can generate multiple pages  
✅ **Backlight Control**: Dimmer channel controls display brightness  

### Critical Issues

❌ **LWT Initialization Bug**: When OpenHAB restarts while device is already online, thing status incorrectly shows OFFLINE. Device must come online AFTER OpenHAB starts to be detected. This is a known MQTT binding issue in OpenHAB 3.x → 4.x migration.

❌ **No Unit Tests**: Zero test coverage. Cannot safely refactor or develop.

❌ **Manual Configuration Incomplete**: Manual mode advertised but not implemented (TODO comments in code).

❌ **Configuration Validation Missing**: No validation of deviceId, sitemap, template paths, brightness values, etc.

❌ **Error Handling Inconsistent**: Missing try-catch blocks, poor error messages, no graceful degradation.

❌ **No Offline Checking**: Commands sent even when device offline (TODOs in code).

### Known Limitations

⚠️ **Limited Widget Support**: Advanced openHASP objects not supported (charts, gauges, images, QR codes, color pickers, etc.)  
⚠️ **Single Template Set**: Only default-portrait template exists  
⚠️ **No Icon Customization**: Icon mapping system incomplete  
⚠️ **Backlight Automation Disabled**: Idle/dim code commented out  
⚠️ **No Diagnostic Channels**: No firmware version, uptime, statistics  
⚠️ **Memory/Performance**: No caching, inefficient string operations  
⚠️ **Incomplete i18n**: Property files contain FIXME placeholders  

### Code Quality Issues

- 20+ TODO comments indicating incomplete features
- Dead code (*.hbs_old template files, commented blocks)
- Inconsistent logging (mixing trace/debug/info)
- Magic numbers and hard-coded values
- Long methods (300+ line methods)
- Missing Javadoc on many methods
- No null safety annotations used consistently
- Duplicate code across widget components

### Migration Note

This binding was originally created for OpenHAB 3.2 and migrated to OpenHAB 4.3.5 with **only API breakage fixes** - no behavior improvements were made. The MQTT binding significantly changed between versions, causing the LWT initialization issue.

---

## TECHNICAL TASK LIST

Tasks organized by priority. All tasks are technical work suitable for AI agent execution.

---

## CRITICAL BUGS (Must Fix)

### TASK 1: Fix LWT Initialization Bug

**Priority:** CRITICAL  
**Issue:** Device shows OFFLINE after OpenHAB restart even when online  
**Root Cause:** Retained LWT message not received when subscribing to availability topic  

**Analysis:**
- Current code calls `addAvailabilityTopic()` correctly (line 169 of OpenHASPThingHandler)
- But AbstractMQTTThingHandler doesn't receive retained messages on subscription
- Need to actively check retained message value or add timeout mechanism
- Related OpenHAB issues: #9715, #9242 in core MQTT binding

**Implementation:**

1. **Research Implementation**: Study how other MQTT bindings handle this
   - Check `HomeAssistantThingHandler` implementation
   - Check `HomieThingHandler` implementation  
   - Look for retained message checking patterns

2. **Implement Solution Option A (Retained Message Check)**:
   ```java
   // After addAvailabilityTopic(), add:
   connection.subscribe(comm.getPlateLWTTopic(), new MqttMessageSubscriber() {
       @Override
       public void processMessage(String topic, byte[] payload) {
           String status = new String(payload, StandardCharsets.UTF_8);
           if ("online".equals(status)) {
               updateStatus(ThingStatus.ONLINE);
               plate.refresh();
           }
       }
   }).thenAccept(success -> {
       if (success) {
           // Check for retained message
           connection.publish(comm.getPlateLWTTopic() + "?", new byte[0], 0, false);
       }
   });
   ```

3. **Implement Solution Option B (Status Query)**:
   - Subscribe to state topic immediately
   - Send status query command: `statusupdate`
   - Device responds with current state
   - Update thing status based on response
   - Set timeout (5 seconds) to mark offline if no response

4. **Testing Requirements**:
   - Test: Device online before OpenHAB starts → Should show ONLINE
   - Test: Device offline before OpenHAB starts → Should show OFFLINE
   - Test: Device comes online after OpenHAB starts → Should show ONLINE
   - Test: Device goes offline → Should show OFFLINE
   - Test: OpenHAB restarts while device online → Should show ONLINE (main fix)

**Files:**
- `src/main/java/org/openhab/binding/openhasp/internal/OpenHASPThingHandler.java`
- `src/main/java/org/openhab/binding/openhasp/internal/OpenHASPCommunicationManager.java`

**Documentation Update:**
- Update `MQTT_BINDING_ANALYSIS.md` with final solution

---

### TASK 2: Implement Comprehensive Error Handling

**Priority:** CRITICAL  
**Issue:** Missing try-catch blocks, poor error messages, no graceful degradation

**Subtasks:**

#### 2.1: Add Try-Catch to MQTT Operations
- Wrap all `connection.publish()` calls in try-catch
- Wrap all `connection.subscribe()` calls in try-catch
- Log errors at appropriate level (error, not trace)
- Update thing status with error message on exception
- Don't let exceptions propagate to framework

**Files:**
- `OpenHASPCommunicationManager.java` (sendHASPCommand method)
- `OpenHASPThingHandler.java` (start method)

#### 2.2: Add Try-Catch to Template Processing
- Wrap template compilation in try-catch
- Wrap template rendering in try-catch
- Provide fallback behavior (use simple JSON without template)
- Log template errors with context
- Continue operation with degraded functionality

**Files:**
- `TemplateProcessor.java` (processTemplate method)
- `OpenHASPLayoutManager.java` (all template usage)

#### 2.3: Add Try-Catch to Sitemap Processing
- Wrap sitemap parsing in try-catch
- Wrap widget processing in try-catch
- Handle recursive group structures gracefully
- Skip invalid widgets, continue with valid ones
- Log skipped widgets with reason

**Files:**
- `OpenHASPLayoutManager.java` (loadFromSiteMap method)
- `OpenHASPLayoutManager.java` (processWidgetList method)

#### 2.4: Add Try-Catch to JSON Parsing
- Wrap all Gson parsing in try-catch
- Validate JSON structure before parsing
- Handle malformed device messages
- Log parsing errors with message content
- Ignore invalid messages, don't crash

**Files:**
- `OpenHASPPlate.java` (plateCallback method)
- `OpenHASPDiscoveryService.java` (receivedMessage method)

#### 2.5: Add Null Safety Checks
- Check all @Nullable fields before use
- Use Optional where appropriate
- Add null checks in widget components
- Validate configuration objects not null
- Add defensive programming throughout

**Files:**
- `OpenHASPPlate.java` (all methods)
- `OpenHASPLayoutManager.java` (all methods)
- All widget component classes

#### 2.6: Improve Error Messages
- Replace technical exceptions with user-friendly messages
- Add specific error causes to thing status updates
- Provide suggestions for fixing errors
- Use thing status detail appropriately:
  - CONFIGURATION_ERROR for config issues
  - COMMUNICATION_ERROR for MQTT issues
  - HANDLER_INITIALIZING_ERROR for startup issues
  - GONE for device disappeared

**Example Improvements:**
```java
// Bad:
updateStatus(ThingStatus.OFFLINE);

// Good:
updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
    "Sitemap 'default' not found. Check sitemap name in configuration.");
```

**Files:** All handler and manager classes

---

### TASK 3: Add Configuration Validation

**Priority:** CRITICAL  
**Issue:** No validation of user inputs, leads to cryptic errors

**Subtasks:**

#### 3.1: Validate Device ID
```java
private boolean validateConfiguration(OpenHASPThingConfiguration config) {
    if (config.deviceId == null || config.deviceId.trim().isEmpty()) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
            "Device ID is required");
        return false;
    }
    
    // Validate format (alphanumeric, underscore, hyphen only)
    if (!config.deviceId.matches("^[a-zA-Z0-9_-]+$")) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
            "Device ID can only contain letters, numbers, underscores, and hyphens");
        return false;
    }
    
    return true;
}
```

#### 3.2: Validate Sitemap Mode Configuration
```java
if ("sitemap".equalsIgnoreCase(config.configMode)) {
    if (config.sitemap == null || config.sitemap.trim().isEmpty()) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
            "Sitemap name is required when using sitemap mode");
        return false;
    }
    
    // Check sitemap exists
    boolean sitemapExists = sitemapProviders.stream()
        .anyMatch(provider -> provider.getSitemap(config.sitemap) != null);
    
    if (!sitemapExists) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
            "Sitemap '" + config.sitemap + "' not found. Available sitemaps: " + 
            getAvailableSitemapNames());
        return false;
    }
}
```

#### 3.3: Validate Manual Mode Configuration
```java
if ("manual".equalsIgnoreCase(config.configMode)) {
    if (config.pages == null || config.pages.length == 0) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
            "Pages configuration is required when using manual mode");
        return false;
    }
    
    // Validate each page JSON
    for (int i = 0; i < config.pages.length; i++) {
        try {
            JsonObject page = JsonParser.parseString(config.pages[i]).getAsJsonObject();
            if (!page.has("page") || !page.has("id") || !page.has("obj")) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Page configuration " + (i+1) + " missing required fields (page, id, obj)");
                return false;
            }
        } catch (JsonSyntaxException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                "Page configuration " + (i+1) + " is not valid JSON");
            return false;
        }
    }
}
```

#### 3.4: Validate Template Path
```java
if ("file".equalsIgnoreCase(config.templatePathType)) {
    if (config.templatePath == null || config.templatePath.isEmpty()) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
            "Template path is required when using file template type");
        return false;
    }
    
    File templateDir = new File(config.templatePath);
    if (!templateDir.exists() || !templateDir.isDirectory()) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
            "Template path does not exist: " + config.templatePath);
        return false;
    }
    
    // Check required files exist
    String[] requiredFiles = {"template.properties", "button.json.hbs", "slider.json.hbs"};
    for (String file : requiredFiles) {
        if (!new File(templateDir, file).exists()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                "Required template file missing: " + file);
            return false;
        }
    }
}
```

#### 3.5: Validate Brightness Values
```java
private boolean validateBrightness(int value, String paramName) {
    if (value < 0 || value > 100) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
            paramName + " must be between 0 and 100 (got: " + value + ")");
        return false;
    }
    return true;
}

if (!validateBrightness(config.backlightHigh, "backlightHigh") ||
    !validateBrightness(config.backlightMedium, "backlightMedium") ||
    !validateBrightness(config.backlightLow, "backlightLow")) {
    return false;
}
```

#### 3.6: Add XML Constraints
In `thing-types.xml`, add validation constraints:
```xml
<parameter name="backlightHigh" type="integer" min="0" max="100" required="false">
    <label>Backlight High</label>
    <description>Brightness when active (0-100%)</description>
    <default>100</default>
</parameter>

<parameter name="deviceId" type="text" required="true" pattern="^[a-zA-Z0-9_-]+$">
    <label>Device ID</label>
    <description>HASP device ID (alphanumeric, underscore, hyphen only)</description>
</parameter>
```

**Files:**
- `OpenHASPThingHandler.java` (add validateConfiguration method)
- `thing-types.xml` (add constraints)

---

### TASK 4: Implement Manual Configuration Mode

**Priority:** HIGH  
**Issue:** Manual mode advertised but not working (TODO in code)

**Current Code (line 84-87 of OpenHASPPlate.java):**
```java
if (OpenHASPBindingConstants.OPENHASP_CONFIGMODE_MANUAL.equalsIgnoreCase(config.configMode)) {
    if (config.pages != null) {
        // TODO
        // objectArray = config.pages;
    }
}
```

**Implementation:**

#### 4.1: Parse Manual Configuration
```java
public void loadFromManualConfig(String[] pageConfigs) {
    logger.info("Loading manual configuration for plate {}", plateId);
    
    List<JsonObject> objects = new ArrayList<>();
    
    for (String pageConfig : pageConfigs) {
        try {
            JsonObject obj = JsonParser.parseString(pageConfig).getAsJsonObject();
            objects.add(obj);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse manual page config: {}", pageConfig, e);
            continue;
        }
    }
    
    // Group objects by page
    Map<Integer, List<JsonObject>> objectsByPage = objects.stream()
        .collect(Collectors.groupingBy(obj -> obj.get("page").getAsInt()));
    
    // Process each page
    for (Map.Entry<Integer, List<JsonObject>> entry : objectsByPage.entrySet()) {
        int pageNum = entry.getKey();
        List<JsonObject> pageObjects = entry.getValue();
        
        logger.debug("Processing page {} with {} objects", pageNum, pageObjects.size());
        
        // Send page initialization if needed
        if (pageNum > 0) {
            comm.sendHASPCommand(CommandType.JSONL, 
                Collections.singletonList("{\"page\":" + pageNum + ",\" prev\":0}"));
        }
        
        // Send all objects for this page
        List<String> commands = pageObjects.stream()
            .map(JsonObject::toString)
            .collect(Collectors.toList());
        
        comm.sendHASPCommand(CommandType.JSONL, commands);
    }
    
    // Create object mappings for manual objects
    createManualObjectMappings(objects);
}
```

#### 4.2: Create Object Mappings
```java
private void createManualObjectMappings(List<JsonObject> objects) {
    for (JsonObject obj : objects) {
        int page = obj.has("page") ? obj.get("page").getAsInt() : 0;
        int id = obj.get("id").getAsInt();
        String objType = obj.get("obj").getAsString();
        String objectId = "p" + page + "b" + id;
        
        // If object has an "item" property, create mapping
        if (obj.has("item")) {
            String itemName = obj.get("item").getAsString();
            Item item = itemRegistry.get(itemName);
            
            if (item != null) {
                IObjItemMapping mapping = createMappingForObjectType(
                    objectId, obj Type, item, obj);
                    
                if (mapping != null) {
                    layoutManager.registerObjectMapping(objectId, mapping);
                    logger.debug("Created mapping: {} -> {}", objectId, itemName);
                }
            } else {
                logger.warn("Item '{}' not found for object {}", itemName, objectId);
            }
        }
    }
}

private IObjItemMapping createMappingForObjectType(String objectId, String objType, 
                                                    Item item, JsonObject config) {
    switch (objType) {
        case "btn":
            return new ButtonMapping(objectId, item, config);
        case "slider":
            return new SliderMapping(objectId, item, config);
        case "dropdown":
            return new SelectionMapping(objectId, item, config);
        // Add other types...
        default:
            logger.warn("Unsupported object type for mapping: {}", objType);
            return null;
    }
}
```

#### 4.3: Update OpenHASPPlate.start()
```java
public void start() {
    logger.info("Initializing plate {}/{}", thingId, plateId);
    if (OpenHASPBindingConstants.OPENHASP_CONFIGMODE_MANUAL.equalsIgnoreCase(config.configMode)) {
        if (config.pages != null && config.pages.length > 0) {
            layoutManager.loadFromManualConfig(config.pages);
        } else {
            logger.error("Manual mode selected but no pages configured");
        }
    } else {
        // Sitemap mode (existing code)
        if (config.sitemap != null) {
            String sitemapName = config.sitemap.trim();
            Sitemap sitemap = findSitemap(sitemapName);
            if (sitemap != null) {
                layoutManager.loadFromSiteMap(sitemap);
            } else {
                logger.warn("Could not find sitemap {}", sitemapName);
            }
        }
    }
}
```

#### 4.4: Add Manual Configuration Examples
In README, document format:
```
Thing mqtt:openhasp_plate:manual "Manual Plate" (mqtt:broker:mybroker) {
    Parameters:
        deviceId="manual01",
        configMode="manual",
        pages=[
            '{  "page":1,"id":1,"obj":"btn","x":10,"y":10,"w":100,"h":50,"text":"Light","item":"LivingRoom_Light"}',
            '{"page":1,"id":2,"obj":"slider","x":10,"y":70,"w":200,"h":30,"min":0,"max":100,"item":"LivingRoom_Dimmer"}'
        ]
}
```

**Files:**
- `OpenHASPPlate.java` (remove TODO, add loadFromManualConfig call)
- `OpenHASPLayoutManager.java` (add loadFromManualConfig method)
- `README.md` (add manual mode documentation)

---

### TASK 5: Implement Offline Command Checking

**Priority:** HIGH  
**Issue:** Commands sent even when device offline (TODOs at lines 57, 222)

**Implementation:**

#### 5.1: Track Device Online State
```java
// In OpenHASPThingHandler:
private volatile boolean deviceOnline = false;

@Override
public void connectionObserved(MqttConnectionState state) {
    if (state == MqttConnectionState.DISCONNECTED) {
        deviceOnline = false;
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
    }
}

// Update when LWT received:
private void handleLWTMessage(String payload) {
    boolean wasOnline = deviceOnline;
    deviceOnline = "online".equals(payload);
    
    if (deviceOnline && !wasOnline) {
        updateStatus(ThingStatus.ONLINE);
        if (plate != null) {
            plate.onLine();
        }
    } else if (!deviceOnline && wasOnline) {
        updateStatus(ThingStatus.OFFLINE);
        if (plate != null) {
            plate.offLine();
        }
    }
}
```

#### 5.2: Check Before Sending Commands
```java
// In OpenHASPCommunicationManager:
private volatile boolean deviceOnline = true; // Assume online initially

public void setDeviceOnline(boolean online) {
    this.deviceOnline = online;
}

public void sendHASPCommand(CommandType type, String command) {
    sendHASPCommand(type, Arrays.asList(command));
}

public void sendHASPCommand(CommandType type, List<String> commands) {
    if (!deviceOnline) {
        logger.debug("Device {} offline, queueing {} commands", plateId, commands.size());
        queueCommands(commands);
        return;
    }
    
    try {
        // ... existing send logic ...
    } catch (Exception e) {
        logger.error("Failed to send commands to device {}: {}", plateId, e.getMessage());
    }
}
```

#### 5.3: Implement Command Queue
```java
// In OpenHASPPlate or OpenHASPCommunicationManager:
private final Queue<QueuedCommand> pendingCommands = new ConcurrentLinkedQueue<>();
private final int maxQueueSize = 100; // Make configurable

private static class QueuedCommand {
    final CommandType type;
    final List<String> commands;
    final long timestamp;
    
    QueuedCommand(CommandType type, List<String> commands) {
        this.type = type;
        this.commands = commands;
        this.timestamp = System.currentTimeMillis();
    }
}

private void queueCommands(CommandType type, List<String> commands) {
    if (pendingCommands.size() >= maxQueueSize) {
        QueuedCommand dropped = pendingCommands.poll();
        logger.warn("Command queue full, dropped oldest command for device {}", plateId);
    }
    
    pendingCommands.offer(new QueuedCommand(type, commands));
}

public void onLine() {
    logger.info("Device {} came online, sending {} queued commands", 
        plateId, pendingCommands.size());
    
    while (!pendingCommands.isEmpty()) {
        QueuedCommand cmd = pendingCommands.poll();
        
        // Skip very old commands (older than 5 minutes)
        if (System.currentTimeMillis() - cmd.timestamp > 300000) {
            logger.debug("Skipping stale queued command");
            continue;
        }
        
        comm.sendHASPCommand(cmd.type, cmd.commands);
    }
    
    // Refresh current state
    layoutManager.updateAllStates();
}
```

#### 5.4: Add Configuration for Queue Size
In `thing-types.xml`:
```xml
<parameter name="commandQueueSize" type="integer" min="0" max="1000" required="false">
    <label>Command Queue Size</label>
    <description>Maximum number of commands to queue when device offline (0 to disable queuing)</description>
    <default>100</default>
    <advanced>true</advanced>
</parameter>
```

**Files:**
- `OpenHASPThingHandler.java` (track online state)
- `OpenHASPCommunicationManager.java` (check before send, remove TODO line 57)
- `OpenHASPPlate.java` (command queue, remove TODO line 222)
- `thing-types.xml` (add queue size parameter)
- `OpenHASPThingConfiguration.java` (add commandQueueSize field)

---

## HIGH PRIORITY TASKS

### TASK 6: Create Unit Test Infrastructure

**Priority:** HIGH  
**Issue:** Zero test coverage blocks safe development

#### 6.1: Add Test Dependencies
In `pom.xml`, add:
```xml
<dependencies>
    <!-- Existing dependencies... -->
    
    <!-- Test dependencies -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.22.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

#### 6.2: Create Base Test Class
File: `src/test/java/org/openhab/binding/openhasp/internal/BaseOpenHASPTest.java`
```java
@ExtendWith(MockitoExtension.class)
public abstract class BaseOpenHASPTest {
    
    @Mock
    protected MqttBrokerConnection connection;
    
    @Mock
    protected ItemRegistry itemRegistry;
    
    @Mock
    protected ItemUIRegistry itemUIRegistry;
    
    @Mock
    protected SitemapProvider sitemapProvider;
    
    protected OpenHASPThingConfiguration createTestConfig() {
        OpenHASPThingConfiguration config = new OpenHASPThingConfiguration();
        config.deviceId = "test01";
        config.configMode = "sitemap";
        config.sitemap = "test";
        config.backlightHigh = 100;
        config.backlightMedium = 50;
        config.backlightLow = 20;
        return config;
    }
    
    protected Item mockSwitchItem(String name) {
        SwitchItem item = mock(SwitchItem.class);
        when(item.getName()).thenReturn(name);
        when(item.getState()).thenReturn(OnOffType.OFF);
        return item;
    }
    
    protected Item mockDimmerItem(String name) {
        DimmerItem item = mock(DimmerItem.class);
        when(item.getName()).thenReturn(name);
        when(item.getState()).thenReturn(new PercentType(50));
        return item;
    }
}
```

#### 6.3: Test Template Processor
File: `src/test/java/org/openhab/binding/openhasp/internal/layout/TemplateProcessorTest.java`
```java
class TemplateProcessorTest extends BaseOpenHASPTest {
    
    private TemplateProcessor processor;
    
    @BeforeEach
    void setUp() {
        processor = new TemplateProcessor("/test-templates/", false);
    }
    
    @Test
    void testLoadTemplateFromClasspath() {
        assertThat(processor).isNotNull();
    }
    
    @Test
    void testRenderButton() throws IOException {
        Map<String, String> context = new HashMap<>();
        context.put("id", "1");
        context.put("page", "1");
        context.put("text", "Test Button");
        context.put("x", "10");
        context.put("y", "10");
        
        List<String> result = processor.processTemplate("button", context);
        
        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).contains("\"obj\":\"btn\"");
        assertThat(result.get(0)).contains("\"text\":\"Test Button\"");
    }
    
    @Test
    void testMathHelper() throws IOException {
        Map<String, String> context = new HashMap<>();
        context.put("value", "10");
        
        // Template: {{math value "+" 5}}
        List<String> result = processor.processTemplate("test-math", context);
        
        assertThat(result.get(0)).contains("15");
    }
    
    @Test
    void testMissingTemplate() {
        assertThatThrownBy(() -> processor.processTemplate("nonexistent", new HashMap<>()))
            .isInstanceOf(IOException.class);
    }
}
```

#### 6.4: Test Widget Components
File: `src/test/java/org/openhab/binding/openhasp/internal/layout/components/ButtonWidgetTest.java`
```java
class ButtonWidgetTest extends BaseOpenHASPTest {
    
    @Test
    void testButtonCreation() {
        SwitchItem item = (SwitchItem) mockSwitchItem("TestSwitch");
        
        ButtonWidget button = new ButtonWidget(1, 1);
        button.setItem(item);
        button.setLabel("Test");
        
        assertThat(button.getItem()).isEqualTo(item);
        assertThat(button.getLabel()).isEqualTo("Test");
    }
    
    @Test
    void testButtonStateOff() {
        SwitchItem item = (SwitchItem) mockSwitchItem("TestSwitch");
        when(item.getState()).thenReturn(OnOffType.OFF);
        
        ButtonWidget button = new ButtonWidget(1, 1);
        button.setItem(item);
        
        String json = button.renderState();
        
        assertThat(json).contains("\"val\":0");
    }
    
    @Test
    void testButtonStateOn() {
        SwitchItem item = (SwitchItem) mockSwitchItem("TestSwitch");
        when(item.getState()).thenReturn(OnOffType.ON);
        
        ButtonWidget button = new ButtonWidget(1, 1);
        button.setItem(item);
        
        String json = button.renderState();
        
        assertThat(json).contains("\"val\":1");
    }
}
```

#### 6.5: Test Object Item Mapper
File: `src/test/java/org/openhab/binding/openhasp/internal/mapping/ObjItemMapperTest.java`
```java
class ObjItemMapperTest extends BaseOpenHASPTest {
    
    private ObjItemMapper mapper;
    
    @BeforeEach
    void setUp() {
        mapper = new ObjItemMapper();
    }
    
    @Test
    void testRegisterMapping() {
        Item item = mockSwitchItem("TestSwitch");
        IObjItemMapping mapping = mock(IObjItemMapping.class);
        when(mapping.getItem()).thenReturn(item);
        
        mapper.register("p1b1", mapping);
        
        assertThat(mapper.getByObject("p1b1")).isEqualTo(mapping);
        assertThat(mapper.getByItem("TestSwitch")).isEqualTo(mapping);
    }
    
    @Test
    void testGetNonexistent() {
        assertThat(mapper.getByObject("p9b9")).isNull();
        assertThat(mapper.getByItem("NonexistentItem")).isNull();
    }
    
    @Test
    void testCaseSensitivity() {
        Item item = mockSwitchItem("TestSwitch");
        IObjItemMapping mapping = mock(IObjItemMapping.class);
        when(mapping.getItem()).thenReturn(item);
        
        mapper.register("p1b1", mapping);
        
        assertThat(mapper.getByObject("P1B1")).isNull(); // Case sensitive
    }
}
```

#### 6.6: Test Discovery Service
File: `src/test/java/org/openhab/binding/openhasp/internal/discovery/OpenHASPDiscoveryServiceTest.java`
```java
class OpenHASPDiscoveryServiceTest {
    
    @Mock
    private MQTTTopicDiscoveryService discoveryService;
    
    @Mock
    private MqttBrokerConnection connection;
    
    private OpenHASPDiscoveryService service;
    private ThingUID bridgeUID;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new OpenHASPDiscoveryService(discoveryService);
        bridgeUID = new ThingUID("mqtt:broker:testBroker");
    }
    
    @Test
    void testDiscoveryMessage() {
        String topic = "hasp/discovery/plate01";
        String payload = "{\"node\":\"plate01\",\"model\":\"WT32-SC01\"}";
        
        service.receivedMessage(bridgeUID, connection, topic, payload.getBytes());
        
        // Verify thing discovered
        // This would require capturing the thingDiscovered() call
    }
    
    @Test
    void testSanitizePlateName() {
        assertThat(OpenHASPDiscoveryService.sanitizePlateName("plate-01"))
            .isEqualTo("plate-01");
        assertThat(OpenHASPDiscoveryService.sanitizePlateName("plate 01"))
            .isEqualTo("plate01");
        assertThat(OpenHASPDiscoveryService.sanitizePlateName("plate@#$01"))
            .isEqualTo("plate01");
    }
    
    @Test
    void testInvalidJson() {
        String topic = "hasp/discovery/plate01";
        String payload = "not json";
        
        // Should not throw exception
        assertThatCode(() -> 
            service.receivedMessage(bridgeUID, connection, topic, payload.getBytes())
        ).doesNotThrowAnyException();
    }
}
```

#### 6.7: Add Test Resources
Create test templates in `src/test/resources/test-templates/`:
- `button.hbs`
- `slider.hbs`
- `test-math.hbs`
- `template.properties`

#### 6.8: Configure Code Coverage
In `pom.xml`, add Jacoco:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.8</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>BUNDLE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.70</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Files to create:**
- `src/test/java/org/openhab/binding/openhasp/internal/BaseOpenHASPTest.java`
- `src/test/java/org/openhab/binding/openhasp/internal/layout/TemplateProcessorTest.java`
- `src/test/java/org/openhab/binding/openhasp/internal/layout/components/ButtonWidgetTest.java`
- `src/test/java/org/openhab/binding/openhasp/internal/mapping/ObjItemMapperTest.java`
- `src/test/java/org/openhab/binding/openhasp/internal/discovery/OpenHASPDiscoveryServiceTest.java`
- `src/test/resources/test-templates/*.hbs`
- Update `pom.xml`

---

### TASK 7: Complete Internationalization

**Priority:** MEDIUM  
**Issue:** Property file contains FIXME placeholders

**Implementation:**

Update `src/main/resources/OH-INF/i18n/openhasp.properties`:
```properties
# Binding
binding.openhasp.name = OpenHASP Binding
binding.openhasp.description = Integrates OpenHASP touch panels via MQTT

# Thing Types
thing-type.mqtt.openhasp_plate.label = OpenHASP Plate
thing-type.mqtt.openhasp_plate.description = An OpenHASP touch screen control panel

# Thing Configuration
thing-type.config.mqtt.openhasp_plate.deviceId.label = Device ID
thing-type.config.mqtt.openhasp_plate.deviceId.description = HASP device ID used in MQTT topics (e.g., "plate01")

thing-type.config.mqtt.openhasp_plate.configMode.label = Configuration Mode
thing-type.config.mqtt.openhasp_plate.configMode.description = Use "sitemap" to generate layout from OpenHAB sitemap, or "manual" for custom configuration

thing-type.config.mqtt.openhasp_plate.sitemap.label = Sitemap Name
thing-type.config.mqtt.openhasp_plate.sitemap.description = Name of the sitemap to display (comma-separated for multiple)

thing-type.config.mqtt.openhasp_plate.templatePathType.label = Template Source
thing-type.config.mqtt.openhasp_plate.templatePathType.description = Load templates from classpath or filesystem

thing-type.config.mqtt.openhasp_plate.templatePath.label = Template Path
thing-type.config.mqtt.openhasp_plate.templatePath.description = Path to custom templates (for file template source)

thing-type.config.mqtt.openhasp_plate.pages.label = Manual Configuration
thing-type.config.mqtt.openhasp_plate.pages.description = JSON configuration for manual mode (array of object definitions)

thing-type.config.mqtt.openhasp_plate.hostname.label = Hostname
thing-type.config.mqtt.openhasp_plate.hostname.description = IP address or hostname of the device

thing-type.config.mqtt.openhasp_plate.password.label = Password
thing-type.config.mqtt.openhasp_plate.password.description = Device access password (if configured)

thing-type.config.mqtt.openhasp_plate.backlightHigh.label = Backlight High
thing-type.config.mqtt.openhasp_plate.backlightHigh.description = Brightness when device is active (0-100%)

thing-type.config.mqtt.openhasp_plate.backlightMedium.label = Backlight Medium
thing-type.config.mqtt.openhasp_plate.backlightMedium.description = Brightness when device is idle for short period (0-100%)

thing-type.config.mqtt.openhasp_plate.backlightLow.label = Backlight Low
thing-type.config.mqtt.openhasp_plate.backlightLow.description = Brightness when device is idle for long period (0-100%)

# Channel Types
channel-type.mqtt.openhasp-backlight.label = Display Backlight
channel-type.mqtt.openhasp-backlight.description = Controls the display brightness (0-100%)

channel-type.mqtt.openhasp-lwt.label = Online Status
channel-type.mqtt.openhasp-lwt.description = Device availability status (online/offline)

channel-type.mqtt.openhasp-button.label = HASP Button
channel-type.mqtt.openhasp-button.description = Manual button channel for custom configuration
```

Update `binding.xml`:
```xml
<name>OpenHASP Binding</name>
<description>This binding integrates OpenHASP touch panels with OpenHAB via MQTT</description>
<author>Alvaro Lobato</author>
```

**Files:**
- `src/main/resources/OH-INF/i18n/openhasp.properties`
- `src/main/resources/OH-INF/binding/binding.xml`

---

### TASK 8: Improve Code Quality

**Priority:** MEDIUM  
**Issue:** Dead code, inconsistent style, long methods

#### 8.1: Remove Dead Code
```bash
# Delete old template files
rm src/main/resources/templates/default-portrait/button_on.json.hbs_old
rm src/main/resources/templates/default-portrait/button_off.json.hbs_old
rm src/main/resources/templates/default-portrait/slider.json.hbs_old
rm src/main/resources/templates/default-portrait/selection.json.hbs_old
```

Remove commented code blocks:
- Lines 138-148 in `OpenHASPPlate.java` (commented idle handling - move to Task 9)
- Unused import statements throughout
- Unused constants in `OpenHASPBindingConstants.java`

#### 8.2: Apply Code Formatting
```bash
mvn spotless:apply
```

#### 8.3: Fix Log Levels
Review and fix inappropriate log levels:
- Change debug stack traces to proper error logging
- Remove trace logging in production paths
- Use appropriate levels: ERROR for errors, WARN for warnings, INFO for important events, DEBUG for debugging, TRACE for detailed tracing

**Examples to fix:**
```java
// Bad:
logger.trace("caller", new Exception());

// Remove this entirely

// Bad:
logger.trace("Error processing event value: {}", value);

// Good:
logger.error("Error processing event value: {}", value);
```

#### 8.4: Extract Constants
```java
// In OpenHASPBindingConstants or new ConfigurationDefaults class:
public class ConfigurationDefaults {
    public static final int DEFAULT_BACKLIGHT_HIGH = 100;
    public static final int DEFAULT_BACKLIGHT_MEDIUM = 50;
    public static final int DEFAULT_BACKLIGHT_LOW = 20;
    public static final int DEFAULT_COMMAND_QUEUE_SIZE = 100;
    public static final int MAX_JSON_COMMAND_SIZE = 400;
    public static final String DEFAULT_TEMPLATE_PATH = "/templates/default-portrait/";
    public static final int DEVICE_STATUS_TIMEOUT_MS = 5000;
}
```

Replace magic numbers throughout code with these constants.

#### 8.5: Add Missing Javadoc
Add Javadoc to all public methods. Example:
```java
/**
 * Sends a command to the OpenHASP device via MQTT.
 * 
 * @param type The command type (CMD, JSON, or JSONL)
 * @param command The command string to send
 * @throws IllegalArgumentException if type or command is null
 */
public void sendHASPCommand(CommandType type, String command) {
    // ...
}
```

Focus on:
- All public methods in handler classes
- All public methods in component classes
- All interface methods
- Complex private methods

#### 8.6: Fix String Operations
Replace string concatenation in loops with StringBuilder:
```java
// Bad (in OpenHASPCommunicationManager.java):
StringBuffer jsonCommand = new StringBuffer();
for (String command : commands) {
    jsonCommand.append(command);
}

// Good:
StringBuilder jsonCommand = new StringBuilder(commands.size() * 50); // Pre-size
for (String command : commands) {
    jsonCommand.append(command);
}
```

#### 8.7: Reduce Method Complexity
Split long methods:
- `OpenHASPLayoutManager.loadFromSiteMap()` (300+ lines) → Extract helper methods
- `OpenHASPLayoutManager.processWidgetList()` → Extract per-widget-type handlers
- `OpenHASPThingHandler.start()` → Extract validation, initialization phases

**Files:** All Java files

---

## MEDIUM PRIORITY TASKS

### TASK 9: Implement Backlight Automation

**Priority:** MEDIUM  
**Issue:** Idle/dim functionality commented out

**Implementation:**

#### 9.1: Uncomment and Fix Idle Constants
In `OpenHASPBindingConstants.java`:
```java
public static final String HASP_STATE_IDLE_TOPIC = "idle";
public static final String HASP_STATE_IDLE_SHORT_VALUE = "short";
public static final String HASP_STATE_IDLE_LONG_VALUE = "long";
public static final String HASP_STATE_IDLE_OFF_VALUE = "off";
```

#### 9.2: Implement Idle Handling
In `OpenHASPPlate.java`, replace commented code (lines 138-148):
```java
public void plateCallback(String strippedTopic, String value) {
    if (strippedTopic.startsWith(OpenHASPBindingConstants.HASP_STATE_TOPIC)) {
        strippedTopic = strippedTopic.substring(OpenHASPBindingConstants.HASP_STATE_TOPIC.length() + 1);
        logger.trace("Plate {} state {}:{}", plateId, strippedTopic, value);
        
        if (OpenHASPBindingConstants.HASP_STATE_IDLE_TOPIC.equals(strippedTopic)) {
            handleIdleStateChange(value);
        } else if (strippedTopic.matches("^[a-zA-Z]+[0-9]+[a-zA-Z]+[0-9]+$")) {
            // Object event handling (existing code)
        }
    }
}

private void handleIdleStateChange(String idleState) {
    int brightness;
    
    switch (idleState) {
        case OpenHASPBindingConstants.HASP_STATE_IDLE_OFF_VALUE:
            brightness = config.backlightHigh;
            logger.debug("Plate {} active, brightness {}", plateId, brightness);
            break;
        case OpenHASPBindingConstants.HASP_STATE_IDLE_SHORT_VALUE:
            brightness = config.backlightMedium;
            logger.debug("Plate {} idle short, brightness {}", plateId, brightness);
            break;
        case OpenHASPBindingConstants.HASP_STATE_IDLE_LONG_VALUE:
            brightness = config.backlightLow;
            logger.debug("Plate {} idle long, brightness {}", plateId, brightness);
            break;
        default:
            logger.warn("Unknown idle state for plate {}: {}", plateId, idleState);
            return;
    }
    
    String command = "backlight {\"state\":\"ON\",\"brightness\":" + brightness + "}";
    comm.sendHASPCommand(CommandType.CMD, command);
}
```

#### 9.3: Add Idle Timeout Configuration
In `thing-types.xml`:
```xml
<parameter name="idleTimeoutShort" type="integer" unit="s" min="0" max="3600" required="false">
    <label>Idle Timeout Short</label>
    <description>Seconds before dimming to medium brightness (0 to disable)</description>
    <default>30</default>
    <advanced>true</advanced>
</parameter>

<parameter name="idleTimeoutLong" type="integer" unit="s" min="0" max="3600" required="false">
    <label>Idle Timeout Long</label>
    <description>Seconds before dimming to low brightness (0 to disable)</description>
    <default>120</default>
    <advanced>true</advanced>
</parameter>
```

Note: These timeouts are configured on the device itself, not in the binding. Document this in README.

**Files:**
- `OpenHASPBindingConstants.java` (uncomment constants)
- `OpenHASPPlate.java` (uncomment and fix idle handling)
- `thing-types.xml` (add timeout parameters documentation)
- `README.md` (document backlight automation)

---

### TASK 10: Add Diagnostic Channels

**Priority:** MEDIUM  
**Issue:** No visibility into device health/status

**Implementation:**

#### 10.1: Add Channels to thing-types.xml
```xml
<channels>
    <channel id="backlight" typeId="backlight"/>
    <channel id="lwt" typeId="lwt"/>
    <channel id="haspbutton" typeId="hasp-button"/>
    
    <!-- Diagnostic channels -->
    <channel id="firmware" typeId="firmware"/>
    <channel id="uptime" typeId="uptime"/>
    <channel id="lastSeen" typeId="last-seen"/>
    <channel id="messagesSent" typeId="messages-sent"/>
    <channel id="messagesReceived" typeId="messages-received"/>
</channels>

<!-- Channel type definitions -->
<channel-type id="firmware">
    <item-type>String</item-type>
    <label>Firmware Version</label>
    <description>OpenHASP firmware version</description>
    <state readOnly="true"/>
</channel-type>

<channel-type id="uptime">
    <item-type>Number:Time</item-type>
    <label>Uptime</label>
    <description>Device uptime in seconds</description>
    <state readOnly="true" pattern="%d s"/>
</channel-type>

<channel-type id="last-seen">
    <item-type>DateTime</item-type>
    <label>Last Seen</label>
    <description>Last communication timestamp</description>
    <state readOnly="true"/>
</channel-type>

<channel-type id="messages-sent">
    <item-type>Number</item-type>
    <label>Messages Sent</label>
    <description>Number of MQTT messages sent to device</description>
    <state readOnly="true"/>
</channel-type>

<channel-type id="messages-received">
    <item-type>Number</item-type>
    <label>Messages Received</label>
    <description>Number of MQTT messages received from device</description>
    <state readOnly="true"/>
</channel-type>
```

#### 10.2: Implement Statistics Tracking
In `OpenHASPCommunicationManager.java`:
```java
private final AtomicLong messagesSent = new AtomicLong(0);
private final AtomicLong messagesReceived = new AtomicLong(0);
private volatile Instant lastSeenTimestamp = Instant.now();

public void sendHASPCommand(CommandType type, List<String> commands) {
    // ... existing code ...
    messagesSent.incrementAndGet();
    lastSeenTimestamp = Instant.now();
}

@Override
public void processMessage(String topic, byte[] payload) {
    messagesReceived.incrementAndGet();
    lastSeenTimestamp = Instant.now();
    // ... existing code ...
}

public long getMessagesSent() {
    return messagesSent.get();
}

public long getMessagesReceived() {
    return messagesReceived.get();
}

public Instant getLastSeen() {
    return lastSeenTimestamp;
}
```

#### 10.3: Query Device for Status
Send periodic status query to device:
```java
// In OpenHASPThingHandler, schedule periodic update:
private @Nullable ScheduledFuture<?> statusQueryJob;

@Override
protected CompletableFuture<@Nullable Void> start(MqttBrokerConnection connection) {
    // ... existing code ...
    
    // Query device status every 60 seconds
    statusQueryJob = scheduler.scheduleWithFixedDelay(
        this::queryDeviceStatus, 10, 60, TimeUnit.SECONDS);
    
    return super.start(connection);
}

private void queryDeviceStatus() {
    if (comm != null) {
        comm.sendHASPCommand(CommandType.CMD, "statusupdate");
    }
}

@Override
public void dispose() {
    if (statusQueryJob != null) {
        statusQueryJob.cancel(true);
        statusQueryJob = null;
    }
    super.dispose();
}
```

#### 10.4: Parse Status Response
openHASP sends status on `hasp/{device}/state/statusupdate`:
```json
{
    "node": "plate01",
    "version": "0.7.0-rc13",
    "uptime": 86420,
    "page": 1,
    ...
}
```

Parse and update channels:
```java
// In OpenHASPPlate.plateCallback():
if ("statusupdate".equals(strippedTopic)) {
    handleStatusUpdate(value);
}

private void handleStatusUpdate(String json) {
    try {
        JsonObject status = JsonParser.parseString(json).getAsJsonObject();
        
        if (status.has("version")) {
            String firmware = status.get("version").getAsString();
            updateChannel("firmware", new StringType(firmware));
        }
        
        if (status.has("uptime")) {
            long uptime = status.get("uptime").getAsLong();
            updateChannel("uptime", new QuantityType<>(uptime, Units.SECOND));
        }
        
        // Update last seen
        updateChannel("lastSeen", new DateTimeType());
        
        // Update counters
        updateChannel("messagesSent", new DecimalType(comm.getMessagesSent()));
        updateChannel("messagesReceived", new DecimalType(comm.getMessagesReceived()));
        
    } catch (JsonSyntaxException e) {
        logger.warn("Failed to parse status update: {}", json);
    }
}

private void updateChannel(String channelId, State state) {
    updateState(new ChannelUID(thing.getUID(), channelId), state);
}
```

**Files:**
- `thing-types.xml` (add channel definitions)
- `OpenHASPCommunicationManager.java` (add statistics tracking)
- `OpenHASPThingHandler.java` (add periodic query, create channels)
- `OpenHASPPlate.java` (parse status response)

---

### TASK 11: Optimize Performance

**Priority:** LOW  
**Issue:** No caching, inefficient operations

#### 11.1: Cache Compiled Templates
In `TemplateProcessor.java`:
```java
private final Map<String, Template> templateCache = new ConcurrentHashMap<>();

public List<String> processTemplate(String name, Map<String, String> context) throws IOException {
    Template template = templateCache.computeIfAbsent(name, n -> {
        try {
            return handlebars.compile(n);
        } catch (IOException e) {
            logger.error("Failed to compile template: {}", n, e);
            return null;
        }
    });
    
    if (template == null) {
        throw new IOException("Template not found or failed to compile: " + name);
    }
    
    String renderedJson = template.apply(context);
    // ... rest of method ...
}

public void clearCache() {
    templateCache.clear();
}
```

#### 11.2: Pre-size Collections
```java
// When size is known:
ArrayList<String> commands = new ArrayList<>(pageObjects.size());

// For StringBuilder:
StringBuilder json = new StringBuilder(commands.size() * 100); // Estimate size
```

#### 11.3: Use EnumMap for Enum Keys
If using widget type as key:
```java
Map<WidgetType, WidgetFactory> factories = new EnumMap<>(WidgetType.class);
```

#### 11.4: Move Layout Generation Off MQTT Thread
In `OpenHASPThingHandler.java`:
```java
private final ExecutorService layoutExecutor = Executors.newSingleThreadExecutor(
    new NamedThreadFactory("openhasp-layout"));

protected CompletableFuture<@Nullable Void> start(MqttBrokerConnection connection) {
    // ... existing code ...
    
    if (plate != null) {
        CompletableFuture.runAsync(() -> {
            try {
                plate.start();
                plate.refresh();
            } catch (Exception e) {
                logger.error("Error starting plate", e);
            }
        }, layoutExecutor);
    }
    
    return super.start(connection);
}

@Override
public void dispose() {
    layoutExecutor.shutdown();
    try {
        if (!layoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            layoutExecutor.shutdownNow();
        }
    } catch (InterruptedException e) {
        layoutExecutor.shutdownNow();
        Thread.currentThread().interrupt();
    }
    super.dispose();
}
```

**Files:**
- `TemplateProcessor.java` (add template caching)
- `OpenHASPLayoutManager.java` (pre-size collections)
- `OpenHASPThingHandler.java` (async layout generation)

---

## LOW PRIORITY TASKS

### TASK 12: Add Landscape Template

**Priority:** LOW  
**Enhancement:** Support landscape orientation

Create `src/main/resources/templates/default-landscape/` with:
- Copy all `.hbs` files from default-portrait
- Update `template.properties`:
  ```properties
  orientation=landscape
  maxy=320
  maxx=480
  # Adjust other dimensions for landscape
  ```
- Adjust widget dimensions in templates for horizontal layout
- Test with landscape device

**Files:** New directory `src/main/resources/templates/default-landscape/`

---

### TASK 13: Implement Additional Widget Types

**Priority:** LOW  
**Enhancement:** Support more openHASP object types

openHASP supports many object types not yet supported:
- `arc` - Arc/circular progress
- `bar` - Progress bar
- `chart` - Line chart
- `checkbox` - Checkbox
- `cpicker` - Color picker
- `dropdown` - Dropdown list (partially implemented)
- `gauge` - Gauge meter
- `img` - Image
- `led` - LED indicator
- `linemeter` - Line meter
- `msgbox` - Message box
- `qrcode` - QR code
- `roller` - Roller selector
- `spinner` - Loading spinner

For each new widget type:
1. Create widget component class (e.g., `ChartWidget.java`)
2. Create Handlebars template (e.g., `chart.json.hbs`)
3. Add mapping in `OpenHASPLayoutManager`
4. Add to sitemap widget processing
5. Test with real device

**Example: Chart Widget**
```java
public class ChartWidget extends AbstractComponent implements IComponent {
    private Item item;
    private int seriesCount = 1;
    
    public ChartWidget(int page, int id) {
        super(page, id);
    }
    
    @Override
    public String getTemplate() {
        return "chart";
    }
    
    public void addDataPoint(double value) {
        // Format: p1b5.add_point 42
        String cmd = String.format("p%db%d.add_point %.2f", page, id, value);
        // Send command...
    }
}
```

**Files:** New widget classes, new template files

---

### TASK 14: Add Multi-page Navigation UI

**Priority:** LOW  
**Enhancement:** Automatic page tabs/navigation

Generate navigation buttons automatically:
```java
public void generatePageNavigation() {
    int pageCount = pages.size();
    if (pageCount <= 1) return;
    
    int buttonWidth = 320 / pageCount;
    
    for (int i = 0; i < pageCount; i++) {
        Map<String, String> context = new HashMap<>();
        context.put("page", "0"); // Navigation on page 0
        context.put("id", String.valueOf(10 + i));
        context.put("obj", "btn");
        context.put("x", String.valueOf(i * buttonWidth));
        context.put("y", "0");
        context.put("w", String.valueOf(buttonWidth));
        context.put("h", "30");
        context.put("text", "Page " + (i + 1));
        context.put("action", "{\"down\":\"page " + (i + 1) + "\"}");
        
        // Generate button JSON...
    }
}
```

**Files:** `OpenHASPLayoutManager.java`

---

### TASK 15: Add Custom Icon Support

**Priority:** LOW  
**Enhancement:** User-specified icons

Allow users to specify icons in sitemap:
```
Switch item=Light label="Living Room [%s]" icon="lightbulb"
```

Map icon names to Font Awesome or Material Design Icons:
```java
private String getIconCode(String iconName) {
    Map<String, String> iconMap = Map.of(
        "light", "\uE335",
        "lightbulb", "\uE335",
        "switch", "\uE8AC",
        "temperature", "\uE1FF",
        "humidity", "\uE3FE"
        // ... more mappings
    );
    
    return iconMap.getOrDefault(iconName, "\uE156"); // Default icon
}
```

Update templates to use icon codes.

**Files:** 
- `OpenHASPLayoutManager.java` (icon mapping)
- Widget templates (use icon codes)
- `template.properties` (configure icon font)

---

## CODE CLEANUP TASKS

### TASK 16: Remove All TODO Comments

**Location**: Multiple files  
**Action**: Resolve or remove TODO comments

**TODOs to address:**

1. `OpenHASPThingHandler.java:115` - Review log levels (part of Task 8)
2. `OpenHASPThingHandler.java:126` - Check config changes handling → Add note that recreate is current behavior
3. `OpenHASPThingHandler.java:135` - Check plate object exists → Add null check and log warning
4. `OpenHASPThingHandler.java:140` - Move event registration → Evaluate if change needed
5. `OpenHASPThingHandler.java:363` - Review unregister location → Timing is correct
6. `OpenHASPThingHandler.java:370-378` - Review observer methods → Mark as reviewed
7. `OpenHASPPlate.java:83` - Manual mode → Covered in Task 4
8. `OpenHASPPlate.java:88` - Sitemap subscription → Add note about automatic subscription via EventSubscriber
9. `OpenHASPPlate.java:162` - Identify plate → Not needed, plateId already known
10. `OpenHASPPlate.java:222` - Don't send if offline → Covered in Task 5
11. `OpenHaspService.java:36` - Single instance → Verify with @Component annotation
12. `OpenHASPCommunicationManager.java:57` - Don't send if offline → Covered in Task 5
13. `OpenHASPLayoutManager.java:77` - ItemUIRegistryImpl → Add reference link, not urgent
14. `OpenHASPLayoutManager.java:102` - Replace objectArray → Part of refactoring
15. `OpenHASPLayoutManager.java:112` - Move to properties → Enhancement for later
16. `OpenHASPLayoutManager.java:124` - Remove object processing → Part of refactoring
17. `OpenHASPLayoutManager.java:261, 340` - Handle recursive groups → Add safeguard
18. `OpenHASPLayoutManager.java:315` - Implement components → Already implemented
19. `OpenHASPLayoutManager.java:358` - Review LinkableWidget → Mark as reviewed

**Actions:**
- Remove TODO or replace with // NOTE: for future enhancements
- Add ticket references if deferring
- Document decisions in code comments

---

### TASK 17: Update README Documentation

**Priority:** HIGH  
**Issue:** README incomplete, needs examples

**Additions Needed:**

#### 17.1: Add Prerequisites Section
```markdown
## Prerequisites

- OpenHAB 4.x or later
- MQTT broker (Mosquitto, HiveMQ, etc.)
- OpenHASP device (ESP32 with display running openHASP firmware)
- Basic knowledge of OpenHAB sitemaps
```

#### 17.2: Expand Getting Started
Add step-by-step tutorial with screenshots:
1. Install MQTT broker
2. Configure MQTT broker in OpenHAB
3. Flash OpenHASP firmware to device
4. Configure device to connect to MQTT
5. Discover device in OpenHAB inbox
6. Create sitemap
7. Configure thing with sitemap
8. Verify layout on device

#### 17.3: Add Troubleshooting Section
```markdown
## Troubleshooting

### Device Shows OFFLINE After OpenHAB Restart

**Symptom**: Thing status shows OFFLINE even though device is online.

**Solution**: 
1. Check OpenHASP device is sending LWT messages
2. Verify MQTT broker is running
3. Check MQTT broker logs for connection issues
4. Restart OpenHAS device after OpenHAB starts
5. Check OpenHAB logs for errors: `log:tail org.openhab.binding.openhasp`

### Layout Not Updating

**Symptom**: Changes to sitemap don't appear on device.

**Solution**:
1. Check sitemap syntax is correct
2. Verify thing is ONLINE
3. Check device is receiving MQTT messages
4. Review OpenHAB logs for JSON errors
5. Try manual refresh by restarting thing

### Controls Not Responding

**Symptom**: Touching controls doesn't trigger items.

**Solution**:
1. Verify items are linked in sitemap
2. Check MQTT message flow with MQTT explorer
3. Review device logs (via web interface)
4. Confirm object IDs match in mapping
```

#### 17.4: Add Manual Mode Documentation
```markdown
## Manual Configuration Mode

For advanced users who want direct control over the layout:

```things
Thing mqtt:openhasp_plate:manual "Manual Plate" (mqtt:broker:mybroker) {
    Parameters:
        deviceId="manual01",
        configMode="manual",
        pages=[
            '{"page":1,"id":1...}',
            '{"page":1,"id":2...}'
        ]
}
```

Each page entry is a JSON object defining an openHASP object. See openHASP documentation for object properties.
```

#### 17.5: Add MQTT Topics Reference
```markdown
## MQTT Topics

This binding uses the following MQTT topics:

| Topic Pattern | Direction | Purpose |
|---------------|-----------|---------|
| `hasp/{device}/command` | Outbound | Send simple commands |
| `hasp/{device}/command/json` | Outbound | Send JSON commands |
| `hasp/{device}/command/jsonl` | Outbound | Send multiple object updates |
| `hasp/{device}/state/#` | Inbound | Receive state updates |
| `hasp/{device}/LWT` | Inbound | Device online/offline status |
| `hasp/discovery/` | Inbound | Device discovery announcements |
```

#### 17.6: Add Template Customization Guide
```markdown
## Customizing Templates

### Creating Custom Templates

1. Copy `/templates/default-portrait/` to your custom location
2. Modify `template.properties` for colors, fonts, sizes
3. Edit `.hbs` template files for widget layouts
4. Configure thing to use custom templates:

```
Thing mqtt:openhasp_plate:custom "Custom Plate" {
    Parameters:
        deviceId="custom01",
        templatePathType="file",
        templatePath="/path/to/templates/my-custom/"
}
```

### Template Properties

Available properties in `template.properties`:
- Colors: `bgcolor`, `text_color`, `section_bgcolor`, etc.
- Sizes: `text_fontsize`, `button_height`, `slider_height`, etc.
- Layout: `maxx`, `maxy`, `components_vmargin`, etc.
- Icons: `icon.light`, `icon.switch`, etc.
```

**Files:** `README.md`

---

### TASK 18: Add Architecture Documentation

**Priority:** MEDIUM  
**Create:** `docs/ARCHITECTURE.md`

**Contents:**
```markdown
# OpenHASP Binding Architecture

## Overview

Component diagram, data flow diagrams

## Component Descriptions

### OpenHASPThingHandlerFactory
[Description, responsibilities, dependencies]

### OpenHASPThingHandler
[Lifecycle, MQTT integration, thing status management]

### OpenHASPPlate
[Event subscription, callback processing, state management]

### OpenHASPCommunicationManager
[MQTT operations, topic management, message batching]

### OpenHASPLayoutManager
[Sitemap processing, layout generation, object management]

### TemplateProcessor
[Handlebars integration, template caching, rendering]

### Widget Components
[Widget hierarchy, state conversion, command handling]

## Threading Model

[Which threads do what, synchronization]

## Memory Management

[Object lifecycles, caching strategies]

## Error Handling Strategy

[Error propagation, status updates, user feedback]

## Extension Points

[How to add new widgets, templates, etc.]
```

**Files:** Create `docs/ARCHITECTURE.md`

---

### TASK 19: Implement Configuration Change Handling

**Priority:** MEDIUM  
**Issue:** TODO about checking config changes (line 126)

**Current behavior**: Thing reinitializes on config change which works but is abrupt.

**Improvement**: Detect what changed and handle gracefully:

```java
private @Nullable OpenHASPThingConfiguration previousConfig;

@Override
public void initialize() {
    OpenHASPThingConfiguration newConfig = getConfigAs(OpenHASPThingConfiguration.class);
    
    if (previousConfig != null) {
        handleConfigurationUpdate(previousConfig, newConfig);
    }
    
    previousConfig = newConfig;
    this.config = newConfig;
    
    super.initialize();
}

private void handleConfigurationUpdate(OpenHASPThingConfiguration old, 
                                       OpenHASPThingConfiguration updated) {
    boolean needsFullRestart = false;
    
    // Check if device ID changed (requires full restart)
    if (!Objects.equals(old.deviceId, updated.deviceId)) {
        logger.info("Device ID changed, full restart required");
        needsFullRestart = true;
    }
    
    // Check if template path changed (requires layout regeneration)
    if (!Objects.equals(old.templatePath, updated.templatePath) ||
        !Objects.equals(old.templatePathType, updated.templatePathType)) {
        logger.info("Template configuration changed, regenerating layout");
        needsFullRestart = true;
    }
    
    // Check if sitemap changed (requires layout regeneration)
    if (!Objects.equals(old.sitemap, updated.sitemap)) {
        logger.info("Sitemap changed, regenerating layout");
        if (plate != null) {
            plate.start(); // Reload sitemap
            plate.refresh(); // Send new layout
        }
        return; // Don't need full restart
    }
    
    // Backlight settings changed (just update, no restart)
    if (old.backlightHigh != updated.backlightHigh ||
        old.backlightMedium != updated.backlightMedium ||
        old.backlightLow != updated.backlightLow) {
        logger.info("Backlight settings changed");
        // Settings take effect on next idle state change
        return;
    }
    
    if (needsFullRestart) {
        // Trigger full restart
        dispose();
        initialize();
    }
}
```

**Files:** `OpenHASPThingHandler.java`

---

### TASK 20: Add Recursive Group Safeguard

**Priority:** MEDIUM  
**Issue:** TODOs about recursive group handling (lines 261, 340)

**Implementation:**

```java
// In OpenHASPLayoutManager:
private final Set<String> processingGroups = new HashSet<>();
private static final int MAX_GROUP_DEPTH = 10;

private void processGroupWidget(Group groupWidget, int depth) {
    if (depth > MAX_GROUP_DEPTH) {
        logger.warn("Maximum group nesting depth exceeded at {}, skipping", 
            groupWidget.getLabel());
        return;
    }
    
    String groupName = groupWidget.getItem();
    if (groupName == null) {
        logger.warn("Group widget has no item name");
        return;
    }
    
    if (processingGroups.contains(groupName)) {
        logger.warn("Circular reference detected in group {}, skipping", groupName);
        return;
    }
    
    processingGroups.add(groupName);
    try {
        // Process group members
        Item item = itemRegistry.get(groupName);
        if (item instanceof GroupItem) {
            GroupItem group = (GroupItem) item;
            for (Item member : group.getMembers()) {
                // Process member with incremented depth
                processGroupMember(member, depth + 1);
            }
        }
    } finally {
        processingGroups.remove(groupName);
    }
}
```

**Files:** `OpenHASPLayoutManager.java`

---

## DOCUMENTATION TASKS

### TASK 21: Add Javadoc to All Public Methods

**Priority:** MEDIUM  
**Issue:** Many methods lack documentation

For each public method, add:
```java
/**
 * Brief description of what method does.
 * 
 * <p>Optional longer description or important notes.
 * 
 * @param paramName description of parameter
 * @return description of return value
 * @throws ExceptionType when and why exception is thrown
 * @see RelatedClass#relatedMethod for related functionality
 * @since 4.3.0
 */
```

Focus on:
- Handler classes (all public methods)
- Component classes (all public methods)  
- Utility classes (all public methods)
- Interface definitions

**Files:** All `.java` files

---

### TASK 22: Create MQTT Protocol Documentation

**Priority:** MEDIUM  
**Create:** `docs/MQTT_PROTOCOL.md`

**Contents:**
```markdown
# OpenHASP MQTT Protocol

## Topic Structure

### Command Topics
- `hasp/{device}/command` - Simple commands
- `hasp/{device}/command/json` - JSON array format
- `hasp/{device}/command/jsonl` - JSONL format

### State Topics
- `hasp/{device}/state/{subtopic}` - State updates
- `hasp/{device}/state/p{page}b{id}` - Object events
- `hasp/{device}/state/idle` - Idle state changes

### Control Topics
- `hasp/{device}/LWT` - Last Will Testament (retained)
- `hasp/discovery/` - Device announcements

## Message Formats

### JSON Command Format
```json
["p1b1.val=42", "p1b2.text=Hello"]
```

### JSONL Format
```json
{"page":1,"id":1,"obj":"btn","x":10,"y":10,"w":100,"h":50}
{"page":1,"id":2,"obj":"slider","x":10,"y":70,"w":200,"h":30}
```

### Object Event Format
```json
{
    "event": "down",
    "val": 1,
    "text": "Button"
}
```

## QoS Requirements

All messages use QoS 1 (at-least-once delivery) to ensure reliability.

## Examples

[Various example message exchanges]
```

**Files:** Create `docs/MQTT_PROTOCOL.md`

---

### TASK 23: Create Template Guide

**Priority:** MEDIUM  
**Create:** `docs/TEMPLATE_GUIDE.md`

Document:
- Template structure and purpose
- Handlebars syntax basics
- Available helpers (math, inc, toInt, etc.)
- Context variables available
- Creating custom templates
- Debugging templates
- Example templates

**Files:** Create `docs/TEMPLATE_GUIDE.md`

---

## COMPLETION CHECKLIST

When all tasks complete, verify:

- [ ] All critical bugs fixed
- [ ] All TODOs resolved or documented
- [ ] Test coverage ≥ 70%
- [ ] All tests passing
- [ ] No compilation errors or warnings
- [ ] Code formatted with spotless
- [ ] Documentation complete
- [ ] README has getting started guide
- [ ] README has troubleshooting section
- [ ] README has examples
- [ ] Javadoc on all public methods
- [ ] i18n properties file complete
- [ ] No dead code (*.hbs_old files removed)
- [ ] No commented code blocks
- [ ] Manual mode working
- [ ] Configuration validation working
- [ ] Error handling comprehensive
- [ ] LWT bug fixed
- [ ] Offline checking implemented
- [ ] Can build with `mvn clean install`
- [ ] Binding loads in OpenHAB without errors
- [ ] Can discover devices
- [ ] Can configure thing
- [ ] Can generate layout from sitemap
- [ ] Controls work bidirectionally
- [ ] Multiple devices work correctly

---

## NOTES

### Development Commands

```bash
# Build
mvn clean install -DskipChecks

# Build with checks
mvn clean spotless:apply install

# Run tests
mvn test

# Check coverage
mvn jacoco:report
# Open target/site/jacoco/index.html

# Copy to OpenHAB
cp target/*.jar ~/openhab_dev/openhab_4.3/addons/
```

### Testing Setup

```bash
# Start MQTT broker
/opt/homebrew/opt/mosquitto/sbin/mosquitto \
  -c /opt/homebrew/etc/mosquitto/mosquitto.conf

# Start openHASP simulator
/Users/alobato/git/openHASP/.pio/build/darwin_sdl_64bits/program \
  --mqttport 1883 \
  --mqtthost localhost \
  --mqttname testdevice
```

### Related Files

- `MQTT_BINDING_ANALYSIS.md` - Analysis of LWT bug
- `configs.txt` - Build commands reference
- `notas.txt` - Development notes

---

**Document Version:** 1.0  
**Last Updated:** February 13, 2026  
**Status:** Ready for Implementation
