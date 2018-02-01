package com.ee202a.attendance.seminarcheckin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BeaconScanner {

    public Boolean detected = false;
//    AWSConnection mAWSConnection;


    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanFilter mScanFilter;
    private ScanSettings mScanSettings;
    private byte[] UUID0;
    private byte[] UUID1;
    private byte[] UUID2;
    private byte[] UUID3;
    private byte[] Major_Minor;
    private Handler mHandler = new Handler();
    private Runnable r = new Runnable() {
        @Override
        public void run() {
            mBluetoothLeScanner.stopScan(mScanCallback);
            Log.d("BeaconScanner", "scan stopped");
        }
    };

    //constructor
//    public BeaconScanner() {
////        UUID0 = ByteBuffer.allocate(4).putInt(uuid_array[0]).array();
////        UUID1 = ByteBuffer.allocate(4).putInt(uuid_array[1]).array();
////        UUID2 = ByteBuffer.allocate(4).putInt(uuid_array[2]).array();
////        UUID3 = ByteBuffer.allocate(4).putInt(uuid_array[3]).array();
////        Major_Minor = ByteBuffer.allocate(4).putInt(uuid_array[4]).array();
//
//    }

    public void startBeaconScan(BluetoothManager bluetoothManager, int [] uuid_array, int SCAN_DURATION_MS) {
        //start the scan timeout after SCAN_DURATION_MS
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        UUID0 = ByteBuffer.allocate(4).putInt(uuid_array[0]).array();
        UUID1 = ByteBuffer.allocate(4).putInt(uuid_array[1]).array();
        UUID2 = ByteBuffer.allocate(4).putInt(uuid_array[2]).array();
        UUID3 = ByteBuffer.allocate(4).putInt(uuid_array[3]).array();
        Major_Minor = ByteBuffer.allocate(4).putInt(uuid_array[4]).array();
        setScanFilter();
        setScanSettings();
        detected = false; //clears detected before scanning
        mBluetoothLeScanner.startScan(Arrays.asList(mScanFilter), mScanSettings, mScanCallback);
        Log.d("BeaconScanner", "scanning...");
        mHandler.postDelayed(r, SCAN_DURATION_MS); //timeout and stop
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        //this scan callback is only called when the desired UUID is found
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (!detected){ //if statement so it only goes here once
                detected = true;
                AWSConnection.RandomAttendanceResponse(detected);
                ScanRecord mScanRecord = result.getScanRecord();
                Log.d("BeaconScanner", mScanRecord != null ? mScanRecord.toString() : "null");
                //return to the server that match is found
            }
        }

        @Override
        public void onScanFailed(int errorCode){
            Log.d("BeaconScanner", "onScanFailed");
        }
    };

    private void setScanFilter() {
        //set a filter that will only scan for the UUID that you want
        ScanFilter.Builder mBuilder = new ScanFilter.Builder();
        ByteBuffer mManufacturerData = ByteBuffer.allocate(23);
        ByteBuffer mManufacturerDataMask = ByteBuffer.allocate(24);

        byte[] uuid = {UUID0[0], UUID0[1], UUID0[2], UUID0[3], UUID1[0], UUID1[1], UUID1[2],
                UUID1[3], UUID2[0], UUID2[1], UUID2[2], UUID2[3], UUID3[0], UUID3[1], UUID3[2],
                UUID3[3], Major_Minor[0], Major_Minor[1], Major_Minor[2], Major_Minor[3], (byte)0xC8};

        mManufacturerData.put(0, (byte)0x02);
        mManufacturerData.put(1, (byte)0x15);
        for (int i=2; i<=22; i++) {
            mManufacturerData.put(i, uuid[i-2]);
        }
        for (int i=0; i<=22; i++) {
            mManufacturerDataMask.put((byte)0);
        }

        //UUID Major Minor from RPi iBeacon needs to match exactly
        mBuilder.setManufacturerData(76, mManufacturerData.array());
        mScanFilter = mBuilder.build();
    }

    private void setScanSettings() {
        ScanSettings.Builder mBuilder = new ScanSettings.Builder();
        mBuilder.setReportDelay(0);
        mBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        mScanSettings = mBuilder.build();
    }
}