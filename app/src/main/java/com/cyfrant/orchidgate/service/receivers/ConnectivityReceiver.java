package com.cyfrant.orchidgate.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;

import com.cyfrant.orchidgate.application.ProxyApplication;

public class ConnectivityReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final ProxyApplication app = (ProxyApplication)context.getApplicationContext();
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        Log.d("Sakura", "Connectivity changed. Stopping current circuits");
        app.stopProxyService();
        if (app.isProxyRunning() && (wifi.isAvailable() || mobile.isAvailable())) {
            // Do something
            app.startProxyService();
            Log.d("Sakura", "Reconnecting");
        }
    }
}
