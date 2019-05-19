package com.subgraph.orchid.socks;

import com.subgraph.orchid.CircuitManager;
import com.subgraph.orchid.SocksPortListener;
import com.subgraph.orchid.TorConfig;
import com.subgraph.orchid.TorException;
import com.subgraph.orchid.logging.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocksPortListenerImpl implements SocksPortListener {
    private static final Logger logger = Logger.getInstance(SocksPortListenerImpl.class);
    private final List<SocksServer> servers;
    private final TorConfig config;
    private final CircuitManager circuitManager;
    private final ExecutorService executor;
    private boolean isStopped;

    public class SocksServer implements Runnable {
        private final ServerSocket socket;
        private final int port;
        private volatile boolean stopped;

        public SocksServer(int port) throws IOException {
            this.port = port;
            this.socket = new ServerSocket(port);
        }

        public void start() {
            this.stopped = false;
            executor.execute(this);
        }

        public void stop() {
            this.stopped = true;
            try {
                socket.close();
            } catch (IOException e) {
                //swallow
            }
        }

        public int getPort() {
            return port;
        }

        public boolean isRunning() {
            return !stopped;
        }

        @Override
        public void run() {
            try {
                runAcceptLoop();
            } catch (IOException e) {
                if (!stopped) {
                    logger.warn("System error accepting SOCKS socket connections: " + e.getMessage());
                }
            } finally {
                synchronized (servers) {
                    servers.remove(this);
                }
            }
        }

        private void runAcceptLoop() throws IOException {
            while (!Thread.interrupted() && !stopped) {
                final Socket s = socket.accept();
                executor.execute(newClientSocket(s));
            }
        }

        private Runnable newClientSocket(final Socket s) {
            return new SocksClientTask(config, s, circuitManager);
        }
    }

    public SocksPortListenerImpl(TorConfig config, CircuitManager circuitManager) {
        this.config = config;
        this.circuitManager = circuitManager;
        this.servers = new CopyOnWriteArrayList<SocksServer>();
        executor = Executors.newCachedThreadPool();
    }

    @Override
    public void addListeningPort(int port) {
        if (port <= 0 || port > 65535) {
            throw new TorException("Illegal listening port: " + port);
        }

        synchronized (servers) {
            if (isStopped) {
                throw new IllegalStateException("Cannot add listening port because Socks proxy has been stopped");
            }

            if (isListening(port)) {
                return;
            }

            try {
                SocksServer server = new SocksServer(port);
                server.start();
                logger.debug("Listening for SOCKS connections on port " + server.getPort());
                servers.add(server);
            } catch (IOException e) {
                throw new TorException("Failed to listen on port " + port + " : " + e.getMessage());
            }
        }

    }

    private boolean isListening(int port) {
        for (SocksServer server : servers) {
            if (port == server.getPort()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void stop() {
        synchronized (servers) {
            for (SocksServer t : servers) {
                t.stop();
            }
            executor.shutdownNow();
            isStopped = true;
        }
    }

    public List<Integer> listeningPorts() {
        List<Integer> result = new CopyOnWriteArrayList<>();
        for (SocksServer server : servers) {
            result.add(server.getPort());
        }
        return Collections.unmodifiableList(result);
    }

}
