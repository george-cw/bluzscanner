package com.sherlock.bluzscanner

import android.annotation.TargetApi
import android.bluetooth.BluetoothDevice
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.util.*

class ScanResult: Parcelable {

    companion object {
        /**
         * For chained advertisements, indicates that the data contained in this
         * scan result is complete.
         */
        val DATA_COMPLETE = 0x00

        /**
         * For chained advertisements, indicates that the controller was
         * unable to receive all chained packets and the scan result contains
         * incomplete truncated data.
         */
        val DATA_TRUNCATED = 0x02

        /**
         * Indicates that the secondary physical layer was not used.
         */
        val PHY_UNUSED = 0x00

        /**
         * Advertising Set ID is not present in the packet.
         */
        val SID_NOT_PRESENT = 0xFF

        /**
         * TX power is not present in the packet.
         */
        val TX_POWER_NOT_PRESENT = 0x7F

        /**
         * Periodic advertising interval is not present in the packet.
         */
        val PERIODIC_INTERVAL_NOT_PRESENT = 0x00

        /**
         * Mask for checking whether event type represents legacy advertisement.
         */
        internal val ET_LEGACY_MASK = 0x10

        /**
         * Mask for checking whether event type represents connectable advertisement.
         */
        internal val ET_CONNECTABLE_MASK = 0x01
    }

    // Remote Bluetooth device.
    private lateinit var device: BluetoothDevice

    // Scan record, including advertising data and scan response data.
    private var scanRecord: ScanRecord? = null

    // Received signal strength.
    private var rssi: Int = 0

    // Device timestamp when the result was last seen.
    private var timestampNanos: Long = 0

    private var eventType: Int = 0
    private var primaryPhy: Int = 0
    private var secondaryPhy: Int = 0
    private var advertisingSid: Int = 0
    private var txPower: Int = 0
    private var periodicAdvertisingInterval: Int = 0

    constructor(@NonNull device: BluetoothDevice, @Nullable scanRecord: ScanRecord? ,
                rssi: Int, timestampNanos: Long) {
        this.device = device;
        this.scanRecord = scanRecord;
        this.rssi = rssi;
        this.timestampNanos = timestampNanos;
        this.eventType = (DATA_COMPLETE.shl(5)) or ET_LEGACY_MASK or ET_CONNECTABLE_MASK;
        this.primaryPhy = 1; // BluetoothDevice.PHY_LE_1M;
        this.secondaryPhy = PHY_UNUSED;
        this.advertisingSid = SID_NOT_PRESENT;
        this.txPower = 127;
        this.periodicAdvertisingInterval = 0;
    }

    constructor(@NonNull device: BluetoothDevice ,  eventType: Int,
                primaryPhy: Int, secondaryPhy: Int,
                advertisingSid: Int, txPower: Int, rssi: Int,
                periodicAdvertisingInterval: Int,
                @Nullable scanRecord: ScanRecord? , timestampNanos: Long) {
        this.device = device;
        this.eventType = eventType;
        this.primaryPhy = primaryPhy;
        this.secondaryPhy = secondaryPhy;
        this.advertisingSid = advertisingSid;
        this.txPower = txPower;
        this.rssi = rssi;
        this.periodicAdvertisingInterval = periodicAdvertisingInterval;
        this.scanRecord = scanRecord;
        this.timestampNanos = timestampNanos;
    }

    private constructor(`in`: Parcel){
        readFromParcel(`in`)
    }

    private fun readFromParcel(`in`: Parcel) {
        device = BluetoothDevice.CREATOR.createFromParcel(`in`)
        if (`in`.readInt() == 1) {
            var parseRecord = ScanRecord();
            scanRecord = parseRecord.parseFromBytes(`in`.createByteArray())
        }
        rssi = `in`.readInt()
        timestampNanos = `in`.readLong()
        eventType = `in`.readInt()
        primaryPhy = `in`.readInt()
        secondaryPhy = `in`.readInt()
        advertisingSid = `in`.readInt()
        txPower = `in`.readInt()
        periodicAdvertisingInterval = `in`.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        if (device == null)
            return
        device?.writeToParcel(dest, flags)
        if (scanRecord != null) {
            dest.writeInt(1)
            dest.writeByteArray(scanRecord!!.getBytes())
        } else {
            dest.writeInt(0)
        }
        dest.writeInt(rssi)
        dest.writeLong(timestampNanos)
        dest.writeInt(eventType)
        dest.writeInt(primaryPhy)
        dest.writeInt(secondaryPhy)
        dest.writeInt(advertisingSid)
        dest.writeInt(txPower)
        dest.writeInt(periodicAdvertisingInterval)
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Returns the remote Bluetooth device identified by the Bluetooth device address.
     */
    fun getDevice(): BluetoothDevice {
        return device
    }

    /**
     * Returns the scan record, which is a combination of advertisement and scan response.
     */
    fun getScanRecord(): ScanRecord? {
        return scanRecord
    }

    /**
     * Returns the received signal strength in dBm. The valid range is [-127, 126].
     */
    fun getRssi(): Int {
        return rssi
    }

    /**
     * Returns timestamp since boot when the scan record was observed.
     */
    fun getTimestampNanos(): Long {
        return timestampNanos
    }

    /**
     * Returns true if this object represents legacy scan result.
     * Legacy scan results do not contain advanced advertising information
     * as specified in the Bluetooth Core Specification v5.
     */
    fun isLegacy(): Boolean {
        return eventType and ET_LEGACY_MASK != 0
    }

    /**
     * Returns true if this object represents connectable scan result.
     */
    fun isConnectable(): Boolean {
        return eventType and ET_CONNECTABLE_MASK != 0
    }

    /**
     * Returns the data status.
     * Can be one of [ScanResult.DATA_COMPLETE] or
     * [ScanResult.DATA_TRUNCATED].
     */
    fun getDataStatus(): Int {
        // return bit 5 and 6
        return eventType shr 5 and 0x03
    }

    /**
     * Returns the primary Physical Layer
     * on which this advertisement was received.
     * Can be one of [BluetoothDevice.PHY_LE_1M] or
     * [BluetoothDevice.PHY_LE_CODED].
     */
    fun getPrimaryPhy(): Int {
        return primaryPhy
    }

    /**
     * Returns the secondary Physical Layer
     * on which this advertisement was received.
     * Can be one of [BluetoothDevice.PHY_LE_1M],
     * [BluetoothDevice.PHY_LE_2M], [BluetoothDevice.PHY_LE_CODED]
     * or [ScanResult.PHY_UNUSED] - if the advertisement
     * was not received on a secondary physical channel.
     */
    fun getSecondaryPhy(): Int {
        return secondaryPhy
    }

    /**
     * Returns the advertising set id.
     * May return [ScanResult.SID_NOT_PRESENT] if
     * no set id was is present.
     */
    fun getAdvertisingSid(): Int {
        return advertisingSid
    }

    /**
     * Returns the transmit power in dBm.
     * Valid range is [-127, 126]. A value of [ScanResult.TX_POWER_NOT_PRESENT]
     * indicates that the TX power is not present.
     */
    fun getTxPower(): Int {
        return txPower
    }

    /**
     * Returns the periodic advertising interval in units of 1.25ms.
     * Valid range is 6 (7.5ms) to 65536 (81918.75ms). A value of
     * [ScanResult.PERIODIC_INTERVAL_NOT_PRESENT] means periodic
     * advertising interval is not present.
     */
    fun getPeriodicAdvertisingInterval(): Int {
        return periodicAdvertisingInterval
    }

    @TargetApi(19)
    override fun hashCode(): Int {
        return Objects.hash(
            device, rssi, scanRecord, timestampNanos,
            eventType, primaryPhy, secondaryPhy,
            advertisingSid, txPower,
            periodicAdvertisingInterval
        )
    }

    @TargetApi(19)
    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null || javaClass != obj.javaClass) {
            return false
        }
        val other = obj as ScanResult?
        return Objects.equals(device, other!!.device) && rssi == other.rssi &&
                Objects.equals(scanRecord, other.scanRecord) &&
                timestampNanos == other.timestampNanos &&
                eventType == other.eventType &&
                primaryPhy == other.primaryPhy &&
                secondaryPhy == other.secondaryPhy &&
                advertisingSid == other.advertisingSid &&
                txPower == other.txPower &&
                periodicAdvertisingInterval == other.periodicAdvertisingInterval
    }

    @TargetApi(19)
    override fun toString(): String {
        return "ScanResult{" + "device=" + device + ", scanRecord=" +
                Objects.toString(scanRecord) + ", rssi=" + rssi +
                ", timestampNanos=" + timestampNanos + ", eventType=" + eventType +
                ", primaryPhy=" + primaryPhy + ", secondaryPhy=" + secondaryPhy +
                ", advertisingSid=" + advertisingSid + ", txPower=" + txPower +
                ", periodicAdvertisingInterval=" + periodicAdvertisingInterval + '}'.toString()
    }

    @JvmField
    val CREATOR: Parcelable.Creator<ScanResult> = object : Parcelable.Creator<ScanResult> {
        override fun createFromParcel(source: Parcel): ScanResult {
            return ScanResult(source)
        }

        override fun newArray(size: Int): Array<ScanResult?> {
            return arrayOfNulls(size)
        }
    }


}