package com.xxx.hzz.mediacodecapplication;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by huangzezhan on 2020/9/7.
 */

public class Encoder {
    private static final String TAG = "Encoder";
    private Context mContext;
    private MediaCodec mCodec;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjection mMediaProjection;
    private boolean isCapturing = false;
    private Thread mCaptureThread;
    private int mWidth;
    private int mHeight;
    private int mDensityDpi;
    private boolean isSaveLocal = true;
    private boolean isQuitting = false;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private static final long TIMEOUT_US = 33333;

//    private String videoPath = "sdcard/mediacodecVideo.h264";
    private static final String videoPath = "/sdcard/test/tmp.h264";

    private OutputStream mVideoStream = null;

    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(TAG, "handleMessage msg.what：" + msg.what);
            if (msg.what == 100) {
                encodeStart();
            }
        }
    };


    public Encoder(int w, int h, int densityDpi, MediaProjection p) {
        mWidth = w;
        mHeight = h;
        mDensityDpi = densityDpi;
        mMediaProjection = p;
    }

    public void initEncoder() {
        Log.i(TAG, "initEncoder");
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mWidth * mHeight);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        try {
            mCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = mCodec.createInputSurface();
            Log.d(TAG, "created input surface: " + surface);
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("surafce", mWidth, mHeight, mDensityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
            Log.d(TAG, "created createVirtualDisplay: " + mVirtualDisplay);
            mHandler.sendEmptyMessageDelayed(100,3000);
        } catch (Exception e) {
            e.printStackTrace();
            mCodec = null;
        }
    }


    public void startCapture() {
        if (isCapturing) {
            Log.w(TAG, "startCapture ignore 1");
            return;
        }

        initEncoder();
        if (mCodec == null) {
            Log.w(TAG, "startCapture ignore 2");
            return;
        }
        Log.i(TAG, "startCapture");

        isCapturing = true;

//        encodeAsync();
//        encodeStart();
        if (isSaveLocal) {
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
                }
            }
        }
    }

    /**
     * Android5.0之后异步获取
     */
    private void encodeAsync() {
        Log.i(TAG, "encodeAsync");
        mCaptureThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mCodec.setCallback(new MediaCodec.Callback() {
                    @Override
                    public void onInputBufferAvailable(MediaCodec codec, int index) {
                        Log.i(TAG, "onInputBufferAvailable");
                    }

                    @Override
                    public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                        //Log.i(TAG,"onOutputBufferAvailable");
                        ByteBuffer buffer = mCodec.getOutputBuffer(index);
                        encodeFrame(buffer, info.size);
                        mCodec.releaseOutputBuffer(index, false);
                    }

                    @Override
                    public void onError(MediaCodec codec, MediaCodec.CodecException e) {
                        Log.i(TAG, "onError");
                    }

                    @Override
                    public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                        Log.i(TAG, "onOutputFormatChanged");
                    }
                });
                mCodec.start();
            }
        });
        mCaptureThread.start();
    }

    private void encodeStart() {
        Log.i(TAG, "encodeAsync");
        mCaptureThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mCodec == null) {
                    return;
                }
                ByteBuffer[] outbuffers = mCodec.getOutputBuffers();
                while (!isQuitting) {
                    if (mCodec == null || mBufferInfo == null) {
                        return;
                    }
                    int index = mCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
                    Log.i(TAG, "mCaptureThread index = " + index);
                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        //todo sps pps
                        MediaFormat format = mCodec.getOutputFormat();
                        ByteBuffer buffer1 = format.getByteBuffer("csd-0");
                        ByteBuffer buffer2 = format.getByteBuffer("csd-1");
                        if (mVideoStream != null) {
                            byte[] sps = new byte[buffer1.capacity()];
                            byte[] pps = new byte[buffer2.capacity()];
                            buffer1.get(sps, 0, sps.length);
                            buffer2.get(pps, 0, pps.length);
                            Log.i(TAG, "mCaptureThread sps = " + sps.toString() + ", len:" + sps.length);
                            Log.i(TAG, "mCaptureThread pps = " + pps.toString() + ", len:" + pps.length);
                            try {
                                mVideoStream.write(sps);
                                mVideoStream.write(pps);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        //todo
                    } else if (index > 0) {
                        //todo data
                        ByteBuffer encodedData = outbuffers[index];
                        byte flag = encodedData.get(4);
                        flag &= 0xf;
                        Log.i(TAG, "mCaptureThread flag = " + flag);
                        if (flag == 7) {
                            Log.i(TAG, "sps pps len = " + mBufferInfo.size);
                            encodedData.clear();
                            mCodec.releaseOutputBuffer(index, false);
                            continue;
                        }
                        byte[] buff = new byte[mBufferInfo.size];
                        encodedData.get(buff);
                        if (mVideoStream != null) {
                            try {
                                mVideoStream.write(buff, 0, mBufferInfo.size);
                            } catch (Exception e) {
                                Log.w(TAG, e);
                            }
                        }

                        encodedData.clear();
                        mCodec.releaseOutputBuffer(index, false);
                    }
                }

                if (mVideoStream != null) {
                    try {
                        mVideoStream.close();
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }
                }
            }
        });
        mCodec.start();
        mCaptureThread.start();
    }

    private void stopEncode() {
        isQuitting = true;
        mBufferInfo = null;
    }


    public void stopCapture() {
        Log.i(TAG, "stopCapture");
        stopEncode();
        isCapturing = false;
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        if (mCodec != null) {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }


    private void encodeFrame(ByteBuffer buffer, int bufferSize) {
        final byte[] bytes = new byte[bufferSize];
        buffer.get(bytes);
        if (null != mVideoCallback) {
            mVideoCallback.onCaptureVideoCallback(bytes);
        }
    }

    private OnCaptureVideoCallback mVideoCallback;

    public void setOnCaptureVideoCallback(OnCaptureVideoCallback videoCallback) {
        mVideoCallback = videoCallback;
    }

    public interface OnCaptureVideoCallback {
        void onCaptureVideoCallback(byte[] bytes);
    }
}
