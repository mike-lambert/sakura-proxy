package com.cyfrant.orchidgate.contract;

public interface ProxyController {
    void startProxyService();
    boolean isProxyRunning();
    void stopProxyService();
    Proxy getProxy();
    void keepAlive();
    boolean isActive();
}
