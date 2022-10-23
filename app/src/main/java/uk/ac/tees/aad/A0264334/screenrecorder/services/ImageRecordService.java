package uk.ac.tees.aad.A0264334.screenrecorder.services;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Objects;

import uk.ac.tees.aad.A0264334.screenrecorder.NotificationUtils;

public class ImageRecordService extends Service {
    private static final String TAG = "ScreenCaptureService";
    public static final String RESULT_CODE = "RESULT_CODE";
    public static final String DATA = "DATA";
    private static final String ACTION = "ACTION";
    private static final String START = "START";
    private static final String STOP = "STOP";
    private static final String SCREENCAP_NAME = "screencap";
    File storeDirectory;
    public MediaProjection mMediaProjection;
    private String mStoreDir;
    private ImageReader mImageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    Point windowSize;
    public Bitmap bitmap;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isStartCommand(intent)) {
            // create notification
            Pair<Integer, Notification> notification = NotificationUtils.getNotification(this);
            startForeground(notification.first, notification.second);
            int resultCode = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED);
            Intent data = intent.getParcelableExtra(DATA);
            mStoreDir = intent.getStringExtra("path_directory");
            startProjection(resultCode, data);
        } else if (isStopCommand(intent)) {
            stopProjection();
            stopSelf();
        } else {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    public static Intent getStartIntent(Context context, int resultCode, Intent data, String directory) {
        Intent intent = new Intent(context, ImageRecordService.class);
        intent.putExtra(ACTION, START);
        intent.putExtra(RESULT_CODE, resultCode);
        Log.d("screenshot_check", "getStartIntent");
        intent.putExtra(DATA, data);
        intent.putExtra("path_directory", directory);
        return intent;
    }
    private static boolean isStartCommand(Intent intent) {
        return intent.hasExtra(RESULT_CODE) && intent.hasExtra(DATA);
    }

    private static boolean isStopCommand(Intent intent) {
        return intent.hasExtra(ACTION) && Objects.equals(intent.getStringExtra(ACTION), STOP);
    }

    private static int getVirtualDisplayFlags() {
        //return DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
        return DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;
    }
    private void startProjection(int resultCode, Intent data) {
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mMediaProjection == null) {
            mMediaProjection = mpManager.getMediaProjection(resultCode, data);
            if (mMediaProjection != null) {
                // display metrics
                mDensity = Resources.getSystem().getDisplayMetrics().densityDpi;
                WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                mDisplay = windowManager.getDefaultDisplay();
                //For height and width
                windowSize = new Point();
                windowManager.getDefaultDisplay().getRealSize(windowSize);
                mWidth = windowSize.x;
                mHeight = windowSize.y;
                // create virtual display depending on device width / height
                createVirtualDisplay();
            }
        }
    }
    private void stopProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
        if (mVirtualDisplay != null) mVirtualDisplay.release();
        if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
    }
    private static void scanFile(Context context, Uri imageUri) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(imageUri);
        context.sendBroadcast(scanIntent);
    }
    @SuppressLint("WrongConstant")
    private void createVirtualDisplay() {
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 1);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight,
                mDensity, getVirtualDisplayFlags(), mImageReader.getSurface(), null, null);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            FileOutputStream fos = null;
            bitmap = null;
            try {
                image = mImageReader.acquireNextImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;
                    // create bitmap
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    // fix the extra width from Image
                    Bitmap croppedBitmap;
                    try {
                        croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, mWidth, mHeight);
                    } catch (OutOfMemoryError e) {
                        croppedBitmap = bitmap;
                    }
                    if (croppedBitmap != bitmap) {
                        bitmap.recycle();
                    }
                    String fileName = Calendar.getInstance().getTime() + ".png";
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/");
                    } else {
                        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                        File file = new File(directory, fileName);
                        values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
                    }
                    Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                    }
                    stopProjection();
                    stopSelf();
                    scanFile(getApplicationContext(), uri);
                    image.close();
                    Toast.makeText(getApplicationContext(), "Screenshot captured", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                e.printStackTrace();
            }
        }
    }

}