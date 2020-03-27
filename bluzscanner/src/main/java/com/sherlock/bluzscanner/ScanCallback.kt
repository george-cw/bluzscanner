package com.sherlock.bluzscanner

abstract class ScanCallback {

    val SCAN_FAILED_ALREADY_STARTED = 1
    val SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2
    val SCAN_FAILED_INTERNAL_ERROR = 3
    val SCAN_FAILED_FEATURE_UNSUPPORTED = 4

    open fun onScanResult(callbackType: Int, result: ScanResult) {}

    open fun onBatchScanResults(results: List<ScanResult>) {}

    open fun onScanFailed(errorCode: Int) {}
}