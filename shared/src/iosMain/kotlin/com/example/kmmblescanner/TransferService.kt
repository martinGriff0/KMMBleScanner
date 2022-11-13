package com.example.kmmblescanner

import platform.CoreBluetooth.*
import platform.Foundation.*

class TransferService {
    companion object {
        val serviceUUID = CBUUID.UUIDWithString("00002415-0000-1000-8000-00805F9B34FB")
        val firstReadCharacteristicUUID = CBUUID.UUIDWithString("00002425-0000-1000-8000-00805f9b34fb")
        val writeCharacteristicUUID = CBUUID.UUIDWithString("00002426-0000-1000-8000-00805f9b34fb")
        val secondReadCharacteristicUUID = CBUUID.UUIDWithString("00002427-0000-1000-8000-00805f9b34fb")
    }
}