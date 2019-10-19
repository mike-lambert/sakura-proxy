package com.cyfrant.orchidgate.contract;

public interface Proxy {
    int getProxyPort();
    boolean isStartPending();
    void proxyStart(ProxyStatusCallback callback);
    void proxyStop();
    void keepAlive();
}
