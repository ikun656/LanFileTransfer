package com.ikun656.lanfile.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

/** 接收端：监听 UDP 广播发现发送方，连接其 TCP 端口并收文件。 */
public class Receiver {
    private Thread discoverThread;
    private volatile boolean running;

    public interface DiscoverListener {
        void onFound(Protocol.Beacon beacon);
        void onError(String msg);
    }

    public interface ReceiveListener {
        void onProgress(long got, long total);
        void onDone(File file);
        void onError(String msg);
    }

    /** 持续监听广播，发现发送方时回调。 */
    public void startDiscover(DiscoverListener l) {
        running = true;
        discoverThread = new Thread(() -> {
            try (java.net.DatagramSocket ds = new java.net.DatagramSocket(Protocol.DISCOVER_PORT)) {
                ds.setBroadcast(true);
                byte[] buf = new byte[512];
                while (running) {
                    java.net.DatagramPacket p = new java.net.DatagramPacket(buf, buf.length);
                    ds.receive(p);
                    String line = new String(p.getData(), 0, p.getLength(),
                            java.nio.charset.StandardCharsets.UTF_8);
                    Protocol.Beacon b = Protocol.parseBeacon(line);
                    if (b != null) l.onFound(b);
                }
            } catch (Exception e) {
                if (running) l.onError(e.getMessage());
            }
        }, "recv-discover");
        discoverThread.start();
    }

    /** 连接指定发送方并接收文件到 outDir。 */
    public void receive(Protocol.Beacon beacon, File outDir, ReceiveListener l) {
        new Thread(() -> {
            try (Socket sock = new Socket(beacon.ip, beacon.tcpPort);
                 InputStream is = sock.getInputStream()) {

                BufferedReader r = new BufferedReader(new InputStreamReader(is,
                        java.nio.charset.StandardCharsets.UTF_8));
                String name = r.readLine();
                String sizeStr = r.readLine();
                long total = sizeStr != null ? Long.parseLong(sizeStr.trim()) : -1;

                File out = new File(outDir, sanitize(name));
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[Protocol.BUF];
                    long got = 0;
                    int n;
                    while ((n = is.read(buf)) > 0) {
                        fos.write(buf, 0, n);
                        got += n;
                        l.onProgress(got, total);
                        if (total > 0 && got >= total) break;
                    }
                }
                l.onDone(out);
            } catch (Exception e) {
                l.onError(e.getMessage());
            }
        }, "recv-tcp").start();
    }

    public void stopDiscover() {
        running = false;
        if (discoverThread != null) discoverThread.interrupt();
    }

    private static String sanitize(String name) {
        if (name == null || name.isEmpty()) name = "received.bin";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
