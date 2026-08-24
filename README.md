# 局域网传文件（Android）

纯原生 TCP + UDP 广播的局域网文件传输 App，不依赖任何服务器、不上云、不需要账号。

## 使用
1. 两台手机连同一个 Wi-Fi。
2. 发送方：选文件 → 启动发送（会广播自身地址）。
3. 接收方：点“扫描发送方” → 列表出现设备后点它 → 文件自动存到 Download 目录。

传输协议：
- 发现：`UDP 广播 255.255.255.255:54321`，内容 `LANFILE|<ip>:<tcpPort>|<name>`
- 传输：`TCP <tcpPort>` 先发两行文本（文件名、大小），再发原始文件字节

## 构建（本地）
需要 Android SDK（platform-34、build-tools 34.0.0）与 JDK 17：
```sh
./gradlew assembleRelease
```
输出：`app/build/outputs/apk/release/LanFileTransfer-vX.Y.Z.apk`（文件名带版本号，与 git tag 一致）

## 自动编译与发布（GitHub Actions）
- 推 `v*` 标签即触发：自动编译 release APK 并发布到 GitHub Release。
- 如需**签名 APK**，在仓库 Settings → Secrets 配置：
  - `KEYSTORE_BASE64`：keystore 文件的 Base64（`base64 -w0 your.jks`）
  - `KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`
  - 未配置时构建未签名 APK（仅可侧载调试）。

## 许可
见 [LICENSE](LICENSE)。允许个人下载使用，不允许二次修改、再分发或商用。
