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


// 接收端数据接收线程
public class DataReceiverThread extends Thread {
    public static final String TAG = "DataReceiverThread";
    // 镜像数据接收服务端socket
    public ServerSocket mSocket = null;
    // 镜像数据端口
    private static final int SERVER_PORT = 8010;
    private InputStream mInputStream = null;
    // 数据接收端监听器（数据接收线程接收到数据后，回调给h264播放器播放）
    private IReceiveDataCallback dataCallback = null;


    // for test测试调试用，验证接收到的数据是否能正确播放，正常情况下，使用ffplay tmp.h264应该能正常播放
    // 是否保存h264到本地文件
    private boolean isSaveLocal = false;
    // h264本地保存路径
    private static final String videoPath = "/sdcard/test/tmp.h264";
    private OutputStream mVideoStream = null;

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
                    //阻塞接收
                    receveData(socket);
                }
            } catch (Exception e) {
                Log.i(TAG, "Exception " + e);
            }
        }
    }

    // 根据头信息，解析当前数据类型
    int getDataType(byte[] data){
        if(data.length != 8){
            return -1;
        }
        if(data[0] != 0x50 || data[1] != 0x4f && data[2] != 53){
            return -1;
        }

        if(data[3] == 0x50){
            return 0;
        }
        if(data[3] == 0x51){
            return 1;
        }
        if(data[3] == 0x52){
            return 2;
        }
        return -1;
    }

    // 根据头信息，解析当前数据包长度
    int getDataLen(byte[] data){
        if(data.length != 8){
            return -1;
        }
        return Util.getInt(data,4);
    }

    // 数据接收处理
    void receveData(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            mInputStream = socket.getInputStream();
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        int numberOfBytesRead = 0;
        int cursor = 0;
        byte[] buffer = new byte[3*1024*1024];
        byte[] head = new byte[8];
        byte[] frameBuffer;
        while (!socket.isClosed()) {
            try{
                cursor = 0;
                // 先读取头信息（8个字节长度）
                while (((numberOfBytesRead = mInputStream.read(head, cursor, 8 - cursor)) != -1) && !socket.isClosed()) {
//                    Log.i(TAG, "receveData read numberOfBytesRead:" + numberOfBytesRead);
                    cursor = cursor + numberOfBytesRead;
                    if (cursor >= 8)
                        break; // got all data
                }
            }catch (Exception e){
                Log.w(TAG,e);
            }

            int type = getDataType(head);
//            Log.i(TAG, "receveData type:" + type);
            if(type == -1){
                continue;
            }

            int frameSize = getDataLen(head);
//            Log.i(TAG, "receveData frameSize:" + frameSize);
            try {
                cursor = 0;
                // 从头信息里解析出实际数据包长度frameSize，阻塞读取指定长度的数据包，防止视频帧数据错乱
                while (((numberOfBytesRead = mInputStream.read(buffer, cursor, frameSize - cursor)) != -1) && !socket.isClosed()) {
                    cursor = cursor + numberOfBytesRead;
                    if (cursor >= frameSize)
                        break; // got all data
                }
//                Log.i(TAG, "receveData cursor:" + cursor);
                frameBuffer = new byte[frameSize];
                System.arraycopy(buffer,0,frameBuffer,0,frameSize);
                dispatchData(type, frameBuffer);
                sleep(10);
            } catch (Exception e) {
                Log.w(TAG, e);
                break;
            }
        }
        Log.i(TAG, "writeToFile socket close:");
        stopWrite();
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

    byte sps[];
    byte pps[];
    // type：0-sps 1-sps 2-i或者p帧
    void dispatchData(int type, byte[] data) {
        if(data.length <= 0 || type < 0){
            return;
        }
        writeToFile(data);

        switch (type){
            case 0:
                sps = new byte[data.length];
                System.arraycopy(data,0,sps,0,data.length);
                break;
            case 1:
                pps = new byte[data.length];
                System.arraycopy(data,0,pps,0,data.length);
                if(sps != null && sps.length != 0
                        && pps != null && pps.length != 0){
                    dataCallback.onDataPrepare(sps,pps);
                    sps = null;
                    pps = null;
                }
                break;
            case 2:
                dataCallback.onDataReceive(data, data.length);
                break;
        }



//        int start = 0;
//        int mOffset264 = 0;
//        int mSPSLen = 0;
//        int mPPSLen = 0;
//
//        // 标准h264 开头sps pps I P
//        // sps（00 00 00 01 67 或者 00 00 01 67）
//        // pps（00 00 00 01 68 或者 00 00 01 68）
//        // I帧（00 00 00 01 65 或者 00 00 01 65）
//
//        boolean isSps = false;
//        boolean isPps = false;
//        boolean isFrame = false;
//        for (start = mOffset264; start < data.length - 4; start++) {
//            if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 0) && (data[start + 3] == 1) && ((data[start + 4] & 0x1f) == 7)) {
//                isSps = true;
//                break;
//            }
//            if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 1) && ((data[start + 3] & 0x1f) == 7)) {
//                isSps = true;
//                break;
//            }
//        }
//        // sps判断
////        if (isSps) {
//            for (start = mOffset264; start < data.length - 4; start++) {
//                if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 0) && (data[start + 3] == 1) && ((data[start + 4] & 0x1f) == 8)) {
//                    isPps = true;
//                    break;
//                }
//                if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 1) && ((data[start + 3] & 0x1f) == 8)) {
//                    isPps = true;
//                    break;
//                }
//            }
////        }
//        // sps
//        if (isSps) {
//            sps = new byte[data.length];
//            System.arraycopy(data,0,sps,0,data.length);
//            return;
//        }
//
//        // pps
//        if (isPps) {
//            pps = new byte[data.length];
//            System.arraycopy(data,0,pps,0,data.length);
//            if(sps != null && sps.length != 0
//                && pps != null && pps.length != 0){
//                dataCallback.onDataPrepare(sps,pps);
//                sps = null;
//                pps = null;
//                return;
//            }
//        }
//
//        // I帧判断
//        for (start = mOffset264; start < data.length - 4; start++) {
//            if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 0) && (data[start + 3] == 1) && ((data[start + 4] & 0x1f) == 5)) {
//                isFrame = true;
//                break;
//            }
//            if ((data[start] == 0) && (data[start + 1] == 0) && (data[start + 2] == 1) && ((data[start + 3] & 0x1f) == 5)) {
//                isFrame = true;
//                break;
//            }
//        }
//        if(isFrame) {
//            dataCallback.onDataReceive(data, data.length);
//        }
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
