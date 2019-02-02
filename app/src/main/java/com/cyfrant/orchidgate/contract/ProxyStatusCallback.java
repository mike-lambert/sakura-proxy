package com.cyfrant.orchidgate.contract;

import com.subgraph.orchid.Tor;

public interface ProxyStatusCallback extends Tor.TorFaultCallback {
    void onStartup(int percentage, String message);
    void onStarted(int port);
    void onStopped(String cause);
    void onHearbeat(long delayUp, long delayDown, String exitAddress);
}
