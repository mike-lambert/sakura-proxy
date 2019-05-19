package com.cyfrant.orchidgate.service.heartbeat;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.subgraph.orchid.TorClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

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
            String responseJson = postJson("https://cyfrant.com/ping/v1/rtt", pingJson, client);
            long received = System.currentTimeMillis();
            Ping response = json.readValue(responseJson, Ping.class);
            long down = Math.abs(received - response.getServerReturned());
            long up = Math.abs(response.getServerReceived() - response.getClientSent());
            String ip  = response.getClientAddress();
            ExitNode result = new ExitNode();
            result.setAddress(ip);
            result.setUplinkDelay(up);
            result.setDownlinkDelay(down);
            return result;
        } catch (Exception e) {
            Log.w("SSL", e);
            return null;
        }
    }

    /*
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
    }*/

    public static String postJson(String url, String json, TorClient client){
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection(new Proxy(
                    Proxy.Type.SOCKS,
                    new InetSocketAddress("127.0.0.1", client.getPrimarySocksPort())
            ));
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(0);
            connection.setRequestProperty("Content-Type", "application/json");
            OutputStream post = connection.getOutputStream();
            post.write(json.getBytes());
            post.flush();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteStreams.copy(connection.getInputStream(), out);
            return new String(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {

        }
    }
}

