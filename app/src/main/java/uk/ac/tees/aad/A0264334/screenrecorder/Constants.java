package uk.ac.tees.aad.A0264334.screenrecorder;

import android.os.Environment;

public class Constants {

    public static boolean isfloatingswitchEnabled = false;
    public static boolean notificationrecordclick = false;
    public static boolean notificationscreenshotclick = false;

    public static String PathFileDirectory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/Screen Recorder";
    public static String pathScreenShotDirectory=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath()+"/DemoScreenShots/";
}
