package com.cyfrant.orchidgate.service;

import android.util.Log;

import com.cyfrant.orchidgate.contract.Proxy;
import com.cyfrant.orchidgate.contract.ProxyStatusCallback;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProxyManager implements Proxy {
    public static final int PROXY_SOCKS5_PORT = 3128;
    private TorClient client = null;
    private ProxyStatusCallback callback = null;
    private Future pingTask = null;
    private final AtomicBoolean starting = new AtomicBoolean(false);

    private void startRouter() {
        if (client != null) {
            return;
        }
        if (starting.get()) {
            return;
        }
        starting.set(true);
        client = new TorClient();
        client.addInitializationListener(new TorInitializationListener() {
            @Override
            public void initializationProgress(final String message, final int percentage) {
                Log.d("Tor", percentage + "%: " + message);
                Background.threadPool().submit(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onStartup(percentage, message);
                        }
                    }
                });
            }

            @Override
            public void initializationCompleted() {
                Log.i("Tor", "Ready");
                starting.set(false);
                Background.threadPool().submit(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onStarted(client.getPrimarySocksPort());
                        }
                    }
                });
            }
        });
        client.disableDashboard();
        client.enableSocksListener(PROXY_SOCKS5_PORT);
        client.start();
    }

    private void stopRouter() {
        Background.threadPool().submit(new Runnable() {
            @Override
            public void run() {
                if (client != null) {
                    client.stop();
                }
                client = null;
                if (callback != null) {
                    callback.onStopped("Stopped by user");
                }
                ProxyManager.this.callback = null;
                starting.set(false);
            }
        });
    }

    @Override
    public int getProxyPort() {
        return client == null ? -1 : client.getPrimarySocksPort();
    }

    @Override
    public boolean isStartPending() {
        return starting.get();
    }

    @Override
    public void proxyStart(ProxyStatusCallback callback) {
        this.callback = callback;
        startRouter();
    }

    @Override
    public void proxyStop() {
        stopRouter();
    }

    @Override
    public void keepAlive() {
        if (client != null) {
            portGuard();
        }
    }

    public static String makeTelegramLink(int socksPort) {
        return "tg://socks?server=127.0.0.1&port=" + socksPort;
    }

    public boolean connectProbe() {
        Socket socket = null;
        try {
            socket = new Socket("localhost", client.getPrimarySocksPort());
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }
    }

    public void portGuard() {
        Background.threadPool().submit(new Runnable() {
            @Override
            public void run() {
                if (!connectProbe()) {
                    ProxyStatusCallback cb = callback;
                    proxyStop();
                    proxyStart(cb);
                }
            }
        });
    }
}
