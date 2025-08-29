package com.example.explicitapp3;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class OverlayFunctions {
    private MediaProjectionManager mediaProjectionManager;
    WindowManager wm;
    private MediaProjection mediaProjection;
    public Notification notification;
    Surface surface;
    TextureView screenMirror;
    VirtualDisplay mVirtualDisplay;
    Context mcontext;
    int dpi;

    // these are for screenshot
    ImageReader imageReader;
    Handler handler;
    HandlerThread handlerThread;
    private static  final String TAG = "OverlayFunctions";


    public MediaProjectionManager setMediaProjectionManager(MediaProjectionManager mediaProjectionManager) {
        this.mediaProjectionManager = mediaProjectionManager;
        return this.mediaProjectionManager;
    }

    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public void setWindowManager(WindowManager windowManager) {
        this.wm = windowManager;
    }

    public Notification setNotification(Context context) {
        NotificationHelper.createNotificationChannel(context);
        notification = NotificationHelper.createNotification(context);
        mcontext = context;
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
        int[] boundsRes = getBounds();
        int width = boundsRes[0];
        int height = boundsRes[1];
        mVirtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                width == 0 ? DisplayMetrics.DENSITY_DEFAULT : width,
                height == 0 ? DisplayMetrics.DENSITY_DEFAULT : height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                handler
        );
    }
    public int[] getBounds(){
        WindowMetrics windowMetrics = wm.getCurrentWindowMetrics();
        Rect bounds = windowMetrics.getBounds();
        int width = bounds.width();
        int height = bounds.height();
        return new int[] {width, height};
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
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                Bitmap b = screenMirror.getBitmap();
                saveToGalleryBitmap(b);
            }
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
        // y 26 for samsung
        // y 40 for google pixel 9
        params.x = 0;
        params.y = -40;
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

    @SuppressLint("ClickableViewAccessibility")
    public void setupOverlayScreenshot(){
        handlerThread = new HandlerThread("ScreenCapture");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        int[] boundsRes = getBounds();
        imageReader = ImageReader.newInstance(
                boundsRes[0],
                boundsRes[1],
                PixelFormat.RGBA_8888,
                2
        );

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null){
                    // save the image for now, deal with it l;ater
                    Bitmap bitmap = imageToBitmap(image);

                    // TODO fit the image/bitmap into the trained model, (e.g yuvImage & tflite interpreter
                    // TODO here:
                    // scale the bitmap, then normalize the rgb channels (float conversion) then put it into model
                    // TODO link: https://firebase.google.com/docs/ml/android/use-custom-models#java_3



                    // OPTIONAL (SAVE TO GALLERY)
                    saveToGalleryBitmap(bitmap);
                    image.close();
                    bitmap.recycle();
                    Log.w(TAG, "onImageAvailable: Running imagee" );
                }
            }
        }, handler);
        surface = imageReader.getSurface();
        if (mediaProjection != null) startScreenCapture();
    }
    public Bitmap imageToBitmap(Image image){
        Image.Plane[] planes=image.getPlanes();
        ByteBuffer buffer=planes[0].getBuffer();
        int pixelStride=planes[0].getPixelStride();
        int rowStride=planes[0].getRowStride();
        int rowPadding=rowStride - pixelStride * image.getWidth();
        int bitmapWidth=image.getWidth() + rowPadding / pixelStride;
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, image.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }
    public void saveToGalleryBitmap(Bitmap bitmap){
        Log.w(TAG, "onSurfaceTextureUpdated: hi..." );


        // can configure the bitmap here
        if (bitmap != null) {
            // save to gallery
            Paint paint = new Paint();
            paint.setColorFilter(new PorterDuffColorFilter(0x55FF0000, PorterDuff.Mode.SRC_IN));
            Bitmap res = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(res);
            canvas.drawBitmap(res, 0, 0, paint);

            try {
                File dir = mcontext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File file = new File(dir, "screenshot_"+ System.currentTimeMillis() +".png");
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG,100, fos);
                fos.close();
                Log.w(TAG, "onSurfaceTextureUpdated: running..." );
            } catch (FileNotFoundException e) {
                Log.e(TAG, "onSurfaceTextureUpdated: NotFoundError " + e.getMessage() );
                throw new RuntimeException(e);
            } catch (IOException e) {
                Log.e(TAG, "onSurfaceTextureUpdated: IOError " + e.getMessage() );
                throw new RuntimeException(e);
            } finally {
                res.recycle();
            }

        }
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

        if (handlerThread != null){
            handlerThread.quitSafely();
            try {
                handlerThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            handlerThread = null;
            handler = null;
        }
        if (imageReader != null){
            imageReader.close();
            imageReader = null;
        }

    }

    public Surface getSurface() {
        return surface;
    }
}
