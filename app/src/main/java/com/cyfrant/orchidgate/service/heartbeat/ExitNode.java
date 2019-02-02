package com.cyfrant.orchidgate.service.heartbeat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subgraph.orchid.TorClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ExitNode {
    private String address;
    private long uplinkDelay;
    private long downlinkDelay;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getUplinkDelay() {
        return uplinkDelay;
    }

    public void setUplinkDelay(long uplinkDelay) {
        this.uplinkDelay = uplinkDelay;
    }

    public long getDownlinkDelay() {
        return downlinkDelay;
    }

    public void setDownlinkDelay(long downlinkDelay) {
        this.downlinkDelay = downlinkDelay;
    }

    public static ExitNode probe(TorClient client) {
        try {
            ObjectMapper json = new ObjectMapper();
            Ping ping = new Ping();
            String pingJson = json.writeValueAsString(ping);
            ping = json.readValue(postJson("https://cyfrant.com/ping/v1/rtt", pingJson, client), Ping.class);
            long received = System.currentTimeMillis();
            String ip2 = ping.getClientAddress();
            long up = Math.abs(ping.getServerReceived() - ping.getClientSent());
            long down = Math.abs(received - ping.getServerReturned());
            ExitNode result = new ExitNode();
            result.setAddress(ip2);
            result.setUplinkDelay(up);
            result.setDownlinkDelay(down);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public static OkHttpClient createTorWebClient(TorClient client) {
        return new OkHttpClient.Builder()
                .socketFactory(client.getSocketFactory())
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public static String get(String url, TorClient client) {
        try {
            return createTorWebClient(client).newCall(
                    new Request.Builder()
                            .url(url)
                            .get()
                            .build()
            ).execute().body().string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String postJson(String url, String json, TorClient client){
        try {
            return createTorWebClient(client).newCall(
                    new Request.Builder()
                            .url(url)
                            .post(
                                    RequestBody.create(MediaType.parse("application/json"), json)
                            )
                            .build()
            ).execute().body().string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

