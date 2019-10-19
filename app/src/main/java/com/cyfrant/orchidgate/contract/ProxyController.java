package com.cyfrant.orchidgate.contract;

public interface ProxyController {
    void startProxyService();
    boolean isProxyRunning();
    boolean isProxyStarting();
    void stopProxyService();
    Proxy getProxy();
    void keepAlive();
    boolean isActive();
}
