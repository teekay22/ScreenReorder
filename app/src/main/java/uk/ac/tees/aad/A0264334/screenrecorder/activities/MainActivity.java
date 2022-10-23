package uk.ac.tees.aad.A0264334.screenrecorder.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;

import uk.ac.tees.aad.A0264334.screenrecorder.Constants;
import uk.ac.tees.aad.A0264334.screenrecorder.R;
import uk.ac.tees.aad.A0264334.screenrecorder.fragments.AboutFragment;
import uk.ac.tees.aad.A0264334.screenrecorder.fragments.HomeFragment;
import uk.ac.tees.aad.A0264334.screenrecorder.fragments.MyRecordingsFragment;
import uk.ac.tees.aad.A0264334.screenrecorder.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {
    public NavigationView nvDrawer;
    private DrawerLayout mDrawer;
    public ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, R.string.drawer_open, R.string.drawer_close);
        mDrawer.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        setupDrawerContent(nvDrawer);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        loadFragment(new HomeFragment());
    }
    @Override
    protected void onNewIntent(Intent intent) {
        checkAction(intent);
        super.onNewIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            final AlertDialog alertDialog = builder.create();
            builder.setMessage("Are you sure you want to exit?")
                    .setCancelable(false)
                    .setTitle("Close App ?")
                    .setPositiveButton("Yes", (dialog, id) -> finishAffinity())
                    .setNegativeButton("No", (dialog, which) -> alertDialog.dismiss()).show();
        }
    }

    private void checkAction(Intent intent) {
        String action = intent.getAction();
        if ("record_intent".equals(action)) {
            Constants.notificationrecordclick = true;
            loadFragment(new HomeFragment());
        }
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        Fragment fragment = null;
        switch(menuItem.getItemId()) {
            case R.id.nav_recordings:
                loadFragment(new MyRecordingsFragment());
                break;
            case R.id.nav_settings:
                loadFragment(new SettingsFragment());
                break;
            case R.id.nav_about:
                loadFragment(new AboutFragment());
                break;
            default:
                loadFragment(new HomeFragment());
        }
        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);
        // Set action bar title
        setTitle(menuItem.getTitle());
        // Close the navigation drawer
        mDrawer.closeDrawers();
    }

    public void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame_layout, fragment);
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.commit();
    }
}