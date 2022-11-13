package com.example.kmmblescanner

import platform.CoreBluetooth.*
import platform.Foundation.*
import platform.darwin.NSObject


class IOSBleScanner(val onSuccess: (String) -> Unit): BleScanner {
    private var centralManager: CBCentralManager? = null

    private var discoveredPeripheral: CBPeripheral? = null
    private var firstReadCharacteristic: CBCharacteristic? = null
    private var writeCharacteristic: CBCharacteristic? = null
    private var secondReadCharacteristic: CBCharacteristic? = null

    private var messageToCard: NSData? = null
    private var messageFromCard: NSData? = null
    private var defaultMessage: NSData? = "Wrong Password".nsdata()

    var userResponse = "Unknown"

    private fun successFun(string: String) {
        onSuccess(string)
    }



    override fun startScanner() {
        centralManager = CBCentralManager(centralDelegateImpl, null)
    }

    override fun stopScanner() {
        TODO("Not yet implemented")
    }

    private fun retrievePeripheral() {
        NSLog(centralManager.toString())
//        val connectedPeripherals = centralManager?.retrieveConnectedPeripheralsWithServices(listOf(TransferService.serviceUUID))

        centralManager?.scanForPeripheralsWithServices(listOf(TransferService.serviceUUID), null)
//        centralManager?.scanForPeripheralsWithServices(null, null)
    }

    private fun cleanup() {
        if (discoveredPeripheral?.state == CBPeripheralStateConnected) {
            centralManager?.cancelPeripheralConnection(discoveredPeripheral!!)
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun String.nsdata(): NSData? {
        return (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)
    }

    private fun NSData.string(): String? {
        return NSString.create(this, NSUTF8StringEncoding) as String?
    }

    private val centralDelegateImpl = object : NSObject(), CBCentralManagerDelegateProtocol {
        override fun centralManager(central: CBCentralManager, didDiscoverPeripheral: CBPeripheral,
                                    advertisementData: Map<Any?, *>, RSSI: NSNumber) {
            // Reject if the signal strength is too low to attempt data transfer.
            // Change the minimum RSSI value depending on your app’s use case.
            if (RSSI.intValue <= -60) {
                NSLog("Discovered peripheral not in expected range, at ${RSSI.intValue}")
                return
            }

            NSLog("Discovered ${didDiscoverPeripheral.name} at ${RSSI.intValue}")

            // Device is in range - have we already seen it?
            if (didDiscoverPeripheral != discoveredPeripheral) {

                // Save a local copy of the peripheral, so CoreBluetooth doesn't get rid of it.
                discoveredPeripheral = didDiscoverPeripheral

                // And finally, connect to the peripheral.
                NSLog("Connecting to peripheral $didDiscoverPeripheral")
                central.connectPeripheral(didDiscoverPeripheral, null)
//                centralManager.connect(peripheral, options: nil)
            }
        }

        override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
            NSLog("Peripheral Connected")

            // Stop scanning
            centralManager?.stopScan()
            NSLog("Scanning stopped")

            // Clear the data that we may already have
            //        data.removeAll(keepingCapacity: false)

            // Make sure we get the discovery callbacks
            didConnectPeripheral.delegate = peripheralDelegateImpl

            // Search only for services that match our UUID
            didConnectPeripheral.discoverServices(listOf(TransferService.serviceUUID))
        }

        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            when (central.state) {
                CBManagerStatePoweredOn -> retrievePeripheral()
                CBManagerStatePoweredOff -> return
                CBManagerStateResetting -> return
                CBManagerStateUnauthorized -> return
                CBManagerStateUnsupported -> return
                CBManagerStateUnknown -> return
                else -> return
            }
        }
    }

    private val peripheralDelegateImpl = object : NSObject(), CBPeripheralDelegateProtocol {
        override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
            if (didDiscoverServices != null) {
                NSLog("Error discovering services: ${didDiscoverServices.localizedDescription}")
                cleanup()
                return
            }

            // Discover the characteristic we want...

            // Loop through the newly filled peripheral.services array, just in case there's more than one.
            val peripheralServices = peripheral.services
            if (peripheralServices != null) {
                for (service in peripheralServices) {
                    peripheral.discoverCharacteristics(listOf(TransferService.firstReadCharacteristicUUID, TransferService.writeCharacteristicUUID, TransferService.secondReadCharacteristicUUID),
                        service as CBService
                    )
                }
            } else {
                NSLog("Services are null")
                return
            }
        }

        override fun peripheral(peripheral: CBPeripheral,
                                didDiscoverCharacteristicsForService: CBService, error: NSError?) {
            val serviceCharacteristics = didDiscoverCharacteristicsForService.characteristics
            if (serviceCharacteristics != null) {
                for (tempCharacteristic in serviceCharacteristics) {
                    val characteristic: CBCharacteristic = tempCharacteristic as CBCharacteristic
                    when (characteristic.UUID) {
                        TransferService.firstReadCharacteristicUUID -> firstReadCharacteristic = characteristic
                        TransferService.writeCharacteristicUUID -> writeCharacteristic = characteristic
                        TransferService.secondReadCharacteristicUUID -> secondReadCharacteristic = characteristic
                    }
                }
                peripheral.readValueForCharacteristic(firstReadCharacteristic!!)
//                val writeData: NSData? = "0".nsdata()
//                peripheral.writeValue(writeData!!, writeCharacteristic!!, CBCharacteristicWriteWithResponse)
//                peripheral.readValueForCharacteristic(secondReadCharacteristic!!)
            }
        }

        override fun peripheral(peripheral: CBPeripheral,
                                didUpdateValueForCharacteristic: CBCharacteristic, error: NSError?) {
            if (error != null) {
                NSLog("Error discovering characteristics: ${error.localizedDescription}")
                cleanup()
                return
            }
//            guard let characteristicData = characteristic.value,
//            let stringFromData = String(data: characteristicData, encoding: .utf8) else { return }
            var stringFromData: String? = ""
            if (didUpdateValueForCharacteristic.value != null) {
                stringFromData = didUpdateValueForCharacteristic.value!!.string()
            } else return

            // First Second Characteristic
            if (didUpdateValueForCharacteristic.UUID == TransferService.firstReadCharacteristicUUID) {
                NSLog("First Read. Received ${didUpdateValueForCharacteristic.value!!.length} bytes: $stringFromData")
                if (passwordCheck(stringFromData!!)) {
                    messageToCard = "0".nsdata()
                    peripheral.writeValue(messageToCard!!, writeCharacteristic!!, CBCharacteristicWriteWithResponse)
                    peripheral.readValueForCharacteristic(secondReadCharacteristic!!)
                } else {
                    peripheral.writeValue(messageToCard!!, writeCharacteristic!!, CBCharacteristicWriteWithResponse)
                }
            } // Read Second Characteristic
            else {
                NSLog("Second Time! Received ${didUpdateValueForCharacteristic.value!!.length} bytes: $stringFromData")
                if (stringFromData!!.first() != 'n') {
                    userResponse = stringFromData
                    successFun(userResponse)
                } else {
                    NSLog("They Said no")
                }
                cleanup()
            }

        }

    }
}

