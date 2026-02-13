# OpenHASP Binding

The OpenHASP binding integrates [OpenHASP](https://www.openhasp.com/) devices with OpenHAB. OpenHASP (Open Hardware Agnostic Smart Panel) is an open-source project that transforms ESP32-based touch screen displays into smart home control panels.

This binding allows you to:
- Automatically generate touch screen layouts from OpenHAB sitemaps
- Control OpenHAB items directly from touch panels
- Monitor device status and backlight settings
- Create custom UI templates for different screen orientations

OpenHASP devices communicate via MQTT and can display interactive controls for lights, switches, sliders, climate controls, and more.

## Supported Things

The binding supports the following thing types:

| Thing Type | Description |
|------------|-------------|
| `openhasp_plate` | An OpenHASP touch screen device |

### Supported Devices

The binding works with any ESP32-based touch screen running OpenHASP firmware, including:
- Lanbon L8 switches
- WT32-SC01 displays  
- M5Stack Core2
- Custom ESP32 + display combinations
- Any device supported by the OpenHASP project

## Discovery

The binding provides automatic discovery of OpenHASP devices on your network. Discovery works by:

1. **MQTT Discovery**: Listens for OpenHASP discovery messages on the `hasp/discovery/` MQTT topic
2. **Automatic Detection**: Devices announce themselves when they come online
3. **Configuration**: Discovered devices appear in the OpenHAB inbox with basic configuration

**Requirements for Discovery:**
- MQTT broker must be configured and running
- OpenHASP devices must be connected to the same MQTT broker
- Discovery service must be enabled in OpenHAB

**Note**: Make sure your OpenHASP devices are configured to send discovery messages to the MQTT broker.

## Binding Configuration

This binding does not require any global configuration files. All configuration is done at the thing level.

The binding extends the MQTT binding, so you need:
1. A configured MQTT broker (either embedded or external)
2. Network connectivity between OpenHAB and your OpenHASP devices

## Thing Configuration

OpenHASP plates can be configured through the UI or via `.things` files.

### Required Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `deviceId` | String | HASP device ID used in MQTT topics (e.g., "plate01") |
| `configMode` | String | Configuration mode: "sitemap" or "manual" |

### Optional Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `sitemap` | String | - | Comma-separated list of sitemaps to display |
| `templatePathType` | String | "classpath" | Template source: "classpath" or "file" |
| `templatePath` | String | "/templates/default-portrait/" | Path to UI templates |
| `pages` | String[] | - | Manual page configurations |
| `hostname` | String | - | Device hostname or IP address |
| `password` | String | - | Device access password |
| `backlightHigh` | Integer | 100 | Brightness when active (0-100%) |
| `backlightMedium` | Integer | 50 | Brightness when idle short (0-100%) |
| `backlightLow` | Integer | 20 | Brightness when idle long (0-100%) |

### Configuration Examples

#### Sitemap Mode (Automatic Layout)
```
Thing mqtt:openhasp_plate:myplate "Living Room Panel" (mqtt:broker:mybroker) {
    Parameters:
        deviceId="plate01",
        configMode="sitemap",
        sitemap="default,lights"
}
```

#### Manual Mode
```
Thing mqtt:openhasp_plate:myplate "Kitchen Panel" (mqtt:broker:mybroker) {
    Parameters:
        deviceId="kitchen",
        configMode="manual",
        pages=["page1config", "page2config"]
}
```

## Channels

The OpenHASP binding provides the following channels:

| Channel | Type | Description |
|---------|------|-------------|
| `backlight` | Dimmer | Controls the display backlight brightness (0-100%) |
| `lwt` | String | Last Will Testament - device online/offline status |
| `haspbutton` | Switch | Dynamic button channels for manual configuration |

### Channel Configuration

#### Backlight Channel
- **Type**: Dimmer
- **Usage**: Control screen brightness
- **Values**: 0-100 (percentage)
- **Tags**: Control, Light

#### LWT Channel  
- **Type**: String
- **Usage**: Monitor device connectivity
- **Values**: "online" or "offline"

#### HASP Button Channels
- **Type**: Switch
- **Usage**: Manual button configuration
- **Configuration**: Requires `buttonId` parameter (e.g., "p0b1" for page 0, button 1)

## Items Configuration

### Example Items
```
Dimmer LivingRoom_Panel_Backlight "Panel Backlight" { channel="mqtt:openhasp_plate:myplate:backlight" }
String LivingRoom_Panel_Status "Panel Status" { channel="mqtt:openhasp_plate:myplate:lwt" }
Switch LivingRoom_Panel_Button1 "Panel Button 1" { channel="mqtt:openhasp_plate:myplate:haspbutton" }
```

## Sitemap Integration

When using `configMode="sitemap"`, the binding automatically:

1. **Reads Sitemaps**: Processes specified sitemap files
2. **Generates Layout**: Creates touch screen layout from sitemap widgets
3. **Maps Controls**: Links sitemap items to touch screen controls
4. **Synchronizes State**: Keeps OpenHAB items and screen controls in sync

### Supported Sitemap Widgets

| Sitemap Widget | OpenHASP Control | Description |
|----------------|------------------|-------------|
| Switch | Button with toggle | On/off controls |
| Slider | Slider control | Continuous value adjustment |
| Selection | Dropdown/buttons | Multiple choice selection |
| Setpoint | +/- buttons | Numeric value adjustment |
| Text | Text display | Read-only information |
| Frame | Section header | Visual grouping |

## Templates and Customization

The binding uses Handlebars templates to generate the JSON configuration sent to OpenHASP devices.

### Template Structure
```
templates/default-portrait/
├── template.properties     # Colors, fonts, dimensions
├── button.json.hbs        # Button controls
├── slider.json.hbs        # Slider controls  
├── text.json.hbs          # Text displays
├── selection.json.hbs     # Selection controls
├── setpoint.json.hbs      # Setpoint controls
├── section.json.hbs       # Section headers
└── page.json.hbs          # Page navigation
```

### Custom Templates

You can create custom templates by:
1. Setting `templatePathType="file"`
2. Setting `templatePath` to your custom template directory
3. Creating your own `.hbs` template files
4. Customizing `template.properties` for colors and layout

## Full Example

### Things Configuration
```
Bridge mqtt:broker:mybroker "MQTT Broker" [ host="192.168.1.100", port=1883 ] {
    Thing mqtt:openhasp_plate:livingroom "Living Room Panel" [
        deviceId="livingroom",
        configMode="sitemap",
        sitemap="default",
        backlightHigh=100,
        backlightMedium=50,
        backlightLow=10
    ]
}
```

### Items Configuration
```
// Panel control
Dimmer LivingRoom_Panel_Backlight "Panel Backlight [%d %%]" { channel="mqtt:openhasp_plate:livingroom:backlight" }
String LivingRoom_Panel_Status "Panel Status [%s]" { channel="mqtt:openhasp_plate:livingroom:lwt" }

// Home automation items (controlled via panel)
Switch LivingRoom_Light "Living Room Light" { channel="hue:0210:bridge:bulb1:color" }
Dimmer LivingRoom_Dimmer "Living Room Dimmer [%d %%]" { channel="hue:0210:bridge:bulb2:brightness" }
Number LivingRoom_Temperature "Temperature [%.1f °C]" { channel="zwave:device:controller:node5:sensor_temperature" }
```

### Sitemap Configuration
```
sitemap default label="Home Control" {
    Frame label="Living Room" {
        Switch item=LivingRoom_Light
        Slider item=LivingRoom_Dimmer
        Text item=LivingRoom_Temperature
    }
    Frame label="Panel" {
        Slider item=LivingRoom_Panel_Backlight
        Text item=LivingRoom_Panel_Status
    }
}
```

## Troubleshooting

### Common Issues

1. **Device Not Discovered**
   - Check MQTT broker connectivity
   - Verify OpenHASP device MQTT configuration
   - Ensure discovery service is enabled

2. **Layout Not Updating**
   - Verify sitemap syntax
   - Check device logs for JSON parsing errors
   - Confirm MQTT topic permissions

3. **Controls Not Responding**
   - Check item linking in sitemap
   - Verify MQTT message flow
   - Review OpenHASP device logs

### Debug Information

Enable debug logging for detailed troubleshooting:
```
log:set DEBUG org.openhab.binding.openhasp
```

## Advanced Configuration

### Manual Page Configuration

For advanced users, manual mode allows direct control over page layout:

```
Thing mqtt:openhasp_plate:advanced "Advanced Panel" [
    deviceId="advanced",
    configMode="manual",
    pages=[
        "{\"page\":1,\"id\":1,\"obj\":\"btn\",\"x\":10,\"y\":10,\"w\":100,\"h\":50,\"text\":\"Button1\"}",
        "{\"page\":1,\"id\":2,\"obj\":\"slider\",\"x\":10,\"y\":70,\"w\":200,\"h\":30}"
    ]
]
```

### Custom MQTT Topics

The binding uses these MQTT topic patterns:
- Command: `hasp/{deviceId}/command`
- State: `hasp/{deviceId}/state/p{page}b{button}`
- LWT: `hasp/{deviceId}/LWT`
- Discovery: `hasp/discovery/`

## Contributing

Contributions are welcome! The binding source code includes:

- **Core Classes**: Thing handlers, communication managers
- **Layout System**: Sitemap processing and template generation  
- **Widget Components**: UI element implementations
- **Templates**: Handlebars templates for different controls
- **Discovery**: MQTT-based device discovery

See the project's Java documentation and existing memories for detailed architecture information.
