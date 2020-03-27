package com.sherlock.bluzscanner

import android.bluetooth.BluetoothDevice
import android.os.Parcel
import android.os.Parcelable

class ScanSettings : Parcelable {

    companion object {
        /**
         * The default value of the maximum time for the device not to be discoverable before it will be
         * assumed lost.
         */
        val MATCH_LOST_DEVICE_TIMEOUT_DEFAULT = 10000L // [ms]

        /**
         * The default interval of the task that calls match lost events.
         */
        val MATCH_LOST_TASK_INTERVAL_DEFAULT = 10000L // [ms]

        /**
         * A special Bluetooth LE scan mode. Applications using this scan mode will passively listen for
         * other scan results without starting BLE scans themselves.
         *
         *
         * On Android Lollipop [.SCAN_MODE_LOW_POWER] will be used instead, as opportunistic
         * mode was not yet supported.
         *
         *
         * On pre-Lollipop devices it is possible to override the default intervals
         * using [Builder.setPowerSave].
         */
        val SCAN_MODE_OPPORTUNISTIC = -1

        /**
         * Perform Bluetooth LE scan in low power mode. This is the default scan mode as it consumes the
         * least power. This mode is enforced if the scanning application is not in foreground.
         *
         *
         * On pre-Lollipop devices this mode will be emulated by scanning for 0.5 second followed
         * by 4.5 second of idle, which corresponds to the low power intervals on Lollipop or newer.
         */
        val SCAN_MODE_LOW_POWER = 0

        /**
         * Perform Bluetooth LE scan in balanced power mode. Scan results are returned at a rate that
         * provides a good trade-off between scan frequency and power consumption.
         *
         *
         * On pre-Lollipop devices this mode will be emulated by scanning for 2 second followed
         * by 3 seconds of idle, which corresponds to the low power intervals on Lollipop or newer.
         */
        val SCAN_MODE_BALANCED = 1

        /**
         * Scan using highest duty cycle. It's recommended to only use this mode when the application is
         * running in the foreground.
         */
        val SCAN_MODE_LOW_LATENCY = 2

        /**
         * Trigger a callback for every Bluetooth advertisement found that matches the filter criteria.
         * If no filter is active, all advertisement packets are reported.
         */
        val CALLBACK_TYPE_ALL_MATCHES = 1

        /**
         * A result callback is only triggered for the first advertisement packet received that matches
         * the filter criteria.
         */
        val CALLBACK_TYPE_FIRST_MATCH = 2

        /**
         * Receive a callback when advertisements are no longer received from a device that has been
         * previously reported by a first match callback.
         */
        val CALLBACK_TYPE_MATCH_LOST = 4

        /*
	 * Determines how many advertisements to match per filter, as this is scarce hw resource
	 */
        /**
         * Match one advertisement per filter
         */
        val MATCH_NUM_ONE_ADVERTISEMENT = 1

        /**
         * Match few advertisement per filter, depends on current capability and availability of
         * the resources in hw
         */
        val MATCH_NUM_FEW_ADVERTISEMENT = 2

        /**
         * Match as many advertisement per filter as hw could allow, depends on current
         * capability and availability of the resources in hw
         */
        val MATCH_NUM_MAX_ADVERTISEMENT = 3

        /**
         * In Aggressive mode, hw will determine a match sooner even with feeble signal strength
         * and few number of sightings/match in a duration.
         */
        val MATCH_MODE_AGGRESSIVE = 1

        /**
         * For sticky mode, higher threshold of signal strength and sightings is required
         * before reporting by hw
         */
        val MATCH_MODE_STICKY = 2

        /**
         * Use all supported PHYs for scanning.
         * This will check the controller capabilities, and start
         * the scan on 1Mbit and LE Coded PHYs if supported, or on
         * the 1Mbit PHY only.
         */
        val PHY_LE_ALL_SUPPORTED = 255
    }

    /**
     * Pre-Lollipop scanning requires a wakelock and the CPU cannot go to sleep.
     * To conserve power we can optionally scan for a certain duration (scan interval)
     * and then rest for a time before starting scanning again.
     */
    private var powerSaveScanInterval: Long = 0
    private var powerSaveRestInterval: Long = 0

    // Bluetooth LE scan mode.
    private var scanMode: Int = 0

    // Bluetooth LE scan callback type
    private var callbackType: Int = 0

    // Time of delay for reporting the scan result
    private var reportDelayMillis: Long = 0

    private var matchMode: Int = 0

    private var numOfMatchesPerFilter: Int = 0

    private var useHardwareFilteringIfSupported: Boolean = false

    private var useHardwareBatchingIfSupported: Boolean = false

    private var useHardwareCallbackTypesIfSupported: Boolean = false

    private var matchLostDeviceTimeout: Long = 0

    private var matchLostTaskInterval: Long = 0

    // Include only legacy advertising results
    private var legacy: Boolean = false

    private var phy: Int = 0

    fun getScanMode(): Int {
        return scanMode
    }

    fun getCallbackType(): Int {
        return callbackType
    }

    fun getMatchMode(): Int {
        return matchMode
    }

    fun getNumOfMatches(): Int {
        return numOfMatchesPerFilter
    }

    fun getUseHardwareFilteringIfSupported(): Boolean {
        return useHardwareFilteringIfSupported
    }

    fun getUseHardwareBatchingIfSupported(): Boolean {
        return useHardwareBatchingIfSupported
    }

    fun getUseHardwareCallbackTypesIfSupported(): Boolean {
        return useHardwareCallbackTypesIfSupported
    }

    /**
     * Some devices with Android Marshmallow (Nexus 6) theoretically support other callback types,
     * but call [android.bluetooth.le.ScanCallback.onScanFailed] with error = 5.
     * In that case the Scanner Compat will disable the hardware support and start using compat
     * mechanism.
     */
    /* package */ internal fun disableUseHardwareCallbackTypes() {
        useHardwareCallbackTypesIfSupported = false
    }

    fun getMatchLostDeviceTimeout(): Long {
        return matchLostDeviceTimeout
    }

    fun getMatchLostTaskInterval(): Long {
        return matchLostTaskInterval
    }

    /**
     * Returns whether only legacy advertisements will be returned.
     * Legacy advertisements include advertisements as specified
     * by the Bluetooth core specification 4.2 and below.
     */
    fun getLegacy(): Boolean {
        return legacy
    }

    /**
     * Returns the physical layer used during a scan.
     */
    fun getPhy(): Int {
        return phy
    }

    /**
     * Returns report delay timestamp based on the device clock.
     */
    fun getReportDelayMillis(): Long {
        return reportDelayMillis
    }

    constructor(){}

    constructor(scanMode: Int,  callbackType: Int,
                             reportDelayMillis: Long,  matchMode: Int,
                             numOfMatchesPerFilter: Int, flegacy: Boolean,  phy: Int,
                             hardwareFiltering: Boolean, hardwareBatching: Boolean,
                             hardwareCallbackTypes: Boolean, matchTimeout: Long,
                             taskInterval: Long, powerSaveScanInterval: Long, powerSaveRestInterval: Long){

        this.scanMode = scanMode
        this.callbackType = callbackType
        this.reportDelayMillis = reportDelayMillis
        this.numOfMatchesPerFilter = numOfMatchesPerFilter
        this.matchMode = matchMode
        this.legacy = legacy
        this.phy = phy
        this.useHardwareFilteringIfSupported = hardwareFiltering
        this.useHardwareBatchingIfSupported = hardwareBatching
        this.useHardwareCallbackTypesIfSupported = hardwareCallbackTypes
        this.matchLostDeviceTimeout = matchTimeout * 1000000L // convert to nanos
        this.matchLostTaskInterval = taskInterval
        this.powerSaveScanInterval = powerSaveScanInterval
        this.powerSaveRestInterval = powerSaveRestInterval
    }

    constructor(`in`: Parcel){
        scanMode = `in`.readInt()
        callbackType = `in`.readInt()
        reportDelayMillis = `in`.readLong()
        matchMode = `in`.readInt()
        numOfMatchesPerFilter = `in`.readInt()
        legacy = `in`.readInt() != 0
        phy = `in`.readInt()
        useHardwareFilteringIfSupported = `in`.readInt() == 1
        useHardwareBatchingIfSupported = `in`.readInt() == 1
        powerSaveScanInterval = `in`.readLong()
        powerSaveRestInterval = `in`.readLong()
    }

    override fun writeToParcel(dest: Parcel, flags: Int){
        dest.writeInt(scanMode)
        dest.writeInt(callbackType)
        dest.writeLong(reportDelayMillis)
        dest.writeInt(matchMode)
        dest.writeInt(numOfMatchesPerFilter)
        dest.writeInt(if (legacy) 1 else 0)
        dest.writeInt(phy)
        dest.writeInt(if (useHardwareFilteringIfSupported) 1 else 0)
        dest.writeInt(if (useHardwareBatchingIfSupported) 1 else 0)
        dest.writeLong(powerSaveScanInterval)
        dest.writeLong(powerSaveRestInterval)
    }

    override fun describeContents(): Int{
        return 0;
    }

    @JvmField
    val CREATOR: Parcelable.Creator<ScanSettings> = object : Parcelable.Creator<ScanSettings> {
        override fun newArray(size: Int): Array<ScanSettings?> {
            return arrayOfNulls(size)
        }

        override fun createFromParcel(`in`: Parcel): ScanSettings {
            return ScanSettings(`in`)
        }
    }

    /**
     * Determine if we should do power-saving sleep on pre-Lollipop
     */
    fun hasPowerSaveMode(): Boolean {
        return powerSaveRestInterval > 0 && powerSaveScanInterval > 0
    }

    fun getPowerSaveRest(): Long {
        return powerSaveRestInterval
    }

    fun getPowerSaveScan(): Long {
        return powerSaveScanInterval
    }

     inner class Builder {
        private var scanMode = SCAN_MODE_LOW_POWER
        private var callbackType = CALLBACK_TYPE_ALL_MATCHES
        private var reportDelayMillis: Long = 0
        private var matchMode = MATCH_MODE_AGGRESSIVE
        private var numOfMatchesPerFilter = MATCH_NUM_MAX_ADVERTISEMENT
        private var legacy = true
        private var phy = PHY_LE_ALL_SUPPORTED
        private var useHardwareFilteringIfSupported = true
        private var useHardwareBatchingIfSupported = true
        private var useHardwareCallbackTypesIfSupported = true
        private var matchLostDeviceTimeout = MATCH_LOST_DEVICE_TIMEOUT_DEFAULT
        private var matchLostTaskInterval = MATCH_LOST_TASK_INTERVAL_DEFAULT
        private var powerSaveRestInterval: Long = 0
        private var powerSaveScanInterval: Long = 0

        /**
         * Set scan mode for Bluetooth LE scan.
         *
         *
         * [.SCAN_MODE_OPPORTUNISTIC] is supported on Android Marshmallow onwards.
         * On Lollipop this mode will fall back [.SCAN_MODE_LOW_POWER], which actually means
         * that the library will start its own scan instead of relying on scans from other apps.
         * This may have significant impact on battery usage.
         *
         *
         * On pre-Lollipop devices, the settings set by [.setPowerSave]
         * will be used. By default, the intervals are the same as for [.SCAN_MODE_LOW_POWER].
         *
         * @param scanMode The scan mode can be one of [ScanSettings.SCAN_MODE_LOW_POWER],
         * [.SCAN_MODE_BALANCED],
         * [.SCAN_MODE_LOW_LATENCY] or
         * [.SCAN_MODE_OPPORTUNISTIC].
         * @throws IllegalArgumentException If the `scanMode` is invalid.
         */
        fun setScanMode(scanMode: Int): Builder {
            if (scanMode < SCAN_MODE_OPPORTUNISTIC || scanMode > SCAN_MODE_LOW_LATENCY) {
                throw IllegalArgumentException("invalid scan mode $scanMode")
            }
            this.scanMode = scanMode
            return this
        }

        /**
         * Set callback type for Bluetooth LE scan.
         *
         * @param callbackType The callback type flags for the scan.
         * @throws IllegalArgumentException If the `callbackType` is invalid.
         */
        fun setCallbackType(callbackType: Int): Builder {
            if (!isValidCallbackType(callbackType)) {
                throw IllegalArgumentException("invalid callback type - $callbackType")
            }
            this.callbackType = callbackType
            return this
        }

        // Returns true if the callbackType is valid.
        private fun isValidCallbackType(callbackType: Int): Boolean {
            return if (callbackType == CALLBACK_TYPE_ALL_MATCHES ||
                callbackType == CALLBACK_TYPE_FIRST_MATCH ||
                callbackType == CALLBACK_TYPE_MATCH_LOST
            ) {
                true
            } else callbackType == CALLBACK_TYPE_FIRST_MATCH or CALLBACK_TYPE_MATCH_LOST
        }

        /**
         * Set report delay timestamp for Bluetooth LE scan.
         *
         * @param reportDelayMillis Delay of report in milliseconds. Set to 0 to be notified of
         * results immediately. Values &gt; 0 causes the scan results
         * to be queued up and delivered after the requested delay or
         * when the internal buffers fill up.
         *
         *
         * For delays below 5000 ms (5 sec) the
         * [ScanCallback.onBatchScanResults]
         * will be called in unreliable intervals, but starting from
         * around 5000 the intervals get even.
         * @throws IllegalArgumentException If `reportDelayMillis` &lt; 0.
         */
        fun setReportDelay(reportDelayMillis: Long): Builder {
            if (reportDelayMillis < 0) {
                throw IllegalArgumentException("reportDelay must be > 0")
            }
            this.reportDelayMillis = reportDelayMillis
            return this
        }

        /**
         * Set the number of matches for Bluetooth LE scan filters hardware match.
         *
         * @param numOfMatches The num of matches can be one of
         * [ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT] or
         * [ScanSettings.MATCH_NUM_FEW_ADVERTISEMENT] or
         * [ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT]
         * @throws IllegalArgumentException If the `matchMode` is invalid.
         */
        fun setNumOfMatches(numOfMatches: Int): Builder {
            if (numOfMatches < MATCH_NUM_ONE_ADVERTISEMENT || numOfMatches > MATCH_NUM_MAX_ADVERTISEMENT) {
                throw IllegalArgumentException("invalid numOfMatches $numOfMatches")
            }
            numOfMatchesPerFilter = numOfMatches
            return this
        }

        /**
         * Set match mode for Bluetooth LE scan filters hardware match
         *
         * @param matchMode The match mode can be one of
         * [ScanSettings.MATCH_MODE_AGGRESSIVE] or
         * [ScanSettings.MATCH_MODE_STICKY]
         * @throws IllegalArgumentException If the `matchMode` is invalid.
         */
        fun setMatchMode(matchMode: Int): Builder {
            if (matchMode < MATCH_MODE_AGGRESSIVE || matchMode > MATCH_MODE_STICKY) {
                throw IllegalArgumentException("invalid matchMode $matchMode")
            }
            this.matchMode = matchMode
            return this
        }

        /**
         * Set whether only legacy advertisements should be returned in scan results.
         * Legacy advertisements include advertisements as specified by the
         * Bluetooth core specification 4.2 and below. This is true by default
         * for compatibility with older apps.
         *
         * @param legacy true if only legacy advertisements will be returned
         */
        fun setLegacy(legacy: Boolean): Builder {
            this.legacy = legacy
            return this
        }

        /**
         * Set the Physical Layer to use during this scan.
         * This is used only if [ScanSettings.Builder.setLegacy]
         * is set to false and only on Android 0reo or newer.
         * [android.bluetooth.BluetoothAdapter.isLeCodedPhySupported]
         * may be used to check whether LE Coded phy is supported by calling
         * [android.bluetooth.BluetoothAdapter.isLeCodedPhySupported].
         * Selecting an unsupported phy will result in failure to start scan.
         *
         * @param phy Can be one of
         * [BluetoothDevice.PHY_LE_1M],
         * [BluetoothDevice.PHY_LE_CODED] or
         * [ScanSettings.PHY_LE_ALL_SUPPORTED]
         */
        fun setPhy(phy: Int): Builder {
            this.phy = phy
            return this
        }

        /**
         * Several phones may have some issues when it comes to offloaded filtering.
         * Even if it should be supported, it may not work as expected.
         * It has been observed for example, that setting 2 filters with different devices
         * addresses on Nexus 6 with Lollipop gives no callbacks if one or both devices advertise.
         * See https://code.google.com/p/android/issues/detail?id=181561.
         *
         * @param use true to enable (default) hardware offload filtering.
         * If false a compat software filtering will be used
         * (uses much more resources).
         */
        fun setUseHardwareFilteringIfSupported(use: Boolean): Builder {
            useHardwareFilteringIfSupported = use
            return this
        }

        /**
         * Some devices, for example Samsung S6 and S6 Edge with Lollipop, return always
         * the same RSSI value for all devices if offloaded batching is used.
         * Batching may also be emulated using a compat mechanism - a periodically called timer.
         * Timer approach requires more resources but reports devices in constant delays
         * and works on devices that does not support offloaded batching.
         * In comparison, when setReportDelay(..) is called with parameter 1000 the standard,
         * hardware triggered callback will be called every 1500ms +-200ms.
         *
         * @param use true to enable (default) hardware offloaded batching if they are supported.
         * False to always use compat mechanism.
         */
        fun setUseHardwareBatchingIfSupported(use: Boolean): Builder {
            useHardwareBatchingIfSupported = use
            return this
        }

        /**
         * This method may be used when callback type is set to a value different than
         * [.CALLBACK_TYPE_ALL_MATCHES]. When disabled, the Scanner Compat itself will
         * take care of reporting first match and match lost. The compat behaviour may differ
         * from the one natively supported on Android Marshmallow or newer.
         *
         *
         * Also, in compat mode values set by [.setMatchMode] and
         * [.setNumOfMatches] are ignored.
         * Instead use [.setMatchOptions] to set timer options.
         *
         * @param use true to enable (default) the offloaded match reporting if hardware supports it,
         * false to enable compat implementation.
         */
        fun setUseHardwareCallbackTypesIfSupported(use: Boolean): Builder {
            useHardwareCallbackTypesIfSupported = use
            return this
        }

        /**
         * The match options are used when the callback type has been set to
         * [ScanSettings.CALLBACK_TYPE_FIRST_MATCH] or
         * [ScanSettings.CALLBACK_TYPE_MATCH_LOST] and hardware does not support those types.
         * In that case [BluetoothLeScannerCompat] starts a task that runs periodically
         * and calls [ScanCallback.onScanResult] with type
         * [.CALLBACK_TYPE_MATCH_LOST] if a device has not been seen for at least given time.
         *
         * @param deviceTimeoutMillis the time required for the device to be recognized as lost
         * (default [.MATCH_LOST_DEVICE_TIMEOUT_DEFAULT]).
         * @param taskIntervalMillis  the task interval (default [.MATCH_LOST_TASK_INTERVAL_DEFAULT]).
         */
        fun setMatchOptions(deviceTimeoutMillis: Long, taskIntervalMillis: Long): Builder {
            if (deviceTimeoutMillis <= 0 || taskIntervalMillis <= 0) {
                throw IllegalArgumentException("maxDeviceAgeMillis and taskIntervalMillis must be > 0")
            }
            matchLostDeviceTimeout = deviceTimeoutMillis
            matchLostTaskInterval = taskIntervalMillis
            return this
        }

        /**
         * Pre-Lollipop scanning requires a wakelock and the CPU cannot go to sleep.
         * To conserve power we can optionally scan for a certain duration (scan interval)
         * and then rest for a time before starting scanning again. Won't affect Lollipop
         * or later devices.
         *
         * @param scanInterval interval in ms to scan at a time.
         * @param restInterval interval to sleep for without scanning before scanning again for
         * scanInterval.
         */
        fun setPowerSave(scanInterval: Long, restInterval: Long): Builder {
            if (scanInterval <= 0 || restInterval <= 0) {
                throw IllegalArgumentException("scanInterval and restInterval must be > 0")
            }
            powerSaveScanInterval = scanInterval
            powerSaveRestInterval = restInterval
            return this
        }

        /**
         * Build [ScanSettings].
         */
        fun build(): ScanSettings {
            if (powerSaveRestInterval == 0L && powerSaveScanInterval == 0L)
                updatePowerSaveSettings()

            return ScanSettings(
                scanMode, callbackType, reportDelayMillis, matchMode,
                numOfMatchesPerFilter, legacy, phy, useHardwareFilteringIfSupported,
                useHardwareBatchingIfSupported, useHardwareCallbackTypesIfSupported,
                matchLostDeviceTimeout, matchLostTaskInterval,
                powerSaveScanInterval, powerSaveRestInterval
            )
        }

        /**
         * Sets power save settings based on the scan mode selected.
         */
        private fun updatePowerSaveSettings() {
            when (scanMode) {
                SCAN_MODE_LOW_LATENCY -> {
                    // Disable power save mode
                    powerSaveScanInterval = 0
                    powerSaveRestInterval = 0
                }
                SCAN_MODE_BALANCED -> {
                    // Scan for 2 seconds every 5 seconds
                    powerSaveScanInterval = 2000
                    powerSaveRestInterval = 3000
                }
                SCAN_MODE_OPPORTUNISTIC,
                    // It is not possible to emulate OPPORTUNISTIC scanning, but in theory
                    // that should be even less battery consuming than LOW_POWER.
                    // For pre-Lollipop devices intervals can be overwritten by
                    // setPowerSave(long, long) if needed.

                    // On Android Lollipop the native SCAN_MODE_LOW_POWER will be used instead
                    // of power save values.
                SCAN_MODE_LOW_POWER -> {
                    // Scan for 0.5 second every 5 seconds
                    powerSaveScanInterval = 500
                    powerSaveRestInterval = 4500
                }
                else -> {
                    powerSaveScanInterval = 500
                    powerSaveRestInterval = 4500
                }
            }
        }
    }
}