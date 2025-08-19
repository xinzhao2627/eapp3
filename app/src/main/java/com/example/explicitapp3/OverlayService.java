package com.example.explicitapp3;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.explicitapp3.OverlayFunctions.*;

import java.nio.ByteBuffer;

public class OverlayService extends Service {
    public static final String TAG = "OverlayService";
    OverlayFunctions overlayFunctions;
    WindowManager windowManager;
    public static int SERVICE_ID = 1667;
    private MediaProjection mediaProjection;
    Notification notification;
    MediaProjectionManager mediaProjectionManager;
    ScreenshotCapture screenshotCapture;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "onCreate: service initialized..");
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (overlayFunctions == null) overlayFunctions = new OverlayFunctions();
        overlayFunctions.setMediaProjectionManager(mediaProjectionManager);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            overlayFunctions.setupOverlay(
                    mediaProjection,
                    getResources().getDisplayMetrics().densityDpi,
                    (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE),
                    (WindowManager) getSystemService(WINDOW_SERVICE)
            );
        }

//        screenshotCapture = new ScreenshotCapture(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notification = overlayFunctions.setNotification(getApplicationContext());
            startForeground(SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            NotificationHelper.createNotificationChannel(getApplicationContext());
//            notification = NotificationHelper.createNotification(getApplicationContext());
//            startForeground(SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
//        }
        int rc = intent.getIntExtra("resultCode", -1);
        Intent d = intent.getParcelableExtra("data");
        Log.w(TAG, "onStartCommand: Media projection will now start capturing");

        if (rc == -1 && d != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(rc, d);
            if (mediaProjection != null && overlayFunctions.getSurface() != null) {
                overlayFunctions.startScreenCapture(mediaProjection, getResources().getDisplayMetrics().densityDpi);
            }
        }
        // nasty -1 (RESULT OK == -1???? (dont change wdk mwdkwudk))
//        if (rc == -1 && d != null) {
//            mediaProjection = mediaProjectionManager.getMediaProjection(rc, d);
//            if (mediaProjection != null) {
//                Log.w(TAG, "onStartCommand: Media projection will now start capturing");
//                screenshotCapture.startCapture(mediaProjection);
//            }
//        }
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenshotCapture != null) {
            screenshotCapture.destroy();
        }
//        overlayFunctions.destroy(windowManager);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}