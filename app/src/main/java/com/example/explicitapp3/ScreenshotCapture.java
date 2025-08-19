package com.example.explicitapp3;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenshotCapture {
    private static final String TAG = "ScreenshotCapture";

    private Context context;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    // Screenshot settings
    private static final int SCREEN_WIDTH = 1080;
    private static final int SCREEN_HEIGHT = 1920;
    private static final int IMAGE_FORMAT = PixelFormat.RGBA_8888;
    private static final int MAX_IMAGES = 2;

    public ScreenshotCapture(Context context) {
        this.context = context;
        startBackgroundThread();
        setupImageReader();
        Log.w(TAG, "ScreenshotCapture: Running");
    }
//DONT USE CODE DEPRECATED WHUDWUDHWUHD
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ScreenshotBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }

    private void setupImageReader() {
        imageReader = ImageReader.newInstance(SCREEN_WIDTH, SCREEN_HEIGHT, IMAGE_FORMAT, MAX_IMAGES);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        Log.w(TAG, "onImageAvailable: The image not null");
                        saveImageToFile(image);
                    } else if (image == null){
                        Log.w(TAG, "onImageAvailable: The image is null");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing image", e);
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }
        }, backgroundHandler);
    }

    private void saveImageToFile(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * SCREEN_WIDTH;

        // Create bitmap
        Bitmap bitmap = Bitmap.createBitmap(
                SCREEN_WIDTH + rowPadding / pixelStride,
                SCREEN_HEIGHT,
                Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);

        // Crop bitmap if there's padding
        if (rowPadding != 0) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        }

        // Save to file
        try {
            // Save to public Pictures directory (easier to find)
            File screenshotDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots");
            Log.d(TAG, "Screenshot directory: " + screenshotDir.getAbsolutePath());

            if (!screenshotDir.exists()) {
                boolean created = screenshotDir.mkdirs();
                Log.d(TAG, "Directory created: " + created);
            }

            String fileName = "screenshot_" + System.currentTimeMillis() + ".png";
            File imageFile = new File(screenshotDir, fileName);
            Log.d(TAG, "Attempting to save to: " + imageFile.getAbsolutePath());

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Log.d(TAG, "Screenshot saved successfully: " + imageFile.getAbsolutePath());
            Log.d(TAG, "File exists: " + imageFile.exists());
            Log.d(TAG, "File size: " + imageFile.length() + " bytes");

            // Optional: Add to media store so it appears in gallery
            addToMediaStore(imageFile);

        } catch (IOException e) {
            Log.e(TAG, "Error saving screenshot", e);
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    private void addToMediaStore(File imageFile) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

            context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            Log.e(TAG, "Error adding to media store", e);
        }
    }

    public void startCapture(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;

        if (mediaProjection == null || imageReader == null) {
            Log.e(TAG, "MediaProjection or ImageReader is null");
            return;
        }

        MediaProjection.Callback callback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                stopCapture();
            }
        };
        mediaProjection.registerCallback(callback, backgroundHandler);

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                SCREEN_WIDTH,
                SCREEN_HEIGHT,
                context.getResources().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                backgroundHandler
        );

        Log.d(TAG, "Screen capture started");
    }

    public void stopCapture() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
            Log.d(TAG, "Screen capture stopped");
        }
    }

    public void destroy() {
        stopCapture();

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        stopBackgroundThread();
    }
}