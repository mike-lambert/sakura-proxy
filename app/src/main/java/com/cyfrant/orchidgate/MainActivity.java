package com.cyfrant.orchidgate;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.cyfrant.orchidgate.application.ProxyApplication;
import com.cyfrant.orchidgate.contract.Proxy;
import com.cyfrant.orchidgate.contract.ProxyStatusCallback;
import com.cyfrant.orchidgate.service.ProxyManager;
import com.demo.ApplicationProperties;

import java.text.DecimalFormat;

public class MainActivity extends Activity implements ProxyStatusCallback {
    private static final DecimalFormat secondFormat = new DecimalFormat("#0.0");
    private static final String PACKAGE_TELEGRAM = "org.telegram.messenger";
    private static final String PACKAGE_TELEGRAMX = "org.thunderdog.challegram";
    private static final int COLOR_LIGHT_GREEN = Color.parseColor("#FFD4FFBF");
    private static final int COLOR_LIGHT_YELLOW = Color.parseColor("#FFFFFDBF");
    private static final int COLOR_LIGHT_RED = Color.parseColor("#FFFFC6BF");

    private Switch enableSwitch;
    private Button linkButton;
    private ProgressBar bootProgress;
    private TextView bootStatus;
    private TextView heartbeatStatus;
    private TextView coreVersion;
    private int port;

    protected ProxyApplication getProxyApplication(){
        return (ProxyApplication)getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        coreVersion = findViewById(R.id.textCoreVersion);
        coreVersion.setText("Router core: " + ApplicationProperties.getName() + "-" + ApplicationProperties.getVersion());
        enableSwitch = findViewById(R.id.switchEnableProxy);
        linkButton = findViewById(R.id.buttonTelegramLink);
        bootProgress = findViewById(R.id.progressBoot);
        bootStatus = findViewById(R.id.statusBoot);
        heartbeatStatus = findViewById(R.id.statusHeartbeat);
        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    getProxyApplication().getProxyController().startProxyService();
                } else {
                    getProxyApplication().getProxyController().stopProxyService();
                    linkButton.setVisibility(View.GONE);
                    heartbeatStatus.setText("");
                    heartbeatStatus.setBackgroundColor(Color.WHITE);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getProxyApplication().addProxyObserver(this);
        syncState(getProxyApplication().isProxyRunning());
    }

    @Override
    protected void onPause() {
        super.onPause();
        getProxyApplication().removeProxyObserver(this);
    }

    // ProxyStatusCallback

    @Override
    public void onStartup(final int percentage, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bootStatus.setVisibility(View.VISIBLE);
                bootProgress.setVisibility(View.VISIBLE);
                heartbeatStatus.setVisibility(View.VISIBLE);
                linkButton.setVisibility(View.GONE);

                bootStatus.setText(percentage + "% : " + message);
            }
        });
    }

    @Override
    public void onStarted(final int port) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.port = port;
                bootStatus.setVisibility(View.GONE);
                bootProgress.setVisibility(View.GONE);
                heartbeatStatus.setVisibility(View.VISIBLE);
                linkButton.setVisibility(View.VISIBLE);
                enableSwitch.setChecked(true);

                heartbeatStatus.setText("Listening on 127.0.0.1:" + port);
                syncLinkButton();
            }
        });
    }

    @Override
    public void onStopped(final String cause) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bootStatus.setVisibility(View.GONE);
                bootProgress.setVisibility(View.GONE);
                linkButton.setVisibility(View.GONE);
                enableSwitch.setChecked(false);

                heartbeatStatus.setText(cause);
            }
        });
    }

    @Override
    public void onHeartbeat(final double delayUp, final double delayDown, final String exitAddress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                heartbeatStatus.setText(exitAddress
                        + ": ⬆" + secondFormat.format(delayUp)
                        + " sec  ⬇" + secondFormat.format(delayDown) + " sec");
                setHeartbeatBackground(delayDown + delayUp);
            }
        });
    }

    private void syncState(boolean running){
        enableSwitch.setChecked(running);
        linkButton.setVisibility(running ? View.VISIBLE : View.GONE);
        heartbeatStatus.setVisibility(View.VISIBLE);
        bootStatus.setVisibility(View.GONE);
        bootProgress.setVisibility(View.GONE);
        port = (getProxyApplication().getProxyController().getProxy() != null ? getProxyApplication().getProxyController().getProxy().getProxyPort() : 0);
        syncLinkButton();
    }

    private void setHeartbeatBackground(double rtt) {
        if (rtt < 3){
            heartbeatStatus.setBackgroundColor(COLOR_LIGHT_GREEN);
            return;
        }

        if (rtt < 8){
            heartbeatStatus.setBackgroundColor(COLOR_LIGHT_YELLOW);
            return;
        }

        heartbeatStatus.setBackgroundColor(COLOR_LIGHT_RED);
    }

    private void syncLinkButton() {
        Drawable telegram = getAppInfo(PACKAGE_TELEGRAM);
        Drawable telegramx = getAppInfo(PACKAGE_TELEGRAMX);
        linkButton.setCompoundDrawables(getLinkIcon(), null, null, null);

        if (telegram == null && telegramx == null){
            linkButton.setText(R.string.label_button_install);
            linkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchPlayMarket(PACKAGE_TELEGRAM);
                }
            });
            return;
        }

        linkButton.setText(R.string.label_button_link);
        linkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Proxy proxy = getProxyApplication().getProxy();
                if (proxy == null){
                    return;
                }
                launchProxyScheme(ProxyManager.makeTelegramLink(proxy.getProxyPort()));
            }
        });
    }

    private Drawable getLinkIcon() {
        Drawable defaultIcon =  scaleDrawable(getDrawable(R.drawable.google_play), 48);
        defaultIcon.setBounds(0, 0, 48, 48);

        Drawable tgx = getApplicationIcon(PACKAGE_TELEGRAMX);
        Drawable tg = getApplicationIcon(PACKAGE_TELEGRAM);
        if (tgx == null && tg == null) {
            return defaultIcon;
        }

        if (tgx != null){
            Drawable scaledTgx = scaleDrawable(tgx, 48);
            scaledTgx.setBounds(0, 0, 48, 48);
            return scaledTgx;
        }

        if (tg != null) {
            Drawable scaledTg = scaleDrawable(tg, 48);
            scaledTg.setBounds(0, 0, 48, 48);
            return scaledTg;
        }
        return defaultIcon;
    }

    private Drawable getAppInfo(String packageName){
        try {
            return  getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
           return null;
        }
    }

    private Drawable getApplicationIcon(String packageName) {
        try {
            return getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public Drawable scaleDrawable(Drawable drawable, int dp) {
        Bitmap src = drawableToBitmap(drawable);
        BitmapDrawable d = new BitmapDrawable(Bitmap.createScaledBitmap(src, dp, dp, true));
        return d;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private void launchPlayMarket(String packageName) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
    }

    private void launchProxyScheme(String link) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
    }

    @Override
    public void onTorBootstrapFailed(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                heartbeatStatus.setText(message);
                heartbeatStatus.setBackgroundColor(COLOR_LIGHT_RED);
            }
        });
    }
}