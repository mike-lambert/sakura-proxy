package com.subgraph.orchid.http;

import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;
import com.subgraph.orchid.logging.Logger;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class TorClientFactory {
    private static final Logger logger = Logger.getInstance(TorClientFactory.class);
    private static final int PROXY_PORT = 9150;
    private static final String PROXY_HOST = "localhost";
    private static TorClient client;
    private static boolean isStarting = false;
    private static boolean isRunning = false;

    public static TorClient getTorClient() {
        return client;
    }

    public static boolean hasOpenTorTunnel() {
        return isStarting || isRunning;
    }

    public static void openTunnel() {
        if (!isRunning) {
            isStarting = true;
            client = new TorClient();
            client.enableSocksListener(PROXY_PORT);
            client.addInitializationListener(createInitalizationListner());
            client.start();
            client.enableSocksListener();
        }
        while (!isRunning) {
            try {
                Thread.sleep(1000l);
            } catch (Exception e) {
                //swallow
            }
        }
    }

    public static Proxy getProxy() {
        return new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(PROXY_HOST, PROXY_PORT));
    }

    public static void closeTunnel() {
        while (isStarting) {
            try {
                Thread.sleep(100l);
            } catch (Exception e) {
                //swallow
            }
        }
        client.stop();
        client = null;
        isRunning = false;
        isStarting = false;
    }

    private static TorInitializationListener createInitalizationListner() {
        return new TorInitializationListener() {
            @Override
            public void initializationProgress(String message, int percent) {
                logger.info(">>> [ " + percent + "% ]: " + message);
            }

            @Override
            public void initializationCompleted() {
                logger.info("Tor is ready to go!");
                isRunning = true;
                isStarting = false;
            }
        };
    }
}