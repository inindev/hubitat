# hubitat

### User drivers and apps for the <a target="_blank" href="https://hubitat.com/">Hubitat Elevation®</a> home automation hub.


 * <a target="_blank" href="https://docs.hubitat.com/index.php?title=Developer_Documentation">Developer Documentation</a>
 * <a target="_blank" href="https://docs.hubitat.com/index.php?title=Device_Object">Device Object</a>
 * <a target="_blank" href="https://docs.hubitat.com/index.php?title=Driver_Capability_List">Driver Capability List</a>

<p>&nbsp;</p>

### Tasmota Device Support

The forked Tasmota repository (github.com/inindev/Tasmota) has been modified to call the Hubitat webhook on port 39501.  This integration was originally produced by Eric Maycock (erocm123) back in 2018: github.com/erocm123/Sonoff-Tasmota

Eric's approach eliminates the need for an MQTT broker and also creates a near instantaneous update of the device state to the hub.

The code provided in this repository is a derivative work that modernizes and simplifies Eric's integration strategy to better fit my own needs.  UDP auto-discovery has been removed, and the previous Hubitat drivers and application for Sonoff have been reduced to a single driver: github.com/inindev/hubitat/blob/main/drivers/tasmota_device.groovy

The Tasmota changes are also provided as a patchset to the 9.1.0 release: github.com/inindev/hubitat/blob/main/tasmota/0001-Reboot-Eric-Maycock-Tasmota-Hubitat-integration-920.patch

To use this patchset:

```
wget https://raw.githubusercontent.com/inindev/hubitat/main/tasmota/0001-Reboot-Eric-Maycock-Tasmota-Hubitat-integration-920.patch

git clone https://github.com/arendst/Tasmota.git
cd Tasmota
git checkout master
git apply ../0001-Reboot-Eric-Maycock-Tasmota-Hubitat-integration.patch

platformio run -e tasmota-lite

esptool.py -p /dev/cu.usbserial-0001 erase_flash
esptool.py -p /dev/cu.usbserial-0001 -b 921600 write_flash 0x000000 .pio/build/tasmota-lite/firmware.bin
```
