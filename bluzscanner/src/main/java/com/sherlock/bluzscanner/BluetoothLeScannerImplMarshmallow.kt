package com.sherlock.bluzscanner

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.os.Build

@TargetApi(Build.VERSION_CODES.M)
open class BluetoothLeScannerImplMarshmallow: BluetoothLeScannerImplLollipop {

    constructor():super(){}

    override fun toNativeScanSettings(
        adapter: BluetoothAdapter,
        settings: ScanSettings?,
        exactCopy: Boolean
    ): android.bluetooth.le.ScanSettings {
        val builder = android.bluetooth.le.ScanSettings.Builder()

        if (exactCopy || adapter.isOffloadedScanBatchingSupported && settings!!.getUseHardwareBatchingIfSupported())
            builder.setReportDelay(settings!!.getReportDelayMillis())

        if (exactCopy || settings!!.getUseHardwareCallbackTypesIfSupported())
            builder.setCallbackType(settings!!.getCallbackType())
                .setMatchMode(settings.getMatchMode())
                .setNumOfMatches(settings.getNumOfMatches())

        builder.setScanMode(settings.getScanMode())

        return builder.build()
    }
}