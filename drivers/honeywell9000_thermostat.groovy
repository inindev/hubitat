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
    input name: 'userEmail', type: 'text', title: 'Honeywell email', required: true
    input name: 'userAuth', type: 'text', title: 'Honeywell password', required: true

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

    if(userAuth != '*************') {
        device.updateSetting('userAuthCrypt', encrypt(userAuth))
        device.updateSetting('userAuth', '*************')
        if(logInfo) log.info "${myLabel}  encrypted password updated"
    }
}


// cookie management

/**
 * serializes the storedCookies persistence list to string suitable for a cookie request header
 * note that expired cookies are removed from storedCookies persistence
 */
def serializeCookies() {
    StringBuffer cookieBuf = new StringBuffer()
    storedCookies.each() { cookie ->
        if(isExpired(cookie)) {
            cookies.remove(cookie)
        } else {
            def nameVal = cookie.split(';').getAt(0).trim()
            if(cookieBuf.size() > 0) cookieBuf << '; '
            cookieBuf << nameVal
        }
    }
    return cookieBuf.toString()
}

/**
 * adds or updates the specified cookie in the storedCookies persistence list
 * note that the cookie is removed ifit is expired
 * @param  cookie: cookie to be updated
 * @param  remove: remove cookie only
 */
def updateCookie(cookie, remove=false) {
    def cookieName = cookie?.split('=')?.getAt(0)?.trim()
    if(!cookieName) return

    if(!storedCookies || !(storedCookies instanceof ArrayList)) storedCookies = []
    def imax = storedCookies?.size()
    for(int i=0; i<imax; i++) {
        def storedCookie = storedCookies.getAt(i)
        def storedCookieName = storedCookie.split('=').getAt(0).trim()
        if(cookieName.equalsIgnoreCase(storedCookieName)) {
            if(remove) {
                log.debug "${myLabel}  removing stored cookie: ${storedCookieName}"
                storedCookies.remove(i)
            } else {
                log.debug "${myLabel}  replacing stored cookie: ${storedCookieName}"
                storedCookies[i] = cookie
            }
            device.updateSetting('storedCookies', storedCookies)
            return
        }
    }

    if(!remove) {
        storedCookies.add(cookie)
        device.updateSetting('storedCookies', storedCookies)
    }
}

/**
 * removes the specified cookie from the storedCookies persistence list
 * @param  cookie: cookie to be removed
 */
def removeCookie(cookie) {
    updateCookie(cookie, true)
}

/**
 * clears the persisted storedCookies variable
 */
def clearCookies() {
    if(logDebug) log.debug "${myLabel}  clearCookies"
    device.updateSetting('storedCookies', [])
}

/**
 * parse epoch data from cookie param to determine ifit is expired
 * @param  cookie: parsed cookie with expiration in unix epoch format
 * @return true ifcookie is expired
 */
def isExpired(cookie) {
    def tokens = cookie.split(';')
    if(tokens.size() < 2) return false

    def expires = 0
    try {
        def expStr = tokens.getAt(1).trim()
        expires = Long.parseLong(expStr)
        if(expires == -1) return false
    } catch (ex) {
        log.error("${myLabel}  parse date: ${ex}")
    }
    return expires < now()
}

/**
 * process Set-Cookie headers
 * cookies will be persisted in the parsedCookie list
 * @param  cookie: raw cookie from Set-Cookie header
 */
def processCookie(String rawCookie) {
    if(logDebug) log.debug "${myLabel}  processCookie"

    def cookieMap = parseCookie(rawCookie)
    if(!cookieMap) return;

    def name = cookieMap.cookie.split('=').getAt(0).trim()
    if(logDebug) log.debug "${myLabel}  found cookie - name: ${name}  exp: ${cookieMap.expires}"
    if((cookieMap.expires > -1) && cookieMap.expires < now()) {
        if(logDebug) log.debug "${myLabel}  removing expired cookie - name: ${name}"
        removeCookie(cookieMap.cookie)
    } else {
        if(logDebug) log.debug "${myLabel}  adding cookie - name: ${name}"
        updateCookie(cookieMap.cookie)
    }
}

/**
 * accepts a raw server cookie as sent from Set-Cookie header
 * @param  cookie: raw cookie from Set-Cookie header
 * @return  map containing the parsed cookie and expiration as a unix epoch long
 */
def parseCookie(String rawCookie) {
    def rawCookieParams = rawCookie?.split(';')
    def nameVal = rawCookieParams?.getAt(0)?.trim()
    if(!nameVal) return null

    def imax = rawCookieParams?.size()
    for(int i=1; i<imax; i++) {
        def kvp = rawCookieParams?.getAt(i)?.split('=')
        def key = kvp?.getAt(0)?.trim()
        if(key?.equalsIgnoreCase('expires')) {
            def exp = 0
            def val = (kvp?.size() > 1) ? kvp?.getAt(1)?.trim() : null
            if(val) {
                try {
                    exp = Date.parse(val)
                } catch (ex) {
                    log.error("${myLabel}  parse date: ${ex}")
                }
            }
            return [cookie: "${nameVal};${expires}", expires: exp]
        }
    }
    return [cookie: nameVal, expires: -1]
}
