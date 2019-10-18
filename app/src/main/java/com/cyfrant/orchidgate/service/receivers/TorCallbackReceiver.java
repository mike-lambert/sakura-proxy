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
            intent.getStringExtra(TorServiceConstants.LOCAL_EXTRA_LOG);
            callback.onStartup(99, text);

        } else if (action.equals(TorServiceConstants.LOCAL_ACTION_BANDWIDTH)) {
            long upload = intent.getLongExtra("up", 0);
            long download = intent.getLongExtra("down", 0);
            long written = intent.getLongExtra("written", 0);
            long read = intent.getLongExtra("read", 0);

        } else if (action.equals(TorServiceConstants.ACTION_STATUS)) {

        } else if (action.equals(TorServiceConstants.LOCAL_ACTION_PORTS)) {
            int port = intent.getIntExtra(TorService.EXTRA_SOCKS_PROXY_PORT, -1);
            callback.onStarted(port);
        }
    }

}
