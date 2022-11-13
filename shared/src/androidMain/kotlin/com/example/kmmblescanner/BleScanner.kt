package com.example.kmmblescanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log

class AndroidBleScanner(context: Context): BleScanner {
    @Suppress("PrivatePropertyName")
    private val TAG: String = "AndroidBleScanner"
    private val appCon = context

    private val bluetoothManager = appCon.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var chromeDevice: BluetoothDevice? = null

    var deviceName = ""
    var isScanning = false


    @SuppressLint("MissingPermission")
    override fun startScanner() {
        Log.d(TAG, "StartScan")
        if (!isScanning) {
            bluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), leScanCallback)
            isScanning = true
        }
    }

    override fun stopScanner() {
        TODO("Not yet implemented")
    }

    @SuppressLint("MissingPermission")
    private fun stopFindingDevices() {
        if (isScanning) {
            bluetoothLeScanner.stopScan(leScanCallback)
            isScanning = false
            Log.d(TAG, "Scanning Stopped Successfully")
        } else {
            Log.d(TAG, "Scanning Already Stopped")
        }
    }

    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG,"onScanResult: ${result.device.address} - ${result.device.name}")
            Log.d(TAG, "rssi:    " + result.rssi)
            chromeDevice = result.device
            deviceName = result.device.name
            Log.d(TAG, deviceName)
            stopFindingDevices()
//            result.device.connectGatt(appCon, false, gattCallback)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d(TAG,"onBatchScanResults:${results.toString()}")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "onScanFailed: $errorCode")
        }

    }

    private fun buildScanFilters(): List<ScanFilter> {
        val scanFilter = ScanFilter.Builder()
//                .setServiceUuid(ScanFilterService_UUID)
            .setDeviceName("Chrome Card")
            .build()
        Log.d(TAG, "buildScanFilters")
        return listOf(scanFilter)
    }

    private fun buildScanSettings() = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build()



}