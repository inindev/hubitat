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
 */

metadata {
    definition (
            name: 'Honeywell WiFi 9000 Thermostat',
            description: 'Hubitat driver for the Honeywell WiFi 9000 Thermostat',
            namespace: 'com.github.inindev',
            author: 'John Clark',
            importUrl: 'https://raw.githubusercontent.com/inindev/hubitat/main/drivers/honeywell9000_thermostat.groovy',
    ) {
        capability "Initialize"
        capability "Polling"
        capability 'Thermostat'
        capability 'TemperatureMeasurement'
        capability 'RelativeHumidityMeasurement'
    }
}

preferences {
    if(getParent() == null) {
        input name: 'userEmail', type: 'text', title: 'Honeywell email', required: true
        input name: 'userAuth', type: 'text', title: 'Honeywell password', required: true
        input name: 'logLevelEnum', type: 'enum', title: 'Logging level', options: [0:'none',1:'error',2:'warn',3:'info',4:'debug',5:'trace'], defaultValue: 3
    }
}

@groovy.transform.Field static final Map config = [
    version: [ '0.0.5' ],
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
        userAgent:       'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.16; rv:83.0) Gecko/20100101 Firefox/83.0',
        acceptAll:       'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
        acceptJson:      'application/json, text/javascript, */*; q=0.01',
        contentTypeForm: 'application/x-www-form-urlencoded',
        contentTypeJson: 'application/json; charset=utf-8',
        acceptLang:      'en-US,en;q=0.5',
    ],
]


/**
 * Initialize::initialize - initial thermostat setup
 */
void initialize() {
    log2.info "initialize"

    def parent = getParent()
    if(parent) { // called on a child, pass up to root
        parent.initialize()
        return
    }

    state.clear()

    def status = webAuthenticate();
    if(status >= 400) {
        log2.error "initialize - webAuthenticate failed with status: ${status}"
        if(status == 401) {
            state.error = '<span style="color:red">Web Authentication Failed: Check email and password then try again.</span>'
        } else {
            state.error = "<span style='color:red'>Web Authentication Failed - status: ${status}</span>"
        }
        return
    }

    try {
        def lld = xPost(config.path.glld)?.first()
        if(!lld) {
            state.error = "<span style='color:red'>No Location List Data returned from server!</span>"
            return
        }

        state.locationId = lld.LocationID
        createThermostats(lld.Devices)
    }
    catch(groovyx.net.http.HttpResponseException ex) {
        status = ex.getStatusCode()
        log2.error "${ex.getResponse()} - status: ${status}"
        state.error = "<span style='color:red'>Failed to fetch Location List Data - status: ${status}</span>"
    }
}

/**
 * Polling::poll - refresh thermostat values
 */
void poll() {
    log2.info "poll"

    def parent = getParent()
    if(parent) { // called on a child, pass up to root
        parent.poll()
        return
    }

    if(!state.locationId) {
        log2.error 'locationId is not set'
        return
    }

    try {
        def zld = xPost(config.path.gzld, [locationId:state.locationId])
        if(!zld) {
            state.error = "<span style='color:red'>No Zone List Data returned from server!</span>"
            return
        }

        zld.each { zd ->
            def therm = childDevices.find {it.getDeviceId() == zd.DeviceID}
            if(!therm) {
                log2.error "thermostat not found - id: ${zd.DeviceID}"
            } else {
                therm.updateValues(zd)
            }
        }
    }
    catch(groovyx.net.http.HttpResponseException ex) {
        status = ex.getStatusCode()
        // if 401 then auth & retry
        log2.error "${ex.getResponse()} - status: ${status}"
        state.error = "<span style='color:red'>Failed to fetch Zone List Data - status: ${status}</span>"
    }
}

/**
 * Thermostat::off - set thermostat mode to 'off'
 */
void off() {
    log2.debug "Thermostat::off"
    setThermostatMode('off')
}
/**
 * Thermostat::cool - set thermostat mode to 'cool'
 */
void cool() {
    log2.debug "Thermostat::cool"
    setThermostatMode('cool')
}
/**
 * Thermostat::heat - set thermostat mode to 'heat'
 */
void heat() {
    log2.debug "Thermostat::heat"
    setThermostatMode('heat')
}
/**
 * Thermostat::emergencyHeat - set thermostat mode to 'emergency heat'
 */
void emergencyHeat() {
    log2.debug "Thermostat::emergencyHeat"
    setThermostatMode('emergency heat')
}
/**
 * Thermostat::auto - set thermostat mode to 'auto'
 */
void auto() {
    log2.debug "Thermostat::auto"
    setThermostatMode('auto')
}

/**
 * Thermostat::fanAuto - set fan mode to 'auto'
 */
void fanAuto() {
    log2.debug "Thermostat::fanAuto"
    setThermostatFanMode('auto')
}
/**
 * Thermostat::fanCirculate - set fan mode to 'circulate'
 */
void fanCirculate() {
    log2.debug "Thermostat::fanCirculate"
    setThermostatFanMode('circulate')
}
/**
 * Thermostat::fanOn - set fan mode to 'on'
 */
void fanOn() {
    log2.debug "Thermostat::fanOn"
    setThermostatFanMode('on')
}

/**
 * Thermostat::setCoolingSetpoint
 * @param temperature - cooling setpoint in degrees C/F
 */
void setCoolingSetpoint(java.math.BigDecimal temperature) {
    log2.info "Thermostat::setCoolingSetpoint - temperature: ${temperature}"
}

/**
 * Thermostat::setHeatingSetpoint
 * @param temperature - heating setpoint in degrees C/F
 */
void setHeatingSetpoint(java.math.BigDecimal temperature) {
    log2.info "Thermostat::setHeatingSetpoint - temperature: ${temperature}"
}

/**
 * Thermostat::setSchedule
 * @param schedule - schedule to set (json string)
 */
void setSchedule(String schedule) {
    log2.info "Thermostat::setSchedule - schedule: ${schedule}"
}

/**
 * Thermostat::fanmode
 * @param mode - fan mode to set: 'on', 'circulate', 'auto'
 */
void setThermostatFanMode(String mode) {
    log2.info "Thermostat::fanmode - mode: ${mode}"
}

/**
 * Thermostat::setThermostatMode
 * @param mode - thermostat mode to set: 'off', 'cool', 'heat', 'emergency heat', 'auto'
 */
void setThermostatMode(String mode) {
    log2.info "Thermostat::setThermostatMode - mode: ${mode}"
}


/**
 * Device::installed - called when the device is first created
 */
void installed() {
    device.updateSetting('logLevel', 3)
    log.info " ${device.getDisplayName()} : virtual thermostat created"
}
/**
 * Device::uninstalled - called when the device is removed
 */
void uninstalled() {
    log2.info "virtual thermostat uninstalled"
}
/**
 * Device::updated - called when the preferences of a device are updated
 */
void updated() {
    def logLevel = logLevelEnum?.isInteger() ? logLevelEnum.toInteger() : 3
    device.updateSetting('logLevel', logLevel)

    if(userAuth != '*************') {
        device.updateSetting('userAuthCrypt', encrypt(userAuth))
        device.updateSetting('userAuth', '*************')
        if(logLevel > 2) log.info " ${device.getDisplayName()} : encrypted password updated"
    }

    if(logLevel > 2) log.info " ${device.getDisplayName()} : virtual thermostat updated - logging level: ${['none','error','warn','info','debug','trace'].getAt(logLevel)}"
}


/**
 * creates child devices from the json response
 */
void createThermostats(thrmDataCollection) {
    if(!isRootDevice()) return // not callable from child device
    log2.debug "createThermostats - tstats: ${thrmDataCollection}"

    childDevices.each { childDevice ->
        deleteChildDevice(childDevice.deviceNetworkId)
    }

    thrmDataCollection.each { thrmData ->
        def childDevice = addChildDevice(device.typeName, thrmData.MacID.toLowerCase(), [label:thrmData.Name, isComponent:true])
        childDevice.setStateParams(thrmData)
        log.info "parent created child: ${childDevice}"
    }
}

/**
 * Set Thermostat state params
 */
void setStateParams(thrmData) {
    log2.info "setStateParams - thrmData: ${thrmData}"

    state.clear()
    state.lastUpdated = new Date().toString()

    state.deviceId = thrmData.DeviceID

    def tempUnit = thrmData.ThermostatData.DisplayUnits==1 ? '°F' : '°C'
    state.tempUnit = tempUnit

    state.status = thrmData.IsAlive ? 'online' : 'offline'
    state.modes = thrmData.ThermostatData.AllowedModes

    state.coolSetpointSchedule = thrmData.ThermostatData.ScheduleCoolSp
    state.coolSetpointRange = [thrmData.ThermostatData.MinCoolSetpoint, thrmData.ThermostatData.MaxCoolSetpoint]
    state.heatSetpointSchedule = thrmData.ThermostatData.ScheduleHeatSp
    state.heatSetpointRange = [thrmData.ThermostatData.MinHeatSetpoint, thrmData.ThermostatData.MaxHeatSetpoint]

    if(thrmData.ThermostatData.OutdoorTemperatureAvailable) {
        state.outdoorTemperature = thrmData.ThermostatData.OutdoorTemperature
    }
    if(thrmData.ThermostatData.OutdoorHumidityAvailable) {
        state.outdoorHumidity = thrmData.ThermostatData.OutdoorHumidity
    }

    device.sendEvent(name: 'temperature', value: thrmData.ThermostatData.IndoorTemperature, unit: tempUnit, descriptionText: "${device.displayName} temperature is ${thrmData.ThermostatData.IndoorTemperature} ${tempUnit}", isStateChange: true)
    device.sendEvent(name: 'humidity', value: thrmData.ThermostatData.IndoorHumidity, unit: '%', descriptionText: "${device.displayName} humidity is ${thrmData.ThermostatData.IndoorHumidity}%", isStateChange: true)
}

/**
 * Update Thermostat values
 */
void updateValues(thrmValues) {
    log2.info "updateValues - thrmValues: ${thrmValues}"

    if(thrmValues.Alerts) {
        state.alerts = thrmValues.Alerts
    } else {
        state.remove('alerts')
    }
    state.fanRunning = thrmValues.IsFanRunning

    device.sendEvent(name: 'temperature', value: thrmValues.DispTemp, unit: state.tempUnit, descriptionText: "${device.displayName} temperature is ${thrmValues.DispTemp} ${state.tempUnit}", isStateChange: true)
    device.sendEvent(name: 'humidity', value: thrmValues.IndoorHumi, unit: '%', descriptionText: "${device.displayName} humidity is ${thrmValues.IndoorHumi}%", isStateChange: true)
}

/**
 * xmlHttpRequest Post
 */
def xPost(path, query=[:]) {
    log2.debug "xPost - path: ${path}"

    def params = [
        uri: config.uri.tccSite,
        path: path,
        query: query << [page:1],
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

        def data = resp.getData().getBytes()
        json = parseJson(new String(data))
    }
    return json
}

/**
 * Populates Authentication Cookies
 */
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

    def status = 200
    try {
        httpPost(params) { resp ->
            status = resp.status
            log2.debug "webAuthenticate - status: ${status}"
            resp.getHeaders('Set-Cookie').each { cookie ->
                cookieMgr.process(cookie.value)
            }
            cookieMgr.save()
        }
    }
    catch(groovyx.net.http.HttpResponseException ex) {
        status = ex.getStatusCode()
        log2.error "${ex.getResponse()} - status: ${status}"
    }

    return status
}

/**
 * @return the device id of this thermostat device
 */
def getDeviceId() {
    return state.deviceId
}

/**
 *  @return true if this device is the root device
 */
def isRootDevice() {
    return (getParent() == null)
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
        if(!cookieList) return
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
    'error': { msg -> if(logLevel > 0) log.error " ${device.getDisplayName()} : ${msg}" },
    'warn' : { msg -> if(logLevel > 1) log.warn  " ${device.getDisplayName()} : ${msg}" },
    'info' : { msg -> if(logLevel > 2) log.info  " ${device.getDisplayName()} : ${msg}" },
    'debug': { msg -> if(logLevel > 3) log.debug " ${device.getDisplayName()} : ${msg}" },
    'trace': { msg -> if(logLevel > 4) log.trace " ${device.getDisplayName()} : ${msg}" },
]
