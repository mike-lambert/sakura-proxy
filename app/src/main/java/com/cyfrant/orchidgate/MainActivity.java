package com.cyfrant.orchidgate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import com.cyfrant.orchidgate.application.ProxyApplication;
import com.cyfrant.orchidgate.fragment.StatusFragment;

public class MainActivity extends Activity {
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
                        displayProxyStatusFragment();
                        return true;

                    case R.id.menu_settings:
                        startSettingsActivity();
                        return true;

                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreView();
    }

    private void restoreView() {
        if (screen == null){
            screen = Screen.ProxyStatus;
        }
        onPrepareOptionsMenu(navigation.getMenu());
        dispatchFragment();
    }

    private void dispatchFragment() {
        switch (screen){
            case ProxyStatus:
                displayProxyStatusFragment();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);
        switch (screen){
            case ProxyStatus:
                menu.findItem(R.id.menu_status).setChecked(true);
                break;
            default:
                break;
        }
        return result;
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
}
