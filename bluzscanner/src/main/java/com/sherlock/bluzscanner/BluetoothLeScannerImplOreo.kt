package com.sherlock.bluzscanner

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresPermission
import java.util.ArrayList
import java.util.HashMap

@TargetApi(Build.VERSION_CODES.O)
class BluetoothLeScannerImplOreo: BluetoothLeScannerImplMarshmallow {

    internal constructor(): super(){}

    override fun toNativeScanSettings(
        adapter: BluetoothAdapter,
        settings: ScanSettings?,
        exactCopy: Boolean
    ):
            /* package */ android.bluetooth.le.ScanSettings {
        val builder = android.bluetooth.le.ScanSettings.Builder()

        if (exactCopy || adapter.isOffloadedScanBatchingSupported && settings!!.getUseHardwareBatchingIfSupported())
            builder.setReportDelay(settings!!.getReportDelayMillis())

        if (exactCopy || settings!!.getUseHardwareCallbackTypesIfSupported())
            builder.setCallbackType(settings!!.getCallbackType())
                .setMatchMode(settings.getMatchMode())
                .setNumOfMatches(settings.getNumOfMatches())

        builder.setScanMode(settings.getScanMode())
            .setLegacy(settings.getLegacy())
            .setPhy(settings.getPhy())

        return builder.build()
    }

    fun fromNativeScanSettings(
        settings: android.bluetooth.le.ScanSettings,
        useHardwareBatchingIfSupported: Boolean,
        useHardwareFilteringIfSupported: Boolean,
        useHardwareCallbackTypesIfSupported: Boolean,
        matchLostDeviceTimeout: Long,
        matchLostTaskInterval: Long,
        matchMode: Int, numOfMatches: Int
    ):
            /* package */ ScanSettings {
        val builder = ScanSettings().Builder()
            .setLegacy(settings.legacy)
            .setPhy(settings.phy)
            .setCallbackType(settings.callbackType)
            .setScanMode(settings.scanMode)
            .setReportDelay(settings.reportDelayMillis)
            .setUseHardwareBatchingIfSupported(useHardwareBatchingIfSupported)
            .setUseHardwareFilteringIfSupported(useHardwareFilteringIfSupported)
            .setUseHardwareCallbackTypesIfSupported(useHardwareCallbackTypesIfSupported)
            .setMatchOptions(matchLostDeviceTimeout, matchLostTaskInterval)
            // Those 2 values are not accessible from the native ScanSettings.
            // They need to be transferred separately in intent extras.
            .setMatchMode(matchMode).setNumOfMatches(numOfMatches)

        return builder.build()
    }

    fun fromNativeScanFilters(filters: List<android.bluetooth.le.ScanFilter>):
            /* package */ ArrayList<ScanFilter> {
        val nativeScanFilters = ArrayList<ScanFilter>()
        for (filter in filters)
            nativeScanFilters.add(fromNativeScanFilter(filter))
        return nativeScanFilters
    }

    fun fromNativeScanFilter(filter: android.bluetooth.le.ScanFilter):
            /* package */ ScanFilter {
        val builder = ScanFilter.Builder()
        builder.setDeviceAddress(filter.deviceAddress)
            .setDeviceName(filter.deviceName)
            .setServiceUuid(filter.serviceUuid, filter.serviceUuidMask)
            .setManufacturerData(
                filter.manufacturerId,
                filter.manufacturerData,
                filter.manufacturerDataMask
            )

        if (filter.serviceDataUuid != null)
            builder.setServiceData(
                filter.serviceDataUuid!!,
                filter.serviceData,
                filter.serviceDataMask
            )

        return builder.build()
    }

    override fun fromNativeScanResult(result: android.bluetooth.le.ScanResult):
            /* package */ ScanResult {
        // Calculate the important bits of Event Type
        val eventType = (result.dataStatus shl 5
                or (if (result.isLegacy) ScanResult.ET_LEGACY_MASK else 0)
                or if (result.isConnectable) ScanResult.ET_CONNECTABLE_MASK else 0)
        // Get data as bytes
        val data = if (result.scanRecord != null) result.scanRecord!!.bytes else null
        // And return the v18.ScanResult
        return ScanResult(
            result.device, eventType, result.primaryPhy,
            result.secondaryPhy, result.advertisingSid,
            result.txPower, result.rssi,
            result.periodicAdvertisingInterval,
            ScanRecord().parseFromBytes(data), result.timestampNanos
        )
    }
}