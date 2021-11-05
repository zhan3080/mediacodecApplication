package com.xxx.hzz.mediacodecapplication;

public interface IReceiveDataCallback {
    void onDataPrepare(byte[] sps, byte[] pps);
    void onDataReceive(byte[] data, int len);
    void onDataStop();
}
