package com.cyfrant.orchidgate.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cyfrant.orchidgate.contract.ProxyStatusCallback;

import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceConstants;

public class TorCallbackReceiver extends BroadcastReceiver {
    private final ProxyStatusCallback callback;

    public TorCallbackReceiver(ProxyStatusCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(TorServiceConstants.LOCAL_ACTION_LOG)) {
            String text = intent.getStringExtra(TorServiceConstants.EXTRA_STATUS);
            String log = intent.getStringExtra(TorServiceConstants.LOCAL_EXTRA_LOG);
            callback.onTorStatus(text);
            int percentage = extractPercentage(log);
            callback.onStartup(percentage, log);

        } else if (action.equals(TorServiceConstants.LOCAL_ACTION_BANDWIDTH)) {
            long upload = intent.getLongExtra("up", 0);
            long download = intent.getLongExtra("down", 0);
            long written = intent.getLongExtra("written", 0);
            long read = intent.getLongExtra("read", 0);

        } else if (action.equals(TorServiceConstants.ACTION_STATUS)) {
            String text = intent.getStringExtra(TorServiceConstants.EXTRA_STATUS);
            callback.onTorStatus(text);
            if (TorServiceConstants.STATUS_OFF.equals(text)) {
                callback.onStopped("");
            }

            if (TorServiceConstants.STATUS_ON.equals(text)){
                callback.onStartup(100, "");
                int port = intent.getIntExtra(TorService.EXTRA_SOCKS_PROXY_PORT, -1);
                if (port > 0) {
                    callback.onStarted(port);
                }
            }

        } else if (action.equals(TorServiceConstants.LOCAL_ACTION_PORTS)) {
            int port = intent.getIntExtra(TorService.EXTRA_SOCKS_PROXY_PORT, -1);
            callback.onStartup(100, "Tor started on port " + port);
            callback.onStarted(port);
        }
    }

    private int extractPercentage(String log) {
        String marker = "Bootstrapped ";
        int index = log.indexOf(marker);
        if (index > -1) {
            String info = log.substring(index + marker.length());
            index = info.indexOf('%');
            if (index > -1) {
                info = info.substring(0, index).trim();
                try {
                    Integer.parseInt(info);
                } catch (Exception e) {
                    return 0;
                }
            }
        }
        return 0;
    }

}
