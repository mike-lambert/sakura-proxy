package com.cyfrant.orchidgate.application;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.cyfrant.orchidgate.contract.Proxy;
import com.cyfrant.orchidgate.contract.ProxyController;
import com.cyfrant.orchidgate.contract.ProxyStatusCallback;
import com.cyfrant.orchidgate.service.Background;
import com.cyfrant.orchidgate.service.ProxyManager;
import com.cyfrant.orchidgate.service.ProxyService;
import com.subgraph.orchid.Tor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProxyApplication extends Application implements ProxyController, ProxyStatusCallback {
    private ProxyManager proxyManager;
    private List<ProxyStatusCallback> observers;
    protected boolean mIsBound;
    protected ProxyService.ProxyServiceConnection mConnection;

    // ProxyController
    @Override
    public void startProxyService() {
        instantiateProxyService();
        proxyManager.proxyStart(this);
    }

    @Override
    public boolean isProxyRunning() {
        return proxyManager != null && proxyManager.getSocketFactory() != null;
    }

    @Override
    public void stopProxyService() {

        ProxyService svc = mConnection.getService();
        if (svc != null) {
            svc.shutdown();
        }

        if (isProxyRunning()) {
            proxyManager.proxyStop();
        }
    }

    @Override
    public Proxy getProxy() {
        return proxyManager;
    }

    // Application
    @Override
    public void onCreate() {
        super.onCreate();
        observers = new CopyOnWriteArrayList<>();
        proxyManager = new ProxyManager();
        Tor.setTorFaultCallback(this);
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
    public void onHearbeat(final long delayUp, final long delayDown, final String exitAddress) {
        for (final ProxyStatusCallback observer : observers) {
            Background.threadPool().submit(new Runnable() {
                @Override
                public void run() {
                    observer.onHearbeat(delayUp, delayDown, exitAddress);
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
}
