package com.cyfrant.orchidgate.contract;

import javax.net.SocketFactory;

import okhttp3.OkHttpClient;

public interface Proxy {
    int getProxyPort();
    SocketFactory getSocketFactory();
    OkHttpClient getWebClient();
    void proxyStart(ProxyStatusCallback callback);
    void proxyStop();
}
