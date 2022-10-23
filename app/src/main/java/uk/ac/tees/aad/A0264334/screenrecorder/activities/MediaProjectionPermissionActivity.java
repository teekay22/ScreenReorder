package uk.ac.tees.aad.A0264334.screenrecorder.activities;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import uk.ac.tees.aad.A0264334.screenrecorder.Constants;
import uk.ac.tees.aad.A0264334.screenrecorder.services.ImageRecordService;

public class MediaProjectionPermissionActivity extends Activity {
    private static final int REQUEST_SCREENSHOT = 59706;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MediaProjectionManager mgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mgr.createScreenCaptureIntent(),
                REQUEST_SCREENSHOT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCREENSHOT) {
            if (resultCode == RESULT_OK) {
                startService(ImageRecordService.getStartIntent(MediaProjectionPermissionActivity.this, resultCode, data, Constants.pathScreenShotDirectory));
            }

        }

    }
}