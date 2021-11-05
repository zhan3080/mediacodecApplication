package com.xxx.hzz.mediacodecapplication;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class DataSender extends Thread{
    public static final String TAG = "DataSender";
    private static final int SERVER_PORT = 8010;
    private static final String SERVER_IP = "192.168.205.110";
    private Socket mSocket = null;
    private OutputStream mOutputStream = null;

    @Override
    public void run() {
        init();
//        sendData("for test");
    }

    private void init(){
        try {
            mSocket = new Socket(SERVER_IP, SERVER_PORT);
            Log.i(TAG,"init connect mSocket: " + mSocket);
            mOutputStream = mSocket.getOutputStream();
//            byte[] b = new byte[1024];
//            InputStream is = mSocket.getInputStream();
//            OutputStream os = mSocket.getOutputStream();
//            while (true){
//                int size = is.read(b);
//                Log.i(TAG,"init mSocket read : " + size + "," + b.toString());
//                os.write("test".getBytes());
//            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendData(byte[] data, int off, int len){
        Log.i(TAG, "sendData off = " + off + ", len:" + len + ", mSocket:" + mSocket);
        if(mSocket == null){
            return;
        }
        if(mOutputStream == null){
            try {
                mOutputStream = mSocket.getOutputStream();
            }catch (Exception e){
                Log.w(TAG,e);
            }
        }
        try {
            mOutputStream.write(data,off,len);
        }catch (Exception e){
            Log.w(TAG, "write " + e);
        }
    }

    public void sendData(byte[] data){
        if(mSocket == null){
            return;
        }
        if(mOutputStream == null){
            try {
                mOutputStream = mSocket.getOutputStream();
            }catch (Exception e){
                Log.w(TAG,e);
            }
        }
        try {
            mOutputStream.write(data);
        }catch (Exception e){
            Log.w(TAG, "write " + e);
        }
    }

    public void close(){
        Log.i(TAG, "close mOutputStream:" + mOutputStream + ", mSocket:" + mSocket);
        if(mOutputStream != null){
            try{
                mOutputStream.flush();
                mOutputStream.close();
                mOutputStream = null;
            }catch (Exception e){
                Log.w(TAG,e);
            }
        }
        if(mSocket != null && !mSocket.isClosed()){
            try{
                mSocket.shutdownOutput();
                mSocket.close();
                mSocket = null;
            }catch (Exception e){
                Log.w(TAG,e);
            }
        }
        mSocket = null;
    }

}
