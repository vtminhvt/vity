metadata {
definition(name: " VITY ZigBee Switch 2T", namespace: "smartthings", author: "vtminhvt@gmail.com", ocfDeviceType: "oic.d.smartplug", mnmn: "SmartThings", vid: "generic-switch-power") {
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"
		capability "Health Check"
		capability "Switch"
		capability "Power Meter"

		command "childOn", ["string"]
		command "childOff", ["string"]


		// Dectect Vity
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0B04", deviceJoinName: "VITY Switch"
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0702", deviceJoinName: "VITY Switch"
	}
	

	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
			}
			tileAttribute("power", key: "SECONDARY_CONTROL") {
				attributeState "power", label: '${currentValue} W'
			}
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
		}

		main "switch"
		details(["switch", "refresh", "power"])
	}
    
}
def installed() {
	log.debug "Installed"
	updateDataValue("onOff", "catchall")
	createChildDevices()
}

def updated() {
	log.debug "Updated"
	updateDataValue("onOff", "catchall")
	refresh()
}


def parse(String description) {
	Map eventMap = zigbee.getEvent(description)
	Map eventDescMap = zigbee.parseDescriptionAsMap(description)

	if (eventMap) {
		if (eventDescMap?.sourceEndpoint == "01" || eventDescMap?.endpoint == "01") {
			sendEvent(eventMap)
		} else {
			def childDevice = childDevices.find {
				it.deviceNetworkId == "$device.deviceNetworkId:${eventDescMap.sourceEndpoint}" || it.deviceNetworkId == "$device.deviceNetworkId:${eventDescMap.endpoint}"
			}
			if (childDevice) {
				childDevice.sendEvent(eventMap)
			} else {
				log.debug "Child device: $device.deviceNetworkId:${eventDescMap.sourceEndpoint} was not found"
			}
		}
	}
}

private void createChildDevices() {
	def numberOfChildDevices = 2 //modelNumberOfChildDevices[device.getDataValue("model")] // Cập nhật số nút ấn công tắc
	log.debug("createChildDevices(), numberOfChildDevices: ${numberOfChildDevices}")

	for(def endpoint : 2..numberOfChildDevices) {
		try {
			log.debug "creating endpoint: ${endpoint}"
			addChildDevice("Child Switch Health Power", "${device.deviceNetworkId}:0${endpoint}", device.hubId,
				[completedSetup: true,
				 label: "${device.displayName[0..-2]}${endpoint}",
				 isComponent: false
				])
		} catch(Exception e) {
			log.debug "Exception: ${e}"
		}
	}
}

def on() {
	zigbee.on()
}

def off() {
	zigbee.off()
}

def childOn(String dni) {
	def childEndpoint = getChildEndpoint(dni)
	zigbee.command(zigbee.ONOFF_CLUSTER, 0x01, "", [destEndpoint: childEndpoint])
}

def childOff(String dni) {
	def childEndpoint = getChildEndpoint(dni)
	zigbee.command(zigbee.ONOFF_CLUSTER, 0x00, "", [destEndpoint: childEndpoint])
}

def ping() {
	refresh()
}

def refresh() {

	def refreshCommands = zigbee.onOffRefresh() + zigbee.electricMeasurementPowerRefresh()
	def numberOfChildDevices = 2 //modelNumberOfChildDevices[device.getDataValue("model")] // Cập nhật số nút ấn công tắc: 2 tương ứng 2 nút ấn
	for(def endpoint : 2..numberOfChildDevices) {
		refreshCommands += zigbee.readAttribute(zigbee.ONOFF_CLUSTER, 0x0000, [destEndpoint: endpoint])
		refreshCommands += zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x050B, [destEndpoint: endpoint])
	}
	log.debug "refreshCommands: $refreshCommands"
	return refreshCommands
}


def configureHealthCheck() {
	log.debug "configureHealthCheck"
	Integer hcIntervalMinutes = 12
	def healthEvent = [name: "checkInterval", value: hcIntervalMinutes * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID]]
	sendEvent(healthEvent)
	childDevices.each {
		it.sendEvent(healthEvent)
	}
}

private getChildEndpoint(String dni) {
	dni.split(":")[-1] as Integer
}


def configure() {
	log.debug "configure"
	configureHealthCheck()
	def numberOfChildDevices =2 //modelNumberOfChildDevices[device.getDataValue("model")] // Cập nhật số nút ấn công tắc
	def configurationCommands = zigbee.onOffConfig(0, 120) + zigbee.electricMeasurementPowerConfig()
	for(def endpoint : 2..numberOfChildDevices) {
		configurationCommands += zigbee.configureReporting(zigbee.ONOFF_CLUSTER, 0x0000, 0x10, 0, 120, null, [destEndpoint: endpoint])
		configurationCommands += zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x050B, 0x29, 1, 600, 0x0005, [destEndpoint: endpoint])
	}
	configurationCommands << refresh()
	log.debug "configurationCommands: $configurationCommands"
	return configurationCommands
}



private Boolean isVity() {
	device.getDataValue("manufacturer") == "VITY"
}

private getChildCount() {
	switch (device.getDataValue("model")) {
		case "V":
			return 1
		case "I":
			return 2
		case "T":
			return 3
		case "Y":
			return 4
		case "M":
		default:
			return 2
	}
}
