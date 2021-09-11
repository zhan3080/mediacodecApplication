package com.xxx.hzz.mediacodecapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by huangzezhan on 2020/8/26.
 */

public class DecodeActivity extends Activity {
    private static final String TAG = "DecodeActivity";
    private LinearLayout rootView;
    private Button startBtn;
    private SurfaceView mSurfaceView = null;
    private Surface mSurface = null;
    private Thread mDecodecListenerThread;
    private MediaCodec mMC;
    private static final String TEST_VIDEO = "/sdcard/test/tmp.h264";

    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private boolean mStop = false;
    private static final int TIMEOUT_US = 16000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initView() {
        rootView = new LinearLayout(this);
        rootView.setOrientation(LinearLayout.VERTICAL);

        startBtn = new Button(this);
        startBtn.setText("开始");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT);
        startBtn.setLayoutParams(params);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDecodecListenerThread = new Thread() {
                    public void run() {
                        renderToSurface();
                    }
                };
                mDecodecListenerThread.start();
            }
        });
        rootView.addView(startBtn);

        mSurfaceView = new SurfaceView(this);
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                , ViewGroup.LayoutParams.MATCH_PARENT);
        mSurfaceView.setLayoutParams(params);
        rootView.addView(mSurfaceView);
        mSurfaceView.getHolder().addCallback(callback);

        setContentView(rootView);
    }


    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "surfaceCreated ");
            mSurface = holder.getSurface();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(TAG, "surfaceChanged format:" + format + ", w/h:" + width + "/" + height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, "surfaceDestroyed ");
        }
    };


    private void renderToSurface() {
        byte[] mTest264;
        File f264 = new File(TEST_VIDEO);
        FileInputStream is264;
        int mSPSLen = 0;
        int mPPSLen = 0;

        try {
            is264 = new FileInputStream(f264);
            mTest264 = new byte[is264.available()];
            Log.i(TAG, "file len = " + is264.available());
            is264.read(mTest264, 0, is264.available());
            is264.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        int start = 0;
        int mOffset264 = 0;

        // 标准h264 开头sps pps I P
        // sps（00 00 00 01 67 或者 00 00 01 67）
        // pps（00 00 00 01 68 或者 00 00 01 68）
        // I帧（00 00 00 01 65 或者 00 00 01 65）

        // 这里没有判断sps数据帧，因为一般sps pps是同时出现，判断出pps，签名的数据就是sps（如果实际使用有需求，根据00 00 00 01 67判断出来）
        // sps判断
        for (start = mOffset264; start < mTest264.length - 4; start++) {
            if ((mTest264[start] == 0) && (mTest264[start + 1] == 0) && (mTest264[start + 2] == 0) && (mTest264[start + 3] == 1) && ((mTest264[start + 4] & 0x1f) == 8)) {
                break;
            }
            if ((mTest264[start] == 0) && (mTest264[start + 1] == 0) && (mTest264[start + 2] == 1) && ((mTest264[start + 3] & 0x1f) == 8)) {
                break;
            }
        }
        Log.i(TAG, "renderToSurface start:" + start);
        Log.i(TAG, "renderToSurface mOffset264:" + mOffset264);
        // sps长度 （这个长度计算，默认数据开头就是sps pps，实际中，不一定是这样，就需要先判断sps的起始位置，也就是mOffset264不为0）
        mSPSLen = start - mOffset264;
        mOffset264 = start;

        // I帧判断
        for (start = mOffset264; start < mTest264.length - 4; start++) {
            if ((mTest264[start] == 0) && (mTest264[start + 1] == 0) && (mTest264[start + 2] == 0) && (mTest264[start + 3] == 1) && ((mTest264[start + 4] & 0x1f) == 5)) {
                break;
            }
            if ((mTest264[start] == 0) && (mTest264[start + 1] == 0) && (mTest264[start + 2] == 1) && ((mTest264[start + 3] & 0x1f) == 5)) {
                break;
            }
        }
        Log.i(TAG, "renderToSurface start:" + start);
        Log.i(TAG, "renderToSurface mOffset264:" + mOffset264);
        // pps长度
        mPPSLen = start - mOffset264;
        mOffset264 = start;

        try {
            // 创建解码器
            mMC = MediaCodec.createDecoderByType("video/avc");
            //mMC = MediaCodec.createByCodecName("OMX.RTK.video.decoder");
            //mMC =  MediaCodec.createByCodecName("OMX.RTK.video.decoder");
            //mMC =  MediaCodec.createByCodecName("OMX.amlogic.avc.decoder.awesome");
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return;
        }
        if (mMC == null) {
            return;
        }
        Log.i(TAG, "createByCodecName");
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1080, 1920);
        format.setByteBuffer("csd-0", ByteBuffer.wrap(mTest264, 0, mSPSLen));
        format.setByteBuffer("csd-1", ByteBuffer.wrap(mTest264, mSPSLen, mPPSLen));

        mMC.configure(format, mSurface, null, 0);
        mMC.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.i(TAG, "start ok");

        mStop = false;
        int inputBufferIndex;
        int frameLen = 0;
        int presentationTimeUs = 0;

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            return mMC.getInputBuffer(index);
//        }else{
//            return mMC.getInputBuffers()[index];
//        }
        startDrawThread();
        while (!mStop) {
            for (start = mOffset264 + 1; start < mTest264.length - 4; start++) {
                if ((mTest264[start] == 0) && (mTest264[start + 1] == 0) && (mTest264[start + 2] == 0) && (mTest264[start + 3] == 1)) {
                    break;
                }
            }
            if (start >= mTest264.length - 5) {
                mStop = true;
                break;
            }
            frameLen = start - mOffset264;
            inputBufferIndex = mMC.dequeueInputBuffer(TIMEOUT_US);
            if (inputBufferIndex > 0) {
                ByteBuffer buffer = mMC.getInputBuffer(inputBufferIndex);
                buffer.put(mTest264, mOffset264, frameLen);
                mMC.queueInputBuffer(inputBufferIndex, 0, frameLen, presentationTimeUs, 0);
                Log.i(TAG, "Feed H264 Frame With Size = " + frameLen + ", file pos=" + mOffset264);
            } else {
                // todo 超时的处理
            }

            mOffset264 = start;
            presentationTimeUs += TIMEOUT_US;

            try {
                Thread.sleep(16);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (mStop) {
                Log.i(TAG, "renderToSurface stop");
                break;
            }
        }

        try {
            mMC.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mMC.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mMC.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void startDrawThread() {
        new Thread() {
            public void run() {
                drawFrame();
            }
        }.start();
    }

    private void drawFrame() {
        Log.i(TAG, "drawFrame");
        int outputBufferIndex;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (!mStop) {
            if (mMC == null) {
                break;
            }
            try {
                outputBufferIndex = mMC.dequeueOutputBuffer(bufferInfo, 10000);
                if (outputBufferIndex > 0) {
                    mMC.releaseOutputBuffer(outputBufferIndex, true);
                } else {
                    Log.i(TAG, "drawFrame outputBufferIndex:" + outputBufferIndex);
                }
            } catch (Exception e) {

            }
        }
    }

}
