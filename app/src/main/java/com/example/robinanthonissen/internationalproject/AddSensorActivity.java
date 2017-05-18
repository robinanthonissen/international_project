package com.example.robinanthonissen.internationalproject;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AddSensorActivity extends AppCompatActivity {

    //private static final int REQUEST_ENABLE_BT = 10;
    ListView listBTdevices;
    ArrayList<String> deviceNames;
    ArrayList<String> deviceAddresses;
    ArrayList<BluetoothDevice> bluetoothDev;
    ArrayList<String> combo;
    ArrayAdapter deviceArrayAdapter;

    Intent mainIntent;

    private BluetoothAdapter mBluetoothAdapter = null;
    private Set<BluetoothDevice> pairedDevices;
    BluetoothManager bluetoothManager = null;

    ProgressDialog mProgressDlg;


    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_add_sensor);

        mHandler = new Handler();



        listBTdevices = (ListView) findViewById(R.id.listBTDevices);
        deviceNames = new ArrayList<>();
        deviceAddresses = new ArrayList<>();
        combo = new ArrayList<>();
        bluetoothDev = new ArrayList<>();
        //listBTdevices.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceNames));
        mainIntent = new Intent(AddSensorActivity.this, MainActivity.class);


        /*ArrayAdapter deviceArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, deviceNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(deviceNames.get(position));
                text2.setText(deviceAddresses.get(position));
                return view;
            }
        };*/

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //mLeDeviceListAdapter = new LeDeviceListAdapter();
        //listBTdevices.setAdapter(mLeDeviceListAdapter);
        //deviceArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, deviceNames);

        listBTdevices.setAdapter(deviceArrayAdapter);

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            //TODO: terug naar main activity wanneer bluetooth niet beschikbaar is
            Toast.makeText(getApplicationContext(), "geen Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //deviceList();
        }
        if(mBluetoothAdapter.isEnabled()){
            //deviceList();
            mBluetoothAdapter = bluetoothManager.getAdapter();
            //mBluetoothAdapter.startDiscovery();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        //listBTdevices.setAdapter(mLeDeviceListAdapter);
        deviceArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, combo);
        listBTdevices.setAdapter(deviceArrayAdapter);
        scanLeDevice(true);
        listBTdevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                //final BluetoothDevice device = m deviceArrayAdapter.getItem(position);
                if (device == null) return;
                final Intent intent = new Intent(AddSensorActivity.this, MainActivity.class);
                intent.putExtra("name", device.getName());
                intent.putExtra("address", device.getAddress());
                intent.putExtra("btDev", device);
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = AddSensorActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                deviceNames.add(device.getName());
                deviceAddresses.add(device.getAddress());
                combo.add(device.getName() + " " + device.getAddress());
            }
            notifyDataSetChanged();
            deviceArrayAdapter.notifyDataSetChanged();
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                //view = mInflator.inflate(R.layout.list_view_sensor_layout, null);
                view = mInflator.inflate(android.R.layout.simple_list_item_2, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(android.R.id.text1);
                viewHolder.deviceName = (TextView) view.findViewById(android.R.id.text2);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            //else
                //viewHolder.deviceName.setText("unknown_device");
            //viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

}
