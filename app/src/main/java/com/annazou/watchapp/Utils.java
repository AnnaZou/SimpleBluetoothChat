package com.annazou.watchapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public class Utils {
    private static final String Url = "https://watch";
    private static final String testUrl = "http://test";

    private static final String SP_NAME = "WatchApp";

    public static final int MSG_RESPONSE = 0;
    
    public static void startRegister(Context context, Handler handler) {
        String imei1 = getImei(context, 0);
        String imei2 = getImei(context, 1);
        String token = MD5("Mofone" + imei1 + imei2);
        JSONObject data = new JSONObject();
        try {
            data.put("IMEIFirst", imei1);
            data.put("IMEISecond", imei2);
            data.put("Token", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendHttp(Url, data.toString(), handler);
    }

    private static void sendHttp(final String urlStr, final String paramStr, final Handler handler) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(urlStr);
                    conn = (HttpURLConnection) url.openConnection();
                    //conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    String param = paramStr;
                    OutputStream outputStream = conn.getOutputStream();
                    DataOutputStream out = new DataOutputStream(outputStream);
                    out.writeBytes(param);
                    out.flush();
                    out.close();

                    InputStream inputStream = conn.getInputStream();
                    String response = readInfo(inputStream);
                    Message msg = handler.obtainMessage(MSG_RESPONSE);
                    msg.obj = response;
                    handler.sendMessage(msg);

                    if (conn != null) {
                        conn.disconnect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static String readInfo(InputStream input) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = input.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            byte[] data = os.toByteArray();
            os.close();
            input.close();
            String str = new String(data);
            return str;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String MD5(String string) {
        if (string.isEmpty()) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    public static String getImei(Context context, int id) throws SecurityException{
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getDeviceId(id) + "";
    }


    private void handleResponse(String response) {
        boolean successed = false;
        JSONObject result = new JSONObject();
        try {
            result = new JSONObject(response);
            successed = (boolean) result.get("success");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!successed) {
            int code = 0;
            try {
                JSONObject failMsg = (JSONObject) result.get("response");
                code = failMsg.getInt("code");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //Log.w(TAG, "Register failed, error code: " + code);
            /*switch(code) {
                case 101:
                    // Token error.
                    break;
                case 102:
                    // Parameter error.
                    break;
                case 103:
                    // Request failed.
                    registerDelayed();
                    break;
                case 201:
                    // Imei not found.
                    registerDelayed();
                    break;
            }*/
        } else {
            //Log.i(TAG, "Register successed, stop service.");
           // Utils.setRegistered(this, true);
        }
    }

    static public BluetoothDevice getPairedDevice(BluetoothAdapter adapter){
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String str = device.getName() + "|" + device.getAddress();
                if(device.getBondState() == BluetoothDevice.BOND_BONDED){
                    //return device;
                }
                if(device.getName().equals("]\\\\_]4[[\\[Z][") || device.getName().equals("mtk")) return device;
            }
        }
        return null;
    }
}
