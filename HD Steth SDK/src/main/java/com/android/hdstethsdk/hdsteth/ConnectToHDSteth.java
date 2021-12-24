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
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.android.hdstethsdk.R;
import com.android.hdstethsdk.bluetooth.ble.IBleBaseActivityUiCallback;
import com.android.hdstethsdk.bluetooth.ble.vsp.SerialManager;
import com.android.hdstethsdk.bluetooth.ble.vsp.SerialManagerUiCallback;

import com.android.hdstethsdk.helpers.ECGModel;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPTableEvent;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
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
    private boolean isRecording;
    private int ecgPoints1 = 0, ecgPoints2 = 0, ecgPoints3 = 0, ecgPoints4 = 0, ecgPoints5 = 0;
    private HashMap<String, ArrayList<Integer>> ecgHashmap, hsHashmap, murHashmap;
    private Context context;
    private SerialManager mSerialManager;
    private HDSteth hdSteth;
    private StringBuilder ecgBuilder, hsBuilder, murBuild;
    private String connectedDevice;
    private ArrayList<Bitmap> screeshots;

    private XYPlot plot, plot2;
    private Redrawer redrawer, hsredrawer;
    ECGModel ecgSeries, hsSeries, murSeries;
    MyFadeFormatter ecgFormatter, hsFormatter, murFormatter;
    int graphIndex = 0, murIndex = 0, hsIndex = 0;
    int TIME_INTERVAL = 3200, KEY_SIZE = 5, ECG_LOWER_BOUND, ECG_UPPER_BOUND, PCG_LOWER_BOUND, PCG_UPPER_BOUND;
    int ECGZoomMinus = 64, ECGZoomPlus = 32, ECGZoom = 32;
    int ECGLowerZoomMinus = -64, ECGLowerZoomPlus = -32, ECGLowerZoom = -32;


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
        isRecording = false;
        ecgHashmap = new HashMap<String, ArrayList<Integer>>();
        hsHashmap = new HashMap<String, ArrayList<Integer>>();
        murHashmap = new HashMap<String, ArrayList<Integer>>();
        screeshots = new ArrayList<Bitmap>();

        TIME_INTERVAL = 3200;
        KEY_SIZE = 5;
        ECGZoomMinus = 64;
        ECGZoomPlus = 32;
        ECGZoom = 32;
        ECGLowerZoomMinus = -64;
        ECGLowerZoomPlus = -32;
        ECGLowerZoom = -32;

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
        String sDeviceType = "6-bit";
        if (connectedDevice.toLowerCase().contains("hdsteth")) {
            sDeviceType = "12-bit";
        }

        hdSteth.fnConnected(sDeviceType);
    }

    @Override
    public void onUiConnecting() {

    }

    @Override
    public void onUiDisconnected(int status) {
        hdSteth.fnDisconnected();
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

    public void fnConnectDevice(Context context, BluetoothDevice bluetoothDevice) {
        if (chkPermission(context)) {
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

    public void fnDisconnectDevice(Context context) {
        if (chkPermission(context)) {
            mSerialManager.disconnect();
            hdSteth.fnDisconnected();

            new AlertDialog.Builder(context)
                    .setTitle("Device Disconnected!")
                    .setMessage("Either device is disconnected or device is out of range.")
                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

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
        try {

            int ecg_LSB = 0, ecg_MSB = 0;
            int ecg_resultant_value = 0;
            int ecg_byte2_msb_last_2bit = 0;
            boolean valid_ecg_Value = false;

//            if (!isFreeze) {
            byte[] messageBytes = dataReceived;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sTime = simpleDateFormat.format(new Date());
            for (byte message : messageBytes) {
                int dataType = 0, sign = 0, fiveBitValue = 0;
                int uValue = message & 0xFF;
                dataType = uValue & 0x03;

                message = (byte) (message >> 2);

                int decimal = message & 0x3F;  //0011 1111


                if (dataType == 0x00) {

                    if (connectedDevice.toLowerCase().contains("hdsteth")) {
                        ecg_LSB = decimal;
                    } else {
                        if (!ecgHashmap.containsKey(sTime)) {
                            ecgHashmap.put(sTime, new ArrayList<Integer>());
                        }

                        if (isRecording) {
                            ecgBuilder.append(decimal + "\n");
                        }

//                        if (is10MPA) {

                        ecgPoints1 = ecgPoints2;
                        ecgPoints2 = ecgPoints3;
                        ecgPoints3 = ecgPoints4;
                        ecgPoints4 = ecgPoints5;
                        ecgPoints5 = decimal;

                        if (ecgHashmap.get(sTime).size() > 5) {
                            decimal = Integer.parseInt(((ecgPoints1 + ecgPoints2 + ecgPoints3 + ecgPoints4 + ecgPoints5 +
                                    ecgHashmap.get(sTime).get(ecgHashmap.get(sTime).size() - 5) +
                                    ecgHashmap.get(sTime).get(ecgHashmap.get(sTime).size() - 4) +
                                    ecgHashmap.get(sTime).get(ecgHashmap.get(sTime).size() - 3) +
                                    ecgHashmap.get(sTime).get(ecgHashmap.get(sTime).size() - 2) +
                                    ecgHashmap.get(sTime).get(ecgHashmap.get(sTime).size() - 1) + decimal) / 11) + "");
                        }

//                        }
                        ecgHashmap.get(sTime).add(decimal);
                        if (isRecording) {
                            if (graphIndex < 3200 - 1) {
                                graphIndex++;
                                ecgSeries.data.add(decimal);
                            } else {
                                ecgSeries.data.remove(0);
                                ecgSeries.data.add(decimal);
                            }
                            try {
                                if (redrawer == null) {
                                    redrawer = new Redrawer(plot, 30, true);
                                }
                                if (redrawer != null) {
                                    redrawer.start();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        hdSteth.fnReceiveData("ecg", decimal);


                    }
                } else if (dataType == 0x01) {
                    boolean plotPoint = true;
                    Set<String> keys = hsHashmap.keySet();
                    List<String> listFromSet = new ArrayList<String>(keys);
                    Collections.sort(listFromSet);
                    if (!hsHashmap.containsKey(sTime)) {
                        hsHashmap.put(sTime, new ArrayList<Integer>());
                    }

                    hsHashmap.get(sTime).add(decimal);
                    if (isRecording) {
                        hsBuilder.append(decimal + "\n");
                        if (hsIndex < 3200 - 1) {
                            hsIndex++;
                            hsSeries.data.add(decimal);
                        } else {
                            hsSeries.data.remove(0);
                            hsSeries.data.add(decimal);

                        }
                        try {
                            if (hsredrawer == null) {
                                hsredrawer = new Redrawer(plot2, 30, true);
                            }
                            if (hsredrawer != null) {
                                hsredrawer.start();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    hdSteth.fnReceiveData("hs", decimal);
//                            }

                } else if (dataType == 0x02) {
                    if (!murHashmap.containsKey(sTime)) {
                        murHashmap.put(sTime, new ArrayList<Integer>());
                    }

                    murHashmap.get(sTime).add(decimal);

                    if (isRecording) {
                        murBuild.append(decimal + "\n");

                        if (murIndex < 3200 - 1) {
                            murIndex++;
                            murSeries.data.add(decimal);
                        } else {
                            murSeries.data.remove(0);
                            murSeries.data.add(decimal);

                        }
                        try {
                            if (hsredrawer == null) {
                                hsredrawer = new Redrawer(plot2, 30, true);
                            }
                            if (hsredrawer != null) {
                                hsredrawer.start();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    hdSteth.fnReceiveData("mur", decimal);
                } else if (dataType == 0x03) {
                    ecg_MSB = decimal;

                    valid_ecg_Value = true;
                }
//
                if (valid_ecg_Value && connectedDevice.toLowerCase().contains("hdsteth")) {
                    Set<String> keys = ecgHashmap.keySet();
                    List<String> listFromSet = new ArrayList<String>(keys);
                    Collections.sort(listFromSet);
                    boolean plotPoint = true;
                    ecg_byte2_msb_last_2bit = ecg_MSB & 0x03;
                    ecg_byte2_msb_last_2bit = ecg_byte2_msb_last_2bit << 6;
                    ecg_LSB = ecg_LSB | ecg_byte2_msb_last_2bit;
                    ecg_MSB = ecg_MSB >> 2;
                    ecg_resultant_value = (((ecg_MSB & 0xFF) << 8) | (ecg_LSB & 0xFF));
                    decimal = ecg_resultant_value;

                    if (!ecgHashmap.containsKey(sTime)) {
                        ecgHashmap.put(sTime, new ArrayList<Integer>());
                    }

                    if (isRecording) {
                        ecgBuilder.append(decimal + "\n");
                    }
//                    if (is10MPA) {

                    ecgPoints1 = ecgPoints2;
                    ecgPoints2 = ecgPoints3;
                    ecgPoints3 = ecgPoints4;
                    ecgPoints4 = ecgPoints5;
                    ecgPoints5 = decimal;

                    if (ecgHashmap.get(sTime).size() > 5) {
                        decimal = Integer.parseInt(((ecgPoints1 + ecgPoints2 + ecgPoints3 + ecgPoints4 + ecgPoints5 +
                                ecgHashmap.get(sTime).get(ecgHashmap.get(sTime).size() - 5) +
                                ecgHashmap.get(sTime).get(ecgHashmap.get(sTime).size() - 4) +
                                ecgHashmap.get(sTime).get(ecgHashmap.get(sTime).size() - 3) +
                                ecgHashmap.get(sTime).get(ecgHashmap.get(sTime).size() - 2) +
                                ecgHashmap.get(sTime).get(ecgHashmap.get(sTime).size() - 1) + decimal) / 11) + "");
                    }

//                    }
                    ecgHashmap.get(sTime).add(decimal);
                    valid_ecg_Value = false;
                    if (isRecording) {
                        if (graphIndex < 3200 - 1) {
                            graphIndex++;
                            ecgSeries.data.add(decimal);
                        } else {
                            ecgSeries.data.remove(0);
                            ecgSeries.data.add(decimal);
                        }
                        try {
                            if (redrawer == null) {
                                redrawer = new Redrawer(plot, 30, true);
                            }
                            if (redrawer != null) {
                                redrawer.start();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    hdSteth.fnReceiveData("ecg", decimal);
                }
            }

//            }
        } catch (Exception e) {
            hdSteth.fnReceiveData("error : " + e.getMessage(), -1);
        }

    }

    @Override
    public void onUiUploaded() {

    }


    //    public void fnRecordData(Context context, int iSeconds, String sDirectory, String sName, String sAge, String sPatientId, String sGender,
//                             String sHeight, String sHemoglobin, String sTemprature, String sWeight, String sBloodPressure, String sBloodGlucose,
//                             String sSPO2, String sAddress, String sCity, String sState, String sZipcode, String sPhone, String sEmail, Bitmap bmPatientImage) {
    public void fnRecordData(Context context, int iSeconds, String sDirectory, String sName, String sPatientId) {

        String[] paths = new String[5];
        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Orientation is Portrait. Need Landscape.", Toast.LENGTH_SHORT).show();
                    paths[0] = "ECG File not Saved";
                    paths[1] = "HS File not Saved";
                    paths[2] = "MUR File not Saved";
                    paths[3] = "Wave File not Saved";
                    paths[4] = "PDF Record File not Saved";
                    hdSteth.fnRecordData(paths);
                }
            });
        } else {
            if (chkPermission(context)) {
//            ProgressDialog progress = new ProgressDialog(context);
//            progress.setMessage("Recording...");
//            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//            progress.setIndeterminate(true);

                isRecording = true;
                graphIndex = 0;
                murIndex = 0;
                hsIndex = 0;
                ecgBuilder = new StringBuilder();
                hsBuilder = new StringBuilder();
                murBuild = new StringBuilder();
                screeshots = new ArrayList<Bitmap>();
//            ((Activity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                androidx.appcompat.app.AlertDialog graphPopUp = new androidx.appcompat.app.AlertDialog.Builder(context).create();
                LayoutInflater graphInflater = LayoutInflater.from(context);
                final View graphView = graphInflater.inflate(R.layout.activity_graph, null);

                plot = (XYPlot) graphView.findViewById(R.id.plot2);
                plot2 = (XYPlot) graphView.findViewById(R.id.plot1);
                LinearLayout llGraph = (LinearLayout) graphView.findViewById(R.id.llGraph);

                ecgSeries = new ECGModel(TIME_INTERVAL, 200);
                hsSeries = new ECGModel(TIME_INTERVAL, 200);
                murSeries = new ECGModel(TIME_INTERVAL, 200);

                murFormatter = new MyFadeFormatter(TIME_INTERVAL, context.getResources().getColor(R.color.colorMUR));
                murFormatter.setLegendIconEnabled(false);

                ecgFormatter = new MyFadeFormatter(TIME_INTERVAL, context.getResources().getColor(R.color.colorECG));
                ecgFormatter.setLegendIconEnabled(false);

                hsFormatter = new MyFadeFormatter(TIME_INTERVAL, context.getResources().getColor(R.color.colorHS));
                hsFormatter.setLegendIconEnabled(false);


                plot.addSeries(ecgSeries, ecgFormatter);
                plot2.addSeries(hsSeries, hsFormatter);
                plot2.addSeries(murSeries, murFormatter);

                XYGraphWidget widget = plot.getGraph();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

                    plot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

                }
                int color = Color.TRANSPARENT;
                plot.getBorderPaint().setColor(color);
                plot.getBackgroundPaint().setColor(color);
                widget.getBackgroundPaint().setColor(color);
                widget.getGridBackgroundPaint().setColor(color);


                widget = plot2.getGraph();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    plot2.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
                plot2.getBorderPaint().setColor(color);
                plot2.getBackgroundPaint().setColor(color);
                widget.getBackgroundPaint().setColor(color);
                widget.getGridBackgroundPaint().setColor(color);

                PCG_LOWER_BOUND = 0;
                PCG_UPPER_BOUND = 64;
                ECG_LOWER_BOUND = 0;
                ECG_UPPER_BOUND = 64;

                if (connectedDevice.toLowerCase().contains("hdsteth")) {
                    ECG_LOWER_BOUND = 0;
                    ECG_UPPER_BOUND = 4096;
                }

                plot2.setRangeBoundaries(PCG_LOWER_BOUND, PCG_UPPER_BOUND, BoundaryMode.FIXED);
                plot2.setDomainBoundaries(0, 3200, BoundaryMode.FIXED);

//
                plot.setRangeBoundaries(ECG_LOWER_BOUND, ECG_UPPER_BOUND, BoundaryMode.FIXED);
                plot.setDomainBoundaries(0, 3200, BoundaryMode.FIXED);


                plot.setLinesPerRangeLabel(1);
                plot2.setLinesPerRangeLabel(1);

                llGraph.setBackground(context.getResources().getDrawable(R.drawable.newgraph));

                plot.getGraph().setDomainGridLinePaint(null);
                plot.getGraph().setRangeGridLinePaint(null);

                plot2.getGraph().setDomainGridLinePaint(null);
                plot2.getGraph().setRangeGridLinePaint(null);


                ecgSeries.start(new WeakReference<>(plot.getRenderer(AdvancedLineAndPointRenderer.class)));
                hsSeries.start(new WeakReference<>(plot2.getRenderer(AdvancedLineAndPointRenderer.class)));
                murSeries.start(new WeakReference<>(plot2.getRenderer(AdvancedLineAndPointRenderer.class)));

                // set a redraw rate of 30hz and start immediately:
                redrawer = new Redrawer(plot, 30, true);
                hsredrawer = new Redrawer(plot2, 30, true);

                graphPopUp.setView(graphView);


                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        graphPopUp.show();
                    }
                });

                final int[] iCompletedSecs = {0};
                final Handler handler = new Handler();
                final int delay = 1000; // 1000 milliseconds == 1 second
                handler.postDelayed(new Runnable() {
                    public void run() {
                        iCompletedSecs[0]++;
                        if (iCompletedSecs[0] < iSeconds) {

                            if (iCompletedSecs[0] > 0 && iCompletedSecs[0] % 5 == 0
                                    && iCompletedSecs[0] < iSeconds) {
                                takeRecordingScreenshot(llGraph);
                            }
                            handler.postDelayed(this, delay);
                        } else {

                            takeRecordingScreenshot(llGraph);
                            ((Activity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    graphPopUp.dismiss();
                                }
                            });
                            isRecording = false;
                            androidx.appcompat.app.AlertDialog adExp = new androidx.appcompat.app.AlertDialog.Builder(context).create();
                            LayoutInflater inflater = LayoutInflater.from(context);
                            final View viewRoot = inflater.inflate(R.layout.select_position, null);

                            final String[] sPosition = {""};
                            final String[] sPosture = {""};

                            TextView tvA = (TextView) viewRoot.findViewById(R.id.tvA);
                            TextView tvP = (TextView) viewRoot.findViewById(R.id.tvP);
                            TextView tvT = (TextView) viewRoot.findViewById(R.id.tvT);
                            TextView tvV1 = (TextView) viewRoot.findViewById(R.id.tvV1);
                            TextView tvV2 = (TextView) viewRoot.findViewById(R.id.tvV2);
                            TextView tvM = (TextView) viewRoot.findViewById(R.id.tvM);
                            TextView tvV6 = (TextView) viewRoot.findViewById(R.id.tvV6);
                            TextView tvV3 = (TextView) viewRoot.findViewById(R.id.tvV3);
                            TextView tvV5 = (TextView) viewRoot.findViewById(R.id.tvV5);
                            TextView tvV4 = (TextView) viewRoot.findViewById(R.id.tvV4);
                            RadioButton rbSitting = (RadioButton) viewRoot.findViewById(R.id.rbSitting);
                            RadioButton rbStanding = (RadioButton) viewRoot.findViewById(R.id.rbStanding);
                            RadioButton rbLeftLateral = (RadioButton) viewRoot.findViewById(R.id.rbLeftLateral);
                            RadioButton rbRightLateral = (RadioButton) viewRoot.findViewById(R.id.rbRightLateral);
                            RadioButton rbSupine = (RadioButton) viewRoot.findViewById(R.id.rbSupine);
                            CheckBox cbDontShow = (CheckBox) viewRoot.findViewById(R.id.cbDontShow);


                            rbSupine.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    rbSitting.setChecked(false);
                                    rbStanding.setChecked(false);
                                    rbLeftLateral.setChecked(false);
                                    rbRightLateral.setChecked(false);
                                    rbSupine.setChecked(false);
                                    rbSupine.setChecked(true);
                                    sPosture[0] = "Supine";
                                }
                            });

                            rbRightLateral.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    rbSitting.setChecked(false);
                                    rbStanding.setChecked(false);
                                    rbLeftLateral.setChecked(false);
                                    rbRightLateral.setChecked(false);
                                    rbSupine.setChecked(false);
                                    rbRightLateral.setChecked(true);
                                    sPosture[0] = "Right Lateral";
                                }
                            });

                            rbLeftLateral.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    rbSitting.setChecked(false);
                                    rbStanding.setChecked(false);
                                    rbLeftLateral.setChecked(false);
                                    rbRightLateral.setChecked(false);
                                    rbSupine.setChecked(false);
                                    rbLeftLateral.setChecked(true);
                                    sPosture[0] = "Left Lateral";
                                }
                            });

                            rbStanding.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    rbSitting.setChecked(false);
                                    rbStanding.setChecked(false);
                                    rbLeftLateral.setChecked(false);
                                    rbRightLateral.setChecked(false);
                                    rbSupine.setChecked(false);
                                    rbStanding.setChecked(true);
                                    sPosture[0] = "Standing";
                                }
                            });


                            rbSitting.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    rbSitting.setChecked(false);
                                    rbStanding.setChecked(false);
                                    rbLeftLateral.setChecked(false);
                                    rbRightLateral.setChecked(false);
                                    rbSupine.setChecked(false);
                                    rbSitting.setChecked(true);
                                    sPosture[0] = "Sitting";
                                }
                            });


                            tvV4.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tvA.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvP.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvT.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV1.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV2.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvM.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV6.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV3.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV5.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV4.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV4.setBackground(context.getDrawable(R.drawable.round_color_black));
                                    sPosition[0] = "V4";
                                }
                            });

                            tvV5.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tvA.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvP.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvT.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV1.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV2.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvM.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV6.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV3.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV5.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV4.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV5.setBackground(context.getDrawable(R.drawable.round_color_black));
                                    sPosition[0] = "V5";
                                }
                            });


                            tvV3.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tvA.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvP.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvT.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV1.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV2.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvM.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV6.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV3.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV5.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV4.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV3.setBackground(context.getDrawable(R.drawable.round_color_black));
                                    sPosition[0] = "V3";
                                }
                            });

                            tvV6.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tvA.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvP.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvT.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV1.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV2.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvM.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV6.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV3.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV5.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV4.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV6.setBackground(context.getDrawable(R.drawable.round_color_black));
                                    sPosition[0] = "V6";
                                }
                            });

                            tvM.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tvA.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvP.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvT.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV1.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV2.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvM.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV6.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV3.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV5.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV4.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvM.setBackground(context.getDrawable(R.drawable.round_color_black));
                                    sPosition[0] = "Mitral";
                                }
                            });

                            tvV2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tvA.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvP.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvT.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV1.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV2.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvM.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV6.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV3.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV5.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV4.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV2.setBackground(context.getDrawable(R.drawable.round_color_black));
                                    sPosition[0] = "V2";
                                }
                            });

                            tvV1.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tvA.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvP.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvT.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV1.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV2.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvM.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV6.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV3.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV5.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV4.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV1.setBackground(context.getDrawable(R.drawable.round_color_black));
                                    sPosition[0] = "V1";
                                }
                            });

                            tvT.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tvA.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvP.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvT.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV1.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV2.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvM.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV6.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV3.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV5.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV4.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvT.setBackground(context.getDrawable(R.drawable.round_color_black));
                                    sPosition[0] = "Tricuspid";
                                }
                            });

                            tvP.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tvA.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvP.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvT.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV1.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV2.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvM.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV6.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV3.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV5.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV4.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvP.setBackground(context.getDrawable(R.drawable.round_color_black));
                                    sPosition[0] = "Pulmonic";
                                }
                            });


                            tvA.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tvA.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvP.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvT.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV1.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV2.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvM.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV6.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV3.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV5.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvV4.setBackground(context.getDrawable(R.drawable.round_black_border));
                                    tvA.setBackground(context.getDrawable(R.drawable.round_color_black));
                                    sPosition[0] = "Aortic";
                                }
                            });

                            adExp.setButton(DialogInterface.BUTTON_POSITIVE, "Save", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO Auto-generated method stub

                                    isRecording = false;
                                    String[] paths = new String[5];
                                    String ecgpath = sDirectory + "/ECG.txt";
                                    File file = new File(ecgpath);
                                    if (!file.exists()) {
                                        try {
                                            file.createNewFile();
                                        } catch (IOException ioe) {
                                            paths[0] = "ECG : " + ioe.getMessage();
                                        }
                                    }
                                    try {
                                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                                        OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
                                        String data = ecgBuilder.toString();
                                        writer.write(data);
                                        writer.close();
                                        fileOutputStream.close();
                                        paths[0] = ecgpath;
                                    } catch (FileNotFoundException e) {
                                        paths[0] = "ECG : " + e.getMessage();
                                    } catch (IOException e) {
                                        paths[0] = "ECG : " + e.getMessage();
                                    }

                                    String hspath = sDirectory + "/HS.txt";
                                    file = new File(hspath);
                                    if (!file.exists()) {
                                        try {
                                            file.createNewFile();
                                        } catch (IOException ioe) {
                                            paths[1] = "HS : " + ioe.getMessage();
                                        }
                                    }
                                    try {
                                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                                        OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
                                        String data = hsBuilder.toString();
                                        writer.write(data);
                                        writer.close();
                                        fileOutputStream.close();
                                        paths[1] = hspath;
                                    } catch (FileNotFoundException e) {
                                        paths[1] = "HS : " + e.getMessage();
                                    } catch (IOException e) {
                                        paths[1] = "HS : " + e.getMessage();
                                    }

                                    String murpath = sDirectory + "/MUR.txt";
                                    file = new File(murpath);
                                    if (!file.exists()) {
                                        try {
                                            file.createNewFile();
                                        } catch (IOException ioe) {
                                            paths[2] = "MUR : " + ioe.getMessage();
                                        }
                                    }
                                    try {
                                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                                        OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
                                        String data = murBuild.toString();
                                        writer.write(data);
                                        writer.close();
                                        fileOutputStream.close();
                                        paths[2] = murpath;
                                    } catch (FileNotFoundException e) {
                                        paths[2] = "MUR : " + e.getMessage();
                                    } catch (IOException e) {
                                        paths[2] = "MUR : " + e.getMessage();
                                    }

                                    String wavfile = sDirectory + "/audio.wav";

                                    paths[3] = rawToWave(hspath, murpath, wavfile, iSeconds);


//                                String sName, String sAge, String sPatientId, String sGender,
//                                        String sHeight, String sHemoglobin, String sTemprature, String sWeight, String sBloodPressure, String sBloodGlucose,
//                                        String sSPO2, String sAddress, String sCity, String sState, String sZipcode, String sPhone, String sEmail, Bitmap bmPatientImage,
//                                        String pdfFilePath, Context context

                                    try {
                                        String pdfFile = sDirectory + "/record.pdf";
//                                        createSmallPdf(sName, sAge, sPatientId, sGender, sHeight, sHemoglobin, sTemprature, sWeight, sBloodPressure, sBloodGlucose,
//                                                sSPO2, sAddress, sCity, sState, sZipcode, sPhone, sEmail, bmPatientImage, pdfFile, context, sPosition[0], sPosture[0]);
                                        createSmallPdf(sName, sPatientId, pdfFile, context, sPosition[0], sPosture[0]);
                                        paths[4] = pdfFile;
                                    } catch (IOException e) {
                                        paths[4] = "PDF Record File not Saved";
                                    } catch (DocumentException e) {
                                        paths[4] = "PDF Record File not Saved";
                                    }
//                                ((Activity) context).runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
////                                        progress.dismiss();
//                                    }
//                                });
                                    hdSteth.fnRecordData(paths);

                                    ((Activity) context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            adExp.dismiss();
                                        }
                                    });
                                }
                            });


                            adExp.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO Auto-generated method stub
                                    ((Activity) context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            adExp.dismiss();
                                        }
                                    });

                                }
                            });
                            adExp.setView(viewRoot);
                            if (!((Activity) context).isFinishing()) {
//                            ((Activity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                ((Activity) context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adExp.show();
                                    }
                                });
                            }
                        }
                    }
                }, delay);


//            final Handler handler = new Handler(Looper.getMainLooper());
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    isRecording = false;
//                    String[] paths = new String[4];
//                    String ecgpath = sDirectory + "/ECG.txt";
//                    File file = new File(ecgpath);
//                    if (!file.exists()) {
//                        try {
//                            file.createNewFile();
//                        } catch (IOException ioe) {
//                            paths[0] = "ECG : " + ioe.getMessage();
//                        }
//                    }
//                    try {
//                        FileOutputStream fileOutputStream = new FileOutputStream(file);
//                        OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
//                        String data = ecgBuilder.toString();
//                        writer.write(data);
//                        writer.close();
//                        fileOutputStream.close();
//                        paths[0] = ecgpath;
//                    } catch (FileNotFoundException e) {
//                        paths[0] = "ECG : " + e.getMessage();
//                    } catch (IOException e) {
//                        paths[0] = "ECG : " + e.getMessage();
//                    }
//
//                    String hspath = sDirectory + "/HS.txt";
//                    file = new File(hspath);
//                    if (!file.exists()) {
//                        try {
//                            file.createNewFile();
//                        } catch (IOException ioe) {
//                            paths[1] = "HS : " + ioe.getMessage();
//                        }
//                    }
//                    try {
//                        FileOutputStream fileOutputStream = new FileOutputStream(file);
//                        OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
//                        String data = hsBuilder.toString();
//                        writer.write(data);
//                        writer.close();
//                        fileOutputStream.close();
//                        paths[1] = hspath;
//                    } catch (FileNotFoundException e) {
//                        paths[1] = "HS : " + e.getMessage();
//                    } catch (IOException e) {
//                        paths[1] = "HS : " + e.getMessage();
//                    }
//
//                    String murpath = sDirectory + "/MUR.txt";
//                    file = new File(murpath);
//                    if (!file.exists()) {
//                        try {
//                            file.createNewFile();
//                        } catch (IOException ioe) {
//                            paths[2] = "MUR : " + ioe.getMessage();
//                        }
//                    }
//                    try {
//                        FileOutputStream fileOutputStream = new FileOutputStream(file);
//                        OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
//                        String data = murBuild.toString();
//                        writer.write(data);
//                        writer.close();
//                        fileOutputStream.close();
//                        paths[2] = murpath;
//                    } catch (FileNotFoundException e) {
//                        paths[2] = "MUR : " + e.getMessage();
//                    } catch (IOException e) {
//                        paths[2] = "MUR : " + e.getMessage();
//                    }
//
//                    String wavfile = sDirectory + "/audio.wav";
//
//                    paths[3] = rawToWave(hspath, murpath, wavfile, iSeconds);
//
//                    ((Activity) context).runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            progress.dismiss();
//                        }
//                    });
//                    hdSteth.fnRecordData(paths);
//                }
//            }, iSeconds * 1000);

            } else {
                paths[0] = "ECG File not Saved";
                paths[1] = "HS File not Saved";
                paths[2] = "MUR File not Saved";
                paths[3] = "Wave File not Saved";
                paths[4] = "PDF Record File not Saved";
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Every Permission is Required.", Toast.LENGTH_SHORT).show();
                    }
                });
                hdSteth.fnRecordData(paths);
            }
        }
    }

    private String rawToWave(String hs_file, String mur_file, String hs_mur_wavefile,
                             int iSeconds) {

        File waveFile = new File(hs_mur_wavefile);

        DataInputStream input = null;
        byte[] rawData = null;

        int index = 0;
        int maxLength = 0;
        String fileName_hs = hs_file;   //"DS_BELL_HS_Tricuspid.txt";  //HS_data_ble.txt //HS.txt
        String fileName_mur = mur_file; // "DS_MUR_Tricuspid.txt";
        String rawHS = "";
        String rawMUR = "";
        File file = new File(hs_file);

        ArrayList<String> hsList = new ArrayList<String>();
        ArrayList<String> murList = new ArrayList<String>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int i = 0, j = 0;
            while ((line = br.readLine()) != null) {
//                    rawHS = rawHS + (line);
                hsList.add(line);
            }
            br.close();
        } catch (IOException e) {
            return e.getMessage();
        }
        file = new File(mur_file);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int i = 0, j = 0;
            while ((line = br.readLine()) != null) {
//                    rawMUR = rawMUR + (line);
                murList.add(line);
            }
            br.close();
        } catch (IOException e) {
            return e.getMessage();
        }
        String[] hsArray_String = new String[hsList.size()];
        for (int j = 0; j < hsList.size(); j++) {
            hsArray_String[j] = hsList.get(j);
        }
        byte[] hsArray_sbyte = new byte[hsArray_String.length];
        String[] murArray_String = new String[murList.size()];
        for (int j = 0; j < murList.size(); j++) {
            murArray_String[j] = murList.get(j);
        }
        byte[] murArray_sbyte = new byte[murArray_String.length];
        byte[] total_hs_sbyte_16bit = new byte[hsArray_String.length];
        byte[] total_mur_sbyte_16bit = new byte[murArray_String.length];

        if (hsArray_String.length <= murArray_String.length) {
            maxLength = hsArray_String.length;
        } else {
            maxLength = murArray_String.length;
        }

        byte[] total_Array_sbyte_16bit = new byte[maxLength];
        for (int i = 0; i < hsArray_String.length; ++i) {
            if (hsArray_String[i] != null) {
                hsArray_sbyte[i] = Byte.parseByte(hsArray_String[i]);
                total_hs_sbyte_16bit[index++] = (byte) hsArray_sbyte[i];
            }
            ;
        }

        index = 0;
        for (int i = 0; i < murArray_String.length; ++i) {
            if (murArray_String[i] != null) {
                murArray_sbyte[i] = Byte.parseByte(murArray_String[i]);
                total_mur_sbyte_16bit[index++] = (byte) murArray_sbyte[i];
            }
        }

        index = 0;

        for (int i = 0; i < maxLength; ++i) {
            byte hs = (byte) (total_hs_sbyte_16bit[index] - 32);
            byte mur = (byte) (total_mur_sbyte_16bit[index] - 32);
//            total_Array_sbyte_16bit[index] = (byte) (total_hs_sbyte_16bit[index] + total_mur_sbyte_16bit[index]);
            total_Array_sbyte_16bit[index] = (byte) (hs + mur);
            total_Array_sbyte_16bit[index] = (byte) ((total_Array_sbyte_16bit[index] * 4) - 127);
            index++;
        }

        rawData = new byte[total_Array_sbyte_16bit.length];
        rawData = total_Array_sbyte_16bit;


        DataOutputStream output = null;
        try {

            long NUM_OF_SAMPLES = rawData.length;
            short NUM_CHANNELS = 1;
            short BITS_PER_SAMPLE = 8;
            //            int SAMPLING_RATE = (int) (NUM_OF_SAMPLES / 10);
            int iRecordTime = iSeconds;
            int SAMPLING_RATE = (int) (NUM_OF_SAMPLES / iRecordTime);
            int CHUNK_SIZE = 16;

            int ByteRate = NUM_CHANNELS * BITS_PER_SAMPLE * SAMPLING_RATE / 8;
            short BlockAlign = (short) (NUM_CHANNELS * BITS_PER_SAMPLE / 8);
            int DataSize = (int) (NUM_CHANNELS * NUM_OF_SAMPLES * BITS_PER_SAMPLE / 8);
            int totalSize = 36 + DataSize;
            short audioFormat = 1; /*PCM format*/


            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            if ((rawData != null) && (rawData.length > 0)) {
                writeString(output, "RIFF"); // chunk id
                writeInt(output, 36 + rawData.length); // chunk size
                writeString(output, "WAVE"); // format
                writeString(output, "fmt "); // subchunk 1 id

                writeInt(output, CHUNK_SIZE); // subchunk 1 size
                writeShort(output, audioFormat); // audio format (1 = PCM)
                writeShort(output, NUM_CHANNELS); // number of channels
                writeInt(output, SAMPLING_RATE); // sample rate
                writeInt(output, ByteRate); // byte rate
                writeShort(output, BlockAlign); // block align
                writeShort(output, BITS_PER_SAMPLE); // bits per sample
                writeString(output, "data"); // subchunk 2 id
                writeInt(output, rawData.length); // subchunk 2 size

                output.write(rawData, 0, rawData.length);
            } else {

            }

            return hs_mur_wavefile;
        } catch (FileNotFoundException exception) {
            return exception.getMessage();
        } catch (IOException exception) {
            return exception.getMessage();
        } finally {
            if (output != null) {
                try {
                    output.flush();
                    output.close();
                } catch (IOException e) {
                    return e.getMessage();
                }
            }
            return "";
        }
    }

    private void writeInt(DataOutputStream output, int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(DataOutputStream output, short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(DataOutputStream output, String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
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


    //    private static void createTable1(Document document, String sName, String sAge, String sPatientId, String sGender,
//                                     String sHeight, String sHemoglobin, String sTemprature, String sWeight, String sBloodPressure, String sBloodGlucose,
//                                     String sSPO2, String sAddress, String sCity, String sState, String sZipcode, String sPhone, String sEmail, Bitmap bmPatientImage,
//                                     Context context, String sTime)
    private static void createTable1(Document document, String sName, String sPatientId,
                                     Context context, String sTime)
            throws DocumentException {
        try {
            PdfPTable table = new PdfPTable(1);
//            table.setWidths(new int[]{80, 80,90});

            // t.setBorderColor(BaseColor.GRAY);
            // t.setPadding(4);
            // t.setSpacing(4);

//        try {
//            if (bmPatientImage != null) {
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                bmPatientImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                Image image = Image.getInstance(stream.toByteArray());
//                PdfPCell c1 = new PdfPCell();
//                c1.setPadding(5);
//                c1.addElement(image);
//                table.addCell(c1);
//            } else {
//                Bitmap bmp = BitmapFactory.decodeResource(context.getResources(),
//                        R.drawable.selectold);
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                Image image = Image.getInstance(stream.toByteArray());
//                PdfPCell c1 = new PdfPCell();
//                c1.setPadding(5);
//                c1.addElement(image);
//                table.addCell(c1);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

            PdfPTable nestedTable = new PdfPTable(3);

            PdfPCell c1 = new PdfPCell(new Phrase("Patient name : " + sName));
            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
            c1.setBorder(0);
            nestedTable.addCell(c1);

//        c1 = new PdfPCell(new Phrase("Age : " + sAge + " Years"));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);


            c1 = new PdfPCell(new Phrase("Recorded On : " + sTime));
            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
            c1.setBorder(0);
            nestedTable.addCell(c1);

            c1 = new PdfPCell(new Phrase("Patient Id : " + sPatientId));
            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
            c1.setBorder(0);
            nestedTable.addCell(c1);

//        c1 = new PdfPCell(new Phrase("Gender : " + sGender));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Height : " + sHeight + " cms"));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//
//        c1 = new PdfPCell(new Phrase("Hemoglobin : " + sHemoglobin + " Hb"));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Temperature : " + sTemprature + " C"));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Weight : " + sWeight + " Kgs"));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Blood Pressure : " + sBloodPressure + " mmHg"));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Blood Glucose : " + sBloodGlucose + " mg/dl"));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("SPO2 : " + sSPO2 + " %"));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Address : " + sAddress));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setColspan(3);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("City : " + sCity));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("State : " + sState));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Zip Code : " + sZipcode));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//
//        c1 = new PdfPCell(new Phrase("Phone : " + sPhone));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Email : " + sEmail));
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setBorder(0);
//        nestedTable.addCell(c1);

//            c1 = new PdfPCell(new Phrase(""));
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//            c1.setBorder(0);
//            table.addCell(c1);
//
            c1 = new PdfPCell(nestedTable);
            c1.setPadding(5);
            table.addCell(c1);

            table.setWidthPercentage(100);
            document.add(table);
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    //    private void createSmallPdf(String sName, String sAge, String sPatientId, String sGender,
//                                String sHeight, String sHemoglobin, String sTemprature, String sWeight, String sBloodPressure, String sBloodGlucose,
//                                String sSPO2, String sAddress, String sCity, String sState, String sZipcode, String sPhone, String sEmail, Bitmap bmPatientImage,
//                                String pdfFilePath, Context context, String sPosition, String sPoture)
    private void createSmallPdf(String sName, String sPatientId,
                                String pdfFilePath, Context context, String sPosition, String sPoture)
            throws DocumentException, IOException {

        if (new File(pdfFilePath).exists()) {
            new File(pdfFilePath).delete();
        }
        Document document = new Document();

        Font subFont = new Font(Font.FontFamily.TIMES_ROMAN, 16,
                Font.BOLD);

//        DataBean patientProfile = GlobalModule.databaseHandler.getPatient(sPatientId);


        OutputStream outputStream = new FileOutputStream(new File(pdfFilePath));
        //PdfWriter.getInstance(document, new FileOutputStream(filename));
        PdfWriter.getInstance(document, outputStream);

        // step 3
        document.open();

        document.add(new Paragraph("AUSCULTATION REPORT", subFont));
        document.add(new Paragraph("\n\n\n"));
        Rectangle rect = new Rectangle(577, 825, 18, 15);
        rect.enableBorderSide(1);
        rect.enableBorderSide(2);
        rect.enableBorderSide(4);
        rect.enableBorderSide(8);
        rect.setBorder(Rectangle.BOX);
        rect.setBorderWidth(1);
        rect.setBorderColor(BaseColor.BLACK);
        document.add(rect);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String sTime = simpleDateFormat.format(new Date());

//        createTable1(document, sName, sAge, sPatientId, sGender,
//                sHeight, sHemoglobin, sTemprature, sWeight, sBloodPressure, sBloodGlucose,
//                sSPO2, sAddress, sCity, sState, sZipcode, sPhone, sEmail, bmPatientImage, context, sTime);
        createTable1(document, sName, sPatientId, context, sTime);

//        createTable2(document, patientProfile);
        document.newPage();
        document.add(rect);

        for (int i = 0; i < 1; i++) {

            if (i > 0) {
                document.newPage();
                document.add(rect);
            }
            document.add(new Paragraph("Recording Of " + sTime));
            document.add(new Paragraph("\n\n"));

            for (int j = 0; j < screeshots.size(); j = j + 2) {
                if (j > 0) {
                    document.newPage();
                    document.add(rect);
                }
                if (screeshots.get(j) != null) {

                    PdfPTable Nextedtable = new PdfPTable(1);
                    Nextedtable.setWidthPercentage(100);

                    PdfPTable table = new PdfPTable(1);

                    PdfPCell c1 = new PdfPCell(new Paragraph("Page " + (j + 1) + "/" + screeshots.size()));
                    c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                    c1.setBorder(0);
                    table.addCell(c1);

                    c1 = new PdfPCell(new Paragraph("  Position : " + sPosition + "       Posture : " + sPoture));
                    c1.setHorizontalAlignment(Element.ALIGN_LEFT);
                    c1.setBorder(0);
                    table.addCell(c1);

                    c1 = new PdfPCell(new Paragraph("PCG"));
                    c1.setHorizontalAlignment(Element.ALIGN_LEFT);
                    c1.setBorder(0);
                    table.addCell(c1);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    screeshots.get(j).compress(Bitmap.CompressFormat.PNG, 100, stream);
                    Image image = Image.getInstance(stream.toByteArray());
                    image.scaleAbsolute(500, 150);
                    c1.setPadding(5);
                    c1.addElement(image);
                    c1.setBorder(0);
                    table.addCell(c1);

                    c1 = new PdfPCell(new Paragraph("ECG"));
                    c1.setHorizontalAlignment(Element.ALIGN_LEFT);
                    c1.setBorder(0);
                    table.addCell(c1);

                    table.setHorizontalAlignment(Element.ALIGN_LEFT);
                    table.setWidthPercentage(100);

                    c1 = new PdfPCell(table);
                    c1.setHorizontalAlignment(Element.ALIGN_LEFT);
                    c1.setPadding(5);
                    Nextedtable.addCell(c1);

                    document.add(Nextedtable);
                }

                if ((j + 1) <= screeshots.size() - 1) {


                    if (screeshots.get(j + 1) != null) {

                        PdfPTable Nextedtable = new PdfPTable(1);
                        Nextedtable.setWidthPercentage(100);

                        PdfPTable table = new PdfPTable(1);

                        PdfPCell c1 = new PdfPCell(new Paragraph("Page " + (j + 2) + "/" + screeshots.size()));
                        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c1.setBorder(0);
                        table.addCell(c1);

                        c1 = new PdfPCell(new Paragraph("  Position : " + sPosition + "       Posture : " + sPoture));
                        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
                        c1.setBorder(0);
                        table.addCell(c1);

                        c1 = new PdfPCell(new Paragraph("PCG"));
                        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
                        c1.setBorder(0);
                        table.addCell(c1);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        screeshots.get(j + 1).compress(Bitmap.CompressFormat.PNG, 100, stream);
                        Image image = Image.getInstance(stream.toByteArray());
                        image.scaleAbsolute(500, 150);
                        c1.setPadding(5);
                        c1.addElement(image);
                        c1.setBorder(0);
                        table.addCell(c1);

                        c1 = new PdfPCell(new Paragraph("ECG"));
                        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
                        c1.setBorder(0);
                        table.addCell(c1);

                        table.setHorizontalAlignment(Element.ALIGN_LEFT);
                        table.setWidthPercentage(100);

                        c1 = new PdfPCell(table);
                        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
                        c1.setPadding(5);
                        Nextedtable.addCell(c1);

                        document.add(Nextedtable);
                    }
                }

            }
        }
        // step 5
        document.close();
        outputStream.close();
    }

    private void takeRecordingScreenshot(LinearLayout llGraph) {

        try {
            llGraph.setDrawingCacheEnabled(true);
            llGraph.layout(0, 0, llGraph.getMeasuredWidth(), llGraph.getMeasuredHeight());
            llGraph.buildDrawingCache(true);
            Bitmap bitmap = Bitmap.createBitmap(llGraph.getDrawingCache());
            llGraph.setDrawingCacheEnabled(false);

//            DataBean bean = new DataBean();
//            bean.setBmScreenGraph(bitmap);

            screeshots.add(bitmap);

//            GlobalModule.savedScreens.add(bean);

        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
        }
    }

    public static class MyFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {

        private int trailSize;
        int color;

        MyFadeFormatter(int trailSize, int color) {
            this.trailSize = trailSize;
            this.color = color;
        }

        @Override
        public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
            // offset from the latest index:
            int offset;
            if (thisIndex > latestIndex) {
                offset = latestIndex + (seriesSize - thisIndex);
            } else {
                offset = latestIndex - thisIndex;
            }


            float scale = 255f / trailSize;
            int alpha = (int) (255 - (offset * scale));
            getLinePaint().setAlpha(alpha > 0 ? alpha : 0);
            getLinePaint().setColor(color);
            return getLinePaint();
        }

    }

}
