package com.xxx.hzz.mediacodecapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by huangzezhan on 2020/8/26.
 */

public class EncodeActivity extends Activity {
    private static final String TAG = "EncodeActivity";
    private LinearLayout rootView;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private static final int REQUEST_CODE = 100;
    private Encoder mScreenCapture;
    private Button mStop;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        request();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initView() {
        rootView = new LinearLayout(this);
        rootView.setOrientation(LinearLayout.VERTICAL);
        rootView.setBackgroundColor(Color.BLUE);

        TextView textView = new TextView(this);
        textView.setText("test");
        textView.setTextColor(Color.RED);
        ViewGroup.LayoutParams layoutParams_txt = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(layoutParams_txt);

        mStop = new Button(this);
        mStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopEncode();
                stopWriteVideo();
            }
        });
        mStop.setText("stopRecord");
        rootView.addView(mStop);
        rootView.addView(textView);
        setContentView(rootView);
    }

    private void request() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult requestCode:" + requestCode + ",resultCode:" + resultCode + ", data:" + data);
        if (requestCode != REQUEST_CODE || resultCode != RESULT_OK || data == null) {
            return;
        }
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            return;
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mScreenCapture = new Encoder(dm.widthPixels, dm.heightPixels,dm.densityDpi, mediaProjection);
//        mScreenCapture = new Encoder(592, 1080, mediaProjection);
        mScreenCapture.setOnCaptureVideoCallback(mVideoCallback);
        mScreenCapture.startCapture();
    }

    int count = 0;

    Encoder.OnCaptureVideoCallback mVideoCallback = new Encoder.OnCaptureVideoCallback() {
        @Override
        public void onCaptureVideoCallback(byte[] buf) {
            count++;
            Log.i(TAG, "onCaptureVideoCallback count:" + count);
//            byte[] bytes = new byte[buf.length + 4];
//            byte[] head = CodecUtils.intToBytes(buf.length);
            Log.i(TAG,"onCaptureVideoCallback  frameLen " + buf.length + "/" + count);
//            System.arraycopy(head, 0, bytes, 0, head.length);
//            System.arraycopy(buf, 0, bytes, head.length, buf.length);
//            writeVideo(buf);
        }
    };
    private String videoPath = "sdcard/mc_video.h264";
    private OutputStream mVideoStream;

    private void writeVideo(byte[] bytes) {
        Log.i(TAG, "writeVideo");
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
        try {
            mVideoStream.write(bytes);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    private void stopWriteVideo() {
        Log.i(TAG, "stopWriteVideo");
        if (mVideoStream != null) {
            try {
                mVideoStream.flush();
                mVideoStream.close();
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            mVideoStream = null;
        }
    }

    private void stopEncode(){
        if (mScreenCapture != null) {
            mScreenCapture.stopCapture();
            mScreenCapture = null;
        }
    }
}
