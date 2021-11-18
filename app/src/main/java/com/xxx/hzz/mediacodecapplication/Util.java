package com.xxx.hzz.mediacodecapplication;

public class Util {
    public static final String TAG = "Util";

    public static byte[] getByte(int data) {
        byte[] len = new byte[4];
        // 从地位到高位保存int的值
        len[0] = (byte) (data & 0xff);
        len[1] = (byte) (data >> 8 & 0xff);
        len[2] = (byte) (data >> 16 & 0xff);
        len[3] = (byte) (data >> 24 & 0xff);
        return len;
    }

    /**
     * 通过byte数组取到int
     *
     * @param bb
     * @param index
     *            第几位开始
     * @return
     */
    public static int getInt(byte[] bb, int index) {
        return (int) ((((bb[index + 3] & 0xff) << 24)
                | ((bb[index + 2] & 0xff) << 16)
                | ((bb[index + 1] & 0xff) << 8) | ((bb[index + 0] & 0xff) << 0)));
    }
}


