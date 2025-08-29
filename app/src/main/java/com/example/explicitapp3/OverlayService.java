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
    public static int SERVICE_ID = 1667;
    private MediaProjection mediaProjection;
    Notification notification;
    MediaProjectionManager mediaProjectionManager;
    WindowManager wm;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int rc = intent.getIntExtra("resultCode", -1);
        Intent d = intent.getParcelableExtra("data");
        Log.w(TAG, "onStartCommand: Media projection will now start capturing");

        if (rc == -1 && d != null) {
            mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (overlayFunctions == null) overlayFunctions = new OverlayFunctions();

            overlayFunctions.setWindowManager(wm);
            overlayFunctions.setMediaProjectionManager(mediaProjectionManager);
            overlayFunctions.setDpi(getResources().getDisplayMetrics().densityDpi);
            notification = overlayFunctions.setNotification(getApplicationContext());
            startForeground(SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);

            if (mediaProjection == null) {
                mediaProjection = mediaProjectionManager.getMediaProjection(rc, d);
                overlayFunctions.setMediaProjection(mediaProjection);
            }
//            overlayFunctions.setupOverlay(
//                    mediaProjection,
//                    getResources().getDisplayMetrics().densityDpi,
//                    (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)
//            );
            overlayFunctions.setupOverlayScreenshot();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        overlayFunctions.destroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}