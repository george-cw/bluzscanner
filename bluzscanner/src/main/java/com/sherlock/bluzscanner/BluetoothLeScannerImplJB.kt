package com.sherlock.bluzscanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import java.util.HashMap

class BluetoothLeScannerImplJB: BluetoothLeScannerCompat{
    private val wrappers = HashMap<ScanCallback?, ScanCallbackWrapper>()

    constructor(): super(){}

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH])
    override fun startScanInternal(
        filters: List<ScanFilter>?,
        settings: ScanSettings?,
        callback: ScanCallback?,
        handler: Handler?
    ) {
        val adapter = BluetoothAdapter.getDefaultAdapter()

        val shouldStart: Boolean

        synchronized(wrappers) {
            if (wrappers.containsKey(callback)) {
                throw IllegalArgumentException("scanner already started with given scanCallback")
            }
            val wrapper = BluetoothLeScannerCompat.ScanCallbackWrapper(
                false, false,
                filters, settings, callback, handler
            )
            shouldStart = wrappers.isEmpty()
            wrappers.put(callback, wrapper)
        }

        if (shouldStart) {
            adapter.startLeScan(scanCallback)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH])
    override fun stopScanInternal(callback: ScanCallback) {
        val adapter = BluetoothAdapter.getDefaultAdapter()

        val shouldStop: Boolean
        val wrapper: BluetoothLeScannerCompat.ScanCallbackWrapper?
        synchronized(wrappers) {
            wrapper = wrappers.remove(callback)
            shouldStop = wrappers.isEmpty()
        }
        if (wrapper == null)
            return

        wrapper.close()

        if (shouldStop) {
            adapter.stopLeScan(scanCallback)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH)
    override fun flushPendingScanResults(callback: ScanCallback) {

        if (callback == null) {
            throw IllegalArgumentException("callback cannot be null!")
        }

        val wrapper: BluetoothLeScannerCompat.ScanCallbackWrapper?
        synchronized(wrappers) {
            wrapper = wrappers[callback]
        }

        if (wrapper == null) {
            throw IllegalArgumentException("callback not registered!")
        }

        wrapper.flushPendingScanResults()
    }

    private val scanCallback =
        BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
            val scanResult = ScanResult(
                device, ScanRecord().parseFromBytes(scanRecord),
                rssi, SystemClock.elapsedRealtimeNanos()
            )

            synchronized(wrappers) {
                val scanCallbackWrappers = wrappers.values
                for (wrapper in scanCallbackWrappers) {
                    wrapper.handler?.post(Runnable {
                        wrapper.handleScanResult(
                            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
                            scanResult
                        )
                    })
                }
            }
        }
}