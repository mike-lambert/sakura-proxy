package com.cyfrant.orchidgate.service.receivers;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.cyfrant.orchidgate.application.ProxyApplication;

public class PingTaskReceiver extends WakefulBroadcastReceiver {
    private PowerManager.WakeLock screenWakeLock = null;
    @Override
    public void onReceive(Context context, Intent intent) {
        ProxyApplication app = ((ProxyApplication) context.getApplicationContext());
        if (screenWakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            screenWakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE,
                    "Sakura:Receiver");
            screenWakeLock.setReferenceCounted(false);
        }
        screenWakeLock.acquire();
        app.keepAlive();
        if (app.isActive()){
            app.scheduleKeepAliveAlarm();
        }

        if (screenWakeLock != null){
            screenWakeLock.release();
            screenWakeLock = null;
        }

    }
}
