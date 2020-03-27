package com.sherlock.bluzscandemo

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.sherlock.bluzscanner.*

import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        EasyPermissions.requestPermissions(
            PermissionRequest.Builder(this, 100, Manifest.permission.ACCESS_FINE_LOCATION)
                .setRationale(R.string.location_rationale)
                .setPositiveButtonText(R.string.yes)
                .setNegativeButtonText(R.string.no)
                .build()
        )

        var scanBtn:Button = findViewById(R.id.scan)
        scanBtn.setOnClickListener(View.OnClickListener { view -> startScan() })
    }

    fun startScan(){
        val scanner = BluetoothLeScannerCompat.getScanner()
        val settings = ScanSettings().Builder()
            .setLegacy(false)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            //.setReportDelay(1000)
            .setUseHardwareBatchingIfSupported(true)
            .build()
        val filters = ArrayList<ScanFilter>()
        filters.add(ScanFilter.Builder().build())
        scanner?.startScan(filters,settings,ScanCallbackImp())
    }

    inner class ScanCallbackImp: ScanCallback(){
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d(TAG, "onScanResult: " + result.getDevice()?.name + " " + result.getDevice()?.address)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for(devices in results.orEmpty())
                Log.d(TAG, "onBatchScanResults: " + devices.getDevice()?.name+ " " + devices.getDevice()?.address)
            Log.d(TAG, "\r\n" )
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d(TAG, "onScanFailed: " + errorCode)
        }
    }
}
