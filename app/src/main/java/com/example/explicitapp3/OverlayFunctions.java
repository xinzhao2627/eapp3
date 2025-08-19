package com.example.explicitapp3;

import android.app.Notification;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

public class OverlayFunctions {
    private MediaProjectionManager mediaProjectionManager;
    public Notification notification;
    Surface surface;
    SurfaceView screenMirror;
    VirtualDisplay mVirtualDisplay;
    public MediaProjectionManager setMediaProjectionManager(MediaProjectionManager mediaProjectionManager) {
        this.mediaProjectionManager = mediaProjectionManager;
        return this.mediaProjectionManager;
    }

    public Notification setNotification(Context context) {
        NotificationHelper.createNotificationChannel(context);
        notification = NotificationHelper.createNotification(context);
        return notification;
    }
    public void startScreenCapture(MediaProjection mMediaProjection, int dpi) {
        MediaProjection.Callback callback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                stopScreenCapture();
            }
        };
        mMediaProjection.registerCallback(callback, null);

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "ScreenCapture",
                900,
                500,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null //mHandler
        );
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setupOverlay(MediaProjection mMediaProjection, int dpi, LayoutInflater lf, WindowManager windowManager) {
        // this is the overlay view
        View view = lf.inflate(R.layout.activity_overlay, null);
        screenMirror = view.findViewById(R.id.screenMirror);
        surface = screenMirror.getHolder().getSurface();
        screenMirror.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surface = holder.getSurface();
                if (mMediaProjection != null) startScreenCapture(mMediaProjection, dpi);
            }
            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override public void surfaceDestroyed(SurfaceHolder holder) {}
        });
//        mHandler = new Handler(Looper.getMainLooper());
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                900,
                500,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.OPAQUE
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;
        // init the imageview
        windowManager.addView(view, params);
    }
    public void stopScreenCapture() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }
    public void destroy(WindowManager windowManager){
        stopScreenCapture();
        if (windowManager != null && screenMirror != null) {
            try {
                windowManager.removeView((View) screenMirror.getParent());
            } catch (Exception e) {
                Log.e("OverlayService", "Error removing overlay view: " + e.getMessage());
            }
            windowManager = null;
        }

    }

    public Surface getSurface() {
        return surface;
    }
}
