package com.example.explicitapp3;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class OverlayFunctions {
    private MediaProjectionManager mediaProjectionManager;
    WindowManager wm;
    private MediaProjection mediaProjection;
    public Notification notification;
    Surface surface;
    TextureView screenMirror;
    VirtualDisplay mVirtualDisplay;
    int dpi;

    public MediaProjectionManager setMediaProjectionManager(MediaProjectionManager mediaProjectionManager) {
        this.mediaProjectionManager = mediaProjectionManager;
        return this.mediaProjectionManager;
    }

    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public void setWindowManager(WindowManager windowManager) {
        this.wm = windowManager;
    }

    public Notification setNotification(Context context) {
        NotificationHelper.createNotificationChannel(context);
        notification = NotificationHelper.createNotification(context);
        return notification;
    }

    public void startScreenCapture() {
        MediaProjection.Callback callback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                stopScreenCapture();
            }
        };
        mediaProjection.registerCallback(callback, null);
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        WindowMetrics windowMetrics = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowMetrics = wm.getCurrentWindowMetrics();
        }
        Rect bounds = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bounds = windowMetrics.getBounds();
        }
        int width = bounds.width();
        int height = bounds.height();
        mVirtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                width == 0 ? DisplayMetrics.DENSITY_DEFAULT : width,
                height == 0 ? DisplayMetrics.DENSITY_DEFAULT : height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null //mHandler
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setupOverlay(MediaProjection mMediaProjection, int dpi, LayoutInflater lf) {
        // this is the overlay view
        this.dpi = dpi;
        View view = lf.inflate(R.layout.activity_overlay, null);
        screenMirror = view.findViewById(R.id.screenMirror);
        screenMirror.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                surface = new Surface(surfaceTexture);
                if (mediaProjection != null) startScreenCapture();
            }
            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                stopScreenCapture();
                return true;
            }
            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
        });
        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        ,
                PixelFormat.TRANSLUCENT
        );

//        params.gravity = Gravity.TOP;
        // -1080
        Log.w("OverlayFunctions", "setupOverlay displaymetric is: "+ -displayMetrics.widthPixels);
        params.x = 0;
        params.y = 26;
        view.setClickable(false);
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
        view.setEnabled(false);
        screenMirror.setClickable(false);
        screenMirror.setFocusable(false);
        screenMirror.setFocusableInTouchMode(false);
        screenMirror.setEnabled(false);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }

        });
        screenMirror.setOnTouchListener((v, event) -> {
            ;
            return false;
        });

        // init the imageview
        wm.addView(view, params);
    }

    public void stopScreenCapture() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

    public void destroy() {
        stopScreenCapture();
        if (wm != null && screenMirror != null) {
            try {
                wm.removeView((View) screenMirror.getParent());
            } catch (Exception e) {
                Log.e("OverlayService", "Error removing overlay view: " + e.getMessage());
            }
            wm = null;
        }

    }

    public Surface getSurface() {
        return surface;
    }
}
