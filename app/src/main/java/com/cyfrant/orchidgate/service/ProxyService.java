package com.cyfrant.orchidgate.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;

import com.cyfrant.orchidgate.MainActivity;
import com.cyfrant.orchidgate.R;
import com.cyfrant.orchidgate.application.ProxyApplication;
import com.cyfrant.orchidgate.contract.ProxyStatusCallback;
import com.cyfrant.orchidgate.fragment.StatusFragment;
import com.subgraph.orchid.Tor;

import java.text.DecimalFormat;

public class ProxyService extends Service implements ProxyStatusCallback {
    public static int REQUEST_NOTIFICATION_PROXY = 1;
    public static int REQUEST_NOTIFICATION_UPDATE = 2;
    public static int REQUEST_NOTIFICATION_MESSAGE = 5;

    private static final DecimalFormat secondFormat = new DecimalFormat("#0.0");
    private ProxyManager proxyManager;
    private ProxyStatusCallback callback;
    private IBinder binder;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private NotificationChannel notificationChannel;

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
            startup();
        }
        return result;
    }

    private void startup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(REQUEST_NOTIFICATION_PROXY, createNotificationOreo("Starting proxy ..."));
        } else {
            startForeground(REQUEST_NOTIFICATION_PROXY, createNotification("Starting proxy ..."));
        }
        if (wakeLock != null){
            wakeLock.release();
            wakeLock = null;
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                "Sakura:WakeLock");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);

        if( wifiLock == null ){
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "Sakura:WiFiLock");
        }
        wifiLock.setReferenceCounted(false);

        if( !wifiLock.isHeld() ){
            wifiLock.acquire();
        }

        ((ProxyApplication)getApplication()).scheduleKeepAliveAlarm();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification createNotificationOreo(String text) {
        String NOTIFICATION_CHANNEL_ID = getApplication().getPackageName();
        if (notificationChannel == null) {
            String channelName = Tor.getApplication().getString(R.string.service_name);
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);
            notificationChannel = chan;
        }

        Notification.Builder notificationBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder
                //.setOngoing(true)
                .setLargeIcon(StatusFragment.drawableToBitmap(getDrawable(R.drawable.sakura)))
                .setSmallIcon(R.drawable.sakura)
                .setContentTitle(getString(R.string.service_name))
                .setContentText(text)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(PendingIntent.getActivity(this, REQUEST_NOTIFICATION_PROXY,
                        new Intent(this, MainActivity.class), 0)
                )
                .build();

        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    private Notification createNotification(String text) {
        Notification notification = new Notification.Builder(this)
                //.setOngoing(true)
                .setLargeIcon(StatusFragment.drawableToBitmap(getDrawable(R.drawable.sakura)))
                .setSmallIcon(R.drawable.sakura)
                .setContentTitle(getString(R.string.service_name))
                .setContentText(text)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(PendingIntent.getActivity(this, REQUEST_NOTIFICATION_PROXY,
                        new Intent(this, MainActivity.class), 0)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .notify(REQUEST_NOTIFICATION_PROXY, createNotificationOreo(text));
        } else {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .notify(REQUEST_NOTIFICATION_PROXY, createNotification(text));
        }
    }

    public static ProxyServiceConnection createConnection(ProxyManager manager, ProxyApplication application) {
        return new ProxyServiceConnection(manager, application);
    }

    public void shutdown() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(REQUEST_NOTIFICATION_PROXY);
        if (wakeLock != null){
            wakeLock.release();
            wakeLock = null;
        }
        if (wifiLock != null){
            wifiLock.release();
            wifiLock = null;
        }
        stopProxyManager();
        stopSelf();
    }

    @Override
    public void onStartup(int percentage, String message) {
        updateNotification(percentage + "% : " + message);
    }

    @Override
    public void onStarted(int port) {
        updateNotification(getString(R.string.status_listen).replace("{}", Integer.toString(port)));
    }

    @Override
    public void onStopped(String cause) {
        //updateNotification("Listening on 127.0.0.1:" + port);
    }

    @Override
    public void onHeartbeat(double delayUp, double delayDown, String exitAddress) {
        if (wakeLock != null){
            wakeLock.acquire();
        }
        updateNotification(
                exitAddress
                + ": " + getString(R.string.status_delay_up).replace("{}", secondFormat.format(delayUp))
                + " "
                +  getString(R.string.status_delay_down).replace("{}", secondFormat.format(delayDown))
        );
    }

    @Override
    public void onTorBootstrapFailed(String message) {
        updateNotification(message);
    }
}
