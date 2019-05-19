package com.subgraph.orchid;

import java.util.List;

public interface SocksPortListener {
    void addListeningPort(int port);

    void stop();

    public List<Integer> listeningPorts();
}
