package com.cyfrant.orchidgate.service;

import android.util.Log;

import com.cyfrant.orchidgate.contract.Proxy;
import com.cyfrant.orchidgate.contract.ProxyStatusCallback;
import com.cyfrant.orchidgate.service.heartbeat.ExitNode;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import okhttp3.OkHttpClient;

public class ProxyManager implements Proxy {
    public static final int PROXY_SOCKS5_PORT = 3128;
    private TorClient client = null;
    private ProxyStatusCallback callback = null;
    private Future pingTask = null;

    private void startRouter() {
        if (client != null) {
            return;
        }

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
                Background.threadPool().submit(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onStarted(client.getPrimarySocksPort());
                            scheduleHeartbeat();
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
                cancelHeartbeat();
            }
        });
    }

    private void cancelHeartbeat(){
        if (pingTask != null && !pingTask.isCancelled()){
            pingTask.cancel(true);
            pingTask = null;
        }
    }

    private void scheduleHeartbeat() {
        cancelHeartbeat();
        pingTask = Background.threadPool().submit(new Runnable() {
            private final Object lock = new Object();
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()){
                    synchronized (lock){
                        try {
                            performHeartbeat();
                            lock.wait(15000);
                        } catch (InterruptedException e) {
                            Log.w("Heartbeat", e);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void performHeartbeat(){
        Background.threadPool().submit(new Runnable() {
            @Override
            public void run() {
                ExitNode node = ExitNode.probe(client);
                if (callback != null && node != null){
                    callback.onHearbeat(
                            Math.round(node.getUplinkDelay()/1000),
                            Math.round(node.getDownlinkDelay()/1000),
                            node.getAddress()
                    );
                }
            }
        });
    }

    @Override
    public int getProxyPort() {
        return client == null ? -1 : client.getPrimarySocksPort();
    }

    @Override
    public SocketFactory getSocketFactory() {
        return client == null ? null : client.getSocketFactory();
    }

    @Override
    public OkHttpClient getWebClient() {
        return client == null ? null :
                new OkHttpClient.Builder()
                        .socketFactory(getSocketFactory())
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .build();
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

    public static String makeTelegramLink(int socksPort) {
        return "tg://socks?server=127.0.0.1&port=" + socksPort;
    }
}
