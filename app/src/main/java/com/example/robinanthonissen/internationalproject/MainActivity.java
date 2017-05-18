package com.example.robinanthonissen.internationalproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> deviceAddresses;
    ArrayList<Sensor> sensors;
    ListView listSensors;
    Intent addSensor;
    Intent detailSensor;
    static final int ADD_DEVICE = 123;
    SimpleArrayAdapter arrAdapter;

    private BluetoothAdapter mBluetoothAdapter = null;
    ArrayList<BluetoothDevice> connectedDevices;

    String testSenName="";
    String testSenAddress="";

    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
   /* private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };*/

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.





    /*private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.mBluetoothGatt);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
               displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };*/

    private void clearUI() {
        listSensors.setAdapter(arrAdapter);
    }






    public class  SimpleArrayAdapter extends ArrayAdapter<Sensor> {
        public SimpleArrayAdapter(Context context, ArrayList<Sensor> values) {
            super(context, -1 , values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View v = null;
            if(convertView != null){
                v = convertView;
            }
            else {
                v = getLayoutInflater().inflate(R.layout.list_view_sensor_layout, parent, false);
            }

            Sensor sensor = getItem(position);
            TextView tv = (TextView) v.findViewById(R.id.nameSensor);
            tv.setText(sensor.getName());
            TextView tvHeart = (TextView) v.findViewById(R.id.heartBeat);
            tvHeart.setText(String.valueOf(sensor.heartB));
            return v;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Toestemming om locatie voor BLE te mogen gebruiken
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);


        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        deviceAddresses = new ArrayList<>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectedDevices = new ArrayList<>();

        addSensor = new Intent(MainActivity.this, AddSensorActivity.class);
        detailSensor = new Intent(MainActivity.this, DetailActivity.class);
        sensors = new ArrayList<>();
        sensors.add(new Sensor("Robin"));
        sensors.add(new Sensor("JP"));
        listSensors = (ListView) findViewById(R.id.sensorList);
        arrAdapter = new SimpleArrayAdapter(this, sensors);
        listSensors.setAdapter(arrAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(addSensor, ADD_DEVICE);
            }
        });
        //Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        //bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ADD_DEVICE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                if(mBluetoothAdapter != null) {
                    if (mBluetoothAdapter.isEnabled()) {
                        //pairedDevices = mBluetoothAdapter.getBondedDevices();
                    }
                }
                //String newSenAddress = data.getStringExtra("devAddress");
                //BluetoothDevice dev = data.getParcelableExtra("BTdev");
                //connectedDevices.add(dev);

                String newSenName = data.getStringExtra("name");
                String newSenAddress = data.getStringExtra("address");
                BluetoothDevice btDev = data.getParcelableExtra("btDev");
                deviceAddresses.add(newSenAddress);

                testSenAddress = newSenAddress;
                testSenName = newSenName;

                Toast.makeText(getApplicationContext(), newSenName + newSenAddress, Toast.LENGTH_SHORT).show();


                /*final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final String action = intent.getAction();
                        if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                            mConnected = true;
                            //updateConnectionState(R.string.connected);
                            invalidateOptionsMenu();
                        } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                            mConnected = false;
                            //updateConnectionState(R.string.disconnected);
                            invalidateOptionsMenu();
                            clearUI();
                        } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                            // Show all the supported services and characteristics on the user interface.
                            displayGattServices(mBluetoothLeService.getSupportedGattServices());
                        } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                            displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                        }
                    }
                };*/





                BluetoothDevice newSen = mBluetoothAdapter.getRemoteDevice(newSenAddress);
                Sensor newSensor = new Sensor(newSen, this);
                sensors.add(newSensor);
                Sensor sen = new Sensor(btDev, this);
                //sensors.add(sen);

                /*if (mGattCharacteristics != null) {
                    //final BluetoothGattCharacteristic characteristic =
                            //mGattCharacteristics;//.get(groupPosition).get(childPosition);
                    final int charaProp = characteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(
                                    mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        mBluetoothLeService.readCharacteristic(characteristic);
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = characteristic;
                        mBluetoothLeService.setCharacteristicNotification(
                                characteristic, true);
                    }
                }*/




                arrAdapter.notifyDataSetChanged();

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }






    @Override
    protected void onResume() {
        super.onResume();
        //registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null && !testSenAddress.isEmpty()) {
            final boolean result = mBluetoothLeService.connect(testSenAddress);
            Log.d("", "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            //mDataField.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(BluetoothGatt gatt) {
        for(BluetoothGattService service : gatt.getServices())
        {
            String TAG = "TESTTESTTEST";
            Log.i(TAG, "Discovered Service: " + service.getUuid().toString() + " with " + "characteristics:");
            for(BluetoothGattCharacteristic characteristic : service.getCharacteristics())
            {
                // Set notifiable
                if(!gatt.setCharacteristicNotification(characteristic, true))
                {
                    Log.e(TAG, "Failed to set notification for: " + characteristic.toString());
                }

                // Enable notification descriptor
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                if(descriptor != null)
                {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }

                // Read characteristic
                if(!gatt.readCharacteristic(characteristic))
                {
                    Log.e(TAG, "Failed to read characteristic: " + characteristic.toString());
                }
            }
        }
        /*
        for (BluetoothGattService gattService : gattServices) {
            for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                if (characteristic.getUuid().equals(UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT))) {
                    mBluetoothLeService.readCharacteristic(characteristic);
                }
            }
        }
        */
/*
        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        //mGattServicesList.setAdapter(gattServiceAdapter);*/
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
