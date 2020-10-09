/**
 *  honeywell9000_thermostat.groovy
 *
 *    Hubitat support for the Honeywell WiFi 9000 Thermostat
 *    https://www.honeywellhome.com/us/en/products/air/thermostats/wifi-thermostats/wifi-9000-color-touchscreen-thermostat-th9320wf5003-u/
 *
 *    Copyright 2000 John Clark
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

metadata {
    definition (
            name: 'Honeywell WiFi 9000 Thermostat',
            description: 'Hubitat driver for the Honeywell WiFi 9000 Thermostat',
            namespace: 'com.github.inindev',
            author: 'John Clark',
            importUrl: 'https://raw.githubusercontent.com/inindev/hubitat/main/drivers/honeywell9000_thermostat.groovy',
    ) {
        capability 'Thermostat'
        capability 'Sensor'
        capability 'TemperatureMeasurement'
        capability 'RelativeHumidityMeasurement'
    }
}

preferences {
    input name: 'userEmail', type: 'text', title: 'Username', description: 'Honeywell email', required: true
    input name: 'userAuth', type: 'password', title: 'Password', description: 'Honeywell password', required: true

    input name: 'logInfo', type: 'bool', title: 'Enable info logging', defaultValue: true
    input name: 'logDebug', type: 'bool', title: 'Enable debug logging', defaultValue: false
}

@groovy.transform.Field static final Map config = [
        uri_tcc_site: 'https://mytotalconnectcomfort.com',
        path_portal : '/portal/',
        path_gzld   : '/portal/Device/GetZoneListData',
        path_scsc   : '/portal/Device/SubmitControlScreenChanges',
]


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
 * @param temperature - cooling setpoint in degrees C/F
 */
def setCoolingSetpoint(java.math.BigDecimal temperature) {
    if(logInfo) log.info "${myLabel}  Thermostat::setCoolingSetpoint - temperature: ${temperature}"
}

/**
 * Thermostat::setHeatingSetpoint
 * @param temperature - heating setpoint in degrees C/F
 */
def setHeatingSetpoint(java.math.BigDecimal temperature) {
    if(logInfo) log.info "${myLabel}  Thermostat::setHeatingSetpoint - temperature: ${temperature}"
}

/**
 * Thermostat::setSchedule
 * @param schedule - schedule to set (json string)
 */
def setSchedule(String schedule) {
    if(logInfo) log.info "${myLabel}  Thermostat::setSchedule - schedule: ${schedule}"
}

/**
 * Thermostat::fanmode
 * @param mode - fan mode to set: 'on', 'circulate', 'auto'
 */
def setThermostatFanMode(String mode) {
    if(logInfo) log.info "${myLabel}  Thermostat::fanmode - mode: ${mode}"
}

/**
 * Thermostat::setThermostatMode
 * @param mode - thermostat mode to set: 'off', 'cool', 'heat', 'emergency heat', 'auto'
 */
def setThermostatMode(String mode) {
    if(logInfo) log.info "${myLabel}  Thermostat::setThermostatMode - mode: ${mode}"
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

    if(userAuth != '+-+-+-+-+-+-+') {
        device.updateSetting('userAuthCrypt', encrypt(userAuth))
        device.updateSetting('userAuth', [value: '+-+-+-+-+-+-+', type: "password"])
    }
}


// cookie management
@groovy.transform.Field Map cookies = [:]

def getCookieString() {
    StringBuffer cookieBuf = new StringBuffer()
    cookies.each() { name, cookie ->
        def expire = cookie[2]
        if ((expire > -1) && expire < now()){
            cookies.remove(name)
        } else {
            if (cookieBuf.size() > 0) cookieBuf << '; '
            cookieBuf << name
            cookieBuf << '='
            def val = cookie[1]
            if(val) cookieBuf << val
        }
    }
    return cookieBuf.toString()
}

def addCookie(String rawCookie) {
    def cookie = parseCookie(rawCookie)
    if (cookie) {
        def name = cookie[0]
        def expire = cookie[2]
        log.debug "${myLabel}  found cookie - name: ${name}  exp: ${expire}"
        if ((expire > -1) && expire < now()){
            log.debug "${myLabel}  removing expired cookie - name: ${name}"
            cookies.remove(name)
        } else {
            cookies[name] = cookie
        }
    }
}

def parseCookie(String rawCookie) {
    String name = null
    String value = null
    long expire = -1

    def rawCookieParams = rawCookie.split(';')
    rawCookieParams.eachWithIndex { rawCookieParam, num ->
        def kvp = rawCookieParam.split('=')
        def key = kvp[0]?.trim()
        def val = (kvp?.length > 1) ? kvp[1]?.trim() : null
        if (num == 0) {
            // first token
            name = key
            value = val
        } else {
            // subsequent tokens
            if (val && key?.equalsIgnoreCase('expires')) {
                try {
                    expire = Date.parse(val)
                } catch (ex) {
                    log.error("${myLabel}  parse date: ${ex}")
                }
            }
            // ignore other elements in this impl
        }
    }

    return name ? [name, value, expire] : null
}
