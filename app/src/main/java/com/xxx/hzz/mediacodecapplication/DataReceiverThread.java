package com.xxx.hzz.mediacodecapplication;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class DataReceiverThread extends Thread {
    public static final String TAG = "DataReceiverThread";
    public ServerSocket mSocket = null;
    private static final int SERVER_PORT = 8010;
    private static final String SERVER_IP = "192.168.205.103";
    private OutputStream mOutputStream = null;
    private InputStream mInputStream = null;
    private static final String videoPath = "/sdcard/test/tmp.h264";
    private OutputStream mVideoStream = null;
    private byte[] dataBuff = new byte[1024];
    private boolean isSave = true;

    @Override
    public void interrupt() {
        super.interrupt();
    }

    @Override
    public void run() {
        createServer();
    }

    private void createServer() {
        Log.i(TAG, "createServer");
        if (mSocket == null) {
            try {
                mSocket = new ServerSocket();
                mSocket.setReuseAddress(true);
                // 0.0.0.0监听任意地址，多网卡情况下，每个网卡都监听
                mSocket.bind(new InetSocketAddress("0.0.0.0", SERVER_PORT));
                while (true) {
                    Log.i(TAG, "accept mSocket:" + mSocket);
                    Socket socket = mSocket.accept();
                    Log.i(TAG, "accept " + socket.getInetAddress());
                    receveData(socket);
//                    mOutputStream = socket.getOutputStream();
//                    Log.i(TAG,"create mOutputStream:" + mOutputStream);
//                    if(mOutputStream != null){
//                        mOutputStream.write("已连接到服务器！".getBytes("UTF-8"));
//                    }
//                    try {
//                        sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    InputStream in = socket.getInputStream();
//                    byte[] bytes = new byte[1024];
//                    in.read(bytes);
//                    Log.i(TAG,"服务器接收到: " + bytes.toString());

                }
            } catch (Exception e) {
                Log.i(TAG, "Exception " + e);
            }
        }
    }

    void receveData(Socket socket) {
        if(socket == null){
            return;
        }
        try {
            mInputStream = socket.getInputStream();
        } catch (Exception e){
            Log.w(TAG, e);
        }
        while (!socket.isClosed()) {
            try {
                int len = mInputStream.read(dataBuff);
                Log.i(TAG, "writeToFile len:" + len);
                writeToFile(dataBuff);
                sleep(1000);
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
        Log.i(TAG, "writeToFile socket close:");
        stopWrite();
    }

    void writeToFile(byte[] data) {
        if(!isSave){
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
        }catch (Exception e){
            Log.w(TAG, e);
        }
    }

    void stopWrite(){
        if (mVideoStream != null) {
            try {
                mVideoStream.flush();
                mVideoStream.close();
            }catch (Exception e){
                Log.w(TAG, e);
            }
        }
    }

}
