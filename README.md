# DreamScreen Binding
[DreamScreen](https://www.dreamscreentv.com/) provides ambient back-lighting for your television that responds to what is being displayed.

This binding allows you to control many of the features of DreamScreen with OpenHAB. 
* Power
* Input Source (4K and HD only)
* Video/Music/Ambient mode
* Ambient Scene
* Ambient Color

## Supported Things
* DreamScreen HD
* DreamScreen 4K
* DreamScreen SideKick

At the moment, only the DreamScreen 4K has been tested with this binding. 

## Discovery
Auto-discovery is the only reasonable way to add a DreamScreen device to OpenHAB. The DreamScreen API uses UDP broadcasts, so it is important to configure your OpenHAB installation so its primary network and broadcast addresses match the network with your DreamScreen devices. 

## Binding Configuration
The only binding configuration parameter is the device's serial number. The only real way to find this serial number is with auto-discovery. 

## Thing Configuration
The binding has no thing configuration.

## Channels
### Power
_Channel ID:_ `power`

The DreamScreen doesn't actually have a power setting. It has a sleep mode. When turning this power channel off, this binding changes the DreamScreen mode to sleep. When turning the power channel on, the binding re-enables the last non-sleep mode it was in. 

### Mode
_Channel ID:_ `mode`

When the DreamScreen power channel is on, this switches between `Video`, `Music` and `Ambient` modes

### Input Source
_Channel ID:_ `input`

Represents the active input source. The input names are read from the DreamScreen. This channel is only available for the DreamScreen HD and 4K.

### Scene
_Channel ID:_ `scene`

Represents the active ambient scene. This also includes a `Color` scene which allows you to specify a solid color.

### Color
_Channel ID:_ `color`

When the _Mode_ channel is `Ambient` and the _Scene_ channel is `Color`, this represents the visible color. 

## Examples
### Rules
```
rule "Watch TV"
when
    Item MyHarmonyHub_currentActivity changed to "Watch TV"
then
    sendCommand(dreamscreen_4k_input, 1)
    sendCommand(dreamscreen_4k_mode, 0)
    sendCommand(dreamscreen_4k_power, ON)
end
```