package com.ikun656.lanfile.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/** 发送端：监听 TCP 连接，收到一个连接就发一份文件。同时对外广播 UDP beacon 供接收端发现。 */
public class Sender {
    private final File file;
    private final String deviceName;
    private ServerSocket server;
    private Thread beaconThread;
    private volatile boolean running;

    public interface Listener {
        void onReady(String ip, int port);
        void onProgress(long sent, long total);
        void onSent();
        void onError(String msg);
    }

    public Sender(File file, String deviceName) {
        this.file = file;
        this.deviceName = deviceName;
    }

    public void start(Listener l) {
        running = true;
        try {
            server = new ServerSocket(Protocol.TCP_PORT);
            String ip = LanNet.getLocalIp();
            l.onReady(ip, Protocol.TCP_PORT);
            startBeacon(ip);
        } catch (Exception e) {
            l.onError(e.getMessage());
            return;
        }

        new Thread(() -> {
            while (running) {
                try (Socket sock = server.accept();
                     FileInputStream fis = new FileInputStream(file);
                     OutputStream os = sock.getOutputStream()) {

                    // 头：文件名 + 大小（各占一行）
                    os.write(Protocol.toBytes(file.getName() + "\n"));
                    os.write(Protocol.toBytes(file.length() + "\n"));
                    os.flush();

                    byte[] buf = new byte[Protocol.BUF];
                    long total = file.length();
                    long sent = 0;
                    int n;
                    while ((n = fis.read(buf)) > 0) {
                        os.write(buf, 0, n);
                        sent += n;
                        l.onProgress(sent, total);
                    }
                    os.flush();
                    l.onSent();
                } catch (Exception e) {
                    if (running) l.onError(e.getMessage());
                    break;
                }
            }
        }, "sender-tcp").start();
    }

    private void startBeacon(String ip) {
        beaconThread = new Thread(() -> {
            try (java.net.DatagramSocket ds = new java.net.DatagramSocket()) {
                ds.setBroadcast(true);
                byte[] data = Protocol.toBytes(
                        Protocol.buildBeacon(ip, Protocol.TCP_PORT, deviceName));
                java.net.DatagramPacket p = new java.net.DatagramPacket(
                        data, data.length,
                        java.net.InetAddress.getByName("255.255.255.255"),
                        Protocol.DISCOVER_PORT);
                while (running) {
                    try {
                        ds.send(p);
                        Thread.sleep(1500);
                    } catch (Exception ignored) {
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }, "sender-beacon");
        beaconThread.start();
    }

    public void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        if (beaconThread != null) beaconThread.interrupt();
    }
}
