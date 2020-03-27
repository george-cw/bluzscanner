package com.sherlock.bluzscanner

import android.bluetooth.BluetoothAdapter
import android.os.Parcel
import android.os.ParcelUuid
import android.os.Parcelable
import androidx.annotation.Nullable
import java.util.*
import kotlin.experimental.and

class ScanFilter private constructor(builder: Builder): Parcelable{
    private var deviceName: String? = null
    private var deviceAddress: String? = null

    private var serviceUuid: ParcelUuid? = null
    private var serviceUuidMask: ParcelUuid? = null

    private var serviceDataUuid: ParcelUuid? = null
    private var serviceData: ByteArray? = null
    private var serviceDataMask: ByteArray? = null

    private var manufacturerId: Int = 0
    private var manufacturerData: ByteArray? = null
    private var manufacturerDataMask: ByteArray? = null

    //private var EMPTY = ScanFilter.Builder().build()

    init {
        this.deviceName = builder.deviceName
        this.serviceUuid = builder.serviceUuid
        this.serviceUuidMask = builder.uuidMask
        this.deviceAddress = builder.deviceAddress
        this.serviceDataUuid = builder.serviceDataUuid
        this.serviceData = builder.serviceData
        this.serviceDataMask = builder.serviceDataMask
        this.manufacturerId = builder.manufacturerId
        this.manufacturerData = builder.manufacturerData
        this.manufacturerDataMask = builder.manufacturerDataMask
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(if (deviceName == null) 0 else 1)
        if (deviceName != null) {
            dest.writeString(deviceName)
        }
        dest.writeInt(if (deviceAddress == null) 0 else 1)
        if (deviceAddress != null) {
            dest.writeString(deviceAddress)
        }
        dest.writeInt(if (serviceUuid == null) 0 else 1)
        if (serviceUuid != null) {
            dest.writeParcelable(serviceUuid, flags)
            dest.writeInt(if (serviceUuidMask == null) 0 else 1)
            if (serviceUuidMask != null) {
                dest.writeParcelable(serviceUuidMask, flags)
            }
        }
        dest.writeInt(if (serviceDataUuid == null) 0 else 1)
        if (serviceDataUuid != null) {
            dest.writeParcelable(serviceDataUuid, flags)
            dest.writeInt(if (serviceData == null) 0 else 1)
            if (serviceData != null) {
                dest.writeInt(serviceData!!.size)
                dest.writeByteArray(serviceData)

                dest.writeInt(if (serviceDataMask == null) 0 else 1)
                if (serviceDataMask != null) {
                    dest.writeInt(serviceDataMask!!.size)
                    dest.writeByteArray(serviceDataMask)
                }
            }
        }
        dest.writeInt(manufacturerId)
        dest.writeInt(if (manufacturerData == null) 0 else 1)
        if (manufacturerData != null) {
            dest.writeInt(manufacturerData!!.size)
            dest.writeByteArray(manufacturerData)

            dest.writeInt(if (manufacturerDataMask == null) 0 else 1)
            if (manufacturerDataMask != null) {
                dest.writeInt(manufacturerDataMask!!.size)
                dest.writeByteArray(manufacturerDataMask)
            }
        }
    }

    /**
     * A [android.os.Parcelable.Creator] to create [ScanFilter] from parcel.
     */
    @JvmField
    val CREATOR: Parcelable.Creator<ScanFilter> = object : Parcelable.Creator<ScanFilter> {

        override fun newArray(size: Int): Array<ScanFilter?> {
            return arrayOfNulls(size)
        }

        override fun createFromParcel(`in`: Parcel): ScanFilter {
            val builder = Builder()
            if (`in`.readInt() == 1) {
                builder.setDeviceName(`in`.readString())
            }
            if (`in`.readInt() == 1) {
                builder.setDeviceAddress(`in`.readString())
            }
            if (`in`.readInt() == 1) {
                val uuid = `in`.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)
                builder.setServiceUuid(uuid)
                if (`in`.readInt() == 1) {
                    val uuidMask = `in`.readParcelable<ParcelUuid>(
                        ParcelUuid::class.java.classLoader
                    )
                    builder.setServiceUuid(uuid, uuidMask)
                }
            }
            if (`in`.readInt() == 1) {
                val serviceDataUuid =
                    `in`.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)
                if (`in`.readInt() == 1) {
                    val serviceDataLength = `in`.readInt()
                    val serviceData = ByteArray(serviceDataLength)
                    `in`.readByteArray(serviceData)
                    if (`in`.readInt() == 0) {

                        builder.setServiceData(serviceDataUuid!!, serviceData)
                    } else {
                        val serviceDataMaskLength = `in`.readInt()
                        val serviceDataMask = ByteArray(serviceDataMaskLength)
                        `in`.readByteArray(serviceDataMask)

                        builder.setServiceData(serviceDataUuid!!, serviceData, serviceDataMask)
                    }
                }
            }

            val manufacturerId = `in`.readInt()
            if (`in`.readInt() == 1) {
                val manufacturerDataLength = `in`.readInt()
                val manufacturerData = ByteArray(manufacturerDataLength)
                `in`.readByteArray(manufacturerData)
                if (`in`.readInt() == 0) {
                    builder.setManufacturerData(manufacturerId, manufacturerData)
                } else {
                    val manufacturerDataMaskLength = `in`.readInt()
                    val manufacturerDataMask = ByteArray(manufacturerDataMaskLength)
                    `in`.readByteArray(manufacturerDataMask)
                    builder.setManufacturerData(
                        manufacturerId, manufacturerData,
                        manufacturerDataMask
                    )
                }
            }

            return builder.build()
        }
    }

    /**
     * Returns the filter set the device name field of Bluetooth advertisement data.
     */
    fun getDeviceName(): String? {
        return deviceName
    }

    /**
     * Returns the filter set on the service uuid.
     */
    fun getServiceUuid(): ParcelUuid? {
        return serviceUuid
    }

    fun getServiceUuidMask(): ParcelUuid? {
        return serviceUuidMask
    }

    fun getDeviceAddress(): String? {
        return deviceAddress
    }

    fun getServiceData(): ByteArray? {
        return serviceData
    }

    fun getServiceDataMask(): ByteArray? {
        return serviceDataMask
    }

    fun getServiceDataUuid(): ParcelUuid? {
        return serviceDataUuid
    }

    /**
     * Returns the manufacturer id. -1 if the manufacturer filter is not set.
     */
    fun getManufacturerId(): Int {
        return manufacturerId
    }

    fun getManufacturerData(): ByteArray? {
        return manufacturerData
    }

    fun getManufacturerDataMask(): ByteArray? {
        return manufacturerDataMask
    }

    /**
     * Check if the scan filter matches a `scanResult`. A scan result is considered as a match
     * if it matches all the field filters.
     */
    fun matches(scanResult: ScanResult?): Boolean {
        if (scanResult == null) {
            return false
        }
        val device = scanResult.getDevice()
        // Device match.
        if (deviceAddress != null && deviceAddress != device!!.address) {
            return false
        }

        val scanRecord = scanResult.getScanRecord()

        // Scan record is null but there exist filters on it.
        if (scanRecord == null && (deviceName != null || serviceUuid != null || manufacturerData != null
                    || serviceData != null)
        ) {
            return false
        }

        // Local name match.
        if (deviceName != null && deviceName != scanRecord!!.getDeviceName()) {
            return false
        }

        // UUID match.
        if (serviceUuid != null && !matchesServiceUuids(
                serviceUuid, serviceUuidMask,
                scanRecord!!.getServiceUuids()
            )
        ) {
            return false
        }

        // Service data match
        if (serviceDataUuid != null && scanRecord != null) {
            if (!matchesPartialData(
                    serviceData, serviceDataMask,
                    scanRecord.getServiceData(serviceDataUuid!!)
                )
            ) {
                return false
            }
        }

        // Manufacturer data match.
        if (manufacturerId >= 0 && scanRecord != null) {

            if (!matchesPartialData(
                    manufacturerData, manufacturerDataMask,
                    scanRecord.getManufacturerSpecificData(manufacturerId)
                )
            ) {
                return false
            }
        }
        // All filters match.
        return true
    }

    /**
     * Check if the uuid pattern is contained in a list of parcel uuids.
     */
    private fun matchesServiceUuids(
        uuid: ParcelUuid?,
        parcelUuidMask: ParcelUuid?,
        uuids: List<ParcelUuid>?
    ): Boolean {
        if (uuid == null) {
            return true
        }
        if (uuids == null) {
            return false
        }

        for (parcelUuid in uuids) {
            val uuidMask = parcelUuidMask?.uuid
            if (matchesServiceUuid(uuid.uuid, uuidMask, parcelUuid.uuid)) {
                return true
            }
        }
        return false
    }

    // Check if the uuid pattern matches the particular service uuid.
    private fun matchesServiceUuid(
        uuid: UUID,
        mask: UUID?,
        data: UUID
    ): Boolean {
        if (mask == null) {
            return uuid == data
        }
        return if (uuid.leastSignificantBits and mask.leastSignificantBits != data.leastSignificantBits and mask.leastSignificantBits) {
            false
        } else uuid.mostSignificantBits and mask.mostSignificantBits == data.mostSignificantBits and mask.mostSignificantBits
    }

    // Check whether the data pattern matches the parsed data.
    private fun matchesPartialData(
        data: ByteArray?,
        dataMask: ByteArray?,
        parsedData: ByteArray?
    ): Boolean {
        if (data == null) {
            // If filter data is null it means it doesn't matter.
            // We return true if any data matching the manufacturerId were found.
            return parsedData != null
        }
        if (parsedData == null || parsedData.size < data.size) {
            return false
        }
        if (dataMask == null) {
            for (i in data.indices) {
                if (parsedData[i] != data[i]) {
                    return false
                }
            }
            return true
        }
        for (i in data.indices) {
            if ((dataMask[i] and parsedData[i]) != dataMask[i] and data[i]) {
                return false
            }
        }
        return true
    }

    override fun toString(): String {
        return ("BluetoothLeScanFilter [deviceName=" + deviceName + ", deviceAddress="
                + deviceAddress
                + ", mUuid=" + serviceUuid + ", uuidMask=" + serviceUuidMask
                + ", serviceDataUuid=" + Objects.toString(serviceDataUuid) + ", serviceData="
                + Arrays.toString(serviceData) + ", serviceDataMask="
                + Arrays.toString(serviceDataMask) + ", manufacturerId=" + manufacturerId
                + ", manufacturerData=" + Arrays.toString(manufacturerData)
                + ", manufacturerDataMask=" + Arrays.toString(manufacturerDataMask) + "]")
    }

    override fun hashCode(): Int {
        return Objects.hash(
            deviceName, deviceAddress, manufacturerId,
            Arrays.hashCode(manufacturerData),
            Arrays.hashCode(manufacturerDataMask),
            serviceDataUuid,
            Arrays.hashCode(serviceData),
            Arrays.hashCode(serviceDataMask),
            serviceUuid, serviceUuidMask
        )
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null || javaClass != obj.javaClass) {
            return false
        }
        val other = obj as ScanFilter?
        return Objects.equals(deviceName, other!!.deviceName) &&
                Objects.equals(deviceAddress, other.deviceAddress) &&
                manufacturerId == other.manufacturerId &&
                Objects.deepEquals(manufacturerData, other.manufacturerData) &&
                Objects.deepEquals(manufacturerDataMask, other.manufacturerDataMask) &&
                Objects.equals(serviceDataUuid, other.serviceDataUuid) &&
                Objects.deepEquals(serviceData, other.serviceData) &&
                Objects.deepEquals(serviceDataMask, other.serviceDataMask) &&
                Objects.equals(serviceUuid, other.serviceUuid) &&
                Objects.equals(serviceUuidMask, other.serviceUuidMask)
    }

    /**
     * Checks if the scan filter is empty.
     */
    internal fun isAllFieldsEmpty():
            /* package */ Boolean {
        //return EMPTY == this
        return false
    }

    /**
     * Builder class for {@link ScanFilter}.
     */
     class Builder {
        internal var deviceName: String? = null
        internal var deviceAddress: String? = null

        internal var serviceUuid: ParcelUuid? = null
        internal var uuidMask: ParcelUuid? = null

        internal var serviceDataUuid: ParcelUuid? = null
        internal var serviceData: ByteArray? = null
        internal var serviceDataMask: ByteArray? = null

        internal var manufacturerId = -1
        internal var manufacturerData: ByteArray? = null
        internal var manufacturerDataMask: ByteArray? = null

        /**
         * Set filter on device name.
         */
        fun setDeviceName(deviceName: String?): Builder {
            this.deviceName = deviceName
            return this
        }

        /**
         * Set filter on device address.
         *
         * @param deviceAddress The device Bluetooth address for the filter. It needs to be in the
         * format of "01:02:03:AB:CD:EF". The device address can be validated using
         * [BluetoothAdapter.checkBluetoothAddress].
         * @throws IllegalArgumentException If the `deviceAddress` is invalid.
         */
        fun setDeviceAddress(deviceAddress: String?): Builder {
            if (deviceAddress != null && !BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
                throw IllegalArgumentException("invalid device address $deviceAddress")
            }
            this.deviceAddress = deviceAddress
            return this
        }

        /**
         * Set filter on service uuid.
         */
        fun setServiceUuid(serviceUuid: ParcelUuid?): Builder {
            this.serviceUuid = serviceUuid
            this.uuidMask = null // clear uuid mask
            return this
        }

        /**
         * Set filter on partial service uuid. The `uuidMask` is the bit mask for the
         * `serviceUuid`. Set any bit in the mask to 1 to indicate a match is needed for the
         * bit in `serviceUuid`, and 0 to ignore that bit.
         *
         * @throws IllegalArgumentException If `serviceUuid` is `null` but
         * `uuidMask` is not `null`.
         */
        fun setServiceUuid(
            serviceUuid: ParcelUuid?,
            uuidMask: ParcelUuid?
        ): Builder {
            if (uuidMask != null && serviceUuid == null) {
                throw IllegalArgumentException("uuid is null while uuidMask is not null!")
            }
            this.serviceUuid = serviceUuid
            this.uuidMask = uuidMask
            return this
        }

        /**
         * Set filtering on service data.
         *
         * @throws IllegalArgumentException If `serviceDataUuid` is null.
         */
        fun setServiceData(
            serviceDataUuid: ParcelUuid,
            serviceData: ByteArray?
        ): Builder {

            if (serviceDataUuid == null) {
                throw IllegalArgumentException("serviceDataUuid is null!")
            }
            this.serviceDataUuid = serviceDataUuid
            this.serviceData = serviceData
            this.serviceDataMask = null // clear service data mask
            return this
        }

        /**
         * Set partial filter on service data. For any bit in the mask, set it to 1 if it needs to
         * match the one in service data, otherwise set it to 0 to ignore that bit.
         *
         *
         * The `serviceDataMask` must have the same length of the `serviceData`.
         *
         * @throws IllegalArgumentException If `serviceDataUuid` is null or
         * `serviceDataMask` is `null` while `serviceData` is not or
         * `serviceDataMask` and `serviceData` has different length.
         */
        fun setServiceData(
            serviceDataUuid: ParcelUuid,
            serviceData: ByteArray?,
            serviceDataMask: ByteArray?
        ): Builder {

            if (serviceDataUuid == null) {
                throw IllegalArgumentException("serviceDataUuid is null")
            }
            if (serviceDataMask != null) {
                if (serviceData == null) {
                    throw IllegalArgumentException(
                        "serviceData is null while serviceDataMask is not null"
                    )
                }
                // Since the serviceDataMask is a bit mask for serviceData, the lengths of the two
                // byte array need to be the same.
                if (serviceData.size != serviceDataMask.size) {
                    throw IllegalArgumentException(
                        "size mismatch for service data and service data mask"
                    )
                }
            }
            this.serviceDataUuid = serviceDataUuid
            this.serviceData = serviceData
            this.serviceDataMask = serviceDataMask
            return this
        }

        /**
         * Set filter on on manufacturerData. A negative manufacturerId is considered as invalid id.
         *
         *
         * Note the first two bytes of the `manufacturerData` is the manufacturerId.
         *
         * @throws IllegalArgumentException If the `manufacturerId` is invalid.
         */
        fun setManufacturerData(
            manufacturerId: Int,
            manufacturerData: ByteArray?
        ): Builder {
            if (manufacturerData != null && manufacturerId < 0) {
                throw IllegalArgumentException("invalid manufacture id")
            }
            this.manufacturerId = manufacturerId
            this.manufacturerData = manufacturerData
            this.manufacturerDataMask = null // clear manufacturer data mask
            return this
        }

        /**
         * Set filter on partial manufacture data. For any bit in the mask, set it the 1 if it needs
         * to match the one in manufacturer data, otherwise set it to 0.
         *
         *
         * The `manufacturerDataMask` must have the same length of `manufacturerData`.
         *
         * @throws IllegalArgumentException If the `manufacturerId` is invalid, or
         * `manufacturerData` is null while `manufacturerDataMask` is not,
         * or `manufacturerData` and `manufacturerDataMask` have different
         * length.
         */
        fun setManufacturerData(
            manufacturerId: Int,
            manufacturerData: ByteArray?,
            manufacturerDataMask: ByteArray?
        ): Builder {
            if (manufacturerData != null && manufacturerId < 0) {
                throw IllegalArgumentException("invalid manufacture id")
            }
            if (manufacturerDataMask != null) {
                if (manufacturerData == null) {
                    throw IllegalArgumentException(
                        "manufacturerData is null while manufacturerDataMask is not null"
                    )
                }
                // Since the manufacturerDataMask is a bit mask for manufacturerData, the lengths
                // of the two byte array need to be the same.
                if (manufacturerData.size != manufacturerDataMask.size) {
                    throw IllegalArgumentException(
                        "size mismatch for manufacturerData and manufacturerDataMask"
                    )
                }
            }
            this.manufacturerId = manufacturerId
            this.manufacturerData = manufacturerData
            this.manufacturerDataMask = manufacturerDataMask
            return this
        }

        /**
         * Build [ScanFilter].
         *
         * @throws IllegalArgumentException If the filter cannot be built.
         */
        fun build(): ScanFilter {
            return ScanFilter(this)
        }
    }

}