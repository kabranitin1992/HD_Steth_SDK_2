package com.android.hdstethsdktesting;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

public class Testing extends AppCompatActivity {

    Button btnScan, btnDisconnect, btnRecord;
    EditText etSecs;
    TextView ecgValue, murValue, hsValue;
    Context context;
    ArrayList<BluetoothDevice> devices;
    int MY_PERMISSIONS_REQUEST = 1;
    String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_BACKGROUND_LOCATION};
    HDStethCallBack hdStethCallBack;
    ConnectToHDSteth connectToHDSteth;
    String sConnetedDeviceType;

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

            hdStethCallBack = new HDStethCallBack();

            connectToHDSteth = new ConnectToHDSteth();
            connectToHDSteth.init(context, hdStethCallBack);

            btnScan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    connectToHDSteth.fnDetectDevice(context);

                }
            });

            btnDisconnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectToHDSteth.fnDisconnectDevice(context);
                }
            });

            btnRecord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
//                        Context context, int iSeconds, String sDirectory, String sName, String sAge, String sPatientId, String sGender,
//                                String sHeight, String sHemoglobin, String sTemprature, String sWeight, String sBloodPressure, String sBloodGlucose,
//                                String sSPO2, String sAddress, String sCity, String sState, String sZipcode, String sPhone, String sEmail, Bitmap
//                        bmPatientImage

                        createFolder();
                        int iSecs = Integer.parseInt(etSecs.getText().toString());

                        Drawable drawable = getResources().getDrawable(R.mipmap.ic_launcher);


                        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        drawable.draw(canvas);


                        connectToHDSteth.fnRecordData(context, iSecs, context.getExternalCacheDir() + "/HDSteth",
                                "Test User", "20");
                    } catch (Exception e) {
                        Toast.makeText(context, "Enter Valid Seconds", Toast.LENGTH_SHORT).show();
                    }
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
        public void fnReceiveData(String sPointType, int iPoint) {
            if (sPointType.equalsIgnoreCase("ecg")) {
                ecgValue.setText("ECG : " + iPoint);
            } else if (sPointType.equalsIgnoreCase("mur")) {
                murValue.setText("MUR : " + iPoint);
            } else if (sPointType.equalsIgnoreCase("hs")) {
                hsValue.setText("HS : " + iPoint);
            }
        }

        @Override
        public void fnDetectDevice(ArrayList<BluetoothDevice> devices) {
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


                    connectToHDSteth.fnConnectDevice(context, devices.get(which));

//                    HDSteth.fnConnectDevice(context, devices.get(which), is10MPA);
                    dialog.dismiss();
                }
            });
            builderSingle.show();
        }

        @Override
        public void fnRecordData(String[] sPaths) {
//            sPaths[0] = ECG Path;
//            sPaths[1] = HS Path;
//            sPaths[2] = MUR Path;
//            sPaths[3] = Wavefile Path Path;

            Toast.makeText(context, sPaths[0], Toast.LENGTH_SHORT).show();
        }

        @Override
        public void fnDisconnected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show();
                }
            });
        }


        @Override
        public void fnConnected(String sDeviceType) {
            sConnetedDeviceType = sDeviceType;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

}