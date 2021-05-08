package com.annazou.watchapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static com.annazou.watchapp.BluetoothService.STATE_CONNECTED;
import static com.annazou.watchapp.BluetoothService.STATE_NONE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, BluetoothService.MessageCallback {

    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;

    public static final int MSG_UPDATE_LIST = 1;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothService mService;
    BluetoothDevice mDevice;

    Button mSendButton;
    EditText mSendText;
    ListView mList;
    MessageAdapter mAdapter;

    List<BluetoothService.MessageItem> mMessage;

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MSG_UPDATE_LIST:
                    mMessage = mService.getMessageList();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 若当前设备不支持蓝牙功能
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mDevice = Utils.getPairedDevice(mBluetoothAdapter);
        Log.e("mytest","mDevice = " + mDevice);
        //if(manager.getConnectionState(device, BluetoothProfile.A2DP) == STATE_CONNECTED){

        //}

        if(!mBluetoothAdapter.isEnabled()){
            // 若当前设备蓝牙功能未开启，则开启蓝牙
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        } else {
            setupChat();
        }

        mSendButton = findViewById(R.id.message_send);
        mSendButton.setOnClickListener(this);

        mSendText = findViewById(R.id.message_edit);

        mList = findViewById(R.id.message_list);
        mAdapter = new MessageAdapter();
        mList.setAdapter(mAdapter);


    }

    @Override
    public void onStart(){
        super.onStart();

    }

    @Override
    public synchronized void onResume(){
        super.onResume();

        if(mService != null) {
            if (mService.getState() == BluetoothService.STATE_NONE) {
                mService.start();
            }
            if(mDevice != null) {
                mService.connect(mDevice);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mService != null) {
            getApplicationContext().unbindService(mServiceConnection);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("mytest","onServiceConnected");
            BluetoothService.Mybinder binder = (BluetoothService.Mybinder) service;
            mService = binder.getService();
            mService.setCallback(MainActivity.this);
            mService.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    public void onActivityResult(int requesstCode, int resultCode, Intent data) {
        switch (requesstCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString("address");
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Toast.makeText(this, "bt_not_enable_leaving",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void openBt(){
        if (!mBluetoothAdapter.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, REQUEST_ENABLE_BT);
        }
    }

    private void closeBt(){
        mBluetoothAdapter.disable();
    }

    private void setupChat(){
        Intent intent = new Intent(this, BluetoothService.class);
        boolean bind = getApplicationContext().bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        Log.e("mytest","bind = " + bind);
    }

    private void ensureDiscoverable(){
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(
                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(discoverableIntent);
        }
    }

    private void scan() {
        //Intent serverIntent=new Intent(this,DeviceList.class);
        //startActivityForResult(serverIntent,REQUEST_CONNECT_DEVICE);
    }


    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.message_send){
            if(mService == null) return;
            String text = mSendText.getText().toString();
            if(text == null || text.isEmpty()){
                return;
            }
            mService.sendMessage(text);
            mSendText.setText("");
        }
    }

    @Override
    public void onMessageReceived() {
        mHandler.sendEmptyMessage(MSG_UPDATE_LIST);
    }

    @Override
    public void onMessageSent() {
        mHandler.sendEmptyMessage(MSG_UPDATE_LIST);
    }

    private class MessageAdapter extends BaseAdapter{

        static final int TYPE_MINE = 0;
        static final int TYPE_THEM = 1;

        public MessageAdapter(){

        }

        @Override
        public int getItemViewType(int position) {
            return mMessage.get(position).name.equals("me") ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getCount() {
            if(mMessage == null) return 0;
            return mMessage.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                int layoutId = getItemViewType(position) == TYPE_MINE ? R.layout.message_item_mine : R.layout.message_item_them;
                convertView = LayoutInflater.from(MainActivity.this).inflate(layoutId, null);
                MyHolder holder = new MyHolder();
                holder.content = convertView.findViewById(R.id.message_content);
                convertView.setTag(holder);
            }

            MyHolder holder = (MyHolder) convertView.getTag();
            holder.content.setText(mMessage.get(position).content);

            return convertView;
        }
    }

    class MyHolder{
        TextView content;
    }
}
