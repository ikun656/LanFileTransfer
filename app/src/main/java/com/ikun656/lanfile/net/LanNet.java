package com.ikun656.lanfile.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

/** 局域网工具：取本机 Wi-Fi IP。 */
public final class LanNet {
    private LanNet() {}

    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces != null) {
                for (NetworkInterface i : Collections.list(ifaces)) {
                    if (i.isLoopback() || !i.isUp()) continue;
                    for (InetAddress a : Collections.list(i.getInetAddresses())) {
                        if (a.isSiteLocalAddress() && !a.isLoopbackAddress()) {
                            return a.getHostAddress().replaceAll("%.*$", "");
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "0.0.0.0";
    }

    public static boolean isWifiConnected(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected()
                && (ni.getType() == ConnectivityManager.TYPE_WIFI
                || ni.getType() == ConnectivityManager.TYPE_ETHERNET);
    }
}
