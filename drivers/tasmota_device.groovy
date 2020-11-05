/**
 *  tasmota_device.groovy
 *
 *    Hubitat support for Tasmota based devices.
 *
 *    Copyright 2020 John Clark
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
        capability 'Bulb'
        capability 'Light'
        capability 'Outlet'
        capability 'Switch'
//        capability 'ContactSensor'
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
    if (logDebug) log.debug 'off command'
    setRelayState(getIndex(), false)
}
def on() {
    if (logDebug) log.debug 'on command'
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

    // we are the root device
    if (!tasmotaIp) {
        log.error 'the ip address of the tasmota switch has not been set'
        return
    }

    // as the root device, num==0 means "all relays" if we have children
    if (num < 1) {
        if (haveChildren()) {
            if (logInfo) log.info "turning all child relays ${state?'on':'off'}"
        } else {
            num = 1
        }
    }
    else if (num > 8) {
        log.error "relay number: ${num} is out of range (n > 8)"
        return
    }

    def params = [uri:"http://${tasmotaIp}", path:'/cm', query:[cmnd: "Power${num} ${state?'on':'off'}"]]
    asynchttpGet(httpCallback, params)
}
def httpCallback(resp, data) {
    if (resp.status != 200) {
        log.error "http response code ${resp.status}"
    } else {
        def json = resp.getJson()
        if (logDebug) log.debug "http 200 ok - json: ${json}"

        int nOff = 0
        int nOn = 0
        if ((json?.size() > 1) && haveChildren()) {
            if (logDebug) log.trace 'processing composite device result'
            json.each { key, val ->
                key = key.toLowerCase()
                val = val.toLowerCase()
                if (logDebug) log.trace "evaluating  key: ${key}  val: ${val}"

                if (val.equals('on')) {
                    nOn++
                } else {
                    nOff++
                }
            }

            def cOnOff
            def cOpenClose
            if ((nOff > 0) && (nOn == 0)) {
                if (logDebug) log.debug "all child devices are off: ${nOff}, setting composite state to 'off'"
                cOnOff = 'off'
                cOpenClose = 'closed'
            }
            else if ((nOff == 0) && (nOn > 0)) {
                if (logDebug) log.debug "all child devices are on: ${nOn}, setting composite state to 'on'"
                cOnOff = 'on'
                cOpenClose = 'open'
            }
            else {
                if (logDebug) log.debug "some child devices are on: ${nOn}, some child devices are off: ${nOff}, setting composite state to 'mixed'"
                cOnOff = 'mixed'
                cOpenClose = 'mixed'
            }

            def devName = device.label ?: device.name
            device.sendEvent(name: 'switch', value: cOnOff, descriptionText: "${devName} (switch 0) is now ${cOnOff}", isStateChange: true)
            if (device.hasCapability('ContactSensor')) {
                device.sendEvent(name: 'contact', value: cOpenClose, descriptionText: "${devName} (contact 0) is now ${cOpenClose}", isStateChange: true)
            }

        }
    }
}

def installed() {
    //log.info 'installed()'
}
def uninstalled() {
    //log.info 'uninstalled()'
}
def updated() {
    if (!isRootDevice()) return // not callable from child device
    //log.info 'updated()'

    // update device network id
    if (tasmotaIp) {
        def ip = tasmotaIp.tokenize(':').first()
        //def mac = getMACFromIP(ip) // mac address nids do not work with multiple subnets
        def ipHex = ip.tokenize('.').collect{intToHexStr(it as int)}.join()
        def dnid = device.getDeviceNetworkId()
        if (ipHex != dnid) {
            if (logInfo) log.info "updating Device Network Id to: ${ipHex}"
            device.setDeviceNetworkId(ipHex)
        }
    }

    createSwitches(relayCount?.isInteger() ? relayCount.toInteger() : 1)
}

def parse(msg) {
    def json = parseLanMessage(msg)?.json
    if (!json) return
    if (logDebug) log.trace "parse - json: ${json}"

    json.each { key, val ->
        key = key.toLowerCase()
        if (logDebug) log.trace "evaluating  key: ${key}  val: ${val}"

        // power state updated
        if (key.startsWith('power')) {
            def idxStr = key.substring(5)
            def idx = idxStr?.isInteger() ? idxStr.toInteger() : 1
            val = val.toLowerCase()
            if (logInfo) log.info "switch ${idx} reported state '${val}'"

            def dev = getDevice(idx)
            if (!dev) {
                log.error "unable to find the device for index ${idx}"
                return;
            }

            def devName = dev.label ?: dev.name
            dev.sendEvent(name: 'switch', value: val, descriptionText: "${devName} (switch ${idx}) is now ${val}", isStateChange: true)
            if (dev.hasCapability('ContactSensor')) {
                def oc = val.equals('on') ? 'open' : 'closed'
                dev.sendEvent(name: 'contact', value: oc, descriptionText: "${devName} (contact ${idx}) is now ${oc}", isStateChange: true)
            }
        }

        // wifi stats
        else if (key.equals('wifi')) {
            if (logInfo) log.info "${device.label ?: device.name} ${val}"
        }
    }
}

/**
 * creates the specified number of child devices
 */
def createSwitches(int num) {
    if (!isRootDevice()) return // not callable from child device
    if (logInfo) log.info "createSwitches - setting child device count to: ${num}"

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
    def mac = device.getDeviceNetworkId()
    labelNames.each { i, label ->
        def name = "${device.name} - Child${i}"
        if (logInfo) log.info "creating child device: ${name}"
        addChildDevice('Tasmota Device', "${mac}-${i}", [name:name, label:label, isComponent:true])
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
    String val = device.name[-1]
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
    if (idx == 0) return device

    def cd = getChildDevices()
    if (cd.size() < 1) {
        // no child devices, index 1 means root device
        return (idx == 1) ? device : null
    }

    return cd.find { it.getIndex() == idx }
}
