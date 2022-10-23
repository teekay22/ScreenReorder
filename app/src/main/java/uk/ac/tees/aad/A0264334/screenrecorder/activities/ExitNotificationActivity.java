package uk.ac.tees.aad.A0264334.screenrecorder.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import uk.ac.tees.aad.A0264334.screenrecorder.fragments.HomeFragment;

public class ExitNotificationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        HomeFragment.notificationManager.cancel(HomeFragment.NotificationID);
        HomeFragment.notificationManager.cancelAll();
        finish();
    }
}