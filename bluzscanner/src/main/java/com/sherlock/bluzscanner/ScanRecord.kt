package com.sherlock.bluzscanner

import android.os.ParcelUuid
import android.util.Log
import android.util.SparseArray
import androidx.annotation.Nullable
import java.util.*
import kotlin.experimental.and

class ScanRecord {
    private val TAG = "ScanRecord"

    // The following data type values are assigned by Bluetooth SIG.
    // For more details refer to Bluetooth 4.1 specification, Volume 3, Part C, Section 18.
    private val DATA_TYPE_FLAGS = 0x01
    private val DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02
    private val DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03
    private val DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04
    private val DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05
    private val DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06
    private val DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07
    private val DATA_TYPE_LOCAL_NAME_SHORT = 0x08
    private val DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09
    private val DATA_TYPE_TX_POWER_LEVEL = 0x0A
    private val DATA_TYPE_SERVICE_DATA_16_BIT = 0x16
    private val DATA_TYPE_SERVICE_DATA_32_BIT = 0x20
    private val DATA_TYPE_SERVICE_DATA_128_BIT = 0x21
    private val DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF

    // Flags of the advertising data.
    private var advertiseFlags: Int = 0

    private var serviceUuids: MutableList<ParcelUuid>? = null

    private var manufacturerSpecificData: SparseArray<ByteArray>? = null

    private var serviceData: Map<ParcelUuid, ByteArray?>? = null

    // Transmission power level(in dB).
    private var txPowerLevel: Int = 0

    // Local name of the Bluetooth LE device.
    private var deviceName: String? = null

    // Raw bytes of scan record.
    private var bytes: ByteArray? = null

    /**
     * Returns the advertising flags indicating the discoverable mode and capability of the device.
     * Returns -1 if the flag field is not set.
     */
    fun getAdvertiseFlags(): Int {
        return advertiseFlags
    }

    /**
     * Returns a list of service UUIDs within the advertisement that are used to identify the
     * bluetooth GATT services.
     */
    fun getServiceUuids(): List<ParcelUuid>? {
        return serviceUuids
    }

    /**
     * Returns a sparse array of manufacturer identifier and its corresponding manufacturer specific
     * data.
     */
    fun getManufacturerSpecificData(): SparseArray<ByteArray>? {
        return manufacturerSpecificData
    }

    /**
     * Returns the manufacturer specific data associated with the manufacturer id. Returns
     * `null` if the `manufacturerId` is not found.
     */
    fun getManufacturerSpecificData(manufacturerId: Int): ByteArray? {
        return manufacturerSpecificData?.get(manufacturerId)
    }

    /**
     * Returns a map of service UUID and its corresponding service data.
     */
    fun getServiceData(): Map<ParcelUuid, ByteArray?>? {
        return serviceData
    }

    /**
     * Returns the service data byte array associated with the `serviceUuid`. Returns
     * `null` if the `serviceDataUuid` is not found.
     */
    fun getServiceData(serviceDataUuid: ParcelUuid): ByteArray? {
        if (serviceDataUuid == null || serviceData == null) {
            return null
        } else {
            return serviceData?.get(serviceDataUuid)
        }
    }

    /**
     * Returns the transmission power level of the packet in dBm. Returns [Integer.MIN_VALUE]
     * if the field is not set. This value can be used to calculate the path loss of a received
     * packet using the following equation:
     *
     *
     * `pathloss = txPowerLevel - rssi`
     */
    fun getTxPowerLevel(): Int {
        return txPowerLevel
    }

    /**
     * Returns the local name of the BLE device. The is a UTF-8 encoded string.
     */
    fun getDeviceName(): String? {
        return deviceName
    }

    /**
     * Returns raw bytes of scan record.
     */
    fun getBytes(): ByteArray? {
        return bytes
    }

    constructor(){

    }

    constructor(@Nullable  serviceUuids: MutableList<ParcelUuid>? , @Nullable manufacturerData: SparseArray<ByteArray>? ,
                    @Nullable serviceData: Map<ParcelUuid, ByteArray?>? , advertiseFlags: Int, txPowerLevel: Int,
                    localName: String?,  bytes: ByteArray) {
        this.serviceUuids = serviceUuids;
        this.manufacturerSpecificData = manufacturerData;
        this.serviceData = serviceData;
        this.deviceName = localName;
        this.advertiseFlags = advertiseFlags;
        this.txPowerLevel = txPowerLevel;
        this.bytes = bytes;
    }

    /**
     * Parse scan record bytes to [ScanRecord].
     *
     *
     * The format is defined in Bluetooth 4.1 specification, Volume 3, Part C, Section 11 and 18.
     *
     *
     * All numerical multi-byte entities and values shall use little-endian **byte**
     * order.
     *
     * @param scanRecord The scan record of Bluetooth LE advertisement and/or scan response.
     */
    internal/* package */ fun parseFromBytes(scanRecord: ByteArray?): ScanRecord? {
        if (scanRecord == null) {
            return null
        }

        var currentPos = 0
        var advertiseFlag = -1
        var txPowerLevel = Integer.MIN_VALUE
        var localName: String? = null
        var serviceUuids: MutableList<ParcelUuid>? = null
        var manufacturerData: SparseArray<ByteArray>? = null
        var serviceData: MutableMap<ParcelUuid, ByteArray?>? = null
        var ff: Byte = 0xFF.toByte()

        try {
            while (currentPos < scanRecord.size) {
                // length is unsigned int.
                val length = (scanRecord.get(currentPos++) and ff)
                if (length == 0.toByte()) {
                    break
                }
                // Note the length includes the length of the field type itself.
                val dataLength = length - 1
                // fieldType is unsigned int.
                val fieldType = (scanRecord[currentPos++] and ff).toInt()
                when (fieldType) {
                    DATA_TYPE_FLAGS -> advertiseFlag = (scanRecord.get(currentPos) and ff).toInt()
                    DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL, DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE -> {
                        if (serviceUuids == null)
                            serviceUuids = ArrayList()
                        parseServiceUuid(
                            scanRecord, currentPos,
                            dataLength, BluetoothUuid.UUID_BYTES_16_BIT, serviceUuids
                        )
                    }
                    DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL, DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE -> {
                        if (serviceUuids == null)
                            serviceUuids = ArrayList()
                        parseServiceUuid(
                            scanRecord, currentPos, dataLength,
                            BluetoothUuid.UUID_BYTES_32_BIT, serviceUuids
                        )
                    }
                    DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL, DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE -> {
                        if (serviceUuids == null)
                            serviceUuids = ArrayList()
                        parseServiceUuid(
                            scanRecord, currentPos, dataLength,
                            BluetoothUuid.UUID_BYTES_128_BIT, serviceUuids
                        )
                    }
                    DATA_TYPE_LOCAL_NAME_SHORT, DATA_TYPE_LOCAL_NAME_COMPLETE -> localName = String(
                        extractBytes(scanRecord, currentPos, dataLength)
                    )
                    DATA_TYPE_TX_POWER_LEVEL -> txPowerLevel = scanRecord[currentPos].toInt()
                    DATA_TYPE_SERVICE_DATA_16_BIT, DATA_TYPE_SERVICE_DATA_32_BIT, DATA_TYPE_SERVICE_DATA_128_BIT -> {
                        var serviceUuidLength = BluetoothUuid.UUID_BYTES_16_BIT
                        if (fieldType == DATA_TYPE_SERVICE_DATA_32_BIT) {
                            serviceUuidLength = BluetoothUuid.UUID_BYTES_32_BIT
                        } else if (fieldType == DATA_TYPE_SERVICE_DATA_128_BIT) {
                            serviceUuidLength = BluetoothUuid.UUID_BYTES_128_BIT
                        }

                        val serviceDataUuidBytes = extractBytes(
                            scanRecord, currentPos,
                            serviceUuidLength
                        )
                        val serviceDataUuid = BluetoothUuid.parseUuidFrom(
                            serviceDataUuidBytes
                        )
                        val serviceDataArray = extractBytes(
                            scanRecord,
                            currentPos + serviceUuidLength, dataLength - serviceUuidLength
                        )
                        if (serviceData == null)
                            serviceData = HashMap()
                        serviceData[serviceDataUuid] = serviceDataArray
                    }
                    DATA_TYPE_MANUFACTURER_SPECIFIC_DATA -> {
                        // The first two bytes of the manufacturer specific data are
                        // manufacturer ids in little endian.
                        var ff = 0xFF
                        val manufacturerId =
                            (scanRecord.get(currentPos + 1) and ff.shl(8).toByte()) + (scanRecord[currentPos] and 0xFF.toByte())
                        val manufacturerDataBytes = extractBytes(
                            scanRecord, currentPos + 2,
                            dataLength - 2
                        )
                        if (manufacturerData == null)
                            manufacturerData = SparseArray()
                        manufacturerData.put(manufacturerId, manufacturerDataBytes)
                    }
                    else -> {
                    }
                }// Just ignore, we don't handle such data type.
                currentPos += dataLength as Int
            }

            return ScanRecord(
                serviceUuids, manufacturerData, serviceData,
                advertiseFlag, txPowerLevel, localName, scanRecord
            )
        } catch (e: Exception) {
            Log.e(TAG, "unable to parse scan record: " + Arrays.toString(scanRecord))
            Log.e(TAG, "exception: " + e.toString())
            // As the record is invalid, ignore all the parsed results for this packet
            // and return an empty record with raw scanRecord bytes in results
            return ScanRecord(
                null, null, null,
                -1, Integer.MIN_VALUE, null, scanRecord
            )
        }

    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null || javaClass != obj.javaClass) {
            return false
        }
        val other = obj as ScanRecord?
        return Arrays.equals(bytes, other!!.bytes)
    }

    override fun toString(): String {
        return ("ScanRecord [advertiseFlags=" + advertiseFlags + ", serviceUuids=" + serviceUuids
                + ", manufacturerSpecificData=" + BluetoothLeUtils.toString(manufacturerSpecificData)
                + ", serviceData=" + BluetoothLeUtils.toString(serviceData)
                + ", txPowerLevel=" + txPowerLevel + ", deviceName=" + deviceName + "]")
    }

    // Parse service UUIDs.
    private fun parseServiceUuid(
        scanRecord: ByteArray,
        currentPos: Int, dataLength: Int,
        uuidLength: Int,
        serviceUuids: MutableList<ParcelUuid>
    ): Int {
        var currentPos = currentPos
        var dataLength = dataLength
        while (dataLength > 0) {
            val uuidBytes = extractBytes(
                scanRecord, currentPos,
                uuidLength
            )
            serviceUuids.add(BluetoothUuid.parseUuidFrom(uuidBytes))
            dataLength -= uuidLength
            currentPos += uuidLength
        }
        return currentPos
    }

    // Helper method to extract bytes from byte array.
    private fun extractBytes(
        scanRecord: ByteArray,
        start: Int, length: Int
    ): ByteArray {
        val bytes = ByteArray(length)
        System.arraycopy(scanRecord, start, bytes, 0, length)
        return bytes
    }
}