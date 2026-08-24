package com.ikun656.lanfile.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.tabs.TabLayout;
import com.ikun656.lanfile.AppPrefs;
import com.ikun656.lanfile.BuildConfig;
import com.ikun656.lanfile.R;
import com.ikun656.lanfile.databinding.ActivityMainBinding;
import com.ikun656.lanfile.net.LanNet;
import com.ikun656.lanfile.net.Protocol;
import com.ikun656.lanfile.net.Receiver;
import com.ikun656.lanfile.net.Sender;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding b;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppPrefs.applyLocale(newBase));
    }
    private File pickedFile;
    private Sender sender;
    private Receiver receiver;
    private final Set<String> foundKeys = new HashSet<>();
    private final List<Protocol.Beacon> beacons = new ArrayList<>();
    private ArrayAdapter<String> deviceAdapter;

    private final ActivityResultLauncher<String[]> pickLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                pickedFile = uriToFile(uri);
                b.tvSendFile.setText("已选：" + queryDisplayName(uri));
                b.btnStartSend.setEnabled(pickedFile != null);
            });

    private final ActivityResultLauncher<String[]> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(toNightMode(AppPrefs.getThemeMode(this)));
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        requestPermissions();

        b.tabs.addTab(b.tabs.newTab().setText(R.string.mode_send));
        b.tabs.addTab(b.tabs.newTab().setText(R.string.mode_receive));
        b.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            public void onTabSelected(TabLayout.Tab tab) {
                boolean send = tab.getPosition() == 0;
                b.sendPanel.setVisibility(send ? android.view.View.VISIBLE : android.view.View.GONE);
                b.recvPanel.setVisibility(send ? android.view.View.GONE : android.view.View.VISIBLE);
            }
            public void onTabUnselected(TabLayout.Tab tab) {}
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        b.listDevices.setAdapter(deviceAdapter);

        b.btnPick.setOnClickListener(v -> pickLauncher.launch(new String[]{"*/*"}));
        b.btnStartSend.setOnClickListener(v -> startSend());
        b.btnStopSend.setOnClickListener(v -> stopSend());
        b.btnScan.setOnClickListener(v -> startDiscover());

        b.listDevices.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos < beacons.size()) receiveFrom(beacons.get(pos));
        });

        setupBottomNav();
    }

    private void setupBottomNav() {
        b.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_tool) {
                showScreen(b.contentTool);
            } else if (id == R.id.nav_settings) {
                showScreen(b.contentSettings);
            }
            return true;
        });

        b.btnAbout.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));

        SwitchCompat sw = b.switchTheme;
        sw.setChecked(AppPrefs.getThemeMode(this) == 2);
        sw.setOnCheckedChangeListener((btn, isChecked) ->
                AppPrefs.setThemeMode(this, isChecked ? 2 : 1));

        b.btnLangZh.setOnClickListener(v -> switchLang("zh"));
        b.btnLangEn.setOnClickListener(v -> switchLang("en"));
    }

    private void switchLang(String lang) {
        AppPrefs.setLang(this, lang);
        recreate();
    }

    private static int toNightMode(int mode) {
        switch (mode) {
            case 1: return AppCompatDelegate.MODE_NIGHT_NO;
            case 2: return AppCompatDelegate.MODE_NIGHT_YES;
            default: return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    private void showScreen(android.view.View screen) {
        b.contentTool.setVisibility(screen == b.contentTool ? android.view.View.VISIBLE : android.view.View.GONE);
        b.contentSettings.setVisibility(screen == b.contentSettings ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void requestPermissions() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.INTERNET);
        // 安卓 13+ 读媒体权限；低版本在 manifest 已声明
        if (Build.VERSION.SDK_INT >= 33) {
            for (String p : new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO}) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                    needed.add(p);
            }
        }
        if (!needed.isEmpty()) permLauncher.launch(needed.toArray(new String[0]));
    }

    private void startSend() {
        if (pickedFile == null) {
            Toast.makeText(this, getString(R.string.hint_pick_first), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!LanNet.isWifiConnected(this)) {
            Toast.makeText(this, getString(R.string.hint_wifi), Toast.LENGTH_LONG).show();
        }
        b.btnStartSend.setVisibility(android.view.View.GONE);
        b.btnStopSend.setVisibility(android.view.View.VISIBLE);
        b.progressSend.setVisibility(android.view.View.VISIBLE);
        b.progressSend.setProgress(0);
        b.tvSendStatus.setText(getString(R.string.status_ready) + "…");

        File src = pickedFile;
        sender = new Sender(src, Build.MODEL);
        sender.start(new Sender.Listener() {
            public void onReady(String ip, int port) {
                runOnUiThread(() -> b.tvSendStatus.setText(
                        getString(R.string.status_ready) + "，" + ip + ":" + port + "\n" + getString(R.string.recv_wait)));
            }
            public void onProgress(long sent, long total) {
                runOnUiThread(() -> {
                    if (total > 0) b.progressSend.setProgress((int) (sent * 100 / total));
                });
            }
            public void onSent() {
                runOnUiThread(() -> b.tvSendStatus.setText(getString(R.string.status_sent)));
            }
            public void onError(String msg) {
                runOnUiThread(() -> b.tvSendStatus.setText(getString(R.string.scan_error, msg)));
            }
        });
    }

    private void stopSend() {
        if (sender != null) sender.stop();
        b.btnStopSend.setVisibility(android.view.View.GONE);
        b.btnStartSend.setVisibility(android.view.View.VISIBLE);
        b.progressSend.setVisibility(android.view.View.GONE);
        b.tvSendStatus.setText(getString(R.string.status_stopped));
    }

    private void startDiscover() {
        foundKeys.clear();
        beacons.clear();
        deviceAdapter.clear();
        receiver = new Receiver();
        receiver.startDiscover(new Receiver.DiscoverListener() {
            public void onFound(Protocol.Beacon beacon) {
                String key = beacon.ip;
                if (foundKeys.add(key)) {
                    beacons.add(beacon);
                    runOnUiThread(() -> deviceAdapter.add(beacon.toString()));
                }
            }
            public void onError(String msg) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        getString(R.string.scan_error, msg), Toast.LENGTH_SHORT).show());
            }
        });
        Toast.makeText(this, getString(R.string.recv_scanning), Toast.LENGTH_SHORT).show();
    }

    private void receiveFrom(Protocol.Beacon beacon) {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists()) dir.mkdirs();
        b.progressRecv.setVisibility(android.view.View.VISIBLE);
        b.progressRecv.setProgress(0);
        b.tvRecvStatus.setText(getString(R.string.recv_connecting, beacon.name));
        if (receiver == null) receiver = new Receiver();
        receiver.receive(beacon, dir, new Receiver.ReceiveListener() {
            public void onProgress(long got, long total) {
                runOnUiThread(() -> {
                    if (total > 0) b.progressRecv.setProgress((int) (got * 100 / total));
                });
            }
            public void onDone(File file) {
                runOnUiThread(() -> b.tvRecvStatus.setText(getString(R.string.recv_done, file.getName())));
            }
            public void onError(String msg) {
                runOnUiThread(() -> b.tvRecvStatus.setText(getString(R.string.recv_fail, msg)));
            }
        });
    }

    // 把 OpenDocument 的 URI 拷贝到一个临时文件，便于 ServerSocket 读取
    private File uriToFile(Uri uri) {
        try {
            File tmp = new File(getCacheDir(), "send_tmp_" + System.currentTimeMillis());
            try (java.io.InputStream is = getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream os = new java.io.FileOutputStream(tmp)) {
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
            }
            return tmp;
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.read_fail, e.getMessage()), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private String queryDisplayName(Uri uri) {
        String result = null;
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = c.getString(idx);
            }
        } catch (Exception ignored) {}
        return result != null ? result : "file";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sender != null) sender.stop();
        if (receiver != null) receiver.stopDiscover();
    }
}
