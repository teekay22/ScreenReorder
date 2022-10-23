package uk.ac.tees.aad.A0264334.screenrecorder.fragments;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;
import com.hbisoft.hbrecorder.NotificationReceiver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import uk.ac.tees.aad.A0264334.screenrecorder.Constants;
import uk.ac.tees.aad.A0264334.screenrecorder.R;
import uk.ac.tees.aad.A0264334.screenrecorder.activities.ExitNotificationActivity;
import uk.ac.tees.aad.A0264334.screenrecorder.activities.MainActivity;
import uk.ac.tees.aad.A0264334.screenrecorder.activities.MediaProjectionPermissionActivity;
import uk.ac.tees.aad.A0264334.screenrecorder.services.FloatingWidgetService;
import uk.ac.tees.aad.A0264334.screenrecorder.services.FloatingWidgetService2;
import uk.ac.tees.aad.A0264334.screenrecorder.services.ImageRecordService;

public class HomeFragment extends Fragment implements HBRecorderListener {

    View view;

    RemoteViews notificationLayoutExpanded;
    private static Notification notification;
    public static NotificationManager notificationManager;
    public static int NotificationID = 1005;
    private static NotificationCompat.Builder mBuilder;
    static boolean mediarunning = false;

    //Permissions
    private static final int DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE = 1222;
    private static final int REQUEST_PERMISSIONS = 100;
    private static final int SCREEN_RECORD_REQUEST_CODE = 777;
    private static final int REQUEST_CODE_SCREENSHOT = 100;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private boolean hasPermissions = false;


    private HBRecorder hbRecorder;
    private com.google.android.material.floatingactionbutton.FloatingActionButton startbtn;
    //HD/SD quality
    private RadioGroup radioGroup;
    //Should record/show audio/notification
    private SwitchCompat recordAudioCheckBox;
    //Reference to checkboxes and radio buttons
    boolean wasHDSelected = true;
    boolean isAudioEnabled = true;
    //Should custom settings be used
    SwitchCompat custom_settings_switch, floating_settings_switch;
    ContentResolver resolver;
    ContentValues contentValues;
    Uri mUri;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews();
        setOnClickListeners();
        RunNotification();
        setRadioGroupCheckListener();
        setRecordAudioCheckBoxListener();
        getpermission();

        if (checkDrawOverlayPermission()) {
            startFloatingWidgetService();
        }
        floating_settings_switch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                if (checkDrawOverlayPermission()) {
                    startFloatingWidgetService();
                }
            } else {
                floating_settings_switch.setChecked(false);
                requireContext().stopService(new Intent(requireContext(), FloatingWidgetService.class));
            }
        });
        hbRecorder = new HBRecorder(requireContext(), this);

        //When the user returns to the application, some UI changes might be necessary,
        //check if recording is in progress and make changes accordingly
        if (hbRecorder.isBusyRecording()) {
            hbRecorder.stopScreenRecording();
            mediarunning = false;
            RunNotification();
            Constants.notificationrecordclick = false;
            startbtn.setImageDrawable(requireContext().getResources().getDrawable(R.drawable.ic_recode));
        }
        if (Constants.notificationrecordclick) {
            custom_settings_switch.setChecked(true);
            performclickstartbtn();
        }
        if (Constants.notificationscreenshotclick) {
            startScreenShot();
        }

        return view;
    }

    //Init Views
    private void initViews() {
        startbtn = view.findViewById(R.id.button_start);
        radioGroup = view.findViewById(R.id.radio_group);
        recordAudioCheckBox = view.findViewById(R.id.record_audio);
        custom_settings_switch = view.findViewById(R.id.custom_settings_switch);
        floating_settings_switch = view.findViewById(R.id.floating_settings_switch);
    }
    private void setOnClickListeners() {
        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performclickstartbtn();

            }
        });
    }
    public void performclickstartbtn() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
            if (hbRecorder.isBusyRecording()) {
                hbRecorder.stopScreenRecording();
                mediarunning = false;
                RunNotification();
                Constants.notificationrecordclick = false;
                startbtn.setImageDrawable(requireContext().getResources().getDrawable(R.drawable.ic_close));
            }
            //else start recording
            else {
                startRecordingScreen();
            }
        }else {
            Log.d("hasPermissions", "hasPermissions false");
            Toast.makeText(requireContext(),"Audio Recording permission denied", Toast.LENGTH_LONG).show();
        }
    }
    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }
    private void RunNotification() {

        notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(requireContext().getApplicationContext(), "notify_001");

        if (mediarunning) {
            notificationLayoutExpanded = new RemoteViews(requireContext().getPackageName(), R.layout.notification_large2);
            //stop btn clicked
            Intent stopIntent = new Intent(requireContext(), NotificationReceiver.class);
            stopIntent.setAction("STOP_ACTION");
            PendingIntent stoppendingIntent = PendingIntent.getBroadcast(requireContext(), 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
            notificationLayoutExpanded.setOnClickPendingIntent(R.id.ll_stopbtn, stoppendingIntent);

            //Pause btn clicked
            Intent pauseIntent = new Intent(requireContext(), NotificationReceiver.class);
            pauseIntent.setAction("Pause_ACTION");
            PendingIntent pausependingIntent = PendingIntent.getBroadcast(requireContext(), 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
            notificationLayoutExpanded.setOnClickPendingIntent(R.id.ll_pausebtn, pausependingIntent);
            //resume btn clicked
            Intent resumeIntent = new Intent(requireContext(), NotificationReceiver.class);
            resumeIntent.setAction("Resume_ACTION");
            PendingIntent resumependingIntent = PendingIntent.getBroadcast(requireContext(), 0, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
            notificationLayoutExpanded.setOnClickPendingIntent(R.id.ll_resume, resumependingIntent);

        } else {
            notificationLayoutExpanded = new RemoteViews(requireContext().getPackageName(), R.layout.notification_large);
            //home btn clicked
            Intent homeIntent = new Intent(requireContext(), MainActivity.class);
            PendingIntent homependingIntent = PendingIntent.getActivity(requireContext(), 0, homeIntent, 0 | PendingIntent.FLAG_IMMUTABLE);
            notificationLayoutExpanded.setOnClickPendingIntent(R.id.ll_notification_home, homependingIntent);

            //Record btn clicked
            Intent recordIntent = new Intent(requireContext(), MainActivity.class);
            recordIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            recordIntent.setAction("record_intent");
            recordIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            PendingIntent recordpendingIntent = PendingIntent.getActivity(requireContext(), 0, recordIntent, PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
            notificationLayoutExpanded.setOnClickPendingIntent(R.id.ll_record, recordpendingIntent);

            //exit btn clicked
            Intent i_exit = new Intent(requireContext(), ExitNotificationActivity.class);
            i_exit.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent exit_pendingintent = PendingIntent.getActivity(requireContext(), 0, i_exit, PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
            notificationLayoutExpanded.setOnClickPendingIntent(R.id.ll_notification_exit, exit_pendingintent);

            //Scrennshot btn clicked
            Intent ScrennshotIntent = new Intent(requireContext(), MediaProjectionPermissionActivity.class);
            ScrennshotIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent ScrennshotpendingIntent = PendingIntent.getActivity(requireContext(), 0, ScrennshotIntent, PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
            notificationLayoutExpanded.setOnClickPendingIntent(R.id.ll_screenshot, ScrennshotpendingIntent);

        }

        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setAutoCancel(false);
        mBuilder.setOngoing(true);
        mBuilder.setPriority(Notification.PRIORITY_HIGH);
        mBuilder.build().flags = Notification.FLAG_NO_CLEAR | Notification.PRIORITY_HIGH;
        mBuilder.setContent(notificationLayoutExpanded);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "channel_id";
            NotificationChannel channel = new NotificationChannel(channelId, "channel name", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }

        notification = mBuilder.build();
        notificationManager.notify(NotificationID, notification);
    }
    private void startRecordingScreen() {
        if (custom_settings_switch.isChecked()) {
            //WHEN SETTING CUSTOM SETTINGS YOU MUST SET THIS!!!
            hbRecorder.enableCustomSettings();
            customSettings();
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
            startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
            // startbtn.setText(R.string.stop_recording);

        } else {
            quickSettings();
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
            startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
            //  startbtn.setText(R.string.stop_recording);
            startbtn.setImageDrawable(requireContext().getResources().getDrawable(R.drawable.ic_close));

        }
    }
    private void customSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        //Is audio enabled
        boolean audio_enabled = prefs.getBoolean("key_record_audio", true);
        hbRecorder.isAudioEnabled(audio_enabled);

        //Audio Source
        String audio_source = prefs.getString("key_audio_source", null);
        if (audio_source != null) {
            switch (audio_source) {
                case "0":
                    hbRecorder.setAudioSource("DEFAULT");
                    break;
                case "1":
                    hbRecorder.setAudioSource("CAMCODER");
                    break;
                case "2":
                    hbRecorder.setAudioSource("MIC");
                    break;
            }
        }

        //Video Encoder
        String video_encoder = prefs.getString("key_video_encoder", null);
        if (video_encoder != null) {
            switch (video_encoder) {
                case "0":
                    hbRecorder.setVideoEncoder("DEFAULT");
                    break;
                case "1":
                    hbRecorder.setVideoEncoder("H264");
                    break;
                case "2":
                    hbRecorder.setVideoEncoder("H263");
                    break;
                case "3":
                    hbRecorder.setVideoEncoder("HEVC");
                    break;
                case "4":
                    hbRecorder.setVideoEncoder("MPEG_4_SP");
                    break;
                case "5":
                    hbRecorder.setVideoEncoder("VP8");
                    break;
            }
        }

        //NOTE - THIS MIGHT NOT BE SUPPORTED SIZES FOR YOUR DEVICE
        //Video Dimensions
        String video_resolution = prefs.getString("key_video_resolution", null);
        if (video_resolution != null) {
            switch (video_resolution) {
                case "0":
                    hbRecorder.setScreenDimensions(426, 240);
                    break;
                case "1":
                    hbRecorder.setScreenDimensions(640, 360);
                    break;
                case "2":
                    hbRecorder.setScreenDimensions(854, 480);
                    break;
                case "3":
                    hbRecorder.setScreenDimensions(1280, 720);
                    break;
                case "4":
                    hbRecorder.setScreenDimensions(1920, 1080);
                    break;
            }
        }

        //Video Frame Rate
        String video_frame_rate = prefs.getString("key_video_fps", null);
        if (video_frame_rate != null) {
            switch (video_frame_rate) {
                case "0":
                    hbRecorder.setVideoFrameRate(60);
                    break;
                case "1":
                    hbRecorder.setVideoFrameRate(50);
                    break;
                case "2":
                    hbRecorder.setVideoFrameRate(48);
                    break;
                case "3":
                    hbRecorder.setVideoFrameRate(30);
                    break;
                case "4":
                    hbRecorder.setVideoFrameRate(25);
                    break;
                case "5":
                    hbRecorder.setVideoFrameRate(24);
                    break;
            }
        }

        //Video Bitrate
        String video_bit_rate = prefs.getString("key_video_bitrate", null);
        if (video_bit_rate != null) {
            switch (video_bit_rate) {
                case "1":
                    hbRecorder.setVideoBitrate(12000000);
                    break;
                case "2":
                    hbRecorder.setVideoBitrate(8000000);
                    break;
                case "3":
                    hbRecorder.setVideoBitrate(7500000);
                    break;
                case "4":
                    hbRecorder.setVideoBitrate(5000000);
                    break;
                case "5":
                    hbRecorder.setVideoBitrate(4000000);
                    break;
                case "6":
                    hbRecorder.setVideoBitrate(2500000);
                    break;
                case "7":
                    hbRecorder.setVideoBitrate(1500000);
                    break;
                case "8":
                    hbRecorder.setVideoBitrate(1000000);
                    break;
            }
        }

        //Output Format
        String output_format = prefs.getString("key_output_format", null);
        if (output_format != null) {
            switch (output_format) {
                case "0":
                    hbRecorder.setOutputFormat("DEFAULT");
                    break;
                case "1":
                    hbRecorder.setOutputFormat("MPEG_4");
                    break;
                case "2":
                    hbRecorder.setOutputFormat("THREE_GPP");
                    break;
                case "3":
                    hbRecorder.setOutputFormat("WEBM");
                    break;
            }
        }

    }
    private void quickSettings() {
        hbRecorder.setAudioBitrate(128000);
        hbRecorder.setAudioSamplingRate(44100);
        hbRecorder.recordHDVideo(wasHDSelected);
        hbRecorder.isAudioEnabled(isAudioEnabled);
        //Customise Notification
        hbRecorder.setNotificationSmallIcon(drawable2ByteArray(R.drawable.icon));
        hbRecorder.setNotificationTitle("Recording your screen");
        hbRecorder.setNotificationDescription("Drag down to stop the recording");

    }
    private byte[] drawable2ByteArray(@DrawableRes int drawableId) {
        Bitmap icon = BitmapFactory.decodeResource(getResources(), drawableId);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
    private void setRadioGroupCheckListener() {
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId) {
                    case R.id.hd_button:
                        //Ser HBRecorder to HD
                        wasHDSelected = true;

                        break;
                    case R.id.sd_button:
                        //Ser HBRecorder to SD
                        wasHDSelected = false;
                        break;
                }
            }
        });
    }
    private void setRecordAudioCheckBoxListener() {
        recordAudioCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //Enable/Disable audio
                isAudioEnabled = isChecked;
            }
        });
    }
    public void getpermission() {
        if (( ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) && ( ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED )) {
            if (( ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) ) && ( ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) )) {

            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSIONS);
            }
        } else {
            createscreenshotfolder();
        }
    }
    public void createscreenshotfolder() {
        File f2 = new File(Constants.pathScreenShotDirectory);
        if (!f2.exists()) {
            if (f2.mkdirs()) {
                Log.i("Folder ", "created");
            }
        }
    }
    public boolean checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (!Settings.canDrawOverlays(requireContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + requireContext().getPackageName()));
            startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE);
            return false;
        } else {
            return true;
        }
    }
    public void startScreenShot() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
            MediaProjectionManager mProjectionManager =
                    (MediaProjectionManager) requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREENSHOT);
        } else {
            Toast.makeText(requireContext(),"permission not granted", Toast.LENGTH_LONG).show();
        }

    }
    private void startFloatingWidgetService() {
        Constants.isfloatingswitchEnabled = true;
        floating_settings_switch.setChecked(Constants.isfloatingswitchEnabled);
        requireContext().startService(new Intent(requireContext(), FloatingWidgetService.class));
    }
    private void createFolder() {
        File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Screen Recorder");
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                Log.i("Folder ", "created");
            }
        }
    }
    private String generateFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate).replace(" ", "");
    }
    private void setOutputPath() {
        String filename = generateFileName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = requireContext().getContentResolver();
            contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Screen Recorder");
            contentValues.put(MediaStore.Video.Media.TITLE, filename);
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            //FILE NAME SHOULD BE THE SAME
            hbRecorder.setFileName(filename);
            hbRecorder.setOutputUri(mUri);
        } else {
            createFolder();
            hbRecorder.setOutputPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/Screen Recorder");
            //hbRecorder.setOutputPath(Constance.PathFileDirectory);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void updateGalleryUri() {
        contentValues.clear();
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);
        requireActivity().getContentResolver().update(mUri, contentValues, null, null);
    }
    private void refreshGalleryFile() {
        MediaScannerConnection.scanFile(requireContext(),
                new String[]{hbRecorder.getFilePath()},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        floating_settings_switch.setChecked(Constants.isfloatingswitchEnabled);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
                } else {
                    hasPermissions = false;
                    Toast.makeText(requireContext(),"No permission for " + Manifest.permission.RECORD_AUDIO, Toast.LENGTH_LONG).show();
                }
                break;
            case PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPermissions = true;
                    /*//Permissions was provided
                    //Start screen recording
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startRecordingScreen();
                    }*/
                } else {
                    hasPermissions = false;
                    Toast.makeText(requireContext(),"No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE, Toast.LENGTH_LONG).show();
                }
                break;
            case REQUEST_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createscreenshotfolder();
                } else {
                    Toast.makeText(requireContext(),"No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE, Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    //Set file path or Uri depending on SDK version
                    setOutputPath();
                    //Start screen recording
                    hbRecorder.startScreenRecording(data, resultCode, getActivity());
                    Toast.makeText(requireContext(), "Recording Start...", Toast.LENGTH_LONG).show();
                    mediarunning = true;
                    RunNotification();
                    Constants.notificationrecordclick = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(requireContext())) {
                            requireContext().stopService(new Intent(requireContext(), FloatingWidgetService.class));
                            requireContext().startService(new Intent(requireContext(), FloatingWidgetService2.class));
                        }
                    }
                    startbtn.setImageDrawable(requireContext().getResources().getDrawable(R.drawable.ic_close));
                } else {
                    Constants.notificationrecordclick = false;
                    startbtn.setImageDrawable(requireContext().getResources().getDrawable(R.drawable.ic_recode));
                }
            }
            if (requestCode == REQUEST_CODE_SCREENSHOT) {
                if (resultCode == RESULT_OK) {
                    Log.d("screenshot_check", "REQUEST_CODE_SCREENSHOT :");
                    requireContext().startService(ImageRecordService.getStartIntent(requireContext(), resultCode, data, Constants.pathScreenShotDirectory));
                    Constants.notificationscreenshotclick = false;
                } else {
                    Log.d("check_permission", "screen capture permission cancel");
                }
            }
            if (requestCode == DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(requireContext())) {
                        startFloatingWidgetService();
                    } else {
                        Constants.isfloatingswitchEnabled = false;
                        floating_settings_switch.setChecked(false);
                    }
                }

            }

        }
    }


    @Override
    public void HBRecorderOnStart() {

    }

    @Override
    public void HBRecorderOnComplete() {
        mediarunning = false;
        RunNotification();
        Constants.notificationrecordclick = false;
        startbtn.setImageDrawable(requireContext().getResources().getDrawable(R.drawable.ic_recode));
        Toast.makeText(requireContext(), "Recording Saved Successfully", Toast.LENGTH_LONG).show();
        //Update gallery depending on SDK Level
        if (hbRecorder.wasUriSet()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                updateGalleryUri();
            }
        } else {
            refreshGalleryFile();
        }
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        if (errorCode == 38) {
            Toast.makeText(requireContext(), "Some settings are not supported by your device", Toast.LENGTH_LONG).show();
            mediarunning = false;
            RunNotification();
            Constants.notificationrecordclick = false;
        } else {
            Toast.makeText(requireContext(), "RecorderOnError - See Log", Toast.LENGTH_LONG).show();
            mediarunning = false;
            RunNotification();
            Constants.notificationrecordclick = false;
            Log.e("HBRecorderOnError", reason);
        }
        //startbtn.setText(R.string.start_recording);
        startbtn.setImageDrawable(requireContext().getResources().getDrawable(R.drawable.ic_recode));
    }

}