/**
 *  virtual_contact_switch.groovy
 *
 *    Hubitat driver for a virtual switch with a contact sensor.
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
            name: 'Virtual Contact Switch',
            description: 'Hubitat driver for a virtual switch with a contact sensor.',
            namespace: 'com.github.inindev',
            author: 'John Clark',
            importUrl: 'https://raw.githubusercontent.com/inindev/hubitat/main/drivers/virtual_contact_switch.groovy',
    ) {
        capability 'Actuator'
        capability 'Switch'
        capability 'Sensor'
        capability 'ContactSensor'
    }
}

preferences {
    input name: 'logInfo', type: 'bool', title: 'Enable info logging', defaultValue: true
}

// https://docs.hubitat.com/index.php?title=Driver_Capability_List
def off() {
    if (logInfo) log.info 'off command'
    sendEvent(name: 'switch', value: 'off', descriptionText: 'switch turned off')
    sendEvent(name: 'contact', value: 'open', descriptionText: 'contacts open')
}
def on() {
    if (logInfo) log.info 'on command'
    sendEvent(name: 'switch', value: 'on', descriptionText: 'switch turned on')
    sendEvent(name: 'contact', value: 'closed', descriptionText: 'contacts closed')
    runIn(8, off)
}

def installed() {
    //log.info 'installed()'
}
def uninstalled() {
    //log.info 'uninstalled()'
}
def updated() {
    //log.info 'updated()'
}
def parse(msg) {
    //log.info 'parse(msg)'
}

