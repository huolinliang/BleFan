/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String GATT_WRITE_SUCCESS = "gatt.write.success";
    int send_block_num;

    AnimationDrawable pagerAnimateDrawable;
    ImageView pagerAnimateView;
    private View view0,view1,view2,view3,view4,view5,view6,view7;//各个页卡
    ArrayList<View> mViewList = new ArrayList<View>();
    LayoutInflater mLayoutInflater;
    MyPagerAdapter mPagerAdapter;
    LinearLayout mNumLayout;
    Button mPreSelectedBt;
    private ImageButton mRight_button;
    private ImageButton mLeft_button;
    private boolean toSendAnimOnly = false;
    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private final int MAX_LIMIT_ONE_TIME = 20;
    private final int BYTE_OF_ONE_WORD = 24;
    byte words[] = null;
    String word_SBC = null;
    String word_uniq = null;
    String word = null;
    private BluetoothGatt mBluetoothGatt;
    private EditText mInputEditText;
    private Button mButton1;
    private MyProgressDialog dialog;
    byte[] character_byte = new byte[BYTE_OF_ONE_WORD*140];
    byte[] character_type = new byte[140];
    byte[] animate_type = new byte[] {0x01,0x02,0x03,0x04,0x05,0x06};
    int progressbar_status = 0;
    ProgressBar bar;
    Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if (msg.what == 0x111) //约定0x111为更新进度条的标识
            {
                bar.setProgress(progressbar_status);
            }
        }
    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                Log.d(TAG, "receive connected broadcast");
                invalidateOptionsMenu();
                mButton1.setEnabled(true);
                mButton1.setBackgroundResource(R.drawable.button_selector);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
                bar.setVisibility(View.INVISIBLE);
                progressbar_status = 0;
                mHandler.sendEmptyMessage(0x111);
                dialog.dismiss();
                if(pagerAnimateDrawable != null) {
                    pagerAnimateDrawable.stop();
                }
                mButton1.setEnabled(false);
                mButton1.setBackgroundResource(R.drawable.send_button_disable);
            }else if(BluetoothLeService.ACTION_GATT_CONNECTING.equals(action)) {
                Log.d(TAG, "receive connecting broadcast");
                showMyDialog(getWindow().getDecorView());
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                dialog.dismiss();
                //auto start animation of the fan
                pagerAnimateView = (ImageView)view1.findViewById(R.id.pager_imageview);
                pagerAnimateDrawable = (AnimationDrawable) pagerAnimateView.getBackground();
                pagerAnimateDrawable.start();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {

                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
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
                            Log.d(TAG, "onchildclick 1 groupPosition=" + groupPosition + "childPosition=" + childPosition);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLayoutInflater = getLayoutInflater();
        //可以按照需求进行动态创建Layout,这里暂用静态的xml layout
        view1 = mLayoutInflater.inflate(R.layout.viewpager_1, null);
        view2 = mLayoutInflater.inflate(R.layout.viewpager_2, null);
        view3 = mLayoutInflater.inflate(R.layout.viewpager_3, null);
        view4 = mLayoutInflater.inflate(R.layout.viewpager_4, null);
        view5 = mLayoutInflater.inflate(R.layout.viewpager_5, null);
        view6 = mLayoutInflater.inflate(R.layout.viewpager_6, null);
        view7 = mLayoutInflater.inflate(R.layout.viewpager_turnback, null);
        mViewList.add(view1);
        mViewList.add(view2);
        mViewList.add(view3);
        mViewList.add(view4);
        mViewList.add(view5);
        mViewList.add(view6);
        mViewList.add(view7);
        setContentView(R.layout.gatt_services_characteristics);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new MyPagerAdapter();
        viewPager.setAdapter(mPagerAdapter);
        mNumLayout = (LinearLayout) findViewById(R.id.ll_pager_num);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dot_normal);
        for (int i = 0; i < mViewList.size() - 1; i++) {
            Button bt = new Button(this);
            bt.setLayoutParams(new ViewGroup.LayoutParams(bitmap.getWidth(), bitmap.getHeight()));
            bt.setBackgroundResource(R.drawable.dot_normal);
            mNumLayout.addView(bt);
        }

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                //++for 循环滚动
                if(position == mViewList.size() - 1){
                    viewPager.setCurrentItem(0, false);
                    position = 0;
                }
                //++
                if(mPreSelectedBt != null){
                    mPreSelectedBt.setBackgroundResource(R.drawable.dot_normal);
                }
                Button currentBt = (Button)mNumLayout.getChildAt(position);
                currentBt.setBackgroundResource(R.drawable.dot_selected);
                mPreSelectedBt = currentBt;
                pagerAnimateView = (ImageView)mViewList.get(position).findViewById(R.id.pager_imageview);
                pagerAnimateDrawable = (AnimationDrawable) pagerAnimateView.getBackground();
                pagerAnimateDrawable.start();
                try {
                    BluetoothGattCharacteristic sendCharacteristic =mGattCharacteristics.get(2).get(0); //fff3
                    if(sendCharacteristic != null){
                        sendCharacteristic.setValue(new byte[]{animate_type[position]});
                        mBluetoothLeService.writeCharacteristic(sendCharacteristic);
                        toSendAnimOnly = true;
                    }
                }catch (NullPointerException | IndexOutOfBoundsException e) {
                    //
                }

                Log.d(TAG, "current item:"+position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub

            }
        });

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        mInputEditText = (EditText)findViewById(R.id.input_edit);
        mButton1 = (Button)findViewById(R.id.btn1);
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        bar = (ProgressBar) findViewById(R.id.send_progressbar);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mButton1.setEnabled(false);
        mRight_button = (ImageButton)findViewById(R.id.right_btn);
        mRight_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(viewPager.getCurrentItem() == mViewList.size() - 2)
                    viewPager.setCurrentItem(0);
                else
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, false);
            }
        });
        mLeft_button = (ImageButton)findViewById(R.id.left_btn);
        mLeft_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(viewPager.getCurrentItem() == 0)
                    viewPager.setCurrentItem(mViewList.size() - 2);
                else
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, false);
            }
        });

        mButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                word = mInputEditText.getText().toString();

                if (word.equals(""))
                    word = getString(R.string.nothing_input);
                Log.d(TAG, "word=" + word + " word.length=" + word.length());
                GetDataFromHzk();
                send_block_num = 0;
                toSendAnimOnly = false;
                try {
                    BluetoothGattCharacteristic eraseFlashCharacteristic = mGattCharacteristics.get(2).get(2);//fff5
                    Log.d(TAG, "try to erase flash");
                    if (eraseFlashCharacteristic != null) {
                        eraseFlashCharacteristic.setValue(new byte[]{0x01});
                        mBluetoothLeService.writeCharacteristic(eraseFlashCharacteristic);
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.character_not_ready),
                                Toast.LENGTH_SHORT).show();
                    }
                    bar.setMax(word.length() * BYTE_OF_ONE_WORD / MAX_LIMIT_ONE_TIME);
                    bar.setVisibility(View.VISIBLE);
                } catch (IndexOutOfBoundsException e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.character_not_ready),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    void  sendSetting(int num){

            BluetoothGattCharacteristic sendCharacteristic =mGattCharacteristics.get(2).get(1); //fff4
            int num_max = word.length() * BYTE_OF_ONE_WORD / MAX_LIMIT_ONE_TIME;
            int character_type_send_max;
            byte[] tmp_character_byte = new byte[MAX_LIMIT_ONE_TIME];
            if(word.length() % MAX_LIMIT_ONE_TIME ==0)
                character_type_send_max = word.length() / MAX_LIMIT_ONE_TIME;
            else
                character_type_send_max = word.length() / MAX_LIMIT_ONE_TIME + 1;

            if(sendCharacteristic != null){
                Log.d(TAG, "in sendSetting num now = "+num);
                if(num < num_max) {
                    for(int i = 0; i < MAX_LIMIT_ONE_TIME; i++)
                        tmp_character_byte[i] = character_byte[num * MAX_LIMIT_ONE_TIME + i];
                    sendCharacteristic.setValue(tmp_character_byte);
                            mBluetoothLeService.writeCharacteristic(sendCharacteristic);
                            progressbar_status = num;
                            mHandler.sendEmptyMessage(0x111);
                }
                if(num == num_max) {
                    Log.d(TAG, "num = num_max = " + num_max);
                    int left_bytes_num = word.length()*24 - num_max*MAX_LIMIT_ONE_TIME;
                    if(left_bytes_num > 0) {
                        byte[] left_character_byte = new byte[left_bytes_num];
                        for (int i = 0; i < left_bytes_num; i++) {
                            left_character_byte[i] = character_byte[word.length() * BYTE_OF_ONE_WORD - left_bytes_num + i];
                        }
                        sendCharacteristic.setValue(left_character_byte);
                        mBluetoothLeService.writeCharacteristic(sendCharacteristic);
                    }else {
                        sendSetting(send_block_num++);
                    }

                }
                //发送字符是否为中文的数组序列，该数组的元素由0x01 和 0x00组成，0x1表示该字符为中文
                //规定最大输入140个字符，每次最多发送20个字节，因此该数组最多需要发送7次
                if(num >= num_max + 1 && num <= num_max + character_type_send_max) {
                    Log.d(TAG, "character_type_send_max=" + character_type_send_max);
                    byte[] tmp_character_type = new byte[MAX_LIMIT_ONE_TIME];
                    BluetoothGattCharacteristic isChineseCharacteristic = mGattCharacteristics.get(2).get(4);//fff7,字符类型是否为中文
                    if(isChineseCharacteristic != null) {
                        for(int i = 0; i < MAX_LIMIT_ONE_TIME; i++) {
                            tmp_character_type[i] = character_type[(num - num_max -1)*MAX_LIMIT_ONE_TIME + i];
                        }
                        isChineseCharacteristic.setValue(tmp_character_type);
                        mBluetoothLeService.writeCharacteristic(isChineseCharacteristic);
                    }else {
                        Toast.makeText(getApplicationContext(), getString(R.string.character_not_ready),
                                Toast.LENGTH_SHORT).show();
                    }
                    progressbar_status = num;
                    mHandler.sendEmptyMessage(0x111);

                }
                //发送更新标识到ble，使其自动刷新
                if(num == num_max + character_type_send_max + 1) {
                    Log.d(TAG, "num = max +1 = " + num);
                    BluetoothGattCharacteristic renewCharacteristic = mGattCharacteristics.get(2).get(3);//fff6
                    if(renewCharacteristic != null) {
                        renewCharacteristic.setValue(new byte[]{(byte)word.length()});//把字符长度发送给ble设备
                        mBluetoothLeService.writeCharacteristic(renewCharacteristic);
                        Toast.makeText(getApplicationContext(), getString(R.string.send_success),
                                Toast.LENGTH_SHORT).show();
                        bar.setVisibility(View.INVISIBLE);
                        progressbar_status = 0;
                        mHandler.sendEmptyMessage(0x111);
                        send_block_num++;
                    }else {
                        Toast.makeText(getApplicationContext(), getString(R.string.character_not_ready),
                                Toast.LENGTH_SHORT).show();
                    }

                }
            }else {
                Toast.makeText(getApplicationContext(), getString(R.string.character_not_ready),
                        Toast.LENGTH_SHORT).show();
            }
       // }
    }


    BroadcastReceiver mContinueSendReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "in broadcastreceive, num now is " + send_block_num);
            String action = intent.getAction();
            if (action.equals(GATT_WRITE_SUCCESS) && !toSendAnimOnly) {
                sendSetting(send_block_num++);
            }
        }
    };

    public byte isChinese(String str) {
        if (String.valueOf(str).matches("[\u4E00-\u9FA5]")) //区分是否中文符号
            return 0x01;
        else
            return 0x00;
    }
        /*
        * get characters from hzk12
        */
    public void GetDataFromHzk() {
        for (int charIndex = 0; charIndex < word.length(); charIndex++) {
            word_uniq = word.substring(charIndex, charIndex + 1);
            character_type[charIndex] = isChinese(word_uniq);
            word_SBC = ToSBC(word_uniq);

            try {
                words = word_SBC.getBytes("GB2312");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            try {
                AssetManager am = null;
                am = getAssets();
                InputStream is = am.open("hzk12");
                long beginIndex = (94 * ((words[0] & 0xff) - 0xa0 - 1) + ((words[1] & 0xff) - 0xa0 - 1)) * BYTE_OF_ONE_WORD;
                byte[] bytes = new byte[BYTE_OF_ONE_WORD];
                int byteread = 0;
                long realskip = 0;

                while ((byteread = is.read()) != -1) {
                    realskip = is.skip(beginIndex - 1);
                    is.read(character_byte, BYTE_OF_ONE_WORD*charIndex, BYTE_OF_ONE_WORD);//bytes
                    break;
                }
                int i, j, k, flag;
                boolean pointflag;
                char key[] = new char[8];
                key[0] = 0x80;
                key[1] = 0x40;
                key[2] = 0x20;
                key[3] = 0x10;
                key[4] = 0x08;
                key[5] = 0x04;
                key[6] = 0x02;
                key[7] = 0x01;

                for (k = 0; k < BYTE_OF_ONE_WORD; k++) {
                    System.out.printf("%02x ", character_byte[k] & 0xff);
                }
                System.out.println("\n");
                for (k = 0; k < 12; k++) {
                    for (j = 0; j < 2; j++) {
                        for (i = 0; i < 8; i++) {
                            flag = bytes[k * 2 + j] & 0xff & key[i];
                            if (flag == 0) {
                                pointflag = false;
                            } else {
                                pointflag = true;
                            }
                            System.out.printf("%s", pointflag ? "o" : "O");
                        }
                    }
                    System.out.println("\n");
                }

            } catch (IOException  | ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     *半角转全角
     */
    public static String ToSBC(String input) {
        char c[] = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == ' ') {
                c[i] = '\u3000';
            } else if (c[i] < '\177') {
                c[i] = (char) (c[i] + 65248);

            }
        }
        return new String(c);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onresume");
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        registerReceiver(mContinueSendReceiver, makeContinuesIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onpause");
        unregisterReceiver(mGattUpdateReceiver);
        unregisterReceiver(mContinueSendReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ondestroy");
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);

            }
        });
    }

    public void showMyDialog(View v){
        dialog =new MyProgressDialog(this, getString(R.string.connecting), R.drawable.loading_animate);
        dialog.show();
        /*Handler handler =new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        }, 3000);*/
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

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
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTING);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    private static IntentFilter makeContinuesIntentFilter() {
        IntentFilter continueSendFilter = new IntentFilter();
        continueSendFilter.addAction(GATT_WRITE_SUCCESS);
        return continueSendFilter;
    }

    class MyPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mViewList.size();
        }

        @Override
        public Object instantiateItem(View container, int position) {
            Log.i("INFO", "instantiate item:"+position);
            ((ViewPager) container).addView(mViewList.get(position),0);
            return mViewList.get(position);
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            Log.i("INFO", "destroy item:"+position);
            ((ViewPager) container).removeView(mViewList.get(position));
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }
    }
}
