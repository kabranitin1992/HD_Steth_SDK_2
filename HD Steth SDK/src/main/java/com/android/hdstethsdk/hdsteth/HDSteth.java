package com.android.hdstethsdk.hdsteth;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;

public interface HDSteth {


    public void fnDetectDevice(ArrayList<BluetoothDevice> bluetoothDevices);


    public void fnConnected();

}
