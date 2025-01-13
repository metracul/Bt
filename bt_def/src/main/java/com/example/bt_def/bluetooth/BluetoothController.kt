package com.example.bt_def.bluetooth
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log


class BluetoothController(
    private val context: Context,
    private val adapter: BluetoothAdapter,) {
    private var connectThread: ConnectThread? = null

    fun connect(mac: String, listener: Listener) {
        if (adapter.isEnabled && mac.isNotEmpty()) {
            try {
                val device = adapter.getRemoteDevice(mac)
                Log.d("MyLog", "getRemoteDevice")
                connectThread = ConnectThread(context, device, listener, )
                Log.d("MyLog", "Starting connection thread for device: $mac")
                connectThread?.start()
            } catch (e: IllegalArgumentException) {
                Log.e("MyLog", "Invalid MAC address: $mac", e)
            } catch (e: Exception) {
                Log.e("MyLog", "Error while initializing connection", e)
            }
        } else {
            Log.e("MyLog", "Bluetooth is disabled or MAC address is empty")
        }
    }

    fun sendMessage(message: String){
        connectThread?.sendMessage(message)
    }

    fun closeConnection(){
        connectThread?.closeConnection()
    }
    companion object{
        const val BLUETOOTH_CONNECTED = "bluetooth_connected"
        const val BLUETOOTH_NO_CONNECTED = "bluetooth_no_connected"
    }
    interface Listener{
        fun onReceive(message: String)
    }
}