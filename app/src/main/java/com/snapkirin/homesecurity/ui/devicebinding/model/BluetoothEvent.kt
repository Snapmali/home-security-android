package com.snapkirin.homesecurity.ui.devicebinding.model

data class BluetoothEvent(
    val event: Int,
    val state: Boolean,
    val result: Boolean? = null,
    val failureCode: Int = 0
) {
    companion object {
        const val EVENT_REFRESHING = 1
        const val EVENT_CONNECTION = 2
        const val EVENT_ENABLED = 3

        const val EVENT_DEVICE_BINDING_STATUS = 4
        const val EVENT_DEVICE_CONNECTING_NETWORK = 5
        const val EVENT_DEVICE_CONNECTING_SERVER = 6
        const val EVENT_DEVICE_BINDING = 7

        const val FAILURE_BLUETOOTH_CONNECTION = 1
        const val FAILURE_WRITE_MESSAGE = 2
        const val FAILURE_READ_MESSAGE = 3

        const val FAILURE_NETWORK_CONNECTION_FAILURE = 4
        const val FAILURE_NETWORK_NOT_FOUND = 5

        const val FAILURE_BINDING_BOUND = 6
        const val FAILURE_BINDING_FAILURE = 7
    }
}
