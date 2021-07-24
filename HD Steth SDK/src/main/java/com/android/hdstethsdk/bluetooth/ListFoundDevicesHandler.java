package com.android.hdstethsdk.bluetooth;//package com.hdsteth.android.bluetooth;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.bluetooth.BluetoothDevice;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseAdapter;
//import android.widget.CompoundButton;
//import android.widget.RadioButton;
//import android.widget.TextView;
//
//import com.ust.medical.hdstethl.LoginOptions2;
//import com.ust.medical.hdstethl.R;
//
//import java.util.ArrayList;
//
//public class ListFoundDevicesHandler extends BaseAdapter {
//    private String TAG = "ListFoundDevicesHandler";
//    private ArrayList<BluetoothDevice> mDevices = new ArrayList();
//    private LayoutInflater mInflater;
//    private ArrayList<Integer> mRSSIs = new ArrayList();
//
//
//    private ArrayList<Boolean> status = new ArrayList<>();
//    private class FieldReferences {
//        TextView valueDeviceAddress;
//        TextView valueDeviceName;
//        RadioButton rbDeviceName;
//
//        private FieldReferences() {
//        }
//    }
//
//    public ListFoundDevicesHandler(Activity par) {
//        refreshList();
//        this.mInflater = par.getLayoutInflater();
//    }
//
//    public void addDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
//        //HDStethLogger.i(this.TAG, "Device added in found devices list: " + device.getAddress());
//        Log.i("prasann", "Device added in found devices list: " + device.getAddress());
//
//        if (this.mDevices.contains(device)) {
//            this.mRSSIs.set(this.mDevices.indexOf(device), Integer.valueOf(rssi));
//        } else {
//            this.mDevices.add(device);
//            this.mRSSIs.add(Integer.valueOf(rssi));
//        }
//        refreshList();
//        notifyDataSetChanged();
//    }
//
//    public void addDevice(BluetoothDevice device, int rssi) {
//        //Log.i("prasann", "Device added in found devices list: " + device.getAddress());
//        if (this.mDevices.contains(device)) {
//            this.mRSSIs.set(this.mDevices.indexOf(device), Integer.valueOf(rssi));
//        } else {
//            this.mDevices.add(device);
//            this.mRSSIs.add(Integer.valueOf(rssi));
//        }
//        refreshList();
//        notifyDataSetChanged();
//
//    }
//
//    public BluetoothDevice getDevice(int index) {
//        refreshList();
//        return (BluetoothDevice) this.mDevices.get(index);
//    }
//
//    public int getRssi(int index) {
//        return ((Integer) this.mRSSIs.get(index)).intValue();
//    }
//
//    public void clearList() {
//        this.mDevices.clear();
//        this.mRSSIs.clear();
//    }
//
//    public int getCount() {
//       // refreshList();
//        return this.mDevices.size();
//    }
//
//    public Object getItem(int position) {
//        return getDevice(position);
//    }
//
//    public long getItemId(int position) {
//        return (long) position;
//    }
//
//    @SuppressLint({"InflateParams"})
//    public View getView(final int position, View convertView, ViewGroup parent) {
//        final FieldReferences fields;
//        if (convertView == null) {
//            convertView = this.mInflater.inflate(R.layout.item_scanned, null, false);
//            fields = new FieldReferences();
//            fields.valueDeviceName = (TextView) convertView.findViewById(R.id.valueDeviceNameItem);
////            fields.valueDeviceAddress = (TextView) convertView.findViewById(R.id.valueDeviceAddressItem);
//            fields.rbDeviceName = (RadioButton) convertView.findViewById(R.id.rbDeviceName);
//            convertView.setTag(fields);
//        } else {
//            fields = (FieldReferences) convertView.getTag();
//        }
//        fields.rbDeviceName.setClickable(false);
//        final BluetoothDevice device = (BluetoothDevice) this.mDevices.get(position);
//        int rssi = ((Integer) this.mRSSIs.get(position)).intValue();
//        String rssiString;
//        if (rssi == 0) {
//            rssiString = "N/A";
//        } else {
//            rssiString = rssi + " db";
//        }
//        String name = device.getName();
//        String address = device.getAddress();
//        if (name == null || name.length() <= 0) {
//            name = "Unknown Device";
//        }
//        name = "["+name+"_"+address+"]";
//        fields.valueDeviceName.setText(name);
////        fields.valueDeviceAddress.setText(address);
//
//
//
//
//        fields.rbDeviceName.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//
//                status.set(position, isChecked);
//
//
//            }
//        });
//        fields.rbDeviceName.setChecked(status.get(position));
//
//
//
//
//
//        fields.rbDeviceName.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                refreshList();
//                status.set(position,true);
//                BluetoothDevice currentDevice =mDevices.get(position);
//                LoginOptions2.setDevice(currentDevice);
//                notifyDataSetChanged();
//            }
//        });
//
//        fields.valueDeviceName.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                refreshList();
//                status.set(position,true);
//                BluetoothDevice currentDevice =mDevices.get(position);
//                LoginOptions2.setDevice(currentDevice);
//                notifyDataSetChanged();
//            }
//        });
//        return convertView;
//    }
//
//    private void refreshList() {
//        status=new ArrayList<>();
//        for (int i = 0; i < this.mDevices.size(); i++) {
//            status.add(false);
//        }
//    }
//
//
//}
