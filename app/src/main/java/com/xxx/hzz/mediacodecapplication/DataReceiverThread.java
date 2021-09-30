package com.xxx.hzz.mediacodecapplication;

import java.net.ServerSocket;
import java.net.Socket;

public class DataReceiverThread extends Thread {
    public static final String TAG = "DataReceiverThread";
    public ServerSocket mSocket = null;

    @Override
    public void interrupt() {
        super.interrupt();
    }

    @Override
    public void run() {
        super.run();
    }

    private void createServer(){
        if(mSocket == null) {
            try {
                mSocket = new ServerSocket(52299);
                Socket socket = mSocket.accept();
            }catch (Exception e){
            }
        }
    }
}
