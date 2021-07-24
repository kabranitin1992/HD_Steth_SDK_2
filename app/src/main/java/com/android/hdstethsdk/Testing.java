package com.android.hdstethsdk;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.hdstethsdk.hdsteth.ConnectToHDSteth;
import com.android.hdstethsdk.hdsteth.HDSteth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Testing extends AppCompatActivity {

    Button btnScan, btnDisconnect, btnRecord;
    EditText etSecs;
    TextView ecgValue, murValue, hsValue;
    Context context;
    ArrayList<BluetoothDevice> devices;
    RadioButton rb10MPPA, rbNot10MPPA;
    boolean is10MPA;
    int MY_PERMISSIONS_REQUEST = 1;
    String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_BACKGROUND_LOCATION};
    HDStethCallBack hdStethCallBack;
    ConnectToHDSteth connectToHDSteth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);


        context = this;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST);
        } else {
            btnScan = (Button) findViewById(R.id.btnScan);
            btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
            btnRecord = (Button) findViewById(R.id.btnRecord);
            etSecs = (EditText) findViewById(R.id.etSecs);
            ecgValue = (TextView) findViewById(R.id.ecgValue);
            murValue = (TextView) findViewById(R.id.murValue);
            hsValue = (TextView) findViewById(R.id.hsValue);
            rbNot10MPPA = (RadioButton) findViewById(R.id.rbNot10MPPA);
            rb10MPPA = (RadioButton) findViewById(R.id.rb10MPPA);


            hdStethCallBack = new HDStethCallBack();

            connectToHDSteth = new ConnectToHDSteth();
            connectToHDSteth.init(context, hdStethCallBack);

            rb10MPPA.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rb10MPPA.setChecked(true);
                    rbNot10MPPA.setChecked(false);
                }
            });

            rbNot10MPPA.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rb10MPPA.setChecked(false);
                    rbNot10MPPA.setChecked(true);
                }
            });

            btnScan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    connectToHDSteth.fnDetectDevice(context);

                }
            });


        }

    }


    private void createFolder() {
        File f1 = new File(context.getExternalCacheDir(), "HDSteth");
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                Log.i("Folder ", "created");
            }
        }

        f1 = new File(Environment.getExternalStorageDirectory() + "/HDSteth");
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                Log.i("Folder ", "created");
            }
        }
    }

    private class HDStethCallBack implements HDSteth {


        @Override
        public void fnDetectDevice(ArrayList<BluetoothDevice> devices) {
            if (rb10MPPA.isChecked()) {
                is10MPA = true;
            } else {
                is10MPA = false;
            }
            ArrayList<String> devicesNames = new ArrayList<String>();
            for (BluetoothDevice device : devices) {
                devicesNames.add(device.getName());
            }
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
            builderSingle.setIcon(R.mipmap.ic_launcher);
            builderSingle.setTitle("Select One Device:-");

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, devicesNames);

            builderSingle.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {


                    connectToHDSteth.fnConnectDevice(context, devices.get(which), is10MPA);

//                    HDSteth.fnConnectDevice(context, devices.get(which), is10MPA);
                    dialog.dismiss();
                }
            });
            builderSingle.show();
        }


        @Override
        public void fnConnected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

}