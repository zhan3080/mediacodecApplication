package com.xxx.hzz.mediacodecapplication;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

// 发送端数据发送线程
public class DataSender extends Thread{
    public static final String TAG = "DataSender";
    private Socket mSocket = null;
    private OutputStream mOutputStream = null;


    // todo 方便测试，当前写死ip及端口
    private static final int SERVER_PORT = 8010;
    private static final String SERVER_IP = "192.168.205.111";
    
    // for test 方便测试，编码后、发送前的数据是否有问题
    // 保存本地文件
    private static final String videoPath = "/sdcard/sendData.h264";
    private OutputStream mVideoStream = null;
    private boolean isSaveLocal = false;

    @Override
    public void run() {
        init();
    }

    private void init(){
        try {
            mSocket = new Socket();
            mSocket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 5000);
            Log.i(TAG,"init connect mSocket: " + mSocket);
            mOutputStream = mSocket.getOutputStream();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    void writeToFile(byte[] data) {
        if (!isSaveLocal) {
            return;
        }
        if (mVideoStream == null) {
            File videoFile = new File(videoPath);
            if (videoFile.exists()) {
                videoFile.delete();
            }
            try {
                videoFile.createNewFile();
                mVideoStream = new FileOutputStream(videoFile);
            } catch (Exception e) {
                Log.w(TAG, e);
                return;
            }
        }
        try {
            Log.i(TAG, "writeToFile datalen:" + data.length);
            mVideoStream.write(data);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public boolean isSendReady(){
        if(mSocket == null || mOutputStream == null){
            return false;
        }
        return true;
    }

    // 发送具体视频数据前，先组装并发送数据头，在发送实际真实帧数据
    // 目前数据头简单定义：
    // 8个字节长度，前三位固定0x50,0x4f,0x53, 第四位是视频数据类型，
    // 第5～8位为帧数据长度，从低到高位保存32位的int数据
    public void sendData(int type , byte[] data, int off, int len){
        byte[] b = Util.getByte(len);
        switch (type){
            case 0:
                byte[] spsHead = new byte[] {0x50,0x4f,0x53,0x50,b[0],b[1],b[2],b[3]};
                sendData(spsHead,0,8);
                break;
            case 1:
                byte[] ppsHead = new byte[] {0x50,0x4f,0x53,0x51,b[0],b[1],b[2],b[3]};
                sendData(ppsHead,0,8);
                break;
            case 2:
                byte[] dataHead = new byte[] {0x50,0x4f,0x53,0x52,b[0],b[1],b[2],b[3]};
                sendData(dataHead,0,8);
                break;
        }
        writeToFile(data);
        sendData(data,off,len);

    }

    public void sendData(byte[] data, int off, int len){
//        Log.i(TAG, "sendData off = " + off + ", len:" + len + ", mSocket:" + mSocket);
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
        if (mVideoStream != null) {
            try {
                mVideoStream.close();
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }
    }

}
