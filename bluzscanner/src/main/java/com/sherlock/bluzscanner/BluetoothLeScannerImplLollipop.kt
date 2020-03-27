package com.sherlock.bluzscanner

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.os.Handler
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import java.util.ArrayList
import java.util.HashMap

@TargetApi(21)
open class BluetoothLeScannerImplLollipop:BluetoothLeScannerCompat {

    private val wrappers = HashMap<ScanCallback?, ScanCallbackWrapperLollipop>()

    constructor(){}

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH])
    override fun startScanInternal(
        filters: List<ScanFilter>?,
        settings: ScanSettings?,
        callback: ScanCallback?,
        handler: Handler?
    ) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val scanner =
            adapter.bluetoothLeScanner ?: throw IllegalStateException("BT le scanner not available")

        val offloadedBatchingSupported = adapter.isOffloadedScanBatchingSupported
        val offloadedFilteringSupported = adapter.isOffloadedFilteringSupported

        val wrapper: ScanCallbackWrapperLollipop

        synchronized(wrappers) {
            if (wrappers.containsKey(callback)) {
                throw IllegalArgumentException("scanner already started with given callback")
            }
            wrapper = ScanCallbackWrapperLollipop(
                offloadedBatchingSupported,
                offloadedFilteringSupported, filters, settings, callback, handler
            )
            wrappers.put(callback, wrapper)
        }

        val nativeScanSettings = toNativeScanSettings(adapter, settings, false)
        var nativeScanFilters: List<android.bluetooth.le.ScanFilter>? = null
        if (!filters!!.isEmpty() && offloadedFilteringSupported && settings!!.getUseHardwareFilteringIfSupported())
            nativeScanFilters = toNativeScanFilters(filters)

        scanner.startScan(nativeScanFilters, nativeScanSettings, wrapper.nativeCallback)
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH])
    override fun stopScanInternal(callback: ScanCallback) {
        val wrapper: ScanCallbackWrapperLollipop?
        synchronized(wrappers) {
            wrapper = wrappers.remove(callback)
        }
        if (wrapper == null)
            return

        wrapper?.close()

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            val scanner = adapter.bluetoothLeScanner
            scanner?.stopScan(wrapper!!.nativeCallback)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH)
    override fun flushPendingScanResults(callback: ScanCallback) {
        val adapter = BluetoothAdapter.getDefaultAdapter()

        if (callback == null) {
            throw IllegalArgumentException("callback cannot be null!")
        }

        val wrapper: ScanCallbackWrapperLollipop?
        synchronized(wrappers) {
            wrapper = wrappers.get(callback)
        }

        if (wrapper == null) {
            throw IllegalArgumentException("callback not registered!")
        }

        val settings = wrapper!!.scanSettings
        if (adapter.isOffloadedScanBatchingSupported && settings!!.getUseHardwareBatchingIfSupported()) {
            val scanner = adapter.bluetoothLeScanner ?: return
            scanner.flushPendingScanResults(wrapper!!.nativeCallback)
        } else {
            wrapper!!.flushPendingScanResults()
        }
    }

    open fun toNativeScanSettings(
        adapter: BluetoothAdapter,
        settings: ScanSettings?,
        exactCopy: Boolean
    ):
            /* package */ android.bluetooth.le.ScanSettings {
        val builder = android.bluetooth.le.ScanSettings.Builder()

        if (exactCopy || adapter.isOffloadedScanBatchingSupported && settings!!.getUseHardwareBatchingIfSupported())
            builder.setReportDelay(settings!!.getReportDelayMillis())

        if (settings!!.getScanMode() != ScanSettings.SCAN_MODE_OPPORTUNISTIC) {
            builder.setScanMode(settings!!.getScanMode())
        } else {
            // SCAN MORE OPPORTUNISTIC is not supported on Lollipop.
            // Instead, SCAN_MODE_LOW_POWER will be used.
            builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        }

        settings.disableUseHardwareCallbackTypes() // callback types other then CALLBACK_TYPE_ALL_MATCHES are not supported on Lollipop

        return builder.build()
    }

    fun toNativeScanFilters(filters: List<ScanFilter>?):
            /* package */ ArrayList<android.bluetooth.le.ScanFilter> {
        val nativeScanFilters = ArrayList<android.bluetooth.le.ScanFilter>()
        for (filter in filters.orEmpty())
            nativeScanFilters.add(toNativeScanFilter(filter))
        return nativeScanFilters
    }

    fun toNativeScanFilter(filter: ScanFilter):
            /* package */ android.bluetooth.le.ScanFilter {
        val builder = android.bluetooth.le.ScanFilter.Builder()
        builder.setDeviceAddress(filter.getDeviceAddress())
            .setDeviceName(filter.getDeviceName())
            .setServiceUuid(filter.getServiceUuid(), filter.getServiceUuidMask())
            .setManufacturerData(
                filter.getManufacturerId(),
                filter.getManufacturerData(),
                filter.getManufacturerDataMask()
            )

        if (filter.getServiceDataUuid() != null)
            builder.setServiceData(
                filter.getServiceDataUuid(),
                filter.getServiceData(),
                filter.getServiceDataMask()
            )

        return builder.build()
    }

    open fun fromNativeScanResult(nativeScanResult: android.bluetooth.le.ScanResult):
            /* package */ ScanResult {
        val data = if (nativeScanResult.scanRecord != null)
            nativeScanResult.scanRecord!!.bytes
        else
            null
        return ScanResult(
            nativeScanResult.device, ScanRecord().parseFromBytes(data),
            nativeScanResult.rssi, nativeScanResult.timestampNanos
        )
    }

    fun fromNativeScanResults(nativeScanResults: List<android.bluetooth.le.ScanResult>):
            /* package */ ArrayList<ScanResult> {
        val results = ArrayList<ScanResult>()
        for (nativeScanResult in nativeScanResults) {
            val result = fromNativeScanResult(nativeScanResult)
            results.add(result)
        }
        return results
    }

    class ScanCallbackWrapperLollipop : ScanCallbackWrapper
    {

        constructor(
            offloadedBatchingSupported: Boolean,
            offloadedFilteringSupported: Boolean,
            filters: List<ScanFilter>?,
            settings: ScanSettings?,
            callback: ScanCallback?,
            handler: Handler?
        ):super(offloadedBatchingSupported, offloadedFilteringSupported,
            filters, settings, callback, handler){

        }

        @TargetApi(21)
        open val nativeCallback = object : android.bluetooth.le.ScanCallback() {
            private var lastBatchTimestamp: Long = 0

            override fun onScanResult(
                callbackType: Int,
                nativeScanResult: android.bluetooth.le.ScanResult
            ) {
                handler?.post(Runnable {
                    val scannerImpl =
                        BluetoothLeScannerCompat.getScanner() as BluetoothLeScannerImplLollipop?
                    val result = scannerImpl!!.fromNativeScanResult(nativeScanResult)
                    handleScanResult(callbackType, result)
                })
            }

            override fun onBatchScanResults(nativeScanResults: List<android.bluetooth.le.ScanResult>) {
                handler?.post(Runnable {
                    // On several phones the onBatchScanResults is called twice for every batch.
                    // Skip the second call if came to early.
                    val now = SystemClock.elapsedRealtime()
                    if (lastBatchTimestamp > now - scanSettings!!.getReportDelayMillis() + 5) {
                        return@Runnable
                    }
                    lastBatchTimestamp = now

                    val scannerImpl =
                        BluetoothLeScannerCompat.getScanner() as BluetoothLeScannerImplLollipop?
                    val results = scannerImpl!!.fromNativeScanResults(nativeScanResults)
                    handleScanResults(results)
                })
            }

            override fun onScanFailed(errorCode: Int) {
                handler?.post(Runnable {
                    // We were able to determine offloaded batching and filtering before we started scan,
                    // but there is no method checking if callback types FIRST_MATCH and MATCH_LOST
                    // are supported. We get an error here it they are not.
                    if (scanSettings!!.getUseHardwareCallbackTypesIfSupported() && scanSettings!!.getCallbackType() != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                        // On Nexus 6 with Android 6.0 (MPA44G, M Pre-release 3) the errorCode = 5 (SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES)
                        // On Pixel 2 with Android 9.0 the errorCode = 4 (SCAN_FAILED_FEATURE_UNSUPPORTED)

                        // This feature seems to be not supported on your phone.
                        // Let's try to do pretty much the same in the code.
                        scanSettings?.disableUseHardwareCallbackTypes()

                        val scanner = BluetoothLeScannerCompat.getScanner()
                        try {
                            scanner!!.stopScan(scanCallback)
                        } catch (e: Exception) {
                            // Ignore
                        }

                        try {
                            scanner!!.startScanInternal(
                                filters,
                                scanSettings,
                                scanCallback,
                                handler
                            )
                        } catch (e: Exception) {
                            // Ignore
                        }

                        return@Runnable
                    }

                    // else, notify user application
                    handleScanError(errorCode)
                })
            }
        }
    }
}