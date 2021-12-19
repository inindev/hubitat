/**
 *  tasmota_device.groovy
 *
 *    Hubitat support for Tasmota based devices
 *
 *    Note: Uncomment the various capabilites below to enable
 *          features as appropriate for your Tasmota device.
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
            name: 'Tasmota Device',
            description: 'Hubitat driver for Tasmota based devices.',
            namespace: 'com.github.inindev',
            author: 'John Clark',
            importUrl: 'https://raw.githubusercontent.com/inindev/hubitat/main/drivers/tasmota_device.groovy',
    ) {
        // switch
        capability 'Bulb'
        capability 'Light'
        capability 'Outlet'
        capability 'Switch'
        // sensor
//        capability 'ContactSensor'
        // power
//        capability 'EnergyMeter'
//        attribute  'energyDuration', 'number'
//        capability 'PowerMeter'
//        capability 'VoltageMeasurement'
        // temp & humidity
//        capability 'RelativeHumidityMeasurement'
//        capability 'TemperatureMeasurement'
    }
}

preferences {
    if (isRootDevice()) {
        input name: 'tasmotaIp', type: 'text', title: 'Tasmota IP Address', description: '<small><i style="opacity:0.7">device ip address[:port]</i></small>'
        input name: 'relayCount', type: 'enum', title: 'Number of Relays', description: '<small><i style="opacity:0.7">(>1 creates child devices)</i></small>', options: ['1','2','3','4','5','6','7','8'], defaultValue: '1'
        input name: 'logInfo', type: 'bool', title: 'Enable info logging', defaultValue: true
        input name: 'logDebug', type: 'bool', title: 'Enable debug logging', defaultValue: false
    }
}


def off() {
    setRelayState(getIndex(), false)
}
def on() {
    setRelayState(getIndex(), true)
}

def setRelayState(num, state) {
    if (num < 0) return

    def parent = getParent()
    if (parent != null) {
        // we are a child device, send the command up to the parent device
        parent.setRelayState(num, state)
        return
    }

    log2.debug "setRelayState - num: ${num}  state: ${state}"

    if (!tasmotaIp) {
        log2.error 'the ip address of the target tasmota device has not been set'
        return
    }

    // num==0 means "all relays" if we have children
    if (num < 1) {
        if (haveChildren()) {
            log2.info "turning all child relays ${state?'on':'off'}"
        } else {
            num = 1
        }
    }
    else if (num > 8) {
        log2.error "relay number: ${num} is out of range (n > 8)"
        return
    }

    def params = [uri:"http://${tasmotaIp}", path:'/cm', query:[cmnd: "Power${num} ${state?'on':'off'}"]]
    asynchttpGet(httpCallback, params)
}
def httpCallback(resp, data) {
    if (resp.status == 200) {
        log2.debug "http 200 ok - json: ${resp.json}"
    } else {
        log2.error "http response code ${resp.status}"
    }
}

def installed() {
}
def uninstalled() {
}
def updated() {
    if (!isRootDevice()) return // not callable from child device

    if (logDebug) device.updateSetting('logInfo', true) // debug implies info

    // update device network id
    if (tasmotaIp) {
        def ip = tasmotaIp.tokenize(':').first()
        //def mac = getMACFromIP(ip) // mac address nids do not work with multiple subnets
        def ipHex = ip.tokenize('.').collect{intToHexStr(it as int)}.join()
        def dnid = device.getDeviceNetworkId()
        if (ipHex != dnid) {
            log2.info "updating Device Network Id to: ${ipHex}"
            device.setDeviceNetworkId(ipHex)
        }

        state.device = "<a target='_blank' href=\'http://${tasmotaIp}\'>http://${tasmotaIp}</a>"
    } else {
        state.remove('device')
    }

    createSwitches(relayCount?.isInteger() ? relayCount.toInteger() : 1)
}

def parse(msg) {
    def json = parseLanMessage(msg)?.json
    if (!json) return
    log2.trace "parse - json: ${json}"

    def tempUnit
    json.each { key, val ->
        key = key.toLowerCase()
        log2.trace "evaluating  key: ${key}  val: ${val}"

        // power state updated
        if (key.startsWith('power')) {
            def idxStr = key.substring(5)
            def idx = idxStr?.isInteger() ? idxStr.toInteger() : 1
            val = val.toLowerCase()
            log2.info "switch ${idx} reported state '${val}'"

            def dev = getDevice(idx)
            if (!dev) {
                log2.warn "unable to find the device for index ${idx}"
                return;
            }

            dev.sendEvent(name: 'switch', value: val, descriptionText: "${dev.displayName} switch is ${val}")
            if (dev.hasCapability('ContactSensor')) {
                def oc = val.equals('on') ? 'open' : 'closed'
                dev.sendEvent(name: 'contact', value: oc, descriptionText: "${dev.displayName} contacts are ${oc}")
            }
        }

        // energy
        else if (key.equals('energy')) {
            if (device.hasCapability('EnergyMeter')) {
                def energy = val['Total']
                device.sendEvent(name: 'energy', value: energy, unit: 'kWh', descriptionText: "${device.displayName} energy is ${energy} kWh")

                def totalStartTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss", val['TotalStartTime'])
                def energyDuration = (now() - totalStartTime.getTime()) / 86400000 // 1000 * 3600 * 24
                def energyDurationStr = String.format("%.3f", energyDuration)
                device.sendEvent(name: 'energyDuration', value: energyDurationStr, unit: 'Days', descriptionText: "${device.displayName} energyDuration is ${energyDurationStr} Days")
            }
            if (device.hasCapability('PowerMeter')) {
                def power = val['Power']
                device.sendEvent(name: 'power', value: power, unit: 'W', descriptionText: "${device.displayName} power is ${power} W")
            }
            if (device.hasCapability('VoltageMeasurement')) {
                def voltage = val['Voltage']
                device.sendEvent(name: 'voltage', value: voltage, unit: 'V', descriptionText: "${device.displayName} voltage is ${voltage} V")
            }
        }

        // temperature unit
        else if (key.equals('tempunit')) {
            tempUnit = "°${val}"
        }
        // si7021: humidity / temperature
        else if (key.equals('si7021')) {
            if (device.hasCapability('RelativeHumidityMeasurement')) {
                def humid = val['Humidity']
                device.sendEvent(name: 'humidity', value: humid, unit: '%', descriptionText: "${device.displayName} humidity is ${humid}%")
            }
            if (device.hasCapability('TemperatureMeasurement')) {
                def temp = val['Temperature']
                device.sendEvent(name: 'temperature', value: temp, unit: tempUnit, descriptionText: "${device.displayName} temperature is ${temp} ${tempUnit}")
            }
        }
        // ds18b20: temperature
        else if (key.equals('ds18b20')) {
            if (device.hasCapability('TemperatureMeasurement')) {
                def temp = val['Temperature']
                device.sendEvent(name: 'temperature', value: temp, unit: tempUnit, descriptionText: "${device.displayName} temperature is ${temp} ${tempUnit}")
            }
        }

        // wifi stats
        else if (key.equals('wifi')) {
            log2.info "wifi: ${val}"
            state.wifi = "ssid: ${val.SSId}, channel: ${val.Channel}, rssi: ${val.RSSI}, signal: ${val.Signal}, reconnects: ${val.LinkCount}"
        }
    }
}

/**
 * creates the specified number of child devices
 */
def createSwitches(int num) {
    if (!isRootDevice()) return // not callable from child device
    log2.info "createSwitches - setting child device count to: ${num}"

    // map of child devices to create: [idx:'label-idx']
    def labelNames = [:]
    if (num > 1) {
        for(int i=1; i<=num && i<9; i++) {
            labelNames[i] = "${device.label}-${i}"
        }
    }

    // do not rename labels
    childDevices.each { childDevice ->
        int i = childDevice.getIndex()
        if (labelNames.containsKey(i)) {
            labelNames[i] = childDevice.label
        }
        deleteChildDevice(childDevice.getDeviceNetworkId())
    }

    // (re)create children
    def dni = device.getDeviceNetworkId()
    labelNames.each { i, label ->
        def name = "${device.name} - Child${i}"
        log2.info "creating child device: ${name}"
        def childDevice = addChildDevice(device.typeName, "${dni}-${i}", [label:label, isComponent:true])
        childDevice.updateSetting('logInfo', logDebug ?: logInfo)
        childDevice.updateSetting('logDebug', logDebug)
    }

    if (num > 1) {
        device.sendEvent(name: 'switch', value: ' ', descriptionText: 'reset composite device state')
    }
}

/**
 *  @return the index of this device
 *    0 : root device
 *    n : child device index
 *   -1 : error
 */
def getIndex() {
    if (isRootDevice()) return 0
    String val = device.label[-1]
    return val?.isInteger() ? val.toInteger() : -1
}

/**
 *  @return true if this device is the root device
 */
def isRootDevice() {
    return (getParent() == null)
}

/**
 *  @return true if this device has child devices
 */
def haveChildren() {
    return (childDevices?.size() > 0)
}

/**
 *  @return the device object for the specified index
 */
def getDevice(int idx) {
    if (idx < 0) return null

    // index zero is the root device
    if (idx == 0) {
        return getParent() ?: device
    }

    def childDevices = getChildDevices()
    if (childDevices.size() < 1) {
        // no child devices, index 1 means root device
        return (idx == 1) ? device : null
    }
    return childDevices.find { it.getIndex() == idx }
}

// logging
@groovy.transform.Field Map log2 = [
    'error': { msg -> /* always */ log.error "${device.getDisplayName()} — ${msg}" },
    'warn' : { msg -> /* always */ log.warn  "${device.getDisplayName()} — ${msg}" },
    'info' : { msg -> if(logInfo)  log.info  "${device.getDisplayName()} — ${msg}" },
    'debug': { msg -> if(logDebug) log.debug "${device.getDisplayName()} — ${msg}" },
    'trace': { msg -> if(logDebug) log.trace "${device.getDisplayName()} — ${msg}" },
]
