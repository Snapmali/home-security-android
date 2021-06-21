package com.snapkirin.homesecurity.network.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import com.snapkirin.homesecurity.model.WifiAuthentication
import com.snapkirin.homesecurity.model.json.BindDeviceBTMessage
import com.snapkirin.homesecurity.model.json.DeviceBTResponse
import com.snapkirin.homesecurity.network.NetworkGlobals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothConnection(private val handler: Handler) {

    companion object {
        private const val TAG = "BluetoothConnection"

        const val SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"

        const val RECV_MSG_NETWORK = 1
        const val RECV_MSG_BINDING = 2
        const val RECV_MSG_BINDING_STATUS = 3
        const val RECV_MSG_LOGIN = 4

        const val MESSAGE_CONNECTION_SUCCESS = 0
        const val MESSAGE_READ = 1
        const val MESSAGE_CONNECTION_FAILURE = 2
        const val MESSAGE_WRITE_FAILURE = 3
        const val MESSAGE_READ_FAILURE = 4
    }

    private val moshi = NetworkGlobals.moshi
    private val responseAdapter = moshi.adapter(DeviceBTResponse::class.java)

    private var connection: ConnectThread? = null
    private var isClosed = true

    fun connect(device: BluetoothDevice) {
        connection = ConnectThread(device)
        connection?.start()
    }

    fun sendBindMessage(ssid: String, wifiPassword: String?, userId: Long, wifiAuthentication: WifiAuthentication) {
        val json = moshi.adapter(BindDeviceBTMessage::class.java).toJson(
            BindDeviceBTMessage(
                1,
                ssid,
                wifiAuthentication.keyManagement,
                wifiAuthentication.cipher,
                wifiPassword,
                userId
            )
        )
        Log.e("send", json)
        GlobalScope.launch(Dispatchers.IO) {
            for (i in 1..5) {
                Log.e("send", "sending")
                if (!isClosed) {
                    connection?.write(json.toByteArray(Charsets.UTF_8))
                    return@launch
                }
                else
                    delay(2000L)
            }
        }
    }

    fun cancel() {
        connection?.cancel()
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket by lazy(LazyThreadSafetyMode.NONE) {
            device.javaClass.getMethod(
                "createRfcommSocket", Int::class.javaPrimitiveType
            ).invoke(device, 1) as BluetoothSocket
//            device.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))
        }

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024)

        override fun run() {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

            try {
                mmSocket.use { socket ->
                    socket.connect()
                    handler.obtainMessage(MESSAGE_CONNECTION_SUCCESS).sendToTarget()
                    read()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when connecting", e)

                val connectionErrorMsg = handler.obtainMessage(
                    MESSAGE_CONNECTION_FAILURE, -1, -1,
                    "Couldn't connect to the other device")
                connectionErrorMsg.sendToTarget()
            }
        }

        private fun read() {
            isClosed = false
            while (true) {
                var length: Int
                try {
                    length = mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    if (!isClosed)
                        handler.obtainMessage(
                            MESSAGE_READ_FAILURE, -1, -1,
                            "Input stream was disconnected").sendToTarget()
                    break
                }
                try {
                    val m = String(mmBuffer, 0, length)
                    val response = responseAdapter.fromJson(m)
                    val readMsg = handler.obtainMessage(MESSAGE_READ, response!!.type, response.code,
                        response.message)
                    readMsg.sendToTarget()
                } catch (e: Throwable) {
                    Log.e(TAG, "Error occurred when decoding data", e)
                    val readErrorMsg = handler.obtainMessage(
                        MESSAGE_READ_FAILURE, -1, -1,
                        "Error occurred when decoding data")
                    readErrorMsg.sendToTarget()
                    break
                }
            }
            isClosed = true
        }

        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                val writeErrorMsg = handler.obtainMessage(
                    MESSAGE_WRITE_FAILURE, -1, -1,
                    "Couldn't send data to the other device")
                writeErrorMsg.sendToTarget()
                return
            }
        }

        fun cancel() {
            try {
                isClosed = true
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
}