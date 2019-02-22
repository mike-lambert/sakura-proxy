package com.cyfrant.orchidgate;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cyfrant.orchidgate.application.ProxyApplication;
import com.cyfrant.orchidgate.fragment.NetworkStatusFragment;
import com.cyfrant.orchidgate.fragment.StatusFragment;
import com.cyfrant.orchidgate.updater.Updates;

public class MainActivity extends Activity {
    private static final int REQUEST_ACCESS_STORAGE = 0x00000010;
    private static final String KEY_SCREEN = "screen";
    private enum Screen {
        ProxyStatus,
        NetworkStatus,
        Settings
    }
    private RelativeLayout viewport;
    private BottomNavigationView navigation;
    private Screen screen;

    protected ProxyApplication getProxyApplication() {
        return (ProxyApplication) getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewport = findViewById(R.id.viewport);
        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_status:
                        screen = Screen.ProxyStatus;
                        dispatchFragment();
                        return true;

                    case R.id.menu_settings:
                        startSettingsActivity();
                        navigation.getMenu().findItem(R.id.menu_settings).setChecked(false);
                        return true;

                    case R.id.menu_network:
                        screen = Screen.NetworkStatus;
                        dispatchFragment();
                        return true;

                    case R.id.menu_update:
                        checkPermissionsAndRequestUpdates();
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String screenData = getIntent().getStringExtra(KEY_SCREEN);
        screen = Screen.valueOf((screenData == null ? Screen.ProxyStatus.toString() : screenData));
        restoreView();
        Updates.checkAndRequestInstallUpdates(getProxyApplication(), false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getIntent().putExtra(KEY_SCREEN, screen.toString());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getIntent().putExtra(KEY_SCREEN, screen.toString());
        outState.putString(KEY_SCREEN, screen.toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String data = savedInstanceState.getString(KEY_SCREEN, Screen.ProxyStatus.toString());
        screen = Screen.valueOf(data);
        getIntent().putExtra(KEY_SCREEN, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length == 2
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Updates.checkAndRequestInstallUpdates(getProxyApplication(), true);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, R.string.notification_storage_permission_denied, Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void displayProxyStatusFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.viewport, StatusFragment.newInstance())
                .addToBackStack("proxy")
                .commit();
    }

    private void displayNetworkStatusFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.viewport, NetworkStatusFragment.newInstance())
                .addToBackStack("status")
                .commit();
    }

    private void restoreView() {
        if (screen == null){
            screen = Screen.ProxyStatus;
        }
        dispatchFragment();
    }

    private void dispatchFragment() {
        switch (screen){
            case ProxyStatus:
                checkMenuItem(navigation.getMenu().findItem(R.id.menu_status));
                displayProxyStatusFragment();
                break;
            case NetworkStatus:
                checkMenuItem(navigation.getMenu().findItem(R.id.menu_network));
                displayNetworkStatusFragment();
                break;
            default:
                break;
        }
    }

    private void checkMenuItem(MenuItem item){
        for(int i = 0; i < navigation.getMenu().size(); i++){
            MenuItem next = navigation.getMenu().getItem(i);
            if (next.equals(item)){
                continue;
            }
            next.setChecked(false);
        }
        item.setChecked(true);
    }

    private void checkPermissionsAndRequestUpdates() {
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                || PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_ACCESS_STORAGE
            );
        } else {
            Updates.checkAndRequestInstallUpdates(getProxyApplication(), true);
        }
    }
}
