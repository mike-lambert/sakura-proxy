package com.cyfrant.orchidgate.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.cyfrant.orchidgate.application.ProxyApplication;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ProxyApplication app = ((ProxyApplication) context.getApplicationContext());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            boolean enabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setting_autostart", false);
            if (enabled){
                app.startProxyService();
            }
        }
    }
}
