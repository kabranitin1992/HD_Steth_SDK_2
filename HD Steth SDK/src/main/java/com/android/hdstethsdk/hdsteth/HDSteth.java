package com.android.hdstethsdk.hdsteth;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;

public interface HDSteth {

    public void fnReceiveData(String sPointType, int iPoint);
    public void fnDetectDevice(ArrayList<BluetoothDevice> bluetoothDevices);
    public void fnRecordData(String[] sPaths);
    public void fnDisconnected();
    public void fnConnected(String sDeviceType);

}
