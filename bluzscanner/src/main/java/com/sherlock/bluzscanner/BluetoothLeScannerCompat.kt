package com.sherlock.bluzscanner

import android.Manifest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import java.util.*

abstract class BluetoothLeScannerCompat {
    companion object{
        private var instance: BluetoothLeScannerCompat? = null

        /**
         * Returns the scanner compat object
         * @return scanner implementation
         */
        @Synchronized
        fun getScanner(): BluetoothLeScannerCompat? {
            if (instance != null)
                return instance
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                instance = BluetoothLeScannerImplOreo()
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                instance = BluetoothLeScannerImplMarshmallow()
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                instance = BluetoothLeScannerImplLollipop()
            else
                instance = BluetoothLeScannerImplJB()
            return instance
        }
    }
    init{//类初始化代码

    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH])
    fun startScan(callback: ScanCallback) {

        if (callback == null) {
            throw IllegalArgumentException("callback is null")
        }
        val handler = Handler(Looper.getMainLooper())
        startScanInternal(
            emptyList<ScanFilter>(), ScanSettings().Builder().build(),
            callback, handler
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH])
    fun startScan(
        filters: List<ScanFilter>?,
        settings: ScanSettings?,
        callback: ScanCallback
    ) {

        if (callback == null) {
            throw IllegalArgumentException("callback is null")
        }
        val handler = Handler(Looper.getMainLooper())
        startScanInternal(
            filters ?: emptyList(),
            settings ?: ScanSettings().Builder().build(),
            callback, handler
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH])
    fun startScan(
        filters: List<ScanFilter>?,
        settings: ScanSettings?,
        callback: ScanCallback,
        handler: Handler?
    ) {

        if (callback == null) {
            throw IllegalArgumentException("callback is null")
        }
        startScanInternal(
            filters ?: emptyList(),
            settings ?: ScanSettings().Builder().build(),
            callback, handler ?: Handler(Looper.getMainLooper())
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    fun stopScan(callback: ScanCallback?) {

        if (callback == null) {
            throw IllegalArgumentException("callback is null")
        }
        stopScanInternal(callback)
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH])
    internal abstract/* package */ fun startScanInternal(
        filters: List<ScanFilter>?,
        settings: ScanSettings?,
        callback: ScanCallback?,
        handler: Handler?
    )

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    internal abstract/* package */ fun stopScanInternal(callback: ScanCallback)

    /**
     * Flush pending batch scan results stored in Bluetooth controller. This will return Bluetooth
     * LE scan results batched on Bluetooth controller. Returns immediately, batch scan results data
     * will be delivered through the `callback`.
     *
     * @param callback Callback of the Bluetooth LE Scan, it has to be the same instance as the one
     * used to start scan.
     */
    abstract fun flushPendingScanResults(callback: ScanCallback)

    open class ScanCallbackWrapper{

        private val LOCK = Any()

        private var emulateFiltering: Boolean = false
        private var emulateBatching: Boolean = false
        private var emulateFoundOrLostCallbackType: Boolean = false
        private var scanningStopped: Boolean = false

        var filters: List<ScanFilter>? = null
        var scanSettings: ScanSettings? = null
        var scanCallback: ScanCallback? = null
        var handler: Handler? = null

        private val scanResults = ArrayList<ScanResult>()

        private val devicesInBatch = HashSet<String>()

        /** A collection of scan result of devices in range.  */
        private val devicesInRange = HashMap<String, ScanResult>()

        private val flushPendingScanResultsTask = object : Runnable {
            override fun run() {
                if (!scanningStopped) {
                    flushPendingScanResults()
                    handler?.postDelayed(this, scanSettings!!.getReportDelayMillis())
                }
            }
        }

        /** A task, called periodically, that notifies about match lost.  */
        private val matchLostNotifierTask = object : Runnable {
            override fun run() {
                val now = SystemClock.elapsedRealtimeNanos()

                synchronized(LOCK) {
                    val iterator = devicesInRange.values.iterator()
                    while (iterator.hasNext()) {
                        val result = iterator.next()
                        if (result.getTimestampNanos() < now - scanSettings!!.getMatchLostDeviceTimeout()) {
                            iterator.remove()
                            handler?.post {
                                scanCallback?.onScanResult(
                                    ScanSettings.CALLBACK_TYPE_MATCH_LOST,
                                    result
                                )
                            }
                        }
                    }

                    if (!devicesInRange.isEmpty()) {
                        handler?.postDelayed(this, scanSettings!!.getMatchLostTaskInterval())
                    }
                }
            }
        }

        /* package */ constructor(
            offloadedBatchingSupported: Boolean,
            offloadedFilteringSupported: Boolean,
            filters: List<ScanFilter>?,
            settings: ScanSettings?,
            callback: ScanCallback?,
            handler: Handler?
        ){
            this.filters = Collections.unmodifiableList(filters)
            this.scanSettings = settings
            this.scanCallback = callback
            this.handler = handler
            this.scanningStopped = false

            // Emulate other callback types
            val callbackTypesSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            emulateFoundOrLostCallbackType =
                settings!!.getCallbackType() != ScanSettings.CALLBACK_TYPE_ALL_MATCHES && (!callbackTypesSupported || !settings!!.getUseHardwareCallbackTypesIfSupported())

            // Emulate filtering
            emulateFiltering =
                !filters!!.isEmpty() && (!offloadedFilteringSupported || !settings.getUseHardwareFilteringIfSupported())

            // Emulate batching
            val delay = settings.getReportDelayMillis()
            emulateBatching =
                delay > 0 && (!offloadedBatchingSupported || !settings.getUseHardwareBatchingIfSupported())
            if (emulateBatching) {
                handler?.postDelayed(flushPendingScanResultsTask, delay)
            }
        }

        /* package */ open fun close() {
            scanningStopped = true
            handler?.removeCallbacksAndMessages(null)
            synchronized(LOCK) {
                devicesInRange.clear()
                devicesInBatch.clear()
                scanResults.clear()
            }
        }

        /* package */  fun flushPendingScanResults() {
            if (emulateBatching && !scanningStopped) {
                synchronized(LOCK) {
                    scanCallback?.onBatchScanResults(ArrayList(scanResults))
                    scanResults.clear()
                    devicesInBatch.clear()
                }
            }
        }

        /* package */  fun handleScanResult(
            callbackType: Int,
            scanResult: ScanResult
        ) {
            if (scanningStopped || !filters.isNullOrEmpty() && !matches(scanResult))
                return

            val deviceAddress = scanResult.getDevice()!!.address

            // Notify if a new device was found and callback type is FIRST MATCH
            if (emulateFoundOrLostCallbackType) { // -> Callback type != ScanSettings.CALLBACK_TYPE_ALL_MATCHES
                val previousResult: ScanResult?
                val firstResult: Boolean
                synchronized(devicesInRange) {
                    // The periodic task will be started only on the first result
                    firstResult = devicesInRange.isEmpty()
                    // Save the first result or update the old one with new data
                    previousResult = devicesInRange.put(deviceAddress, scanResult)
                }

                if (previousResult == null) {
                    if (scanSettings!!.getCallbackType() and ScanSettings.CALLBACK_TYPE_FIRST_MATCH > 0) {
                        scanCallback?.onScanResult(
                            ScanSettings.CALLBACK_TYPE_FIRST_MATCH,
                            scanResult
                        )
                    }
                }

                // In case user wants to be notified about match lost, we need to start a task that
                // will check the timestamp periodically
                if (firstResult) {
                    if (scanSettings!!.getCallbackType() and ScanSettings.CALLBACK_TYPE_MATCH_LOST > 0) {
                        handler?.removeCallbacks(matchLostNotifierTask)
                        handler?.postDelayed(
                            matchLostNotifierTask,
                            scanSettings!!.getMatchLostTaskInterval()
                        )
                    }
                }
            } else {
                // A callback type may not contain CALLBACK_TYPE_ALL_MATCHES and any other value.
                // If devicesInRange is empty, report delay > 0 means we are emulating hardware
                // batching. Otherwise handleScanResults(List) is called, not this method.
                if (emulateBatching) {
                    synchronized(LOCK) {
                        if (!devicesInBatch.contains(deviceAddress)) {  // add only the first record from the device, others will be skipped
                            scanResults.add(scanResult)
                            devicesInBatch.add(deviceAddress)
                        }
                    }
                    return
                }

                scanCallback?.onScanResult(callbackType, scanResult)
            }
        }

        /* package */  fun handleScanResults(results: MutableList<ScanResult>) {
            if (scanningStopped)
                return

            var filteredResults: MutableList<ScanResult> = results

            if (emulateFiltering) {
                filteredResults = ArrayList()
                for (result in results)
                    if (matches(result))
                        filteredResults.add(result)
            }

            scanCallback?.onBatchScanResults(filteredResults)
        }

        /* package */  fun handleScanError(errorCode: Int) {
            scanCallback?.onScanFailed(errorCode)
        }

        private fun matches(result: ScanResult): Boolean {
            if (filters == null)
                return false
            for (filter in filters.orEmpty()) {
                if (filter.matches(result))
                    return true
            }
            return false
        }
    }
}