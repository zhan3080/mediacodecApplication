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
    private static final String SERVER_IP = "192.168.205.103";
    private Socket mSocket = null;

    @Override
    public void run() {
        init();
        sendData("for test");
    }

    private void init(){
        try {
            mSocket = new Socket(SERVER_IP, SERVER_PORT);
            Log.i(TAG,"init connect mSocket: " + mSocket);
            byte[] b = new byte[1024];
            InputStream is = mSocket.getInputStream();
            OutputStream os = mSocket.getOutputStream();
            while (true){
                int size = is.read(b);
                Log.i(TAG,"init mSocket read : " + size + "," + b.toString());
                os.write("test".getBytes());
            }


            //读取服务器端数据
//            BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
//            String ret = input.readLine();
//            Log.i(TAG,"服务器端返回: " + ret);

//            DataInputStream in = new DataInputStream(mSocket.getInputStream()); //接收完成读取Socket的输入流
//            Log.i(TAG,"init connect mSocket: " + in.readUTF());
//            DataOutputStream out = new DataOutputStream(mSocket.getOutputStream()); //发送数据
//            out.writeUTF("client test".toString());


        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendData(String data){
        if(mSocket == null){
            return;
        }
        try {
            OutputStream out = mSocket.getOutputStream();
            out.write(data.getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
