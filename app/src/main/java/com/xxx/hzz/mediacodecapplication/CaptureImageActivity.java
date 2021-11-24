package com.xxx.hzz.mediacodecapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by huangzezhan on 2020/8/26.
 */

public class CaptureImageActivity extends Activity {
    private static final String TAG = "EncodeActivity";
    private LinearLayout rootView;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private static final int REQUEST_CODE = 100;

    private Surface mSurface;
    private ImageReader mImageReader;
    private String mImagePath;

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

        Button button = new Button(this);
        button.setText("开始");
        ViewGroup.LayoutParams btnParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        button.setLayoutParams(btnParams);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCapture();
            }
        });
        rootView.addView(button);
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
        createVirtualDisplay(dm.widthPixels,dm.heightPixels, dm.densityDpi, mediaProjection);
    }

    private void createEnvironment(int w, int h) {
        mImagePath = Environment.getExternalStorageDirectory().getPath() + "/screenshort/";
        Log.i(TAG, "createEnvironment mImagePath:" + mImagePath);
        File files = new File(mImagePath);
        deleteAllFiles(files);
        mImageReader = ImageReader.newInstance(w, h, 0x1, 2);
        mSurface = mImageReader.getSurface();
    }

    private void deleteAllFiles(File file) {
        File files[] = file.listFiles();
        if (files != null)
            for (File f : files) {
                if (f.isDirectory()) { // 判断是否为文件夹
                    deleteAllFiles(f);
                    try {
                        f.delete();
                    } catch (Exception e) {
                    }
                } else {
                    if (f.exists()) { // 判断是否存在
                        deleteAllFiles(f);
                        try {
                            f.delete();
                        } catch (Exception e) {
                        }
                    }
                }
            }
    }

    private VirtualDisplay createVirtualDisplay(int w, int h, int densityDpi, MediaProjection p) {
        createEnvironment(w,h);

        return p.createVirtualDisplay("CaptureImage",
                w, h, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null /*Callbacks*/, null /*Handler*/);
    }

    private void startCapture() {

        String mImageName = System.currentTimeMillis() + ".png";

        Log.e(TAG, "image name is : " + mImageName);

        Image image = mImageReader.acquireLatestImage();

        int width = image.getWidth();

        int height = image.getHeight();

        Log.e(TAG, "image width/height : " + width + "/" + height);

        final Image.Plane[] planes = image.getPlanes();

        final ByteBuffer buffer = planes[0].getBuffer();

        int pixelStride = planes[0].getPixelStride();

        int rowStride = planes[0].getRowStride();

        int rowPadding = rowStride - pixelStride * width;

        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);

        bitmap.copyPixelsFromBuffer(buffer);

        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);

        image.close();

        if (bitmap != null) {

            Log.e(TAG, "bitmap  create success ");

            try {

                File fileFolder = new File(mImagePath);

                if (!fileFolder.exists())

                    fileFolder.mkdirs();

                File file = new File(mImagePath, mImageName);

                if (!file.exists()) {

                    Log.e(TAG, "file create success ");

                    file.createNewFile();

                }

                FileOutputStream out = new FileOutputStream(file);

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

                out.flush();

                out.close();

                Log.e(TAG, "file save success ");

                Toast.makeText(this.getApplicationContext(), "截图成功", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {

                Log.e(TAG, e.toString());

            }

        }

    }

}
