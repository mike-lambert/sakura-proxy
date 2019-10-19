package com.cyfrant.orchidgate.application;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.cyfrant.orchidgate.contract.Proxy;
import com.cyfrant.orchidgate.contract.ProxyController;
import com.cyfrant.orchidgate.contract.ProxyStatusCallback;
import com.cyfrant.orchidgate.service.Background;
import com.cyfrant.orchidgate.service.ProxyManager;
import com.cyfrant.orchidgate.service.ProxyService;
import com.cyfrant.orchidgate.service.receivers.PingTaskReceiver;
import com.cyfrant.orchidgate.service.receivers.TorCallbackReceiver;
import com.subgraph.orchid.Tor;

import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceConstants;
import org.torproject.android.service.util.Prefs;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.torproject.android.service.TorServiceConstants.STATUS_ON;
import static org.torproject.android.service.TorServiceConstants.STATUS_STARTING;

public class ProxyApplication extends Application implements ProxyController, ProxyStatusCallback {
    public static int REQUEST_RECEIVER = 4;
    private ProxyManager proxyManager;
    private List<ProxyStatusCallback> observers;
    private boolean active;
    protected boolean mIsBound;
    protected String torStatus;
    protected ProxyService.ProxyServiceConnection mConnection;
    protected TorCallbackReceiver receiver;
    protected int torPort;

    // ProxyController
    @Override
    public void startProxyService() {
        active = true;
        boolean useTor = isTorServiceUsed();
        if (useTor) {
            startTor();
        } else {
            instantiateProxyService();
            proxyManager.proxyStart(this);
        }
    }

    private boolean isTorServiceUsed() {
        String engine = PreferenceManager.getDefaultSharedPreferences(this).getString("setting_engine", "tor");
        return engine.equalsIgnoreCase("tor");
    }

    @Override
    public boolean isProxyRunning() {
        boolean useTor = isTorServiceUsed();
        return proxyManager != null || (useTor && STATUS_ON.equals(torStatus));
    }

    @Override
    public boolean isProxyStarting() {
        boolean useTor = isTorServiceUsed();
        return (getProxy() != null && getProxy().isStartPending()) || (useTor && STATUS_STARTING.equals(torStatus));
    }

    @Override
    public void stopProxyService() {
        boolean useTor = isTorServiceUsed();
        if (useTor) {
            stopTor();
        } else {
            ProxyService svc = mConnection.getService();
            if (svc != null) {
                svc.shutdown();
                doUnbindService();
            }

            if (isActive()) {
                proxyManager.proxyStop();
            }
        }
        active = false;
    }

    @Override
    public Proxy getProxy() {
        return proxyManager;
    }

    @Override
    public void keepAlive() {
        if (isActive()) {
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
        proxyManager = new ProxyManager(this);
        receiver = new TorCallbackReceiver(this);
        Tor.setTorFaultCallback(this);
        Tor.setApplication(this);
        Prefs.setContext(this);
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(receiver,
                new IntentFilter(TorServiceConstants.ACTION_STATUS));
        lbm.registerReceiver(receiver,
                new IntentFilter(TorServiceConstants.LOCAL_ACTION_BANDWIDTH));
        lbm.registerReceiver(receiver,
                new IntentFilter(TorServiceConstants.LOCAL_ACTION_LOG));
        lbm.registerReceiver(receiver,
                new IntentFilter(TorServiceConstants.LOCAL_ACTION_PORTS));
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
        this.torPort = port;
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

    @Override
    public void onTorStatus(String status) {
        torStatus = status;
    }

    @Override
    public int getSocksPort() {
        return torPort;
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
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, PingTaskReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, REQUEST_RECEIVER, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 30000, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, 30000, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, 30000, pendingIntent);
        }
    }

    public List<Certificate> additionalCertificates() {
        List<Certificate> result = new CopyOnWriteArrayList<>();
        try {
            result.add(certificateFromAsset("DSTRootCAX3.der"));
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            result.add(certificateFromAsset("StartComCertificationAuthority.der"));
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;

    }

    private Certificate certificateFromAsset(String asset) throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream in = null;
        try {
            in = getAssets().open(asset);
            return cf.generateCertificate(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    // OrBot copypaste
    private void sendIntentToService(final String action) {
        Intent torService = new Intent(this, TorService.class);
        //torService.setClassName("org.torproject.android.service", TorService.class.getName());
        torService.setAction(action);
        startService(torService);
    }

    private void requestTorRereadConfig() {
        sendIntentToService(TorServiceConstants.CMD_SIGNAL_HUP);
    }

    public void stopVpnService() {
        sendIntentToService(TorServiceConstants.CMD_VPN_CLEAR);
    }

    /**
     * Starts tor and related daemons by sending an
     * {@link TorServiceConstants#ACTION_START} {@link Intent} to
     * {@link TorService}
     */
    private void startTor() {
        sendIntentToService(TorServiceConstants.ACTION_START);
    }

    private void stopTor() {
        Intent torService = new Intent(this, TorService.class);
        stopService(torService);
    }

    /**
     * Request tor status without starting it
     * {@link TorServiceConstants#ACTION_START} {@link Intent} to
     * {@link TorService}
     */
    private void requestTorStatus() {
        sendIntentToService(TorServiceConstants.ACTION_STATUS);
    }
}
