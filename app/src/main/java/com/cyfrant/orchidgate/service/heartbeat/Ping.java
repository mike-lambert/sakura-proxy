package com.cyfrant.orchidgate.service.heartbeat;

import java.util.UUID;

public class Ping {
    private UUID id;
    private Long clientSent;
    private Long serverReceived;
    private Long serverReturned;
    private String clientAddress;
    public Ping() {
        clientSent = System.currentTimeMillis();
        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getClientSent() {
        return clientSent;
    }

    public void setClientSent(Long clientSent) {
        this.clientSent = clientSent;
    }

    public Long getServerReceived() {
        return serverReceived;
    }

    public void setServerReceived(Long serverReceived) {
        this.serverReceived = serverReceived;
    }

    public Long getServerReturned() {
        return serverReturned;
    }

    public void setServerReturned(Long serverReturned) {
        this.serverReturned = serverReturned;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }
}
