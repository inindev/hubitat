/*
 *  honeywell9000_thermostat.groovy
 *
 *    Hubitat support for the Honeywell WiFi 9000 Thermostat
 *    https://www.honeywellhome.com/us/en/products/air/thermostats/wifi-thermostats/wifi-9000-color-touchscreen-thermostat-th9320wf5003-u/
 *
 */

metadata {
    definition (
            name: 'Honeywell WiFi 9000 Thermostat',
            description: 'Hubitat driver for the Honeywell WiFi 9000 Thermostat',
            namespace: 'com.github.inindev',
            author: 'John Clark',
            importUrl: 'https://raw.githubusercontent.com/inindev/hubitat/main/drivers/honeywell9000_thermostat.groovy'
    ) {
        capability 'Thermostat'
    }
}

preferences {
    input name: 'logInfo', type: 'bool', title: 'Enable info logging', defaultValue: true
    input name: 'logDebug', type: 'bool', title: 'Enable debug logging', defaultValue: false
}


/**
 * Thermostat::off - set thermostat mde to 'off'
 */
def off() {
    if(logDebug) log.debug "${myLabel}  Thermostat::off"
    setThermostatMode('off')
}
/**
 * Thermostat::cool - set thermostat mde to 'cool'
 */
def cool() {
    if(logDebug) log.debug "${myLabel}  Thermostat::cool"
    setThermostatMode('cool')
}
/**
 * Thermostat::heat - set thermostat mde to 'heat'
 */
def heat() {
    if(logDebug) log.debug "${myLabel}  Thermostat::heat"
    setThermostatMode('heat')
}
/**
 * Thermostat::emergencyHeat - set thermostat mde to 'emergency heat'
 */
def emergencyHeat() {
    if(logDebug) log.debug "${myLabel}  Thermostat::emergencyHeat"
    setThermostatMode('emergency heat')
}
/**
 * Thermostat::auto - set thermostat mde to 'auto'
 */
def auto() {
    if(logDebug) log.debug "${myLabel}  Thermostat::auto"
    setThermostatMode('auto')
}

/**
 * Thermostat::fanAuto - set fan mode to 'auto'
 */
def fanAuto() {
    if(logDebug) log.debug "${myLabel}  Thermostat::fanAuto"
    setThermostatFanMode('auto')
}
/**
 * Thermostat::fanCirculate - set fan mode to 'circulate'
 */
def fanCirculate() {
    if(logDebug) log.debug "${myLabel}  Thermostat::fanCirculate"
    setThermostatFanMode('circulate')
}
/**
 * Thermostat::fanOn - set fan mode to 'on'
 */
def fanOn() {
    if(logDebug) log.debug "${myLabel}  Thermostat::fanOn"
    setThermostatFanMode('on')
}

/**
 * Thermostat::setCoolingSetpoint
 * @param temp - cooling setpoint in degrees F/C
 */
def setCoolingSetpoint(java.math.BigDecimal temperature) {
    if(logInfo) log.info "${myLabel}  - temperature: ${temperature}"
}

/**
 * Thermostat::setHeatingSetpoint
 * @param temp - heating setpoint in degrees F/C
 */
def setHeatingSetpoint(java.math.BigDecimal temperature) {
    if(logInfo) log.info "${myLabel}  - temperature: ${temperature}"
}

/**
 * Thermostat::setSchedule
 * @param schedule - schedule to set (json string)
 */
def setSchedule(String schedule) {
    if(logInfo) log.info "${myLabel}  - schedule: ${schedule}"
}

/**
 * Thermostat::fanmode
 * @param mode - fan mode to set: 'on', 'circulate', 'auto'
 */
def setThermostatFanMode(String mode) {
    if(logInfo) log.info "${myLabel}  - mode: ${mode}"
}

/**
 * Thermostat::setThermostatMode
 * @param mode - thermostat mode to set: 'off', 'cool', 'heat', 'emergency heat', 'auto'
 */
def setThermostatMode(String mode) {
    if(logInfo) log.info "${myLabel}  - mode: ${mode}"
}


/**
 * Device::installed - called when the device is first created
 */
def installed() {
    def myLabel = device.label ? device.label : device.name
    device.updateSetting('myLabel', [value: myLabel, type: "text"])
    if(logInfo) log.info "${myLabel}  virtual thermostat created"
}
/**
 * Device::uninstalled - called when the device is removed
 */
def uninstalled() {
    if(logInfo) log.info "${myLabel}  virtual thermostat removed"
}
/**
 * Device::updated - called when the preferences of a device are updated
 */
def updated() {
    def myLabel = device.label ? device.label : device.name
    device.updateSetting('myLabel', [value: myLabel, type: "text"])
    if(logInfo) log.info "${myLabel}  virtual thermostat updated"
}
