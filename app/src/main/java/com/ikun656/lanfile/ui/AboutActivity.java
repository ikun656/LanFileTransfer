package com.ikun656.lanfile.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ikun656.lanfile.AppPrefs;
import com.ikun656.lanfile.BuildConfig;
import com.ikun656.lanfile.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppPrefs.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ((TextView) findViewById(R.id.tvAboutVersion)).setText("版本 " + BuildConfig.VERSION_NAME);
        ((TextView) findViewById(R.id.tvOss)).setText(getString(R.string.oss_list));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvRepo).setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/ikun656/LanFileTransfer")));
        });
    }
}
