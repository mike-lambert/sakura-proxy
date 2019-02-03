package com.cyfrant.orchidgate.contract;

import javax.net.SocketFactory;

public interface Proxy {
    int getProxyPort();
    SocketFactory getSocketFactory();
    void proxyStart(ProxyStatusCallback callback);
    void proxyStop();
    void keepAlive();
}
