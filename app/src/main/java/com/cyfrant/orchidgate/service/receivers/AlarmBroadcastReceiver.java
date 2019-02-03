package com.cyfrant.orchidgate.service.receivers;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.cyfrant.orchidgate.application.ProxyApplication;

public class AlarmBroadcastReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager.WakeLock screenWakeLock = null;
        ProxyApplication app = ((ProxyApplication) context.getApplicationContext());
        if (screenWakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            screenWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "Sakura:Receiver");
            screenWakeLock.acquire();
        }
        app.keepAlive();
        if (app.isProxyRunning()){
            app.scheduleKeepAliveAlarm();
        }
        if (screenWakeLock != null){
            screenWakeLock.release();
            screenWakeLock = null;
        }

    }
}
