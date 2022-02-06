package com.pke.slope_helper.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import java.util.*
import java.util.UUID


import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.github.douglasjunior.bluetoothclassiclibrary.*
import com.github.douglasjunior.bluetoothlowenergylibrary.BluetoothLeService
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName


data class MapperStatus(
    @SerializedName("version") val version: Int,
    @SerializedName("pos_east") val posEast: Int,
    @SerializedName("pos_north") val posNorth: Int,
    @SerializedName("pos_down") val posDown: Int,
    @SerializedName("acc_north") val accNorth: Int,
    @SerializedName("acc_east") val accEast: Int,
    @SerializedName("acc_down") val accDown: Int,
    @SerializedName("station_lon") val stationLon: Float,
    @SerializedName("station_lat") val stationLat: Float,
    @SerializedName("flags") val flags: Byte,
    @SerializedName("battery") val battery: Int,
    @SerializedName("station_battery") val stationBattery: Int,
    @SerializedName("packet_rate") val packetRate: Int,
    @SerializedName("rssi") val rssi: Int
)


class BleViewModel(context: Context): BluetoothService.OnBluetoothScanCallback,
    BluetoothService.OnBluetoothEventCallback {
    var config: BluetoothConfiguration
    var service: BluetoothService
    var writer: BluetoothWriter
    var scanning: Boolean

    // GUI ONLY VARS
    var scanningStatus: String
    var connectionStatus: String
    var dataReadStatus: String

    init {
        this.scanning = false
        this.config = BluetoothConfiguration()
        this.config.bluetoothServiceClass = BluetoothLeService::class.java
        this.config.context = context
        this.config.bufferSize = 1024
        this.config.characterDelimiter = '\n'
        this.config.deviceName = "GNSS Mapper"
        this.config.callListenersInMainThread = true
        this.config.uuid = null

        // TODO fix the missing XXXXX with input
        this.config.uuidService = UUID.nameUUIDFromBytes("3cfc0000-89d0-472c-ad37-80d868ffbaa5".toByteArray())
        this.config.uuidCharacteristic = UUID.nameUUIDFromBytes("3cfc4000-89d0-472c-ad37-80d868ffbaa5".toByteArray())

        BluetoothService.init(this.config);
        this.service = BluetoothService.getDefaultInstance()
        this.service.setOnScanCallback(this)
        this.service.setOnEventCallback(this)

        this.writer = BluetoothWriter(this.service)

        // GUI STUFF REMOVE WHEN DONE
        this.scanningStatus = "Off"
        this.connectionStatus = "None"
        this.dataReadStatus = ""
    }

    fun startStopScan() {
        if (!this.scanning) {
            this.scanningStatus = "Scanning"
            this.service.startScan()
        } else {
            this.scanningStatus = "Scanning canceled"
            this.service.stopScan()
        }
    }
    fun startScanning() {
        if (!this.scanning) {
            this.scanningStatus = "Scanning"
            this.service.startScan()
        }
    }
    fun stopScanning(){
        if (this.scanning) {
            this.scanningStatus = "Scanning stopped"
            this.service.stopScan()
        }
    }
    override fun onDeviceDiscovered(device: BluetoothDevice?, rssi: Int) {
        Log.d(TAG, "onDeviceDiscovered: " + device?.name + " - " + device?.address + " - " + Arrays.toString(device?.uuids))
        val dv = BluetoothDeviceDecorator(device, rssi)
        if(true){ // TODO make this only connect with selected device
            this.service.connect(dv.device)
        }
    }
    override fun onStartScan() {
        Log.d(TAG, "onStartScan")
        this.scanning = true
    }
    override fun onStopScan() {
        Log.d(TAG, "onStopScan")
        this.scanning = false
    }
    override fun onDataRead(buffer: ByteArray?, length: Int) {
        Log.d(TAG, "onDataRead: " + String(buffer!!, 0, length))
        this.sendMapperStatus(buffer, length)
    }
    override fun onStatusChange(status: BluetoothStatus?) {
        Log.d(TAG, "onStatusChange: $status")
        this.connectionStatus = BluetoothStatus.CONNECTED.toString()
        if (status == BluetoothStatus.CONNECTED) {
            //this.stopScanning()
        }else if (status == BluetoothStatus.CONNECTING) {

        }else if (status == BluetoothStatus.NONE) {

        }
    }
    override fun onDeviceName(deviceName: String?) {
        Log.d(TAG, "onDeviceName: $deviceName")
    }
    override fun onToast(message: String?) {
        Log.d(TAG, "onToast: $message")
    }
    override fun onDataWrite(buffer: ByteArray?) {
        Log.d(TAG, "onDataWrite: ")
    }
    private fun sendMapperStatus(buffer: ByteArray, length: Int){
        // TODO Properly parse this
        val rsp = String(buffer, 0, length)
        val data = Gson().fromJson(rsp, MapperStatus::class.java)
        this.postRequest(
                data,
                onCompletion = { response -> this.onCompletion(response) },
                onError = { response -> this.onError(response)}
            )
    }
    private fun onCompletion(response: Response){
        Log.d(TAG, "onCompletion: ${response.statusCode} ${response.body()}")
    }
    private fun onError(response: Response){
        Log.d(TAG, "onError: ${response.statusCode} ${response.body()}")
    }
    private fun postRequest(data: MapperStatus,
                            onCompletion: (Response) -> Unit ,
                            onError: (Response) -> Unit) {
        val json = Gson().toJson(data)
        var request = Fuel.post("http://127.0.0.0:8000").body(json)
        request.responseString { request, response, result ->
            responseHandle(
                request = request, response = response, result = result,
                onCompletion = onCompletion, onError = onError
            )
        }
    }
    private fun responseHandle(request: Request,
                               response: Response,
                               result: Result<String, FuelError>,
                               onCompletion: (Response) -> Unit,
                               onError:  (Response) -> Unit){
        when (result) {
            is Result.Failure -> {
                onError(response)
            }
            is Result.Success -> {
                onCompletion(response)
            }
        }
    }
    companion object {
        const val TAG = "BluetoothSlopeHelper"
    }
}