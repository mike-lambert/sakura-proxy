package com.cyfrant.orchidgate.contract;

import javax.net.SocketFactory;

public interface Proxy {
    int getProxyPort();
    boolean isStartPending();
    void proxyStart(ProxyStatusCallback callback);
    void proxyStop();
    void keepAlive();
}
