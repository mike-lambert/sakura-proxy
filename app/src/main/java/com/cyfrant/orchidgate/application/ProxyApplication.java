package com.cyfrant.orchidgate.application;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceManager;

import com.cyfrant.orchidgate.contract.Proxy;
import com.cyfrant.orchidgate.contract.ProxyController;
import com.cyfrant.orchidgate.contract.ProxyStatusCallback;
import com.cyfrant.orchidgate.service.Background;
import com.cyfrant.orchidgate.service.ProxyManager;
import com.cyfrant.orchidgate.service.ProxyService;
import com.cyfrant.orchidgate.service.receivers.PingTaskBroadcastReceiver;
import com.cyfrant.orchidgate.service.receivers.UpdateTaskReceiver;
import com.subgraph.orchid.Tor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProxyApplication extends Application implements ProxyController, ProxyStatusCallback {
    public static int REQUEST_RECEIVER = 4;
    private ProxyManager proxyManager;
    private List<ProxyStatusCallback> observers;
    private boolean active;
    protected boolean mIsBound;
    protected ProxyService.ProxyServiceConnection mConnection;

    // ProxyController
    @Override
    public void startProxyService() {
        active = true;
        instantiateProxyService();
        proxyManager.proxyStart(this);
    }

    @Override
    public boolean isProxyRunning() {
        return proxyManager != null && proxyManager.connectProbe();
    }

    @Override
    public void stopProxyService() {

        ProxyService svc = mConnection.getService();
        if (svc != null) {
            svc.shutdown();
            doUnbindService();
        }

        if (isActive()) {
            proxyManager.proxyStop();
        }
        active = false;
    }

    @Override
    public Proxy getProxy() {
        return proxyManager;
    }

    @Override
    public void keepAlive() {
        if (isActive()){
            proxyManager.keepAlive();
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    // Application
    @Override
    public void onCreate() {
        super.onCreate();
        observers = new CopyOnWriteArrayList<>();
        proxyManager = new ProxyManager();
        Tor.setTorFaultCallback(this);
        scheduleAutoUpdate();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        stopProxyService();
        observers.clear();
    }

    // ProxyStatusCallback
    @Override
    public void onStartup(final int percentage, final String message) {
        for (final ProxyStatusCallback observer : observers) {
            Background.threadPool().submit(new Runnable() {
                @Override
                public void run() {
                    observer.onStartup(percentage, message);
                }
            });
        }
    }

    @Override
    public void onStarted(final int port) {
        for (final ProxyStatusCallback observer : observers) {
            Background.threadPool().submit(new Runnable() {
                @Override
                public void run() {
                    observer.onStarted(port);
                }
            });
        }
    }

    @Override
    public void onStopped(final String cause) {
        for (final ProxyStatusCallback observer : observers) {
            Background.threadPool().submit(new Runnable() {
                @Override
                public void run() {
                    observer.onStopped(cause);
                }
            });
        }
    }

    @Override
    public void onHeartbeat(final double delayUp, final double delayDown, final String exitAddress) {
        for (final ProxyStatusCallback observer : observers) {
            Background.threadPool().submit(new Runnable() {
                @Override
                public void run() {
                    observer.onHeartbeat(delayUp, delayDown, exitAddress);
                }
            });
        }
    }

    // internal
    private void instantiateProxyService() {
        doBindService();
    }

    public ProxyController getProxyController() {
        return this;
    }

    public void addProxyObserver(ProxyStatusCallback observer) {
        if (observer == null) {
            return;
        }
        if (observers.contains(observer)) {
            return;
        }
        observers.add(observer);
    }

    public void removeProxyObserver(ProxyStatusCallback observer) {
        if (observer == null) {
            return;
        }
        if (!observers.contains(observer)) {
            return;
        }
        observers.remove(observer);
    }

    protected void doBindService() {
        startService(new Intent(this, ProxyService.class));
        if (mConnection == null) {
            mConnection = ProxyService.createConnection(proxyManager, this);
        }
        mIsBound = bindService(new Intent(this, ProxyService.class),
                mConnection,
                Context.BIND_AUTO_CREATE);

    }

    protected void doUnbindService() {
        if (mIsBound) {
            if (mConnection != null && mConnection.getService() != null) {

            }
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    public void onTorBootstrapFailed(final String message) {
        for (final ProxyStatusCallback observer : observers) {
            Background.threadPool().submit(new Runnable() {
                @Override
                public void run() {
                    observer.onTorBootstrapFailed(message);
                }
            });
        }
    }

    public void scheduleKeepAliveAlarm() {
        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, PingTaskBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, REQUEST_RECEIVER, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 30000, pendingIntent);
        }
        else if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, 30000, pendingIntent);
        }
        else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, 30000, pendingIntent);
        }
    }

    public void scheduleAutoUpdate() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, UpdateTaskReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, REQUEST_RECEIVER, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String value = PreferenceManager.getDefaultSharedPreferences(this).getString("setting_update_interval", "1800");
        long interval = Long.parseLong(value);
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, interval, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, interval, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, interval, pendingIntent);
        }
    }
}
