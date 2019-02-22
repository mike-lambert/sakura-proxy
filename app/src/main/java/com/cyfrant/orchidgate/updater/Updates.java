package com.cyfrant.orchidgate.updater;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.cyfrant.orchidgate.R;
import com.cyfrant.orchidgate.application.ProxyApplication;
import com.cyfrant.orchidgate.contract.Proxy;
import com.cyfrant.orchidgate.fragment.StatusFragment;
import com.cyfrant.orchidgate.service.Background;
import com.cyfrant.orchidgate.service.ProxyService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Updates {
    private List<Update> updates;

    public Updates() {
        this.updates = new CopyOnWriteArrayList<>();
    }

    static Update checkUpdates(Proxy proxy, Application application) throws IOException {
        Update result = null;
        String address = application.getString(R.string.updates);
        String json = UpdateValidator.get(address, proxy);
        if (json != null && !json.trim().isEmpty()) {
            Updates feed = new ObjectMapper().readValue(json, Updates.class);
            if (feed != null && feed.updates != null && !feed.updates.isEmpty()) {
                long version = 0;
                for (Update update : feed.updates) {
                    if (update.getVersion() > version) {
                        version = update.getVersion();
                        result = update;
                    }
                }
                Log.d("Update", address + ": recent version is " + version);
            }
        }
        return result;
    }

    static File getUpdateTarget(Update source, Application application) throws IOException {
        String name = source.getLocation();
        int lastSlash = name.lastIndexOf('/') + 1;
        name = name.substring(lastSlash).trim();
        Log.d("Update", source.getLocation() + " -> " + name);
        File publicRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File result = new File(publicRoot, name);
        Log.d("Update", name + " -> " + result.getAbsolutePath());
        return result;
    }

    public static void checkAndRequestInstallUpdates(final ProxyApplication application, final boolean showFailNotification) {
        Background.threadPool().submit(new Runnable() {
            @Override
            public void run() {
                final Proxy proxy = application.getProxy();
                PowerManager.WakeLock wakeLock = null;
                try {
                    wakeLock = ((PowerManager) application.getSystemService(Context.POWER_SERVICE)).newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK |
                                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                    PowerManager.ON_AFTER_RELEASE,
                            "Sakura/Update:WakeLock");
                    wakeLock.setReferenceCounted(false);
                    wakeLock.acquire();
                    long now = System.currentTimeMillis();
                    PreferenceManager.getDefaultSharedPreferences(application)
                            .edit()
                            .putLong("setting_update_last_check", now)
                            .apply();

                    Update last = checkUpdates(proxy, application);
                    if (last != null) {
                        File target = getUpdateTarget(last, application);
                        UpdateValidator validator = new UpdateValidator(application, last, target);
                        String lastError = validator.validateUpdate(proxy);
                        NotificationManager nm = ((NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE));
                        if ("".equals(lastError)) {
                            String message = application.getString(R.string.update_message)
                                    .replace("{}", Long.toString(last.getVersion()));
                            nm.notify(ProxyService.REQUEST_NOTIFICATION_UPDATE,
                                            createNotification(message, application, target)
                                    );
                        } else {
                            if (showFailNotification) {
                                Notification notification = new Notification.Builder(application)
                                        .setLargeIcon(StatusFragment.drawableToBitmap(application.getDrawable(R.drawable.sakura)))
                                        .setSmallIcon(R.drawable.sakura)
                                        .setContentTitle(application.getString(R.string.update_title))
                                        .setContentText(lastError)
                                        .setWhen(System.currentTimeMillis())
                                        .build();
                                nm.notify(ProxyService.REQUEST_NOTIFICATION_MESSAGE, notification);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.w("Updates", e);
                } finally {
                    if (wakeLock != null) {
                        wakeLock.release();
                    }
                }
            }
        });
    }

    private static Notification createNotification(String text, Application application, File apk) {
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(apk);
        String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        install.setDataAndType(uri, type);
        Notification notification = new Notification.Builder(application)
                .setLargeIcon(StatusFragment.drawableToBitmap(application.getDrawable(R.drawable.sakura)))
                .setSmallIcon(R.drawable.sakura)
                .setContentTitle(application.getString(R.string.update_title))
                .setContentText(text)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(PendingIntent.getActivity(application, ProxyService.REQUEST_NOTIFICATION_UPDATE,
                        install, 0)
                )
                .build();
        return notification;
    }

    public List<Update> getUpdates() {
        return updates;
    }

    public void setUpdates(List<Update> updates) {
        this.updates = updates;
    }
}
