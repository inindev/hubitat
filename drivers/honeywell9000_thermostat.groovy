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
 * Thermostat::off
 */
def off() {
    logInfo'Thermostat::off'
}

/**
 * Thermostat::cool
 */
def cool() {
    logInfo'Thermostat::cool'
}

/**
 * Thermostat::heat
 */
def heat() {
    logInfo'Thermostat::heat'
}

/**
 * Thermostat::emergencyHeat
 */
def emergencyHeat() {
    logInfo'Thermostat::emergencyHeat'
}

/**
 * Thermostat::auto
 */
def auto() {
    logInfo'Thermostat::auto'
}

/**
 * Thermostat::fanAuto
 */
def fanAuto() {
    logInfo'Thermostat::fanAuto'
}

/**
 * Thermostat::fanCirculate
 */
def fanCirculate() {
    logInfo'Thermostat::fanCirculate'
}

/**
 * Thermostat::fanOn
 */
def fanOn() {
    logInfo'Thermostat::fanOn'
}

/**
 * Thermostat::setCoolingSetpoint
 * @param temp - cooling setpoint in degrees F/C
 */
def setCoolingSetpoint(temp) {
    logInfo"Thermostat::setCoolingSetpoint - temperature: ${temp}"
}

/**
 * Thermostat::setHeatingSetpoint
 * @param temp - heating setpoint in degrees F/C
 */
def setHeatingSetpoint(temp) {
    logInfo"Thermostat::setHeatingSetpoint - temperature: ${temp}"
}

/**
 * Thermostat::setSchedule
 * @param schedule - schedule to set (json object)
 */
def setSchedule(schedule) {
    logInfo"Thermostat::setSchedule - schedule: ${schedule}"
}

/**
 * Thermostat::fanmode
 * @param mode - fan mode to set (enum)
 */
def setThermostatFanMode(mode) {
    logInfo"Thermostat::setThermostatFanMode - mode: ${mode}"
}

/**
 * Thermostat::setThermostatMode
 * @param mode - thermostat mode to set (enum)
 */
def setThermostatMode(mode) {
    logInfo"Thermostat::setThermostatMode - mode: ${mode}"
}



def installed() {
    logInfo 'virtual thermostat created'
}

def uninstalled() {
    logInfo 'virtual thermostat removed'
}

def updated() {
    logInfo 'virtual thermostat updated'
}

def logInfo(String msg) {
    if(logInfo) {
        def label = device.label ? device.label : device.name
        log.info "${label} ${msg}"
    }
}

def logDebug(String msg) {
    if(logDebug) {
        def label = device.label ? device.label : device.name
        log.debug "${label} ${msg}"
    }
}
