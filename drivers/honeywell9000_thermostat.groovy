/**
 *  honeywell9000_thermostat.groovy
 *
 *    Hubitat support for the Honeywell WiFi 9000 Thermostat
 *    https://www.honeywellhome.com/us/en/products/air/thermostats/wifi-thermostats/wifi-9000-color-touchscreen-thermostat-th9320wf5003-u/
 *
 *    Copyright 2020 John Clark (inindev)
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
 *    version history
 *      0.0.1: 11/07/2020 - base skeleton
 *      0.0.2: 11/11/2020 - working auth, xmlhttprequest post
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
        capability 'TemperatureMeasurement'
        capability 'RelativeHumidityMeasurement'
    }
}

preferences {
    input name: 'userEmail', type: 'text', title: 'Honeywell email', required: true
    input name: 'userAuth', type: 'text', title: 'Honeywell password', required: true
    input name: 'logLevelEnum', type: 'enum', title: 'Logging level', options: [0:'none',1:'error',2:'warn',3:'info',4:'debug',5:'trace'], defaultValue: 3
}

@groovy.transform.Field static final Map config = [
    version: [ '0.0.2' ],
    uri: [
        tccSite: 'https://mytotalconnectcomfort.com',
    ],
    path: [
        portal: '/portal/',
        gzld:   '/portal/Device/GetZoneListData',
        scsc:   '/portal/Device/SubmitControlScreenChanges',
        glld:   '/portal/Location/GetLocationListData',
    ],
    header: [
        userAgent:       'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:82.0) Gecko/20100101 Firefox/82.0',
        acceptAll:       'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
        acceptJson:      'application/json, text/javascript, */*; q=0.01',
        contentTypeForm: 'application/x-www-form-urlencoded',
        contentTypeJson: 'application/json; charset=utf-8',
        acceptLang:      'en-US,en;q=0.5',
    ],
]


/**
 * Thermostat::off - set thermostat mde to 'off'
 */
def off() {
    log2.debug "Thermostat::off"
    setThermostatMode('off')
}
/**
 * Thermostat::cool - set thermostat mde to 'cool'
 */
def cool() {
    log2.debug "Thermostat::cool"
    setThermostatMode('cool')
}
/**
 * Thermostat::heat - set thermostat mde to 'heat'
 */
def heat() {
    log2.debug "Thermostat::heat"
    setThermostatMode('heat')
}
/**
 * Thermostat::emergencyHeat - set thermostat mde to 'emergency heat'
 */
def emergencyHeat() {
    log2.debug "Thermostat::emergencyHeat"
    setThermostatMode('emergency heat')
}
/**
 * Thermostat::auto - set thermostat mde to 'auto'
 */
def auto() {
    log2.debug "Thermostat::auto"
    setThermostatMode('auto')
}

/**
 * Thermostat::fanAuto - set fan mode to 'auto'
 */
def fanAuto() {
    log2.debug "Thermostat::fanAuto"
    setThermostatFanMode('auto')
}
/**
 * Thermostat::fanCirculate - set fan mode to 'circulate'
 */
def fanCirculate() {
    log2.debug "Thermostat::fanCirculate"
    setThermostatFanMode('circulate')
}
/**
 * Thermostat::fanOn - set fan mode to 'on'
 */
def fanOn() {
    log2.debug "Thermostat::fanOn"
    setThermostatFanMode('on')
}

/**
 * Thermostat::setCoolingSetpoint
 * @param temperature - cooling setpoint in degrees C/F
 */
def setCoolingSetpoint(java.math.BigDecimal temperature) {
    log2.info "Thermostat::setCoolingSetpoint - temperature: ${temperature}"
}

/**
 * Thermostat::setHeatingSetpoint
 * @param temperature - heating setpoint in degrees C/F
 */
def setHeatingSetpoint(java.math.BigDecimal temperature) {
    log2.info "Thermostat::setHeatingSetpoint - temperature: ${temperature}"
}

/**
 * Thermostat::setSchedule
 * @param schedule - schedule to set (json string)
 */
def setSchedule(String schedule) {
    log2.info "Thermostat::setSchedule - schedule: ${schedule}"
}

/**
 * Thermostat::fanmode
 * @param mode - fan mode to set: 'on', 'circulate', 'auto'
 */
def setThermostatFanMode(String mode) {
    log2.info "Thermostat::fanmode - mode: ${mode}"
}

/**
 * Thermostat::setThermostatMode
 * @param mode - thermostat mode to set: 'off', 'cool', 'heat', 'emergency heat', 'auto'
 */
def setThermostatMode(String mode) {
    log2.info "Thermostat::setThermostatMode - mode: ${mode}"
}


/**
 * Device::installed - called when the device is first created
 */
def installed() {
    device.updateSetting('logLevel', 3)
    log.info " ${device.label ?: device.name} : virtual thermostat created"
}
/**
 * Device::uninstalled - called when the device is removed
 */
def uninstalled() {
    log2.info "virtual thermostat uninstalled"
}
/**
 * Device::updated - called when the preferences of a device are updated
 */
def updated() {
    def logLevel = logLevelEnum?.isInteger() ? logLevelEnum.toInteger() : 3
    device.updateSetting('logLevel', logLevel)

    if(userAuth != '*************') {
        device.updateSetting('userAuthCrypt', encrypt(userAuth))
        device.updateSetting('userAuth', '*************')
        if(logLevel > 2) log.info " ${device.label ?: device.name} : encrypted password updated"
    }

    if(logLevel > 2) log.info " ${device.label ?: device.name} : virtual thermostat updated - logging level: ${['none','error','warn','info','debug','trace'].getAt(logLevel)}"
}


def xPost(path) {
    log2.debug "xPost - path: ${path}"

    def params = [
        uri: config.uri.tccSite,
        path: path,
        query: [page:1],
        contentType: config.header.acceptJson, // Accept
        requestContentType: config.header.contentTypeJson, // Content-Type
        headers: [
            'User-Agent'      : config.header.userAgent,
            'Accept-Language' : config.header.acceptLang,
            'Accept-Encoding' : 'gzip, deflate',
            'X-Requested-With': 'XMLHttpRequest',
            'DNT'             : '1',
            'Sec-GPC'         : '1',
            'Connection'      : 'keep-alive',
        ],
        textParser: true,
    ]
    def cookieStr = cookieMgr.serialize()
    if(cookieStr) params.headers.Cookie = cookieStr

    def json
    httpPost(params) { resp ->
        log2.debug "xPost - status: ${resp.status}"
        resp.getHeaders('Set-Cookie').each { cookie ->
            cookieMgr.process(cookie.value)
        }
        cookieMgr.save()

        def data = new String(resp.getData().getBytes())
        json = parseJson(data)
    }
    return json
}

def webAuthenticate() {
    log2.trace "webAuthenticate - user: ${userEmail}"

    def params = [
        uri: config.uri.tccSite,
        path: config.path.portal,
        contentType: config.header.acceptAll,
        requestContentType: config.header.contentTypeForm,
        headers: [
            'User-Agent'     : config.header.userAgent,
            'Accept-Language': config.header.acceptLang,
            'Accept-Encoding': 'gzip, deflate',
            'DNT'            : '1',
            'Sec-GPC'        : '1',
            'Connection'     : 'keep-alive',
        ],
        body: "timeOffset=240&UserName=${URLEncoder.encode(userEmail,'UTF-8')}&Password=${URLEncoder.encode(decrypt(userAuthCrypt),'UTF-8')}&RememberMe=false",
    ]

    // reset all cookies
    cookieMgr.flush()

    try {
        httpPost(params) { resp ->
            log2.debug "webAuthenticate - status: ${resp.status}"
            resp.getHeaders('Set-Cookie').each { cookie ->
                cookieMgr.process(cookie.value)
            }
            cookieMgr.save()
        }
    }
    catch(groovyx.net.http.HttpResponseException ex) {
        log2.error "HttpResponseException thrown: ${ex}"
        return false // failure
    }

    return true // success
}


// cookie management
@groovy.transform.Field Map cookieMgr = [
    'store': null,
    'serialize': {
        log2.trace 'cookieMgr.serialize'
        if(cookieMgr.store == null) cookieMgr.load()
        cookieMgr.store.removeIf { (it[1] > -1) && (it[1] < now()) }
        def str = cookieMgr.store.collect{ it.first() }.join('; ')
        log2.trace "cookieMgr.serialize - str: ${str}"
        return str
    },
    'process': { rawCookie ->
        def cookieList = cookieMgr.parse(rawCookie)
        log2.trace "cookieMgr.process - cookieList: ${cookieList}"
        if(!cookieList) return;
        if(cookieMgr.store == null) cookieMgr.load()

        def pos = cookieList.first().indexOf('=')
        def name = (pos == -1) ? cookieList.first() : cookieList.first().take(pos+1); pos = name.size()
        def expires = (cookieList.size() > 1) ? cookieList[1] : -1
        cookieMgr.store.removeIf { it.first().take(pos).equalsIgnoreCase(name) }
        if((expires > -1) && (expires < now())) {
            log2.debug "cookieMgr.process - removing expired cookie - name: ${name}  exp: ${new Date(expires)}"
        } else {
            log2.debug "cookieMgr.process - adding cookie - name: ${name}  exp: ${(expires==-1)?'-1':new Date(expires)}"
            cookieMgr.store.add(cookieList)
        }
    },
    'parse': { rawCookie ->
        log2.trace "cookieMgr.parse - rawCookie: ${rawCookie}"
        def rawCookieParams = rawCookie?.split(';')
        def nameVal = rawCookieParams?.first()?.trim()
        if(!nameVal || nameVal.startsWith('=')) return null

        def expires = -1
        def imax = rawCookieParams?.size()
        for(int i=1; i<imax; i++) {
            def kvp = rawCookieParams?.getAt(i)?.split('=')
            def key = kvp?.first()?.trim()
            if(key?.equalsIgnoreCase('expires')) {
                expires = 0
                def val = (kvp?.size() > 1) ? kvp?.getAt(1)?.trim() : null
                if(val) {
                    try {
                        expires = Date.parse(val)
                    } catch (ex) {
                        log2.error("cookieMgr.parse - Date.parse: ${ex}")
                    }
                }
                break
            }
        }
        return [nameVal, expires]
    },
    'load': {
        cookieMgr.store = null
        def json = getDataValue('cookies')
        log2.trace "cookieMgr.load - json: ${json}"
        try {
            def store = json ? parseJson(json) : []
            if(!store instanceof List) {
                log2.error('cookieMgr.load - stored data value "cookies" is not a list, flushing cookies')
                removeDataValue('cookies')
                return
            }
            cookieMgr.store = store.collect { it instanceof List ? it : [it, -1] }
        } catch (ex) {
            log2.error("cookieMgr.load - parseJson: ${ex}")
            cookieMgr.flush()
        }
    },
    'save': {
        def json = groovy.json.JsonOutput.toJson(cookieMgr.store.collect { it.last() == -1 ? it.first() : it })
        log2.trace "cookieMgr.save - json: ${json}"
        updateDataValue('cookies', json)
    },
    'flush': {
        log2.debug 'cookieMgr.flush'
        cookieMgr.store = []
        removeDataValue('cookies')
    }
]

// logging
@groovy.transform.Field Map log2 = [
    'error': { msg -> if(logLevel > 0) log.error " ${device.label ?: device.name} : ${msg}" },
    'warn' : { msg -> if(logLevel > 1) log.warn  " ${device.label ?: device.name} : ${msg}" },
    'info' : { msg -> if(logLevel > 2) log.info  " ${device.label ?: device.name} : ${msg}" },
    'debug': { msg -> if(logLevel > 3) log.debug " ${device.label ?: device.name} : ${msg}" },
    'trace': { msg -> if(logLevel > 4) log.trace " ${device.label ?: device.name} : ${msg}" },
]
