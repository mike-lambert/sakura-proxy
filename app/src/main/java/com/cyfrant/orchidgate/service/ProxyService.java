package com.cyfrant.orchidgate.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import com.cyfrant.orchidgate.R;
import com.cyfrant.orchidgate.application.ProxyApplication;
import com.cyfrant.orchidgate.contract.ProxyStatusCallback;

import java.text.DecimalFormat;

public class ProxyService extends Service implements ProxyStatusCallback {
    public static int REQUEST_NOTIFICATION_PROXY = 1;
    private static final DecimalFormat secondFormat = new DecimalFormat("#0.0");
    private ProxyManager proxyManager;
    private ProxyStatusCallback callback;
    private IBinder binder;

    public class LocalBinder extends Binder {
        ProxyService getService() {
            return ProxyService.this;
        }
    }

    public static class ProxyServiceConnection implements ServiceConnection {
        private ProxyManager manager;
        private ProxyService service;
        private ProxyStatusCallback callback;
        private ProxyApplication application;

        public ProxyServiceConnection(ProxyManager manager, ProxyApplication application) {
            this.manager = manager;
            if (this.callback == null && application != null){
                this.callback = application;
                this.application = application;
            }

        }

        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has
            // been established, giving us the service object we can use
            // to interact with the service.  Because we have bound to a
            // explicit service that we know is running in our own
            // process, we can cast its IBinder to a concrete class and
            // directly access it.
            this.service = ((ProxyService.LocalBinder) service).getService();
            this.service.proxyManager = manager;
            this.service.callback = callback;
            this.application.addProxyObserver(this.service);
            this.service.startProxyManager();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has
            // been unexpectedly disconnected -- that is, its process
            // crashed. Because it is running in our same process, we
            // should never see this happen.
            this.service.stopProxyManager();
            this.application.removeProxyObserver(this.service);
            this.service = null;
        }

        public ProxyService getService() {
            return service;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new LocalBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (intent != null && intent.getAction() != null
                && intent.getAction().equals("stop")) {
            // User clicked the notification. Need to stop the service.
            ((ProxyApplication)getApplication()).stopProxyService();
        } else {
            // Service starting. Create a notification.
            startForeground(REQUEST_NOTIFICATION_PROXY, createNotification("Starting proxy ..."));
        }
        return result;
    }

    private Notification createNotification(String text) {
        Notification notification = new Notification.Builder(this)

                .setSmallIcon(R.drawable.sakura)
                .setContentTitle(getString(R.string.service_name))
                .setContentText(text)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(PendingIntent.getService(this, REQUEST_NOTIFICATION_PROXY,
                        new Intent("stop", null, this, this.getClass()), 0)
                )
                .build();

        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    private void startProxyManager() {
        proxyManager.proxyStart(callback);
    }

    private void stopProxyManager() {
        proxyManager.proxyStop();
    }

    public void updateNotification(String text) {
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                .notify(REQUEST_NOTIFICATION_PROXY, createNotification(text));
    }

    public static ProxyServiceConnection createConnection(ProxyManager manager, ProxyApplication application) {
        return new ProxyServiceConnection(manager, application);
    }

    public void shutdown() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(REQUEST_NOTIFICATION_PROXY);
        stopProxyManager();
        stopSelf();
    }

    @Override
    public void onStartup(int percentage, String message) {
        updateNotification(percentage + "% : " + message);
    }

    @Override
    public void onStarted(int port) {
        updateNotification("Listening on 127.0.0.1:" + port);
    }

    @Override
    public void onStopped(String cause) {
        //updateNotification("Listening on 127.0.0.1:" + port);
    }

    @Override
    public void onHeartbeat(double delayUp, double delayDown, String exitAddress) {
        updateNotification(exitAddress
                + ": ⬆" + secondFormat.format(delayUp)
                + " sec  ⬇" + secondFormat.format(delayDown) + " sec");
    }

    @Override
    public void onTorBootstrapFailed(String message) {
        updateNotification(message);
    }
}
