package com.cyfrant.orchidgate.service.receivers;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.cyfrant.orchidgate.application.ProxyApplication;
import com.cyfrant.orchidgate.updater.Updates;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UpdateTaskReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ProxyApplication app = ((ProxyApplication) context.getApplicationContext());
        boolean enabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setting_update_enabled", true);
        long lastCheck = PreferenceManager.getDefaultSharedPreferences(context).getLong("setting_update_last_check", 0);
        String value = PreferenceManager.getDefaultSharedPreferences(context).getString("setting_update_interval", "1800");
        long interval = (Long.parseLong(value) * 1000L);
        long now = System.currentTimeMillis();
        if (lastCheck == 0) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong("setting_update_last_check", now)
                    .apply();
            app.scheduleAutoUpdate();
        }
        if (lastCheck + interval >= now) {
            Log.d("Updates",
                    "Updates tracked at " + dateFormat.format(new Date(lastCheck)) +
                            " next attempt at " + dateFormat.format(new Date(lastCheck + interval))
            );
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong("setting_update_last_check", now)
                .apply();
        app.scheduleAutoUpdate();
        if (enabled) {
            Log.d("Updates", "Scheduled updates check: " + dateFormat.format(new Date(now)));
            Updates.checkAndRequestInstallUpdates(app);
        }
    }
}
