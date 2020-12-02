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
    version: [ '0.5.0' ],
    uri: [
        tccSite: 'https://mytotalconnectcomfort.com',
    ],
    path: [
        portal: '/portal/',
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
    log2.debug 'Initialize::initialize'

    def parent = getParent()
    if(parent) { // called on a child, pass up to root
        parent.initialize()
        return
    }
    // we are on the root object now

    state.clear()

    // force reauth
    def status = webAuthenticate();
    if((status < 200) || (status >= 400)) {
        log2.error "initialize - webAuthenticate failed with status: ${status}"
        if(status == 401) {
            state.error = '<span style="color:red">Web Authentication Failed (status: 401): Check email and password then try again.</span>'
        } else {
            state.error = "<span style='color:red'>Web Authentication Failed (status: ${status})</span>"
        }
        return
    }
    log2.info 'initialize - authentication succeeded'

    // recreate children
    childDevices.each { childDevice ->
        deleteChildDevice(childDevice.deviceNetworkId)
    }

    locationListDataUpdate()
}

/**
 * Polling::poll - refresh thermostat values
 */
void poll() {
    log2.debug 'Polling::poll'

    def parent = getParent()
    if(parent) { // called on a child, pass up to root
        parent.poll()
        return
    }
    // we are on the root object now

    locationListDataUpdate()
}

/**
 * Thermostat::off - set thermostat mode to 'off'
 */
void off() {
    log2.debug 'Thermostat::off'
    setThermostatMode('off')
}
/**
 * Thermostat::cool - set thermostat mode to 'cool'
 */
void cool() {
    log2.debug 'Thermostat::cool'
    setThermostatMode('cool')
}
/**
 * Thermostat::heat - set thermostat mode to 'heat'
 */
void heat() {
    log2.debug 'Thermostat::heat'
    setThermostatMode('heat')
}
/**
 * Thermostat::emergencyHeat - set thermostat mode to 'emergency heat'
 */
void emergencyHeat() {
    log2.debug 'Thermostat::emergencyHeat'
    setThermostatMode('emergency heat')
}
/**
 * Thermostat::auto - set thermostat mode to 'auto'
 */
void auto() {
    log2.debug 'Thermostat::auto'
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
    log2.debug 'Thermostat::fanCirculate'
    setThermostatFanMode('circulate')
}
/**
 * Thermostat::fanOn - set fan mode to 'on'
 */
void fanOn() {
    log2.debug 'Thermostat::fanOn'
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
    log.info " ${device.getDisplayName()} — virtual thermostat created"
}
/**
 * Device::uninstalled - called when the device is removed
 */
void uninstalled() {
    log2.info 'virtual thermostat uninstalled'
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
        log2.info 'encrypted password updated'
    }

    log2.info "virtual thermostat updated - logging level: ${['none','error','warn','info','debug','trace'].getAt(logLevel)} (${logLevel})"

    // auto-initialize the first time
    if(!state.locationId) {
        initialize()
    }

    // propogate logLevel
    childDevices.each { childDevice ->
        childDevice.updateSetting('logLevel', logLevel)
    }
}

/**
 * @params
 *   systemSwitch    [0: emheat, 1: heat, 2: off, 3: cool] automatic
 *   heatSetpoint    [50..90]
 *   coolSetpoint    [50..90]
 *   heatNextPeriod  [n 15mins from 00:00]
 *   coolNextPeriod  [n 15mins from 00:00]
 *   statusHeat      [0: follow schedule, 1: temporary hold, 2: permanent hold]
 *   statusCool      [0: follow schedule, 1: temporary hold, 2: permanent hold]
 *   fanMode         [0: auto, 1: on, 2: circulate]
 */
def submitControlScreen(params) {
    log2.debug 'submitControlScreen'

    if(!state.deviceId) {
        log2.error 'deviceId is required'
        return
    }

    state.remove('error')

    def settings = "{\"DeviceID\":${state.deviceId},\"SystemSwitch\":${params.systemSwitch},\"HeatSetpoint\":${params.heatSetpoint},\"CoolSetpoint\":${params.coolSetpoint},\"HeatNextPeriod\":${params.heatNextPeriod},\"CoolNextPeriod\":${params.coolNextPeriod},\"StatusHeat\":${params.statusHeat},\"StatusCool\":${params.statusCool},\"FanMode\":${params.fanMode}}"
    log2.debug "submitControlScreen - settings: ${settings}"

    try {
        def res = xPostAuth(config.path.scsc, null, settings)
    }
    catch(groovyx.net.http.HttpResponseException ex) {
        def status = ex.getStatusCode()
        log2.error "submitControlScreen - ${ex.getResponse()} - status: ${status}"
        state.error = "<span style='color:red'>Failed to update thermostat settings (status: ${status})</span>"
    }
}

/**
 * Execute a Location List Data refresh
 */
void locationListDataUpdate() {
    log2.debug 'locationListDataUpdate'

    state.remove('error')

    try {
        def lld = xPostAuth(config.path.glld, [page:1])?.first()
        if(!lld) {
            state.error = "<span style='color:red'>No Location List Data returned from server</span>"
            return
        }
        log2.debug "locationListDataUpdate - lld: ${lld}"

        state.locationId = lld.LocationID
        lld.Devices.each { thrmData ->
            def dev = getDeviceById(thrmData.DeviceID)
            if(!dev) {
                log2.info "device id: ${thrmData.DeviceID} does not exist, creating thermostat: ${thrmData.Name}"
                dev = addChildDevice(device.typeName, thrmData.MacID.toLowerCase(), [label:thrmData.Name, isComponent:true])
            }

            def devState = [
                lastUpdated:        new Date().toString(),
                deviceId:           thrmData.DeviceID,
                tempUnit:           thrmData.ThermostatData.DisplayUnits==1 ? '°F' : '°C',
                status:             thrmData.IsAlive ? 'online' : 'offline',
                modes:              thrmData.ThermostatData.AllowedModes,
                coolSetpointRange: [thrmData.ThermostatData.MinCoolSetpoint, thrmData.ThermostatData.MaxCoolSetpoint],
                heatSetpointRange: [thrmData.ThermostatData.MinHeatSetpoint, thrmData.ThermostatData.MaxHeatSetpoint],
            ]
            if(thrmData.ThermostatData.OutdoorTemperatureAvailable) {
                devState.outdoorTemperature = thrmData.ThermostatData.OutdoorTemperature
            }
            if(thrmData.ThermostatData.OutdoorHumidityAvailable) {
                devState.outdoorHumidity = thrmData.ThermostatData.OutdoorHumidity
            }
            dev.setState(devState)

            dev.sendEvent(name: 'temperature', value: thrmData.ThermostatData.IndoorTemperature, unit: devState.tempUnit, descriptionText: "${dev.displayName} temperature is ${thrmData.ThermostatData.IndoorTemperature} ${devState.tempUnit}", isStateChange: true)
            dev.sendEvent(name: 'humidity', value: thrmData.ThermostatData.IndoorHumidity, unit: '%', descriptionText: "${dev.displayName} humidity is ${thrmData.ThermostatData.IndoorHumidity}%", isStateChange: true)
            log2.info "${thrmData.Name}: temperature: ${thrmData.ThermostatData.IndoorTemperature} ${devState.tempUnit}"
            log2.info "${thrmData.Name}: humidity: ${thrmData.ThermostatData.IndoorHumidity}%"

            dev.sendEvent(name: 'coolingSetpoint', value: thrmData.ThermostatData.ScheduleCoolSp, unit: devState.tempUnit, descriptionText: "${dev.displayName} coolingSetpoint is ${thrmData.ThermostatData.ScheduleCoolSp} ${devState.tempUnit}", isStateChange: true)
            dev.sendEvent(name: 'heatingSetpoint', value: thrmData.ThermostatData.ScheduleHeatSp, unit: devState.tempUnit, descriptionText: "${dev.displayName} heatingSetpoint is ${thrmData.ThermostatData.ScheduleHeatSp} ${devState.tempUnit}", isStateChange: true)
            log2.info "${thrmData.Name}: coolingSetpoint: ${thrmData.ThermostatData.ScheduleCoolSp} ${devState.tempUnit}"
            log2.info "${thrmData.Name}: heatingSetpoint: ${thrmData.ThermostatData.ScheduleHeatSp} ${devState.tempUnit}"
        }
    }
    catch(groovyx.net.http.HttpResponseException ex) {
        def status = ex.getStatusCode()
        log2.error "locationListDataUpdate - ${ex.getResponse()} - status: ${status}"
        state.error = "<span style='color:red'>Failed to fetch Location List Data (status: ${status})</span>"
    }
}

/**
 * xmlHttpRequest Post with Authenticate & Retry
 */
def xPostAuth(path, query=null, body=null) {
    try {
        return xPost(path, query, body)
    }
    catch(groovyx.net.http.HttpResponseException ex) {
        def status = ex.getStatusCode() // if 401 then auth & retry
        if(status != 401) {
            log2.error "xPostAuth - xPost failed with status: ${status}"
            throw ex
        }
    }

    def status = webAuthenticate();
    if(status >= 400) {
        log2.error "xPostAuth - webAuthenticate failed with status: ${status}"
        throw new groovyx.net.http.HttpResponseException(status)
    }

    return xPost(path, query, body)
}

/**
 * xmlHttpRequest Post
 */
def xPost(path, query=null, body=null) {
    log2.debug "xPost - path: ${path}"

    def params = [
        uri: config.uri.tccSite,
        path: path,
        query: query,
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
        body: body,
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
    def userId = device.getSetting('userEmail')
    log2.debug "webAuthenticate - userEmail: ${userId}"
    def authCrypt = device.getSetting('userAuthCrypt')
    if(!(userId && authCrypt)){
        log2.error "webAuthenticate - both 'userEmail' and 'userAuth' are required"
        return 401
    }

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
        textParser: true,
        body: "timeOffset=240&UserName=${URLEncoder.encode(userId,'UTF-8')}&Password=${URLEncoder.encode(decrypt(authCrypt),'UTF-8')}&RememberMe=false",
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
        log2.error "webAuthenticate - ${ex.getResponse()} - status: ${status}"
    }
    return status
}

/**
 * Get device state
 */
def getState(name=null) {
    return name ? state[name] : state
}
/**
 * Replace device state
 */
void setState(newState) {
    state = newState
}
/**
 * Modify device state
 */
void updateState(newState) {
    state << newState
}
/**
 * Delete device state element
 */
void deleteState(name) {
    state.remove(name)
}

/**
 *  @return device for the specified device id
 *    device id 0 is an alias for the root device
 */
def getDeviceById(deviceId) {
    def parent = getParent() // search from the root
    if(parent) {
        parent.getDeviceById(deviceId)
        return
    }

    // is the requested device the root device?
    if((deviceId == 0) || (deviceId == state.deviceId)) return device

    // search child devices
    return childDevices.find {it.getState('deviceId') == deviceId}
}

/**
 *  @return true if this device is the root device
 */
def isRootDevice() {
    return (getParent() == null)
}

/**
 *  @return root device object instance
 */
def getRootDevice() {
    return getParent() ?: device
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
        def json = getRootDevice().getDataValue('cookies')
        log2.trace "cookieMgr.load - json: ${json}"
        try {
            def store = json ? parseJson(json) : []
            if(!store instanceof List) {
                log2.error('cookieMgr.load - stored data value "cookies" is not a list, flushing cookies')
                getRootDevice().removeDataValue('cookies')
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
        getRootDevice().updateDataValue('cookies', json)
    },
    'flush': {
        log2.debug 'cookieMgr.flush'
        cookieMgr.store = []
        getRootDevice().removeDataValue('cookies')
    }
]

// logging
@groovy.transform.Field Map log2 = [
    'error': { msg -> if(logLevel > 0) log.error " ${device.getDisplayName()} — ${msg}" },
    'warn' : { msg -> if(logLevel > 1) log.warn  " ${device.getDisplayName()} — ${msg}" },
    'info' : { msg -> if(logLevel > 2) log.info  " ${device.getDisplayName()} — ${msg}" },
    'debug': { msg -> if(logLevel > 3) log.debug " ${device.getDisplayName()} — ${msg}" },
    'trace': { msg -> if(logLevel > 4) log.trace " ${device.getDisplayName()} — ${msg}" },
]
