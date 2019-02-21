package com.cyfrant.orchidgate.service.receivers;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.cyfrant.orchidgate.application.ProxyApplication;
import com.cyfrant.orchidgate.updater.Updates;

public class UpdateTaskReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ProxyApplication app = ((ProxyApplication) context.getApplicationContext());
        app.scheduleAutoUpdate();
        boolean enabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setting_update_enabled", false);
        if (enabled) {
            Updates.checkAndRequestInstallUpdates(app);
        }
    }
}
