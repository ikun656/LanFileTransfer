package com.ikun656.lanfile.net;

import java.nio.charset.StandardCharsets;

/** 局域网传输协议常量与简单编解码。
 *  UDP 广播（发现）： LANFILE|<ip>:<tcpPort>|<deviceName>
 *  TCP 文件流： 先发两行文本（文件名、大小），再发原始字节。 */
public final class Protocol {
    public static final int DISCOVER_PORT = 54321;
    public static final String MAGIC = "LANFILE";
    public static final int TCP_PORT = 54322;
    public static final int BUF = 16 * 1024;

    private Protocol() {}

    public static String buildBeacon(String ip, int tcpPort, String name) {
        return MAGIC + "|" + ip + ":" + tcpPort + "|" + name;
    }

    public static Beacon parseBeacon(String line) {
        if (line == null) return null;
        String[] parts = line.split("\\|");
        if (parts.length < 3 || !MAGIC.equals(parts[0])) return null;
        String[] hp = parts[1].split(":");
        if (hp.length != 2) return null;
        try {
            return new Beacon(hp[0], Integer.parseInt(hp[1]), parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static byte[] toBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static final class Beacon {
        public final String ip;
        public final int tcpPort;
        public final String name;

        Beacon(String ip, int tcpPort, String name) {
            this.ip = ip;
            this.tcpPort = tcpPort;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + " (" + ip + ")";
        }
    }
}
