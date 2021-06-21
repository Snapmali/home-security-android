package com.snapkirin.homesecurity.model

object DeviceList {

    private val deviceList = mutableListOf<Device>()
    private val deviceMap = HashMap<Long, Device>()

    fun get(id: Long) : Device? {
        return deviceMap[id]
    }

    fun add(device: Device) : Int {
        deviceList.add(device)
        deviceMap[device.id] = device
        return deviceList.size - 1
    }

    fun add(index: Int, device: Device) {
        deviceList.add(index, device)
        deviceMap[device.id] = device
    }

    fun addAllWithSorting(index: Int, devices: Collection<Device>) {
        addAll(index, devices)
        deviceList.sortByDescending { device: Device -> device.bindingTime }
    }

    fun addAll(index: Int, devices: Collection<Device>) {
        deviceList.addAll(index, devices)
        for (device in devices) {
            deviceMap[device.id] = device
        }
    }

    fun remove(index: Int) {
        val device = deviceList.removeAt(index)
        deviceMap.remove(device.id)
    }

    fun remove(id: Long) : Int? {
        val device = deviceMap.remove(id) ?: return null
        val index = deviceList.indexOf(device)
        deviceList.removeAt(index)
        return index
    }

    fun remove(device: Device) : Int? {
        deviceMap.remove(device.id) ?: return null
        val index = deviceList.indexOf(device)
        deviceList.removeAt(index)
        return index
    }

    fun getDeviceName(id: Long) : String? {
        val device = deviceMap[id] ?: return null
        return device.name
    }

    fun updateDeviceName(id: Long, name: String) : Int? {
        val device = deviceMap[id] ?: return null
        device.name = name
        return deviceList.indexOf(device)
    }

    fun updateDeviceInfo(new: Device): Int? {
        val device = deviceMap[new.id] ?: return null
        device.name = new.name
        device.online = new.online
        device.monitoring = new.monitoring
        device.streaming = new.streaming
        return deviceList.indexOf(device)
    }

    fun updateDeviceStatus(status: DeviceStatus): Int? {
        val device = deviceMap[status.deviceId] ?: return null
        device.online = status.online
        device.monitoring = status.monitoring
        device.streaming = status.streaming
        return deviceList.indexOf(device)
    }

    fun getList() : MutableList<Device> {
        return deviceList
    }

    fun size() = deviceList.size

}