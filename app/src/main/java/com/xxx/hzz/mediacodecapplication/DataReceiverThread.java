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
import java.nio.ByteBuffer;

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
    private IReceiveDataCallback dataCallback = null;

    @Override
    public void interrupt() {
        super.interrupt();
    }

    @Override
    public void run() {
        createServer();
    }

    public void setDataCallback(IReceiveDataCallback callback) {
        dataCallback = callback;
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
        if (socket == null) {
            return;
        }
        try {
            mInputStream = socket.getInputStream();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        while (!socket.isClosed()) {
            try {
                byte[] buffer;
                int length = mInputStream.available();
                Log.i(TAG, "receveData length:" + length);
                buffer = new byte[length];
                int len = mInputStream.read(buffer);
                Log.i(TAG, "writeToFile len:" + len);
                writeToFile(buffer);
                parseData(buffer);
                sleep(1000);
            } catch (Exception e) {
                Log.w(TAG, e);
                break;
            }
        }
        Log.i(TAG, "writeToFile socket close:");
        stopWrite();
    }

    void writeToFile(byte[] data) {
        if (!isSave) {
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

    void parseData(byte[] data) {
        if(data.length <= 0){
            return;
        }
        int start = 0;
        int mOffset264 = 0;
        int mSPSLen = 0;
        int mPPSLen = 0;

        // 标准h264 开头sps pps I P
        // sps（00 00 00 01 67 或者 00 00 01 67）
        // pps（00 00 00 01 68 或者 00 00 01 68）
        // I帧（00 00 00 01 65 或者 00 00 01 65）

        boolean isSps = false;
        boolean isPps = false;
        for (start = mOffset264; start < data.length - 4; start++) {
            if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 0) && (data[start + 3] == 1) && ((data[start + 4] & 0x1f) == 7)) {
                isSps = true;
                break;
            }
            if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 1) && ((data[start + 3] & 0x1f) == 7)) {
                isSps = true;
                break;
            }
        }
        // sps判断
        if (isSps) {
            for (start = mOffset264; start < data.length - 4; start++) {
                if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 0) && (data[start + 3] == 1) && ((data[start + 4] & 0x1f) == 8)) {
                    isPps = true;
                    break;
                }
                if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 1) && ((data[start + 3] & 0x1f) == 8)) {
                    isPps = true;
                    break;
                }
            }
        }
        // sps长度 （这个长度计算，默认数据开头就是sps pps，实际中，不一定是这样，就需要先判断sps的起始位置，也就是mOffset264不为0）
        if (isSps) {
            mSPSLen = start - mOffset264;
            mOffset264 = start;
        }

        // I帧判断
        for (start = mOffset264; start < data.length - 4; start++) {
            if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 0) && (data[start + 3] == 1) && ((data[start + 4] & 0x1f) == 5)) {
                break;
            }
            if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 1) && ((data[start + 3] & 0x1f) == 5)) {
                break;
            }
        }

        // pps长度
        if (isPps) {
            mPPSLen = start - mOffset264;
            mOffset264 = start;
        }
        Log.i(TAG, "parseData isSps:" + isSps + ", isPps:" + isPps + ", mSPSLen:" + mSPSLen + ", mPPSLen:" + mPPSLen );
        Log.i(TAG, "parseData mOffset264:" + mOffset264);
        if (isSps && isPps && dataCallback != null) {
            dataCallback.onDataPrepare(ByteBuffer.wrap(data, 0, mSPSLen-1).array(), ByteBuffer.wrap(data, mSPSLen, mPPSLen-1).array());
            if(data.length > (mSPSLen + mPPSLen)) {
                dataCallback.onDataReceive(ByteBuffer.wrap(data, mSPSLen + mPPSLen, (data.length-1)).array(), (data.length - mSPSLen - mPPSLen));
            }
            return;
        }
        dataCallback.onDataReceive(data, data.length);
    }

    void stopWrite() {
        if (mVideoStream != null) {
            try {
                mVideoStream.flush();
                mVideoStream.close();
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
    }

}
