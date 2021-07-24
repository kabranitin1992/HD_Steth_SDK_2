package com.android.hdstethsdk.hdsteth;


import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.android.hdstethsdk.bluetooth.ble.IBleBaseActivityUiCallback;
import com.android.hdstethsdk.bluetooth.ble.vsp.SerialManager;
import com.android.hdstethsdk.bluetooth.ble.vsp.SerialManagerUiCallback;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ConnectToHDSteth implements SerialManagerUiCallback, IBleBaseActivityUiCallback {

    private HashMap<String, BluetoothDevice> mScanResults;
    private BtleScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private boolean is10MPA, isRecording;
    private int ecgPoints1 = 0, ecgPoints2 = 0, ecgPoints3 = 0, ecgPoints4 = 0, ecgPoints5 = 0;
    private HashMap<String, ArrayList<Integer>> ecgHashmap, hsHashmap, murHashmap;
    private Context context;
    private SerialManager mSerialManager;
    private HDSteth hdSteth;
    private StringBuilder ecgBuilder, hsBuilder, murBuild;
    private String connectedDevice;

    public void init(Context context, HDSteth hdSteth) {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        mScanResults = new HashMap<String, BluetoothDevice>();
        mScanCallback = new BtleScanCallback();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        this.context = context;
        this.hdSteth = hdSteth;
        mSerialManager = new SerialManager((Activity) context, this);
        is10MPA = false;
        isRecording = false;
        ecgHashmap = new HashMap<String, ArrayList<Integer>>();
        hsHashmap = new HashMap<String, ArrayList<Integer>>();
        murHashmap = new HashMap<String, ArrayList<Integer>>();
    }

    public void fnDetectDevice(Context context) {
        ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<BluetoothDevice>();
        if (chkPermission(context)) {
            ProgressDialog progress = new ProgressDialog(context);
            progress.setMessage("Scanning...");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progress.show();
                }
            });

            List<ScanFilter> filters = new ArrayList<>();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build();
            mScanResults = new HashMap<>();
            mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                        }
                    });
                    scanComplete(context);
                }
            }, 5000);


        } else {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Every Permission is Required.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void scanComplete(Context context) {
        ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<BluetoothDevice>();
        mBluetoothLeScanner.stopScan(mScanCallback);
        if (mScanResults.isEmpty()) {
            hdSteth.fnDetectDevice(bluetoothDevices);
        }
        ArrayList<String> devices = new ArrayList<String>();

        for (String deviceAddress : mScanResults.keySet()) {
            Log.d("TAG", "Found device: " + deviceAddress);
            if (!devices.contains(deviceAddress) && deviceAddress != null) {
                devices.add(deviceAddress);
                bluetoothDevices.add(mScanResults.get(deviceAddress));
            }
        }
        hdSteth.fnDetectDevice(bluetoothDevices);
    }

    private class BtleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("TAG", "BLE Scan Failed with code " + errorCode);
        }

        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceAddress = device.getName();
            if (deviceAddress != null && (deviceAddress.contains("LAIRD") || deviceAddress.toLowerCase().contains("hdsteth"))) {
                mScanResults.put(deviceAddress, device);
            }
        }
    }

    @Override
    public void onUiConnected() {
        hdSteth.fnConnected();
    }

    @Override
    public void onUiConnecting() {

    }

    @Override
    public void onUiDisconnected(int status) {
    }

    @Override
    public void onUiDisconnecting() {

    }

    @Override
    public void onUiBatteryRead(String valueBattery) {

    }

    @Override
    public void onUiReadRemoteRssi(int valueRSSI) {

    }

    public void fnConnectDevice(Context context, BluetoothDevice bluetoothDevice,
                                boolean is10MPA) {
        if (chkPermission(context)) {
            this.is10MPA = is10MPA;
            mSerialManager.connect(bluetoothDevice, false);
            connectedDevice = bluetoothDevice.getName();
        } else {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Every Permission is Required.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }



    @Override
    public void onUiVspServiceFound(boolean found) {

    }

    @Override
    public void onUiSendDataSuccess(String dataSend) {

    }

    @Override
    public void onUiReceiveData(byte[] dataReceived) {

    }

    @Override
    public void onUiUploaded() {

    }


    private static boolean chkPermission(Context context) {
        int MY_PERMISSIONS_REQUEST = 1;
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_BACKGROUND_LOCATION};
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            ActivityCompat.requestPermissions((Activity) context, permissions, MY_PERMISSIONS_REQUEST);
            return false;
        } else {
            return true;
        }
    }
}
