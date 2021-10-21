package com.xxx.hzz.mediacodecapplication;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
    private OutputStream mOutputStream;

    @Override
    public void interrupt() {
        super.interrupt();
    }

    @Override
    public void run() {
        createServer();
    }

    private void createServer(){
        Log.i(TAG,"createServer");
        if(mSocket == null) {
            try {
                mSocket = new ServerSocket();
                mSocket.setReuseAddress(true);
                // 0.0.0.0监听任意地址，多网卡情况下，每个网卡都监听
                mSocket.bind(new InetSocketAddress("0.0.0.0", SERVER_PORT));
                while (true) {
                    Log.i(TAG,"accept mSocket:" + mSocket);
                    Socket socket = mSocket.accept();
                    Log.i(TAG,"accept " + socket.getInetAddress());

//                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//                    out.writeUTF("server test: ");
//                    DataInputStream input = new DataInputStream(socket.getInputStream());
//                    String clientInputStr = input.readUTF();
//                    Log.i(TAG,"accept clientInputStr:" + clientInputStr);

                    mOutputStream = socket.getOutputStream();
                    Log.i(TAG,"create mOutputStream:" + mOutputStream);
                    if(mOutputStream != null){
                        mOutputStream.write("已连接到服务器！".getBytes("UTF-8"));
                    }
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    InputStream in = socket.getInputStream();
                    byte[] bytes = new byte[1024];
                    in.read(bytes);
                    Log.i(TAG,"服务器接收到: " + bytes.toString());

                }
            }catch (Exception e){
                Log.i(TAG,"Exception " + e);
            }
        }
    }
}
