package com.cyfrant.orchidgate.fragment;


import android.app.Fragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cyfrant.orchidgate.R;
import com.subgraph.orchid.ApplicationProperties;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NetworkStatusFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NetworkStatusFragment extends Fragment {

    private TextView coreVersion;
    private TextView addresses;

    public NetworkStatusFragment() {
        // Required empty public constructor
    }

    public static NetworkStatusFragment newInstance() {
        NetworkStatusFragment fragment = new NetworkStatusFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_network_status, container, false);
        initControls(view);
        return view;
    }

    private void initControls(View view) {
        coreVersion = view.findViewById(R.id.textCoreVersion);
        addresses = view.findViewById(R.id.textAddresses);
        String appVersionText = getString(R.string.status_version_app).replace("{}", getAppVersion());
        String coreVersionText = getString(R.string.status_version_core).replace("{}",
                ApplicationProperties.getName() + "-" + ApplicationProperties.getVersion()
        );
        String osVersionText = getString(R.string.status_version_os).replace("{}", Build.VERSION.RELEASE);
        String sdkLevelText = getString(R.string.status_level_sdk).replace("{}", Integer.toString(Build.VERSION.SDK_INT));
        String deviceNameText = getString(R.string.status_device).replace("{}",
                Build.MANUFACTURER + " " + Build.MODEL
        );

        String info = alignAndMergeText(appVersionText,
                coreVersionText,
                osVersionText,
                sdkLevelText,
                deviceNameText);
        coreVersion.setText(info);
        addresses.setText(join(enumeratePublicAddresses()));
    }

    private String alignAndMergeText(String... strings) {
        StringBuffer result = new StringBuffer();
        int delim = -1;
        for (String string : strings) {
            delim = Math.max(delim, string.indexOf(':'));
        }

        for (String string : strings) {
            int curr = string.indexOf(':');
            int pad = (delim - curr) + 1;
            String prefix = padRight(string.substring(0, curr), pad);
            String trail = string.substring(curr + 1);
            result
                    .append(prefix)
                    .append(':')
                    .append(' ')
                    .append(trail)
                    .append('\r')
                    .append('\n');
        }

        return result.toString();
    }

    private String padRight(String text, int pad) {
        String result = text;
        for (int i = 0; i < pad; i++) {
            result += " ";
        }
        return result;
    }

    private String join(List<String> strings) {
        StringBuffer result = new StringBuffer();
        for (String entry : strings) {
            result.append(entry).append("\r\n");
        }
        return result.toString();
    }

    private String getAppVersion() {
        return getPackageInfo(getActivity().getPackageName()) != null ?
                getPackageInfo(getActivity().getPackageName()).versionName :
                "<unknown>";
    }

    private PackageInfo getPackageInfo(String packageName) {
        try {
            return getActivity().getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private List<String> enumeratePublicAddresses() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> eni = NetworkInterface.getNetworkInterfaces();
            while (eni.hasMoreElements()) {
                NetworkInterface ni = eni.nextElement();
                Enumeration<InetAddress> nia = ni.getInetAddresses();

                while (nia.hasMoreElements()) {
                    InetAddress address = nia.nextElement();
                    if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
                        continue;
                    }
                    if (address instanceof Inet6Address) {
                        String strAddress = canonicalizeV6Address((Inet6Address) address);
                        result.add(strAddress);
                    } else {
                        result.add(address.getHostAddress().replace("/", ""));
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (result.isEmpty()){
            result.add("<" + getString(R.string.status_no_grips) + ">");
        }
        return result;
    }

    private String canonicalizeV6Address(Inet6Address address) {
        String addr = address.getHostAddress();
        if (addr.contains("%")) {
            addr = addr.substring(0, addr.indexOf('%'));
        }
        return addr;
    }

    private String machex(byte[] address) {
        StringBuilder mac = new StringBuilder();
        for (byte b : address) {
            mac.append(String.format("%02x", b)).append(":");
        }
        String ms = mac.toString();
        return ms.substring(0, ms.length() - 1);
    }
}
